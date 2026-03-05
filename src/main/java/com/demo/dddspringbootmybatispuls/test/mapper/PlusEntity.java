package com.demo.dddspringbootmybatispuls.test.mapper;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@AutoMapper(target = PlusDTO.class) // 指定转换目标
public class PlusEntity {
  private String username;
  private String password;
  private String mobile;
}
