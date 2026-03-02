package com.demo.dddspringbootmybatispuls.module.order.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.dddspringbootmybatispuls.module.order.domain.repository.OrderItemRepository;
import com.demo.dddspringbootmybatispuls.module.order.infrastructure.dataobject.OrderItemDO;
import com.demo.dddspringbootmybatispuls.module.order.infrastructure.mapper.OrderItemMapper;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class OrderItemRepositoryImpl implements OrderItemRepository {

  @Resource private OrderItemMapper orderItemMapper;

  @Override
  public List<OrderItemDO> selectByOrderId(Long id) {
    return orderItemMapper.selectList(
        new LambdaQueryWrapper<OrderItemDO>().eq(OrderItemDO::getOrderId, id));
  }
}
