package com.demo.dddspringbootmybatispuls.module.user.interfaces.controller;

import com.demo.dddspringbootmybatispuls.common.response.Result;
import com.demo.dddspringbootmybatispuls.module.user.application.query.UserQueryService;
import com.demo.dddspringbootmybatispuls.module.user.application.query.dto.UserDTO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {


    @Resource
    private UserQueryService userQueryService;

    @GetMapping("/list")
    public Result<List<UserDTO>> getAllUsers() {
        List<UserDTO> userDTOS = userQueryService.getUsers();
        return Result.success(userDTOS);
    }

    @GetMapping("/{id}")
    public Result<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO userDTO = userQueryService.getUserById(id);
        return Result.success(userDTO);
    }
}
