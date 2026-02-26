package com.ecommerce.producer;

import com.ecommerce.model.message.PaymentSuccessMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 事务消息生产者 - 用于支付场景
 * @author lvdaxianer
 * @date 2025-02-25
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionOrderProducer {

    private final TransactionMQProducer transactionMQProducer;
    private final ObjectMapper objectMapper;

    @Value("${rocketmq.topic.payment:payment-topic}")
    private String paymentTopic;

    /**
     * 发送事务消息
     */
    public TransactionSendResult sendTransactionMessage(PaymentSuccessMessage message, 
                                                        Object transactionExecutor) {
        try {
            String json = objectMapper.writeValueAsString(message);
            Message rocketMsg = new Message(paymentTopic, "PAYMENT_SUCCESS", json.getBytes());
            
            TransactionSendResult result = transactionMQProducer.sendMessageInTransaction(rocketMsg, transactionExecutor);
            log.info("Transaction message sent: paymentId={}, orderId={}, status={}", 
                    message.getPaymentId(), message.getOrderId(), result.getSendStatus());
            
            return result;
        } catch (Exception e) {
            log.error("Failed to send transaction message: paymentId={}", message.getPaymentId(), e);
            throw new RuntimeException("Failed to send transaction message", e);
        }
    }
}
