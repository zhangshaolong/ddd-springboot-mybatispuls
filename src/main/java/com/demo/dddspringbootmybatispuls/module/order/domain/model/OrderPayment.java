package com.demo.dddspringbootmybatispuls.module.order.domain.model;

import com.demo.dddspringbootmybatispuls.common.aggregate.BaseDomainEntity;
import com.demo.dddspringbootmybatispuls.module.order.infrastructure.dataobject.OrderPaymentDO;
import io.github.linpeilie.annotations.AutoMapper;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@AutoMapper(target = OrderPaymentDO.class)
public class OrderPayment extends BaseDomainEntity {
  private Long orderId;
  private BigDecimal amount;
  private String payType;
}
