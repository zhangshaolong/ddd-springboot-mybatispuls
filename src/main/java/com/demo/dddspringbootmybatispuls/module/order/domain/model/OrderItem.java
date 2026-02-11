package com.demo.dddspringbootmybatispuls.module.order.domain.model;

import com.demo.dddspringbootmybatispuls.common.aggregate.BaseDomainEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class OrderItem extends BaseDomainEntity {
  private Long orderId;
  private String skuCode;
  private Integer quantity;
}
