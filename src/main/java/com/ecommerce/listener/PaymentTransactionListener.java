package com.ecommerce.listener;

import com.ecommerce.model.OrderStatus;
import com.ecommerce.model.Payment;
import com.ecommerce.model.PaymentStatus;
import com.ecommerce.model.message.PaymentSuccessMessage;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Component;

/**
 * 支付事务监听器
 * @author lvdaxianer
 * @date 2025-02-25
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentTransactionListener implements TransactionListener {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    /**
     * 执行本地事务
     */
    @Override
    public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        String body = new String(msg.getBody());
        
        try {
            PaymentSuccessMessage paymentMessage = objectMapper.readValue(body, PaymentSuccessMessage.class);
            String orderId = paymentMessage.getOrderId();
            String transactionId = paymentMessage.getTransactionId();
            
            log.info("Executing local transaction: paymentId={}, orderId={}", 
                    paymentMessage.getPaymentId(), orderId);
            
            // 更新订单状态为已支付
            orderService.updateOrderStatus(orderId, OrderStatus.PAID);
            
            // 更新支付状态为成功
            paymentService.processPaymentSuccess(paymentMessage.getPaymentId(), transactionId);
            
            log.info("Local transaction committed: orderId={}", orderId);
            return LocalTransactionState.COMMIT_MESSAGE;
            
        } catch (Exception e) {
            log.error("Local transaction failed", e);
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }
    }

    /**
     * 回查本地事务状态
     */
    @Override
    public LocalTransactionState checkLocalTransaction(MessageExt msg) {
        String body = new String(msg.getBody());
        
        try {
            PaymentSuccessMessage paymentMessage = objectMapper.readValue(body, PaymentSuccessMessage.class);
            String orderId = paymentMessage.getOrderId();
            
            log.info("Checking local transaction: orderId={}", orderId);
            
            // 查询订单状态
            return orderService.getOrderById(orderId)
                    .map(order -> {
                        if (order.getStatus() == OrderStatus.PAID) {
                            log.info("Transaction committed: orderId={}", orderId);
                            return LocalTransactionState.COMMIT_MESSAGE;
                        } else {
                            log.info("Transaction unknown: orderId={}, status={}", 
                                    orderId, order.getStatus());
                            return LocalTransactionState.COMMIT_MESSAGE;
                        }
                    })
                    .orElseGet(() -> {
                        log.warn("Order not found: orderId={}, rollback", orderId);
                        return LocalTransactionState.ROLLBACK_MESSAGE;
                    });
                    
        } catch (Exception e) {
            log.error("Failed to check local transaction", e);
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }
    }
}
