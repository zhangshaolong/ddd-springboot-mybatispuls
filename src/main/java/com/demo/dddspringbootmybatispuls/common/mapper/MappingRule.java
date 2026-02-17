package com.demo.dddspringbootmybatispuls.common.mapper;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 动态映射规则：支持字段映射、忽略、自定义转换 不可变设计，线程安全
 *
 * @author zhangshaolong
 */
public record MappingRule<S, T>(
    String sourceField,
    String targetField,
    boolean ignore,
    BiFunction<Object, S, Object> converter,
    BiConsumer<S, T> globalHandler) {
  public MappingRule(
      String sourceField,
      String targetField,
      boolean ignore,
      BiFunction<Object, S, Object> converter,
      BiConsumer<S, T> globalHandler) {
    this.sourceField = sourceField;
    this.targetField = targetField;
    this.ignore = ignore;
    this.converter = converter;
    this.globalHandler = globalHandler;

    // 合法性校验
    validate();
  }

  /** 基础字段映射：源字段 → 目标字段 */
  public static <S, T> MappingRule<S, T> of(String sourceField, String targetField) {
    return new MappingRule<>(sourceField, targetField, false, null, null);
  }

  /** 忽略指定字段 */
  public static <S, T> MappingRule<S, T> ignore(String fieldName) {
    return new MappingRule<>(fieldName, fieldName, true, null, null);
  }

  /**
   * 自定义转换规则
   *
   * @param sourceField 源字段名
   * @param targetField 目标字段名
   * @param converter 处理函数，入参1=源字段值，入参2=源对象实例，返回值=目标字段值
   */
  public static <S, T> MappingRule<S, T> custom(
      String sourceField, String targetField, BiFunction<Object, S, Object> converter) {
    if (converter == null) {
      throw new IllegalArgumentException("自定义转换器不能为空");
    }
    return new MappingRule<>(sourceField, targetField, false, converter, null);
  }

  /**
   * 单参数处理器
   *
   * @param sourceField 源字段名
   * @param targetField 目标字段名
   * @param converter 处理函数，仅入参=源字段值，返回值=目标字段值
   */
  public static <S, T> MappingRule<S, T> custom(
      String sourceField, String targetField, Function<Object, Object> converter) {
    // 封装为BiFunction：忽略instance参数，仅用fieldValue
    BiFunction<Object, S, Object> biConverter =
        (fieldValue, instance) -> converter.apply(fieldValue);
    return custom(sourceField, targetField, biConverter);
  }

  /**
   * 全局自定义处理器：支持任意类型的定制化处理，脱离字段绑定
   *
   * @param globalHandler 全局处理函数，入参1=源对象实例，入参2=目标对象实例，直接修改目标对象
   * @param <S> 源对象类型
   * @param <T> 目标对象类型
   * @return 全局处理规则
   */
  public static <S, T> MappingRule<S, T> global(BiConsumer<S, T> globalHandler) {
    if (globalHandler == null) {
      throw new IllegalArgumentException("全局处理器不能为空");
    }
    return new MappingRule<>(null, null, false, null, globalHandler);
  }

  private void validate() {
    if (globalHandler != null) {
      if (sourceField != null || targetField != null || ignore || converter != null) {
        throw new IllegalArgumentException("全局规则不能同时设置字段名、忽略标记或字段转换器");
      }
      return;
    }
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

  public boolean isGlobalRule() {
    return globalHandler != null;
  }
}
