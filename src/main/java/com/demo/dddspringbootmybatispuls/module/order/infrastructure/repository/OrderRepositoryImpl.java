package com.demo.dddspringbootmybatispuls.module.order.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.dddspringbootmybatispuls.module.order.domain.repository.OrderRepository;
import com.demo.dddspringbootmybatispuls.module.order.infrastructure.dataobject.OrderDO;
import com.demo.dddspringbootmybatispuls.module.order.infrastructure.mapper.OrderMapper;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepositoryImpl implements OrderRepository {

  @Resource private OrderMapper orderMapper;

  @Override
  public OrderDO selectById(Long id) {
    return orderMapper.selectById(id);
  }

  @Override
  public List<OrderDO> selectList() {
    return orderMapper.selectList(new LambdaQueryWrapper<OrderDO>());
  }
}
