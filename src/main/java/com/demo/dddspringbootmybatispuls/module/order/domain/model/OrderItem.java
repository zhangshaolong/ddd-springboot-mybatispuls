package com.demo.dddspringbootmybatispuls.module.order.domain.model;

import com.demo.dddspringbootmybatispuls.common.aggregate.BaseDomainEntity;
import com.demo.dddspringbootmybatispuls.module.order.infrastructure.dataobject.OrderItemDO;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@AutoMapper(target = OrderItemDO.class)
public class OrderItem extends BaseDomainEntity {
  private Long orderId;
  private String skuCode;
  private Integer quantity;
}
