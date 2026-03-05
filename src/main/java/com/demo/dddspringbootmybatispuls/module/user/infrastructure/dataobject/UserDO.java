package com.demo.dddspringbootmybatispuls.module.user.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.demo.dddspringbootmybatispuls.module.user.application.query.dto.UserDTO;
import io.github.linpeilie.annotations.AutoMapper;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("t_user")
@AutoMapper(target = UserDTO.class)
public class UserDO {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String name;
  private Integer age;
  private LocalDateTime createTime;
}
