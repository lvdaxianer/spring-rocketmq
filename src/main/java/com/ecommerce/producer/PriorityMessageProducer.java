package com.ecommerce.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 优先级消息生产者
 * 面试要点：消息优先级处理
 * RocketMQ本身不支持消息优先级，但可以通过以下方式实现：
 * 1. 多个队列对应不同优先级
 * 2. 消费者按优先级顺序消费
 * @author lvdaxianer
 * @date 2025-02-25
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriorityMessageProducer {

    private final DefaultMQProducer defaultMQProducer;
    private final ObjectMapper objectMapper;

    @Value("${rocketmq.topic.order:order-topic}")
    private String orderTopic;

    /**
     * 优先级枚举
     */
    public enum Priority {
        HIGH(0, "高优先级"),
        NORMAL(1, "普通优先级"),
        LOW(2, "低优先级");

        private final int level;
        private final String desc;

        Priority(int level, String desc) {
            this.level = level;
            this.desc = desc;
        }

        public int getLevel() {
            return level;
        }
    }

    /**
     * 发送优先级消息
     * RocketMQ通过不同队列实现优先级，队列0为高优先级，队列1为普通，队列2为低优先级
     *
     * @param message  消息内容
     * @param priority 优先级
     */
    public void sendPriorityMessage(Object message, Priority priority) {
        try {
            String json = objectMapper.writeValueAsString(message);

            int queueIndex = priority.getLevel();
            Message rocketMsg = new Message(orderTopic, json.getBytes());

            defaultMQProducer.send(rocketMsg, (mqs, msg, arg) -> {
                int queueNum = (Integer) arg;
                int size = mqs.size();
                int index = queueNum % size;
                return mqs.get(index);
            }, queueIndex);

            log.info("Priority message sent: priority={}, queueIndex={}", priority, queueIndex);

        } catch (Exception e) {
            log.error("Failed to send priority message: priority={}", priority, e);
            throw new RuntimeException("Failed to send priority message", e);
        }
    }

    /**
     * 发送高优先级消息
     */
    public void sendHighPriorityMessage(Object message) {
        sendPriorityMessage(message, Priority.HIGH);
    }

    /**
     * 发送普通优先级消息
     */
    public void sendNormalPriorityMessage(Object message) {
        sendPriorityMessage(message, Priority.NORMAL);
    }

    /**
     * 发送低优先级消息
     */
    public void sendLowPriorityMessage(Object message) {
        sendPriorityMessage(message, Priority.LOW);
    }

    /**
     * 根据订单金额判断优先级
     * 高金额订单 -> 高优先级
     */
    public void sendMessageByAmount(Object message, java.math.BigDecimal amount) {
        Priority priority;
        if (amount.compareTo(new java.math.BigDecimal("10000")) >= 0) {
            priority = Priority.HIGH;
        } else if (amount.compareTo(new java.math.BigDecimal("1000")) >= 0) {
            priority = Priority.NORMAL;
        } else {
            priority = Priority.LOW;
        }
        sendPriorityMessage(message, priority);
    }
}
