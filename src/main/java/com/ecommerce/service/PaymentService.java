package com.ecommerce.service;

import com.ecommerce.model.Order;
import com.ecommerce.model.OrderStatus;
import com.ecommerce.model.Payment;
import com.ecommerce.model.PaymentStatus;
import com.ecommerce.model.message.PaymentSuccessMessage;
import com.ecommerce.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 支付服务
 * @author lvdaxianer
 * @date 2025-02-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderService orderService;

    /**
     * 创建支付记录
     */
    @Transactional
    public Payment createPayment(String orderId, String userId, java.math.BigDecimal amount) {
        String paymentId = generatePaymentId();
        
        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setOrderId(orderId);
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        
        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment created: {}", savedPayment.getPaymentId());
        
        return savedPayment;
    }

    /**
     * 处理支付成功
     */
    @Transactional
    public Payment processPaymentSuccess(String paymentId, String transactionId) {
        Optional<Payment> paymentOpt = paymentRepository.findByPaymentId(paymentId);
        if (paymentOpt.isEmpty()) {
            throw new RuntimeException("Payment not found: " + paymentId);
        }
        
        Payment payment = paymentOpt.get();
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionId(transactionId);
        payment.setPaidAt(LocalDateTime.now());
        
        // 更新订单状态
        String orderId = payment.getOrderId();
        orderService.updateOrderStatus(orderId, OrderStatus.PAID);
        
        log.info("Payment success: paymentId={}, orderId={}", paymentId, orderId);
        return paymentRepository.save(payment);
    }

    /**
     * 处理支付失败
     */
    @Transactional
    public Payment processPaymentFailure(String paymentId) {
        Optional<Payment> paymentOpt = paymentRepository.findByPaymentId(paymentId);
        if (paymentOpt.isEmpty()) {
            throw new RuntimeException("Payment not found: " + paymentId);
        }
        
        Payment payment = paymentOpt.get();
        payment.setStatus(PaymentStatus.FAILED);
        
        log.info("Payment failed: paymentId={}", paymentId);
        return paymentRepository.save(payment);
    }

    /**
     * 根据支付ID查询支付记录
     */
    public Optional<Payment> getPaymentById(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId);
    }

    /**
     * 根据订单ID查询支付记录
     */
    public Optional<Payment> getPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    /**
     * 生成支付ID
     */
    private String generatePaymentId() {
        return "PAY-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 构建支付成功消息
     */
    public PaymentSuccessMessage buildPaymentSuccessMessage(Payment payment) {
        PaymentSuccessMessage message = new PaymentSuccessMessage();
        message.setPaymentId(payment.getPaymentId());
        message.setOrderId(payment.getOrderId());
        message.setUserId(payment.getUserId());
        message.setAmount(payment.getAmount());
        message.setTransactionId(payment.getTransactionId());
        message.setPaidAt(System.currentTimeMillis());
        message.setTags("PAYMENT_SUCCESS");
        return message;
    }
}
