package com.ecommerce.producer;

import com.ecommerce.model.message.OrderCreatedMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单消息生产者 - 支持顺序消息
 * @author lvdaxianer
 * @date 2025-02-25
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProducer {

    private final DefaultMQProducer defaultMQProducer;
    private final ObjectMapper objectMapper;

    @Value("${rocketmq.topic.order:order-topic}")
    private String orderTopic;

    /**
     * 发送订单创建消息（顺序消息）
     */
    public void sendOrderCreatedMessage(OrderCreatedMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            Message rocketMsg = new Message(orderTopic, "ORDER_CREATED", json.getBytes());
            
            // 使用订单ID作为sharding key，保证同一订单的消息发往同一队列
            defaultMQProducer.send(rocketMsg, new MessageQueueSelector() {
                @Override
                public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                    String orderId = (String) arg;
                    int hash = orderId.hashCode();
                    int index = Math.abs(hash) % mqs.size();
                    return mqs.get(index);
                }
            }, message.getOrderId());
            
            log.info("Order created message sent successfully: orderId={}", message.getOrderId());
        } catch (Exception e) {
            log.error("Failed to send order created message: orderId={}", message.getOrderId(), e);
            throw new RuntimeException("Failed to send order created message", e);
        }
    }

    /**
     * 发送订单创建消息（同步）
     */
    public void sendOrderCreatedMessageSync(OrderCreatedMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            Message rocketMsg = new Message(orderTopic, "ORDER_CREATED", json.getBytes());
            
            defaultMQProducer.send(rocketMsg);
            log.info("Order created message sent synchronously: orderId={}", message.getOrderId());
        } catch (Exception e) {
            log.error("Failed to send order created message: orderId={}", message.getOrderId(), e);
            throw new RuntimeException("Failed to send order created message", e);
        }
    }

    /**
     * 发送订单创建消息（异步）
     */
    public void sendOrderCreatedMessageAsync(OrderCreatedMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            Message rocketMsg = new Message(orderTopic, "ORDER_CREATED", json.getBytes());
            
            defaultMQProducer.send(rocketMsg, new org.apache.rocketmq.client.producer.SendCallback() {
                @Override
                public void onSuccess(org.apache.rocketmq.client.producer.SendResult sendResult) {
                    log.info("Order created message sent asynchronously success: orderId={}, msgId={}", 
                            message.getOrderId(), sendResult.getMsgId());
                }

                @Override
                public void onException(Throwable e) {
                    log.error("Order created message sent asynchronously failed: orderId={}", 
                             message.getOrderId(), e);
                }
            });
        } catch (Exception e) {
            log.error("Failed to send order created message: orderId={}", message.getOrderId(), e);
            throw new RuntimeException("Failed to send order created message", e);
        }
    }
}
