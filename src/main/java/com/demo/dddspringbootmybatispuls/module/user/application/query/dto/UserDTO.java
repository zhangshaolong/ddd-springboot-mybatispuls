package com.demo.dddspringbootmybatispuls.module.user.application.query.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserDTO {
  private Long id;
  private String name;
  //    private Integer age;
  private LocalDateTime createTime;
}
