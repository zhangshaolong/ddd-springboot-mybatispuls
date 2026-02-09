package com.demo.dddspringbootmybatispuls.common.convert;


import org.springframework.cglib.beans.BeanCopier;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 高性能动态映射工具类
 * 核心API：ConvertMapper.to(source, targetClass, rules)
 * 性能优化：ASM字节码（BeanCopier）+ 双层缓存 + 空值安全
 */
public final class ConvertMapper {
    // ========== 性能缓存 ==========
    /**
     * BeanCopier缓存：key=源类型名+目标类型名，value=BeanCopier实例（ASM字节码实现，接近硬编码性能）
     */
    private static final Map<String, BeanCopier> BEAN_COPIER_CACHE = new ConcurrentHashMap<>();

    /**
     * Field缓存：key=类名+字段名，value=Field实例（避免重复反射）
     */
    private static final Map<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();

    // ========== 空值常量 ==========
    /**
     * 空规则常量：复用，减少对象创建
     */
    private static final List<MappingRule<?, ?>> EMPTY_RULES = List.of();

    // 私有构造器：禁止实例化
    private ConvertMapper() {
    }

    // ========== 核心API：单个对象转换 ==========
    /**
     * 动态映射单个对象
     *
     * @param source       源对象（非null）
     * @param targetClass  目标类（非null，需有无参构造器）
     * @param mappingRules 动态映射规则（可为null，null则仅基础映射）
     * @param <S>          源类型
     * @param <T>          目标类型
     * @return 转换后的目标对象
     */
    @SuppressWarnings("unchecked")
    public static <S, T> T to(S source, Class<T> targetClass, List<MappingRule<S, T>> mappingRules) {
        // 1. 空值校验
        if (source == null) {
            throw new IllegalArgumentException("源对象不能为空");
        }
        if (targetClass == null) {
            throw new IllegalArgumentException("目标类不能为空");
        }

        // 2. 空规则兜底（泛型友好，无强制转换）
        List<MappingRule<S, T>> rules = mappingRules == null ? (List<MappingRule<S, T>>) (List<?>)EMPTY_RULES : mappingRules;

        try {
            // 3. 创建目标对象实例（要求无参构造器）
            T target = targetClass.getDeclaredConstructor().newInstance();

            // 4. 第一步：基础映射（ASM字节码，高性能）
            copyBaseFields(source, target, rules);

            // 5. 第二步：处理动态规则（仅特殊字段反射）
            applyMappingRules(source, target, rules);

            return target;
        } catch (Exception e) {
            throw new RuntimeException("对象转换失败：源类型=" + source.getClass().getName() + "，目标类型=" + targetClass.getName(), e);
        }
    }

    /**
     * 简化重载：无动态规则（仅基础字段映射）
     */
    public static <S, T> T to(S source, Class<T> targetClass) {
        return to(source, targetClass, null);
    }

    // ========== 扩展API：集合转换 ==========
    /**
     * 动态映射集合对象
     *
     * @param sourceList   源集合（可为null/空）
     * @param targetClass  目标类
     * @param mappingRules 动态映射规则
     * @param <S>          源类型
     * @param <T>          目标类型
     * @return 转换后的目标集合（非null）
     */
    public static <S, T> List<T> toList(List<S> sourceList, Class<T> targetClass, List<MappingRule<S, T>> mappingRules) {
        if (sourceList == null || sourceList.isEmpty()) {
            return List.of();
        }
        return sourceList.stream()
                .map(source -> to(source, targetClass, mappingRules))
                .toList();
    }

    /**
     * 简化重载：集合转换（无动态规则）
     */
    public static <S, T> List<T> toList(List<S> sourceList, Class<T> targetClass) {
        return toList(sourceList, targetClass, null);
    }

