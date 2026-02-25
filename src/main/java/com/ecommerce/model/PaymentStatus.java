package com.ecommerce.model;

/**
 * 支付状态枚举
 * @author lvdaxianer
 * @date 2025-02-25
 */
public enum PaymentStatus {
    /**
     * 待支付
     */
    PENDING,
    
    /**
     * 支付成功
     */
    SUCCESS,
    
    /**
     * 支付失败
     */
    FAILED,
    
    /**
     * 已退款
     */
    REFUNDED
}
