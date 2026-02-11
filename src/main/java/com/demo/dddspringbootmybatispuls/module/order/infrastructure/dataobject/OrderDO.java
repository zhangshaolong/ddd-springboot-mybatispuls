package com.demo.dddspringbootmybatispuls.module.order.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.demo.dddspringbootmybatispuls.common.aggregate.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_order")
public class OrderDO extends BaseDO {
  private Long version;
  private String orderNo;
  private String status;
}
