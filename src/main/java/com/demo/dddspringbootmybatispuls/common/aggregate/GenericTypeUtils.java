package com.demo.dddspringbootmybatispuls.common.aggregate;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.Map;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

@Component
public class GenericTypeUtils implements ApplicationContextAware {
  // 保留静态上下文（备用）
  private static ApplicationContext defaultContext;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    GenericTypeUtils.defaultContext = applicationContext;
  }

  // ========== 核心：新增双参数方法（匹配你的调用语法） ==========
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

  // ========== 单参数重载方法（备用） ==========
  public static <T> BaseMapper<T> getMapperByDoType(Class<T> doClass) {
    if (defaultContext == null) {
      throw new RuntimeException("ApplicationContext未初始化，请确保工具类被Spring管理");
    }
    return getMapperByDoType(defaultContext, doClass);
  }

  // ========== 辅助方法：解析泛型 ==========
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
