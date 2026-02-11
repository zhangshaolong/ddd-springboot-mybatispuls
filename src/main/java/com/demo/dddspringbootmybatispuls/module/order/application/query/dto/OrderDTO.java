package com.demo.dddspringbootmybatispuls.module.order.application.query.dto;

import lombok.Data;

@Data
public class OrderDTO {
  private Long version;
  private String orderNo;
  private String status;
}
