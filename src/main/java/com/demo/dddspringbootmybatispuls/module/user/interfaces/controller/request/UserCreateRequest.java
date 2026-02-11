package com.demo.dddspringbootmybatispuls.module.user.interfaces.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 接口层：创建用户的请求参数（仅接收前端数据，无业务逻辑） */
@Data
public class UserCreateRequest {
  @NotBlank(message = "用户名不能为空")
  private String username;

  @NotBlank(message = "密码不能为空")
  @Size(min = 6, message = "密码长度不能小于6位")
  private String password;

  @NotBlank(message = "手机号不能为空")
  private String phone;
}
