package com.demo.dddspringbootmybatispuls.module.order.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@TableName("t_order_item")
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDO implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  private Long id;

  /** 关联订单ID（对应Order的id） */
  private Long orderId;

  /** 商品SKU编码 */
  private String skuCode;

  /** 购买数量 */
  private Integer quantity;
}
