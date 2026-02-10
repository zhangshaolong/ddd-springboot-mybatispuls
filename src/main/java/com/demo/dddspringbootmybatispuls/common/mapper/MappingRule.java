package com.demo.dddspringbootmybatispuls.common.mapper;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 动态映射规则：支持字段映射、忽略、自定义转换
 * 不可变设计，线程安全
 *
 * @author zhangshaolong
 */
public final class MappingRule<S, T> {
    private final String sourceField;
    private final String targetField;
    private final boolean ignore;
    private final BiFunction<Object, S, Object> converter;

    private MappingRule(String sourceField, String targetField, boolean ignore, BiFunction<Object, S, Object> converter) {
        this.sourceField = sourceField;
        this.targetField = targetField;
        this.ignore = ignore;
        this.converter = converter;

        // 合法性校验
        validate();
    }

    /**
     * 基础字段映射：源字段 → 目标字段
     */
    public static <S, T> MappingRule<S, T> of(String sourceField, String targetField) {
        return new MappingRule<>(sourceField, targetField, false, null);
    }

    /**
     * 忽略指定字段
     */
    public static <S, T> MappingRule<S, T> ignore(String fieldName) {
        return new MappingRule<>(fieldName, fieldName, true, null);
    }

    /**
     * 自定义转换规则
     *
     * @param sourceField 源字段名
     * @param targetField 目标字段名
     * @param converter   处理函数，入参1=源字段值，入参2=源对象实例，返回值=目标字段值
     */
    public static <S, T> MappingRule<S, T> custom(String sourceField, String targetField, BiFunction<Object, S, Object> converter) {
        if (converter == null) {
            throw new IllegalArgumentException("自定义转换器不能为空");
        }
        return new MappingRule<>(sourceField, targetField, false, converter);
    }

    /**
     * 单参数处理器
     *
     * @param sourceField 源字段名
     * @param targetField 目标字段名
     * @param converter   处理函数，仅入参=源字段值，返回值=目标字段值
     */
    public static <S, T> MappingRule<S, T> custom(String sourceField, String targetField, Function<Object, Object> converter) {
        // 封装为BiFunction：忽略instance参数，仅用fieldValue
        BiFunction<Object, S, Object> biConverter = (fieldValue, instance) -> converter.apply(fieldValue);
        return custom(sourceField, targetField, biConverter);
    }

    private void validate() {
        if (targetField == null || targetField.isBlank()) {
            throw new IllegalArgumentException("目标字段名不能为空");
        }
        if (ignore) {
            if (sourceField == null || sourceField.isBlank()) {
                throw new IllegalArgumentException("忽略规则的源字段名不能为空");
            }
            if (converter != null) {
                throw new IllegalArgumentException("忽略规则不能设置转换器");
            }
        } else if (converter != null && sourceField == null) {
            throw new IllegalArgumentException("自定义转换规则的源字段名不能为空");
        }
    }

    public String getSourceField() {
        return sourceField;
    }

    public String getTargetField() {
        return targetField;
    }

    public boolean isIgnore() {
        return ignore;
    }

    public BiFunction<Object, S, Object> getConverter() {
        return converter;
    }
}