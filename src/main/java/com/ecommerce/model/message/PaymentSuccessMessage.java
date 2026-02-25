package com.ecommerce.model.message;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 支付成功消息
 * @author lvdaxianer
 * @date 2025-02-25
 */
@Data
public class PaymentSuccessMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 支付ID
     */
    private String paymentId;

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 交易号
     */
    private String transactionId;

    /**
     * 支付时间戳
     */
    private Long paidAt;

    /**
     * 消息标签
     */
    private String tags;
}
