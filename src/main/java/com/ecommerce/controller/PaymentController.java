package com.ecommerce.controller;

import com.ecommerce.model.Payment;
import com.ecommerce.model.message.PaymentSuccessMessage;
import com.ecommerce.producer.TransactionOrderProducer;
import com.ecommerce.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 支付Controller
 * @author lvdaxianer
 * @date 2025-02-25
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final TransactionOrderProducer transactionOrderProducer;

    /**
     * 发起支付（事务消息）
     */
    @PostMapping
    public ResponseEntity<Payment> createPayment(@RequestBody CreatePaymentRequest request) {
        log.info("Received payment request: orderId={}, userId={}, amount={}", 
                 request.getOrderId(), request.getUserId(), request.getAmount());
        
        // 创建支付记录
        Payment payment = paymentService.createPayment(
                request.getOrderId(),
                request.getUserId(),
                request.getAmount()
        );
        
        // 构建支付成功消息
        PaymentSuccessMessage message = paymentService.buildPaymentSuccessMessage(payment);
        message.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 16));
        
        // 发送事务消息
        try {
            transactionOrderProducer.sendTransactionMessage(message, "payment-executor");
            log.info("Transaction message sent: paymentId={}", payment.getPaymentId());
        } catch (Exception e) {
            log.error("Failed to send transaction message", e);
            // 标记支付失败
            paymentService.processPaymentFailure(payment.getPaymentId());
            return ResponseEntity.internalServerError().build();
        }
        
        return ResponseEntity.ok(payment);
    }

    /**
     * 根据支付ID查询支付记录
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getPayment(@PathVariable String paymentId) {
        return paymentService.getPaymentById(paymentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 根据订单ID查询支付记录
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<Payment> getPaymentByOrderId(@PathVariable String orderId) {
        return paymentService.getPaymentByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 创建支付请求
     */
    public static class CreatePaymentRequest {
        private String orderId;
        private String userId;
        private BigDecimal amount;

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }
}
