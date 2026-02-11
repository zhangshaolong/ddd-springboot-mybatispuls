package com.demo.dddspringbootmybatispuls.module.order.domain.model;

import com.demo.dddspringbootmybatispuls.common.aggregate.BaseDomainEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class OrderPayment extends BaseDomainEntity {
  private Long orderId;
  private BigDecimal amount;
  private String payType;
}
