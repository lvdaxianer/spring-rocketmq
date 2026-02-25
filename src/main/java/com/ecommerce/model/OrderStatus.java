package com.ecommerce.model;

/**
 * 订单状态枚举
 * @author lvdaxianer
 * @date 2025-02-25
 */
public enum OrderStatus {
    /**
     * 待支付
     */
    PENDING_PAYMENT,
    
    /**
     * 已支付
     */
    PAID,
    
    /**
     * 已取消
     */
    CANCELLED,
    
    /**
     * 超时取消
     */
    TIMEOUT_CANCELLED,
    
    /**
     * 已完成
     */
    COMPLETED
}
