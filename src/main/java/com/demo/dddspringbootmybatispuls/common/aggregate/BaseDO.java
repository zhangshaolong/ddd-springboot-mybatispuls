package com.demo.dddspringbootmybatispuls.common.aggregate;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class BaseDO implements Serializable {
  private Long id;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
  private String createBy;
  private String updateBy;
}
