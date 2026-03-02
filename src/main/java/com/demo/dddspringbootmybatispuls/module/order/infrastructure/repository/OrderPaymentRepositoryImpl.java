package com.demo.dddspringbootmybatispuls.module.order.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.dddspringbootmybatispuls.module.order.domain.repository.OrderPaymentRepository;
import com.demo.dddspringbootmybatispuls.module.order.infrastructure.dataobject.OrderPaymentDO;
import com.demo.dddspringbootmybatispuls.module.order.infrastructure.mapper.OrderPaymentMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

@Repository
public class OrderPaymentRepositoryImpl implements OrderPaymentRepository {

  @Resource private OrderPaymentMapper orderPaymentMapper;

  @Override
  public OrderPaymentDO selectByOrderId(Long id) {
    return orderPaymentMapper.selectOne(
        new LambdaQueryWrapper<OrderPaymentDO>().eq(OrderPaymentDO::getOrderId, id));
  }
}
