package com.demo.dddspringbootmybatispuls.common.aggregate;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.Map;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

@Component
abstract class GenericTypeUtils implements ApplicationContextAware {

  @SuppressWarnings("unchecked")
  public static <T> BaseMapper<T> getMapperByDoType(
      ApplicationContext applicationContext, Class<T> doClass) {
    if (applicationContext == null) {
      throw new RuntimeException("ApplicationContext不能为空");
    }

    // 1. 获取所有BaseMapper类型的Bean
    Map<String, BaseMapper> mapperMap = applicationContext.getBeansOfType(BaseMapper.class);

    // 2. 遍历匹配泛型
    for (BaseMapper<?> mapper : mapperMap.values()) {
      Class<?> mapperClass = mapper.getClass();
      Class<?> genericType = getMapperGenericType(mapperClass, BaseMapper.class);
      if (genericType != null && genericType.equals(doClass)) {
        return (BaseMapper<T>) mapper;
      }
    }

    // 3. 匹配失败抛异常
    throw new RuntimeException(
        String.format(
            "未找到DO类型[%s]对应的BaseMapper Bean，请检查：\n"
                + "1. Mapper接口是否继承BaseMapper<%s>；\n"
                + "2. Mapper是否被Spring扫描（@MapperScan）；\n"
                + "3. DO类型是否与Mapper泛型一致",
            doClass.getName(), doClass.getSimpleName()));
  }

  private static Class<?> getMapperGenericType(Class<?> mapperClass, Class<?> baseInterface) {
    // 获取动态代理类的原始接口
    Class<?>[] interfaces = ClassUtils.getAllInterfacesForClass(mapperClass);
    for (Class<?> iface : interfaces) {
      if (baseInterface.isAssignableFrom(iface) && iface.getGenericInterfaces().length > 0) {
        // 解析BaseMapper<T>中的T类型
        return (Class<?>)
            ((java.lang.reflect.ParameterizedType) iface.getGenericInterfaces()[0])
                .getActualTypeArguments()[0];
      }
    }
    return null;
  }
}
