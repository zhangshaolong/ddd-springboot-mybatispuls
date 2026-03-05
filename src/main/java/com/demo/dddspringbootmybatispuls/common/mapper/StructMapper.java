package com.demo.dddspringbootmybatispuls.common.mapper;

import io.github.linpeilie.Converter;
import io.github.linpeilie.CycleAvoidingMappingContext;
import java.util.List;
import java.util.Map;

public class StructMapper {
  public static Converter converter = new Converter();

  private StructMapper() {}

  public static <S, T> T to(S source, Class<T> targetType) {
    return converter.convert(source, targetType);
  }

  public static <S, T> T to(S source, T target) {
    return converter.convert(source, target);
  }

  public static <S, T> List<T> to(List<S> source, Class<T> targetType) {
    return converter.convert(source, targetType);
  }

  public static <S, T> T to(S source, Class<T> target, CycleAvoidingMappingContext context) {
    return converter.convert(source, target, context);
  }

  public static <S, T> List<T> to(
      List<S> source, Class<T> targetType, CycleAvoidingMappingContext context) {
    return converter.convert(source, targetType, context);
  }

  public static <T> T to(Map<String, Object> map, Class<T> target) {
    return converter.convert(map, target);
  }
}
