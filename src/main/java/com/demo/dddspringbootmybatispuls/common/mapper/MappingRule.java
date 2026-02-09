package com.demo.dddspringbootmybatispuls.common.mapper;
import java.util.function.Function;

/**
 * 动态映射规则：支持字段映射、忽略、自定义转换
 * 不可变设计，线程安全
 * @author zhangshaolong
 */
public final class MappingRule<S, T> {
    private final String sourceField;
    private final String targetField;
    private final boolean ignore;
    private final Function<Object, Object> customConverter;

    // 私有构造器，仅通过静态方法创建
    private MappingRule(String sourceField, String targetField, boolean ignore, Function<Object, Object> customConverter) {
        this.sourceField = sourceField;
        this.targetField = targetField;
        this.ignore = ignore;
        this.customConverter = customConverter;
    }

    /**
     * 基础字段映射：源字段 → 目标字段
     */
    public static <S, T> MappingRule<S, T> of(String sourceField, String targetField) {
        if (sourceField == null || targetField == null) {
            throw new IllegalArgumentException("源字段/目标字段不能为空");
        }
        return new MappingRule<>(sourceField, targetField, false, null);
    }

    /**
     * 忽略指定字段
     */
    public static <S, T> MappingRule<S, T> ignore(String fieldName) {
        if (fieldName == null) {
            throw new IllegalArgumentException("忽略的字段名不能为空");
        }
        return new MappingRule<>(fieldName, fieldName, true, null);
    }

    /**
     * 自定义转换逻辑的字段映射
     */
    public static <S, T> MappingRule<S, T> custom(String sourceField, String targetField, Function<Object, Object> converter) {
        if (sourceField == null || targetField == null) {
            throw new IllegalArgumentException("源字段/目标字段不能为空");
        }
        if (converter == null) {
            throw new IllegalArgumentException("自定义转换器不能为空");
        }
        return new MappingRule<>(sourceField, targetField, false, converter);
    }

    // Getter（无Setter，保证不可变）
    public String getSourceField() {
        return sourceField;
    }

    public String getTargetField() {
        return targetField;
    }

    public boolean isIgnore() {
        return ignore;
    }

    public Function<Object, Object> getCustomConverter() {
        return customConverter;
    }
}