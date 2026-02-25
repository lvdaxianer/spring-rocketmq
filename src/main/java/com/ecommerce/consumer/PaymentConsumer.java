package com.ecommerce.consumer;

import com.ecommerce.model.message.PaymentSuccessMessage;
import com.ecommerce.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;

/**
 * 支付消息消费者
 * @author lvdaxianer
 * @date 2025-02-25
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConsumer {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;
    private final DefaultMQPushConsumer defaultMQPushConsumer;

    @Value("${rocketmq.topic.payment:payment-topic}")
    private String paymentTopic;

    @PostConstruct
    public void start() {
        try {
            defaultMQPushConsumer.subscribe(paymentTopic, "PAYMENT_SUCCESS");
            
            defaultMQPushConsumer.registerMessageListener((msgs, context) -> {
                for (MessageExt msg : msgs) {
                    try {
                        String body = new String(msg.getBody());
                        PaymentSuccessMessage paymentMessage = objectMapper.readValue(body, PaymentSuccessMessage.class);
                        
                        log.info("Received payment success message: paymentId={}, orderId={}, msgId={}", 
                                paymentMessage.getPaymentId(), paymentMessage.getOrderId(), msg.getMsgId());
                        
                        processPaymentSuccessMessage(paymentMessage);
                        
                    } catch (Exception e) {
                        log.error("Failed to process payment message: msgId={}", msg.getMsgId(), e);
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });
            
            defaultMQPushConsumer.start();
            log.info("Payment consumer started, topic: {}", paymentTopic);
            
        } catch (Exception e) {
            log.error("Failed to start payment consumer", e);
            throw new RuntimeException("Failed to start payment consumer", e);
        }
    }

    @PreDestroy
    public void stop() {
        if (defaultMQPushConsumer != null) {
            defaultMQPushConsumer.shutdown();
            log.info("Payment consumer stopped");
        }
    }

    private void processPaymentSuccessMessage(PaymentSuccessMessage message) {
        log.info("Processing payment: paymentId={}, orderId={}, amount={}", 
                message.getPaymentId(), message.getOrderId(), message.getAmount());
        log.info("Payment processed successfully: paymentId={}", message.getPaymentId());
    }
}
