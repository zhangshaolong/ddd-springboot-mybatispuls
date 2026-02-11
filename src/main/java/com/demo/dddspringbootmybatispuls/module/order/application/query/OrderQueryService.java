package com.demo.dddspringbootmybatispuls.module.order.application.query;

import com.demo.dddspringbootmybatispuls.common.mapper.StructMapper;
import com.demo.dddspringbootmybatispuls.module.order.application.query.dto.OrderDTO;
import com.demo.dddspringbootmybatispuls.module.order.domain.repository.OrderRepository;
import com.demo.dddspringbootmybatispuls.module.order.infrastructure.dataobject.OrderDO;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderQueryService {

  @Resource private OrderRepository orderRepository;

  public OrderDTO getOrderById(Long id) {
    OrderDO orderDO = orderRepository.selectById(id);
    if (orderDO == null) {
      return null;
    }
    return StructMapper.to(orderDO, OrderDTO.class);
  }

  public List<OrderDTO> getOrders() {
    List<OrderDO> orderDOList = orderRepository.selectList();
    if (orderDOList.isEmpty()) {
      return new ArrayList<>();
    }
    return StructMapper.toList(orderDOList, OrderDTO.class);
  }
}
