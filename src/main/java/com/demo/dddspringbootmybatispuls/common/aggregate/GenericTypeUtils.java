package com.demo.dddspringbootmybatispuls.common.aggregate;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Map;
import org.springframework.context.ApplicationContext;

/** 泛型类型解析工具（终极兼容版：解决Spring/Java版本兼容问题） */
public class GenericTypeUtils {

  /**
   * 解析泛型接口的实际类型参数（纯JDK反射，无Spring依赖）
   *
   * @param clazz 实现类（如OrderMapper.class）
   * @param genericInterface 泛型接口（如BaseMapper.class）
   * @return 泛型接口的实际类型参数（如OrderDO.class）
   */
  public static Class<?> getGenericInterfaceType(Class<?> clazz, Class<?> genericInterface) {
    // 处理代理类：获取原始类
    Class<?> targetClass = getTargetClass(clazz);

    // 遍历所有接口，找到目标泛型接口
    for (Type type : targetClass.getGenericInterfaces()) {
      if (type instanceof ParameterizedType parameterizedType) {
        Type rawType = parameterizedType.getRawType();
        if (rawType instanceof Class<?> && genericInterface.isAssignableFrom((Class<?>) rawType)) {
          Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
          if (actualTypeArguments.length > 0) {
            Type actualType = actualTypeArguments[0];
            return resolveActualType(actualType, targetClass);
          }
        }
      }
    }

    // 递归查找父类的接口
    Class<?> superClass = targetClass.getSuperclass();
    if (superClass != null && superClass != Object.class) {
      return getGenericInterfaceType(superClass, genericInterface);
    }

    return null;
  }

  /** 解析实际类型（处理TypeVariable/ParameterizedType） */
  private static Class<?> resolveActualType(Type type, Class<?> clazz) {
    if (type instanceof Class<?>) {
      return (Class<?>) type;
    } else if (type instanceof ParameterizedType parameterizedType) {
      Type rawType = parameterizedType.getRawType();
      return rawType instanceof Class<?> ? (Class<?>) rawType : null;
    } else if (type instanceof java.lang.reflect.TypeVariable<?>) {
      java.lang.reflect.TypeVariable<?> typeVariable = (java.lang.reflect.TypeVariable<?>) type;
      Type[] bounds = typeVariable.getBounds();
      if (bounds.length > 0) {
        return resolveActualType(bounds[0], clazz);
      }
    }
    return null;
  }

  /** 获取原始类（兼容所有Java版本的代理判断） */
  private static Class<?> getTargetClass(Class<?> clazz) {
    // 处理CGLIB代理
    if (clazz.getName().contains("$$EnhancerBySpringCGLIB$$")) {
      return clazz.getSuperclass();
    }

    // 处理JDK动态代理（兼容所有Java版本）
    if (Proxy.isProxyClass(clazz)) {
      Class<?>[] interfaces = clazz.getInterfaces();
      return interfaces.length > 0 ? interfaces[0] : clazz;
    }

    return clazz;
  }

  /**
   * 从Spring容器中根据DO类型查找对应的BaseMapper
   *
   * @param applicationContext Spring上下文
   * @param doClass DO类型
   * @param <DO> DO泛型
   * @return 对应的BaseMapper<DO>
   */
  @SuppressWarnings("unchecked")
  public static <DO> org.apache.ibatis.annotations.Mapper getMapperByDoType(
      ApplicationContext applicationContext, Class<DO> doClass) {

    Map<String, org.apache.ibatis.annotations.Mapper> mapperBeans =
        applicationContext.getBeansOfType(org.apache.ibatis.annotations.Mapper.class);

    for (org.apache.ibatis.annotations.Mapper mapper : mapperBeans.values()) {
      Class<?> mapperClass = mapper.getClass();
      Class<?> genericDoType =
          getGenericInterfaceType(
              mapperClass, com.baomidou.mybatisplus.core.mapper.BaseMapper.class);

      if (genericDoType != null && genericDoType.equals(doClass)) {
        return mapper;
      }
    }

    throw new RuntimeException("未找到DO类型[" + doClass.getName() + "]对应的BaseMapper Bean");
  }
}
