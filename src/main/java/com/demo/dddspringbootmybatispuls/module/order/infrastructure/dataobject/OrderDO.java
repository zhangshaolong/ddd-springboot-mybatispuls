package com.demo.dddspringbootmybatispuls.module.order.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@TableName("t_order")
@NoArgsConstructor
@AllArgsConstructor
public class OrderDO {
  private Long id;
  private Long version;
  private String orderNo;
  private String status;
}
