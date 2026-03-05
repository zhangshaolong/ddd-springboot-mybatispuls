package com.demo.dddspringbootmybatispuls.module.order.domain.model;

import com.demo.dddspringbootmybatispuls.common.aggregate.AggregateRoot;
import com.demo.dddspringbootmybatispuls.module.order.infrastructure.dataobject.OrderDO;
import io.github.linpeilie.annotations.AutoMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@AutoMapper(target = OrderDO.class)
public class Order extends AggregateRoot {
  private String orderNo;
  private String status;
  private List<OrderItem> items = new ArrayList<>();
  private OrderPayment payment;
}
