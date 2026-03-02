package com.demo.dddspringbootmybatispuls.module.order.domain.repository;

import com.demo.dddspringbootmybatispuls.module.order.infrastructure.dataobject.OrderPaymentDO;

public interface OrderPaymentRepository {
  OrderPaymentDO selectByOrderId(Long id);
}
