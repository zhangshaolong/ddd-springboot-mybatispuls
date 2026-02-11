package com.demo.dddspringbootmybatispuls.module.order.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.demo.dddspringbootmybatispuls.common.aggregate.BaseDO;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_order_payment")
public class OrderPaymentDO extends BaseDO implements Serializable {
  private static final long serialVersionUID = 1L;

  /** 关联订单ID（对应Order的id） */
  private Long orderId;

  /** 支付金额 */
  private java.math.BigDecimal amount;

  /** 支付方式（ALIPAY/WECHAT/CASH） */
  private String payType;

  /** 支付状态（UNPAID/PAYING/PAID/REFUNDED） */
  private String payStatus;

  /** 第三方支付流水号 */
  private String outTradeNo;
}
