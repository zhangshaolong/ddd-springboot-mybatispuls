package com.demo.dddspringbootmybatispuls.module.user.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@TableName("t_user")
public class UserDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer age;
    private LocalDateTime createTime;
}
