package com.demo.dddspringbootmybatispuls;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author zhangshaolong
 */
@MapperScan(
    basePackages = "com.demo.dddspringbootmybatispuls.module.**.infrastructure.mapper",
    annotationClass = Mapper.class)
@SpringBootApplication
public class DddSpringbootMybatispulsApplication {
  public static void main(String[] args) {
    ConfigurableApplicationContext context =
        SpringApplication.run(DddSpringbootMybatispulsApplication.class, args);

    if (context.containsBean("orderMapper")) {
      System.out.println("✅ OrderMapper Bean加载成功");
    } else {
      System.out.println("❌ OrderMapper Bean未加载");
    }
    if (context.containsBean("orderItemMapper")) {
      System.out.println("✅ orderItemMapper Bean加载成功");
    } else {
      System.out.println("❌ orderItemMapper Bean未加载");
    }
    if (context.containsBean("orderPaymentMapper")) {
      System.out.println("✅ orderPaymentMapper Bean加载成功");
    } else {
      System.out.println("❌ orderPaymentMapper Bean未加载");
    }
  }
}
