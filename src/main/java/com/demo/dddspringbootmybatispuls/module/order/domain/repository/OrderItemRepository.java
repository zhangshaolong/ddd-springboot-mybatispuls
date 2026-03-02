package com.demo.dddspringbootmybatispuls.module.order.domain.repository;

import com.demo.dddspringbootmybatispuls.module.order.infrastructure.dataobject.OrderItemDO;
import java.util.List;

public interface OrderItemRepository {
  List<OrderItemDO> selectByOrderId(Long id);
}
