package com.demo.dddspringbootmybatispuls.test.mapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDTO {
  private Long orderId;
  private String orderName;

  @Override
  public String toString() {
    return "OrderDTO{" + "orderId=" + orderId + ", orderName='" + orderName + '\'' + '}';
  }
}
