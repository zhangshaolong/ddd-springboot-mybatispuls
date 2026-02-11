package com.demo.dddspringbootmybatispuls.test.mapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity {
  private Long orderId;
  private String orderName;
}
