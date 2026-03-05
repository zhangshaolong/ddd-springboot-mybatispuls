//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package io.github.linpeilie.annotations;

import java.lang.annotation.*;
import org.mapstruct.*;
import org.mapstruct.control.MappingControl;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(AutoMappers.class)
public @interface AutoMapper {
  Class<?> target();

  Class<?>[] uses() default {};

  Class<?>[] useEnums() default {};

  Class<?>[] imports() default {};

  boolean convertGenerate() default true;

  String mapperName() default "";

  String mapperNameSuffix() default "";

  boolean reverseConvertGenerate() default true;

  boolean cycleAvoiding() default false;

  ReportingPolicy unmappedSourcePolicy() default ReportingPolicy.IGNORE;

  ReportingPolicy unmappedTargetPolicy() default ReportingPolicy.WARN;

  ReportingPolicy typeConversionPolicy() default ReportingPolicy.IGNORE;

  CollectionMappingStrategy collectionMappingStrategy() default
      CollectionMappingStrategy.ACCESSOR_ONLY;

  NullValueMappingStrategy nullValueMappingStrategy() default NullValueMappingStrategy.RETURN_NULL;

  NullValueMappingStrategy nullValueIterableMappingStrategy() default
      NullValueMappingStrategy.RETURN_NULL;

  NullValuePropertyMappingStrategy nullValuePropertyMappingStrategy() default
      NullValuePropertyMappingStrategy.SET_TO_NULL;

  NullValueCheckStrategy nullValueCheckStrategy() default
      NullValueCheckStrategy.ON_IMPLICIT_CONVERSION;

  Class<? extends Annotation> mappingControl() default MappingControl.class;
}
