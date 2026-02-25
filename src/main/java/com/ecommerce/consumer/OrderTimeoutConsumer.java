package com.ecommerce.consumer;

import com.ecommerce.model.OrderStatus;
import com.ecommerce.model.message.OrderTimeoutMessage;
import com.ecommerce.service.OrderService;
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
 * 订单超时消费者
 * @author lvdaxianer
 * @date 2025-02-25
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutConsumer {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;
    private final DefaultMQPushConsumer defaultMQPushConsumer;

    @Value("${rocketmq.topic.order-timeout:order-timeout-topic}")
    private String orderTimeoutTopic;

    @PostConstruct
    public void start() {
        try {
            defaultMQPushConsumer.subscribe(orderTimeoutTopic, "ORDER_TIMEOUT");
            
            defaultMQPushConsumer.registerMessageListener((msgs, context) -> {
                for (MessageExt msg : msgs) {
                    try {
                        String body = new String(msg.getBody());
                        OrderTimeoutMessage timeoutMessage = objectMapper.readValue(body, OrderTimeoutMessage.class);
                        
                        log.info("Received order timeout message: orderId={}, msgId={}", 
                                timeoutMessage.getOrderId(), msg.getMsgId());
                        
                        processOrderTimeoutMessage(timeoutMessage);
                        
                    } catch (Exception e) {
                        log.error("Failed to process timeout message: msgId={}", msg.getMsgId(), e);
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });
            
            defaultMQPushConsumer.start();
            log.info("Order timeout consumer started, topic: {}", orderTimeoutTopic);
            
        } catch (Exception e) {
            log.error("Failed to start order timeout consumer", e);
            throw new RuntimeException("Failed to start order timeout consumer", e);
        }
    }

    @PreDestroy
    public void stop() {
        if (defaultMQPushConsumer != null) {
            defaultMQPushConsumer.shutdown();
            log.info("Order timeout consumer stopped");
        }
    }

    /**
     * 处理订单超时消息
     */
    private void processOrderTimeoutMessage(OrderTimeoutMessage message) {
        String orderId = message.getOrderId();
        
        log.info("Processing order timeout: orderId={}", orderId);
        
        // 检查订单状态，只有待支付的订单才需要取消
        orderService.getOrderById(orderId).ifPresentOrElse(
                order -> {
                    if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
                        orderService.timeoutCancelOrder(orderId);
                        log.info("Order cancelled due to timeout: orderId={}", orderId);
                    } else {
                        log.info("Order already processed, skip timeout cancellation: orderId={}, status={}", 
                                orderId, order.getStatus());
                    }
                },
                () -> log.warn("Order not found: orderId={}", orderId)
        );
    }
}