    // ========== 内部核心：基础字段映射（ASM字节码） ==========
    @SuppressWarnings("unchecked")
    private static <S, T> void copyBaseFields(S source, T target, List<MappingRule<S, T>> rules) {
        Class<S> sourceClass = (Class<S>) source.getClass();
        Class<T> targetClass = (Class<T>) target.getClass();

        // 1. 获取缓存的BeanCopier
        String copierKey = buildCopierKey(sourceClass, targetClass);
        BeanCopier copier = BEAN_COPIER_CACHE.computeIfAbsent(copierKey, k -> BeanCopier.create(sourceClass, targetClass, false));

        // 2. ASM字节码拷贝基础字段（字段名一致，且未被忽略）
        // 注：BeanCopier会拷贝所有字段名一致的字段，后续通过规则覆盖/忽略
        copier.copy(source, target, null);

        // 3. 移除被忽略的字段（置null）
        rules.stream()
                .filter(MappingRule::isIgnore)
                .forEach(rule -> {
                    try {
                        Field targetField = getCachedField(targetClass, rule.getTargetField());
                        targetField.setAccessible(true);
                        targetField.set(target, null);
                    } catch (Exception e) {
                        // 忽略字段不存在时，不抛异常
                    }
                });
    }

    // ========== 内部核心：应用动态映射规则 ==========
    @SuppressWarnings("unchecked")
    private static <S, T> void applyMappingRules(S source, T target, List<MappingRule<S, T>> rules) {
        if (rules.isEmpty()) {
            return;
        }

        Class<S> sourceClass = (Class<S>) source.getClass();
        Class<T> targetClass = (Class<T>) target.getClass();

        for (MappingRule<S, T> rule : rules) {
            // 跳过忽略规则（已在基础映射处理）
            if (rule.isIgnore()) {
                continue;
            }

            try {
                // 1. 获取源字段值
                Field sourceField = getCachedField(sourceClass, rule.getSourceField());
                sourceField.setAccessible(true);
                Object sourceValue = sourceField.get(source);

                // 2. 应用自定义转换
                if (rule.getCustomConverter() != null) {
                    sourceValue = rule.getCustomConverter().apply(sourceValue);
                }

                // 3. 设置目标字段值（覆盖基础映射）
                Field targetField = getCachedField(targetClass, rule.getTargetField());
                targetField.setAccessible(true);
                targetField.set(target, sourceValue);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException("映射规则错误：字段不存在 → 源字段=" + rule.getSourceField() + "，目标字段=" + rule.getTargetField(), e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("映射规则错误：字段访问失败（可能是final字段） → 源字段=" + rule.getSourceField() + "，目标字段=" + rule.getTargetField(), e);
            }
        }
    }

    // ========== 工具方法：构建BeanCopier缓存Key ==========
    private static <S, T> String buildCopierKey(Class<S> sourceClass, Class<T> targetClass) {
        return sourceClass.getName() + "->" + targetClass.getName();
    }

    // ========== 工具方法：获取缓存的Field（性能优化） ==========
    private static Field getCachedField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        String fieldKey = clazz.getName() + "#" + fieldName;
        if (FIELD_CACHE.containsKey(fieldKey)) {
            return FIELD_CACHE.get(fieldKey);
        }

        // 递归查找父类字段（支持继承）
        Field field = findFieldRecursively(clazz, fieldName);
        FIELD_CACHE.put(fieldKey, field);
        return field;
    }

    // ========== 工具方法：递归查找字段（支持父类） ==========
    private static Field findFieldRecursively(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null && superClass != Object.class) {
                return findFieldRecursively(superClass, fieldName);
            }
            throw new NoSuchFieldException("字段不存在：" + clazz.getName() + "." + fieldName);
        }
    }

    // ========== 性能优化建议：JVM参数 ==========
    /**
     * 可选JVM参数：关闭反射安全检查，进一步提升性能（部署时添加）
     * -Dsun.reflect.noCheckMemberAccess=true
     */
    public static void printPerformanceTips() {
        System.out.println("【ConvertMapper性能优化】建议添加JVM参数：-Dsun.reflect.noCheckMemberAccess=true");
    }
}