package com.demo.dddspringbootmybatispuls.module.user.application.query.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDTO {
    private Long id;
    private String name;
    //    private Integer age;
    private LocalDateTime createTime;
}
