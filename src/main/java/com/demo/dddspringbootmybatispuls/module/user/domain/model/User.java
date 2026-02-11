package com.demo.dddspringbootmybatispuls.module.user.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class User {
  private Long id;
  private String name;
  private Integer age;
  private String email;
}
