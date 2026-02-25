package com.ecommerce.model.message;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单创建消息
 * @author lvdaxianer
 * @date 2025-02-25
 */
@Data
public class OrderCreatedMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 商品ID
     */
    private String productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 创建时间戳
     */
    private Long createdAt;

    /**
     * 消息标签
     */
    private String tags;
}
