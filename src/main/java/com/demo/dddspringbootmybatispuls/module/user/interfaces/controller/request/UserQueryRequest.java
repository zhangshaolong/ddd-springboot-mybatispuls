package com.demo.dddspringbootmybatispuls.module.user.interfaces.controller.request;

import com.demo.dddspringbootmybatispuls.common.request.PageRequest;
import lombok.Data;

@Data
public class UserQueryRequest extends PageRequest {
    private String username; // 模糊查询用户名
    private String phone;    // 精准查询手机号
}