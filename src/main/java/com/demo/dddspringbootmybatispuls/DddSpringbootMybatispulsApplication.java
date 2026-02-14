package com.demo.dddspringbootmybatispuls;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author zhangshaolong
 */
@MapperScan(
    basePackages = "com.demo.dddspringbootmybatispuls.module.**.infrastructure.mapper",
    annotationClass = Mapper.class)
@SpringBootApplication
public class DddSpringbootMybatispulsApplication {
  public static void main(String[] args) {
    SpringApplication.run(DddSpringbootMybatispulsApplication.class, args);
  }
}
