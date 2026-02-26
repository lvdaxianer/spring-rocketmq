package com.ecommerce.producer;

import com.ecommerce.model.message.OrderTimeoutMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 延迟消息生产者
 * @author lvdaxianer
 * @date 2025-02-25
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DelayMessageProducer {

    private final DefaultMQProducer defaultMQProducer;
    private final ObjectMapper objectMapper;

    @Value("${rocketmq.topic.order-timeout:order-timeout-topic}")
    private String orderTimeoutTopic;

    /**
     * 发送延迟消息
     * @param message 订单超时消息
     * @param delayLevel 延迟级别 (1-18)
     *                   1: 1秒  2: 5秒  3: 10秒  4: 30秒
     *                   5: 1分钟 6: 2分钟 7: 3分钟 8: 4分钟
     *                   9: 5分钟 10: 6分钟 11: 8分钟 12: 10分钟
     *                   13: 20分钟 14: 30分钟 15: 1小时 16: 2小时
     */
    public void sendDelayMessage(OrderTimeoutMessage message, int delayLevel) {
        try {
            String json = objectMapper.writeValueAsString(message);
            Message rocketMsg = new Message(orderTimeoutTopic, "ORDER_TIMEOUT", json.getBytes());
            
            // 设置延迟级别
            rocketMsg.setDelayTimeLevel(delayLevel);
            
            defaultMQProducer.send(rocketMsg);
            log.info("Delay message sent: orderId={}, delayLevel={}", message.getOrderId(), delayLevel);
            
        } catch (Exception e) {
            log.error("Failed to send delay message: orderId={}", message.getOrderId(), e);
            throw new RuntimeException("Failed to send delay message", e);
        }
    }

    /**
     * 发送订单超时取消消息（默认15分钟）
     */
    public void sendOrderTimeoutMessage(OrderTimeoutMessage message) {
        // 延迟级别15对应1小时，这里使用5（1分钟）用于测试
        sendDelayMessage(message, 5);
    }
}
