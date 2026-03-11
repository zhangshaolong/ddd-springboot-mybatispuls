package com.demo.dddspringbootmybatispuls.module.order.application.command;

import com.demo.dddspringbootmybatispuls.common.aggregate.Aggregate;
import com.demo.dddspringbootmybatispuls.common.aggregate.AggregatePersistenceManager;
import com.demo.dddspringbootmybatispuls.common.aggregate.AggregateTracker;
import com.demo.dddspringbootmybatispuls.common.mapper.StructMapper;
import com.demo.dddspringbootmybatispuls.module.order.domain.model.Order;
import com.demo.dddspringbootmybatispuls.module.order.domain.model.OrderItem;
import com.demo.dddspringbootmybatispuls.module.order.domain.model.OrderPayment;
import com.demo.dddspringbootmybatispuls.module.order.domain.repository.OrderItemRepository;
import com.demo.dddspringbootmybatispuls.module.order.domain.repository.OrderPaymentRepository;
import com.demo.dddspringbootmybatispuls.module.order.domain.repository.OrderRepository;
import com.demo.dddspringbootmybatispuls.module.order.infrastructure.dataobject.OrderDO;
import com.demo.dddspringbootmybatispuls.module.order.infrastructure.dataobject.OrderItemDO;
import com.demo.dddspringbootmybatispuls.module.order.infrastructure.dataobject.OrderPaymentDO;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderCommandService {

  @Autowired private OrderRepository orderRepository;
  @Autowired private OrderItemRepository orderItemRepository;
  @Autowired private OrderPaymentRepository orderPaymentRepository;

  @Resource private AggregateTracker aggregateTracker;
  @Resource private AggregatePersistenceManager aggregatePersistenceManager;

  private final boolean isDebugMode = true;

  private final Long orderId = 1L;

  public void create() {

    // 为Order实体注册追踪，新建场景固定写法
    Aggregate<Order> aggregate = aggregateTracker.build(Order.class);

    // 1. 构造新建时的各个实体对象
    Order order = new Order();
    order.setId(orderId);
    order.setOrderNo("ORDER_20260211_001" + orderId);
    order.setStatus("UNPAID");
    OrderItem item1 = new OrderItem();
    item1.setId(orderId);
    item1.setOrderId(orderId);
    item1.setSkuCode("SKU_001");
    item1.setQuantity(2);
    order.getItems().add(item1);
    OrderPayment payment = new OrderPayment();
    payment.setId(orderId);
    payment.setOrderId(orderId);
    payment.setAmount(new java.math.BigDecimal("200.00"));
    payment.setPayType("ALIPAY");
    order.setPayment(payment);
    aggregate.setRoot(order);

    // 持久化聚合根各实体
    boolean hasChanged = aggregatePersistenceManager.persist(aggregateTracker, isDebugMode);

    if (hasChanged) {
      System.out.println("📌 聚合根最新版本：" + aggregateTracker.getCurrentAggregateRoot().getVersion());
    }
    System.out.println("✅ 聚合根变更持久化完成！");
  }

  public void update() {
    // 获取最新的持久化数据并创建对应实体
    OrderDO orderDO = orderRepository.selectById(orderId);
    Order order = StructMapper.to(orderDO, Order.class);
    List<OrderItemDO> orderItemDOS = orderItemRepository.selectByOrderId(orderId);
    List<OrderItem> orderItems = StructMapper.to(orderItemDOS, OrderItem.class);
    order.setItems(orderItems);
    OrderPaymentDO orderPaymentDO = orderPaymentRepository.selectByOrderId(orderId);
    OrderPayment payment = StructMapper.to(orderPaymentDO, OrderPayment.class);
    order.setPayment(payment);

    // 注册追踪聚合根变更
    aggregateTracker.build(order);

    // 业务做update操作
    payment.setPayType("e1r");
    orderItems.removeFirst();
    OrderItem orderItem = new OrderItem();
    orderItem.setId(2L + orderId);
    orderItem.setOrderId(orderId);
    orderItem.setSkuCode("SKU_001111");
    orderItem.setQuantity(2001);
    orderItems.add(orderItem);
    // 完成聚合根业务处理后，持久化操作
    boolean hasChanged = aggregatePersistenceManager.persist(aggregateTracker, isDebugMode);

    if (hasChanged) {
      System.out.println("📌 聚合根最新版本：" + aggregateTracker.getCurrentAggregateRoot().getVersion());
    }
    System.out.println("✅ 聚合根变更持久化完成！");
  }

  public void remove() {
    // 获取最新的持久化数据并创建对应实体
    OrderDO orderDO = orderRepository.selectById(orderId);
    Order order = StructMapper.to(orderDO, Order.class);
    List<OrderItemDO> orderItemDOS = orderItemRepository.selectByOrderId(orderId);
    List<OrderItem> orderItems = StructMapper.to(orderItemDOS, OrderItem.class);
    order.setItems(orderItems);
    OrderPaymentDO orderPaymentDO = orderPaymentRepository.selectByOrderId(orderId);
    OrderPayment payment = StructMapper.to(orderPaymentDO, OrderPayment.class);
    order.setPayment(payment);

    // 注册追踪聚合根变更
    aggregateTracker.build(order);

    // 业务做删除标记操作
    order.markAsDeleted();

    // 完成聚合根业务处理后，持久化操作
    boolean hasChanged = aggregatePersistenceManager.persist(aggregateTracker, isDebugMode);

    if (hasChanged) {
      System.out.println("📌 聚合根最新版本：" + aggregateTracker.getCurrentAggregateRoot().getVersion());
    }
    System.out.println("✅ 聚合根变更持久化完成！");
  }
}
