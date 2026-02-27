package com.ecommerce.consumer;

import com.ecommerce.model.message.OrderCreatedMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 消息幂等性消费者
 * 面试要点：消息重复消费、幂等性处理
 * @author lvdaxianer
 * @date 2025-02-25
 */
@Slf4j
@Component
public class IdempotenceConsumer {

    private final DefaultMQPushConsumer defaultMQPushConsumer;
    private final ObjectMapper objectMapper;

    @Value("${rocketmq.topic.order:order-topic}")
    private String orderTopic;

    private final Map<String, Long> processedCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor();

    public IdempotenceConsumer(DefaultMQPushConsumer defaultMQPushConsumer, ObjectMapper objectMapper) {
        this.defaultMQPushConsumer = defaultMQPushConsumer;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void start() {
        try {
            defaultMQPushConsumer.subscribe(orderTopic, "*");

            defaultMQPushConsumer.registerMessageListener((msgs, context) -> {
                for (MessageExt msg : msgs) {
                    try {
                        String msgId = msg.getMsgId();
                        String body = new String(msg.getBody());
                        OrderCreatedMessage orderMessage = objectMapper.readValue(body, OrderCreatedMessage.class);

                        // 幂等性检查
                        if (!checkAndRecord(msgId)) {
                            log.warn("Duplicate message detected, skipping: msgId={}, orderId={}",
                                    msgId, orderMessage.getOrderId());
                            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                        }

                        log.info("Processing message: msgId={}, orderId={}", msgId, orderMessage.getOrderId());

                        // 业务处理
                        processMessage(orderMessage);

                        log.info("Message processed successfully: msgId={}", msgId);

                    } catch (Exception e) {
                        log.error("Failed to process message: msgId={}", msg.getMsgId(), e);
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });

            defaultMQPushConsumer.start();

            // 启动缓存清理定时任务
            startCacheCleanup();

            log.info("Idempotence consumer started, topic: {}", orderTopic);

        } catch (Exception e) {
            log.error("Failed to start idempotence consumer", e);
            throw new RuntimeException("Failed to start idempotence consumer", e);
        }
    }

    /**
     * 检查消息是否已处理过，并记录处理状态
     * 使用布隆过滤器或Redis可以实现分布式幂等
     */
    private boolean checkAndRecord(String msgId) {
        Long previousTimestamp = processedCache.get(msgId);
        if (previousTimestamp != null) {
            return false;
        }
        processedCache.put(msgId, System.currentTimeMillis());
        return true;
    }

    /**
     * 启动缓存清理任务，防止内存溢出
     * 实际生产环境建议使用Redis
     */
    private void startCacheCleanup() {
        cleanupScheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            long expireTime = now - 5 * 60 * 1000;

            processedCache.entrySet().removeIf(entry -> entry.getValue() < expireTime);
            log.debug("Cache cleanup completed, current size: {}", processedCache.size());
        }, 5, 5, TimeUnit.MINUTES);
    }

    private void processMessage(OrderCreatedMessage message) {
        log.info("Processing order: orderId={}, userId={}, amount={}",
                message.getOrderId(), message.getUserId(), message.getAmount());
    }

    @PreDestroy
    public void stop() {
        if (defaultMQPushConsumer != null) {
            defaultMQPushConsumer.shutdown();
            log.info("Idempotence consumer stopped");
        }
        if (cleanupScheduler != null) {
            cleanupScheduler.shutdown();
        }
    }
}
