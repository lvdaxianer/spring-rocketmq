package com.ecommerce.model.message;

import lombok.Data;
import java.io.Serializable;

/**
 * 订单超时消息
 * @author lvdaxianer
 * @date 2025-02-25
 */
@Data
public class OrderTimeoutMessage implements Serializable {

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
     * 订单创建时间戳
     */
    private Long createdAt;

    /**
     * 消息标签
     */
    private String tags;
}
