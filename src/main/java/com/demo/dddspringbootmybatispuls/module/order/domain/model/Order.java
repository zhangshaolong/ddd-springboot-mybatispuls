package com.demo.dddspringbootmybatispuls.module.order.domain.model;

import com.demo.dddspringbootmybatispuls.common.aggregate.AggregateRoot;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class Order extends AggregateRoot {
  private String orderNo;
  private String status;
  private List<OrderItem> items = new ArrayList<>();
  private OrderPayment payment;
}
