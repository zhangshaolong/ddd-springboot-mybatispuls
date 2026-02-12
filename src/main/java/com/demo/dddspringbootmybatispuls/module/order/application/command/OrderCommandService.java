package com.demo.dddspringbootmybatispuls.module.order.application.command;

import com.demo.dddspringbootmybatispuls.common.aggregate.AggregateChanges;
import com.demo.dddspringbootmybatispuls.common.aggregate.AggregatePersistenceManager;
import com.demo.dddspringbootmybatispuls.common.aggregate.AggregateTracker;
import com.demo.dddspringbootmybatispuls.module.order.domain.model.Order;
import com.demo.dddspringbootmybatispuls.module.order.domain.model.OrderItem;
import com.demo.dddspringbootmybatispuls.module.order.domain.model.OrderPayment;
import com.demo.dddspringbootmybatispuls.module.order.infrastructure.dataobject.OrderDO;
import com.demo.dddspringbootmybatispuls.module.order.infrastructure.dataobject.OrderItemDO;
import com.demo.dddspringbootmybatispuls.module.order.infrastructure.dataobject.OrderPaymentDO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderCommandService {
  @Autowired private AggregateTracker aggregateTracker;
  @Autowired private AggregatePersistenceManager aggregatePersistenceManager;

  /** 实体→DO映射（可配置到配置文件） */
  private static final Map<Class<?>, Class<?>> entityDOMapping;

  static {
    entityDOMapping = new HashMap<>();
    entityDOMapping.put(Order.class, OrderDO.class);
    entityDOMapping.put(OrderItem.class, OrderItemDO.class);
    entityDOMapping.put(OrderPayment.class, OrderPaymentDO.class);
  }

  public void update() {
    // 1. 构造初始聚合根
    Order order = new Order();
    order.setId(1L);
    order.setVersion(1L);
    order.setOrderNo("ORDER_20260211_001");
    order.setStatus("UNPAID");

    // 构造订单项
    OrderItem item1 = new OrderItem();
    item1.setId(2021783464974913538L);
    item1.setOrderId(1L);
    item1.setSkuCode("SKU_001");
    item1.setQuantity(2);

    // 构造支付信息
    OrderPayment payment = new OrderPayment();
    payment.setId(1L);
    payment.setOrderId(1L);
    payment.setAmount(new java.math.BigDecimal("200.00"));
    payment.setPayType("ALIPAY");
    List<OrderItem> items = new ArrayList<OrderItem>();
    items.add(item1);
    order.setItems(items);
    order.setPayment(payment);
    aggregatePersistenceManager.setDebug(true);
    aggregateTracker.buildSnapshot(order);

    // 3. 模拟业务修改
    order.setStatus("zsl-tt"); // 修改订单状态

    // 新增订单项
    OrderItem item2 = new OrderItem();
    item2.setOrderId(1L);
    item2.setSkuCode("SKU_002112");
    item2.setQuantity(30);
    order.getItems().add(item2);

    // 删除原有订单项
    order.getItems().remove(item1);

    aggregateTracker.buildSnapshot(order);
    payment.setPayType("zsl-test"); // 修改支付方式
    payment.setAmount(new java.math.BigDecimal("211100.00"));
    //    payment.setOrderId(1L);

    // 4. 对比变更
    AggregateChanges changes = aggregateTracker.compareChanges(order, entityDOMapping);

    // 5. 持久化所有变更
    aggregatePersistenceManager.persist(changes);

    System.out.println("✅ 聚合根变更持久化完成！");
    System.out.println("📌 聚合根最新版本：" + changes.getAggregateVersion());
  }
}
