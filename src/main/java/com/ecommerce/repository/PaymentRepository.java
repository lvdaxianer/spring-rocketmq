package com.ecommerce.repository;

import com.ecommerce.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 支付Repository
 * @author lvdaxianer
 * @date 2025-02-25
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * 根据支付ID查询
     */
    Optional<Payment> findByPaymentId(String paymentId);

    /**
     * 根据订单ID查询
     */
    Optional<Payment> findByOrderId(String orderId);
}
