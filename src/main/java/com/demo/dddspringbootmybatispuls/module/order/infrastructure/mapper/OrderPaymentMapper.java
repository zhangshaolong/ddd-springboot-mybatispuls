package com.demo.dddspringbootmybatispuls.module.order.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.dddspringbootmybatispuls.module.order.infrastructure.dataobject.OrderPaymentDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderPaymentMapper extends BaseMapper<OrderPaymentDO> {}
