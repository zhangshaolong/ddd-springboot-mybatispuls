package com.demo.dddspringbootmybatispuls.module.order.application.command;

import com.demo.dddspringbootmybatispuls.common.aggregate.AggregateChanges;
import com.demo.dddspringbootmybatispuls.common.aggregate.AggregatePersistenceManager;
import com.demo.dddspringbootmybatispuls.common.aggregate.AggregateTracker;
import com.demo.dddspringbootmybatispuls.common.aggregate.BaseDomainEntity;
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
  private static final Map<Class<?>, Class<?>> ENTITY_DO_MAPPING;

  static {
    ENTITY_DO_MAPPING = new HashMap<>();
    ENTITY_DO_MAPPING.put(Order.class, OrderDO.class);
    ENTITY_DO_MAPPING.put(OrderItem.class, OrderItemDO.class);
    ENTITY_DO_MAPPING.put(OrderPayment.class, OrderPaymentDO.class);
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
    item1.setId(1001L);
    item1.setOrderId(1L);
    item1.setSkuCode("SKU_001");
    item1.setQuantity(2);

    // 构造支付信息
    OrderPayment payment = new OrderPayment();
    payment.setId(2001L);
    payment.setOrderId(1L);
    payment.setAmount(new java.math.BigDecimal("200.00"));
    payment.setPayType("ALIPAY");
    List<OrderItem> items = new ArrayList<OrderItem>();
    order.setItems(items);
    order.setPayment(payment);

    // 2. 生成快照
    Map<Object, BaseDomainEntity> snapshot = aggregateTracker.buildSnapshot(order);

    // 3. 模拟业务修改
    order.setStatus("PAID"); // 修改订单状态
    payment.setPayType("WECHAT"); // 修改支付方式
    payment.setOrderId(1L);

    // 新增订单项
    OrderItem item2 = new OrderItem();
    item2.setOrderId(1L);
    item2.setSkuCode("SKU_002");
    item2.setQuantity(3);
    order.getItems().add(item2);

    // 删除原有订单项
    order.getItems().remove(item1);

    // 4. 对比变更
    AggregateChanges changes = aggregateTracker.compareChanges(snapshot, order, ENTITY_DO_MAPPING);

    // 5. 持久化所有变更
    aggregatePersistenceManager.persist(changes);

    System.out.println("✅ 聚合根变更持久化完成！");
    System.out.println("📌 聚合根最新版本：" + changes.getAggregateVersion()); // 预期2
  }

  /** 保存订单聚合根变更 */
  public void saveOrder(Order order) {
    // 1. 生成快照（首次保存时快照为空，可跳过）
    Map<Object, BaseDomainEntity> snapshot = aggregateTracker.buildSnapshot(order);

    // 2. 模拟业务修改（实际业务中由业务逻辑修改）
    order.setStatus("PAID");
    order.getPayment().setPayType("WECHAT");

    // 3. 对比变更
    AggregateChanges changes = aggregateTracker.compareChanges(snapshot, order, ENTITY_DO_MAPPING);

    // 4. 持久化变更
    aggregatePersistenceManager.persist(changes);
  }
}
