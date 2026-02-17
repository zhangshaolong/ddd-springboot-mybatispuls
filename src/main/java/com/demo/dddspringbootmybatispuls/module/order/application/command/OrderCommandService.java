package com.demo.dddspringbootmybatispuls.module.order.application.command;

import com.demo.dddspringbootmybatispuls.common.aggregate.Aggregate;
import com.demo.dddspringbootmybatispuls.common.aggregate.AggregatePersistenceManager;
import com.demo.dddspringbootmybatispuls.common.aggregate.AggregateTracker;
import com.demo.dddspringbootmybatispuls.module.order.domain.model.Order;
import com.demo.dddspringbootmybatispuls.module.order.domain.model.OrderItem;
import com.demo.dddspringbootmybatispuls.module.order.domain.model.OrderPayment;
import com.demo.dddspringbootmybatispuls.module.order.infrastructure.dataobject.OrderDO;
import com.demo.dddspringbootmybatispuls.module.order.infrastructure.dataobject.OrderItemDO;
import com.demo.dddspringbootmybatispuls.module.order.infrastructure.dataobject.OrderPaymentDO;
import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OrderCommandService {
  @Resource private AggregateTracker aggregateTracker;
  @Resource private AggregatePersistenceManager aggregatePersistenceManager;
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
    OrderItem item1 = new OrderItem();
    item1.setId(1001L);
    item1.setOrderId(1L);
    item1.setSkuCode("SKU_001");
    item1.setQuantity(2);
    OrderPayment payment = new OrderPayment();
    payment.setId(2001L);
    payment.setOrderId(1L);
    payment.setAmount(new java.math.BigDecimal("200.00"));
    payment.setPayType("ALIPAY");
    order.setPayment(payment);
    order.getItems().add(item1);

    //    aggregate.setRoot(order);
    payment.setAmount(new java.math.BigDecimal("20011.00"));
    order.setOrderNo("abc");
    // 构造订单项

    //    // 构造支付信息

    //    List<OrderItem> items = new ArrayList<OrderItem>();
    //    items.add(item1);
    //    order.setItems(items);

    // 3. 模拟业务修改
    //    order.setStatus("PAID122"); // 修改订单状态
    Aggregate<Order> aggregate = aggregateTracker.build(Order.class);
    // 新增订单项
    //    OrderItem item2 = new OrderItem();
    //    item2.setId(222L);
    //    item2.setOrderId(1L);
    //    item2.setSkuCode("SKU_002");
    //    item2.setQuantity(3);
    //    order.getItems().add(item2);
    order.getPayment().setPayType("ccc");

    aggregate.setRoot(order);
    //    order.setStatus("ppp");

    // 删除原有订单项
    //    aggregate.getRoot().getItems().remove(item1);

    //    aggregateTracker.buildSnapshot(order);
    //    payment.setPayType("WECHAT"); // 修改支付方式
    //    payment.setOrderId(1L);
    //    order.setStatus("abc");
    // 4. 对比变更
    //    AggregateChanges changes = aggregateTracker.compareChanges();
    // 5. 持久化所有变更

    //    aggregate.getRoot().markAsDeleted();
    //    order.setOrderNo("ddf");
    boolean hasChanged =
        aggregatePersistenceManager.persist(aggregateTracker, entityDOMapping, false);

    if (hasChanged) {
      System.out.println("📌 聚合根最新版本：" + aggregateTracker.getCurrentAggregateRoot().getVersion());
    }
    System.out.println("✅ 聚合根变更持久化完成！");
  }
}
