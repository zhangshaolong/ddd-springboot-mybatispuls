package com.demo.dddspringbootmybatispuls.module.order.domain.repository;

import com.demo.dddspringbootmybatispuls.module.order.infrastructure.dataobject.OrderDO;
import java.util.List;

public interface OrderRepository {
  OrderDO selectById(Long id);

  List<OrderDO> selectList();
}
