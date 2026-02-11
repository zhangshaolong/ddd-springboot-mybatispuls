package com.demo.dddspringbootmybatispuls.module.order.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.demo.dddspringbootmybatispuls.common.aggregate.BaseDO;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_order_item")
public class OrderItemDO extends BaseDO implements Serializable {
  private static final long serialVersionUID = 1L;

  /** 关联订单ID（对应Order的id） */
  private Long orderId;

  /** 商品SKU编码 */
  private String skuCode;

  /** 购买数量 */
  private Integer quantity;

  /** 商品单价 */
  private java.math.BigDecimal price;

  /** 小计金额（quantity * price） */
  private java.math.BigDecimal subAmount;
}
