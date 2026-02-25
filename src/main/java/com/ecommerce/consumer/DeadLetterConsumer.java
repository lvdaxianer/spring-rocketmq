package com.ecommerce.consumer;

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
 * 死信队列消费者
 * @author lvdaxianer
 * @date 2025-02-25
 */
@Slf4j
@Component
public class DeadLetterConsumer {

    private final DefaultMQPushConsumer defaultMQPushConsumer;

    @Value("${rocketmq.topic.dlq:DLQ}")
    private String dlqTopic;

    public DeadLetterConsumer(DefaultMQPushConsumer defaultMQPushConsumer) {
        this.defaultMQPushConsumer = defaultMQPushConsumer;
    }

    @PostConstruct
    public void start() {
        try {
            // 订阅死信队列主题，使用*匹配所有标签
            defaultMQPushConsumer.subscribe(dlqTopic, "*");
            
            defaultMQPushConsumer.registerMessageListener((msgs, context) -> {
                for (MessageExt msg : msgs) {
                    try {
                        String body = new String(msg.getBody());
                        log.error("Received dead letter message: msgId={}, topic={}, tags={}, body={}", 
                                msg.getMsgId(), msg.getTopic(), msg.getTags(), body);
                        
                        // 记录死信消息用于人工处理
                        handleDeadLetterMessage(msg);
                        
                    } catch (Exception e) {
                        log.error("Failed to process dead letter message: msgId={}", msg.getMsgId(), e);
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });
            
            defaultMQPushConsumer.start();
            log.info("Dead letter consumer started, topic: {}", dlqTopic);
            
        } catch (Exception e) {
            log.error("Failed to start dead letter consumer", e);
            throw new RuntimeException("Failed to start dead letter consumer", e);
        }
    }

    @PreDestroy
    public void stop() {
        if (defaultMQPushConsumer != null) {
            defaultMQPushConsumer.shutdown();
            log.info("Dead letter consumer stopped");
        }
    }

    /**
     * 处理死信消息
     */
    private void handleDeadLetterMessage(MessageExt msg) {
        // 记录死信消息详情
        log.error("Dead letter details: msgId={}, topic={}, tags={}, keys={}, reconsumeTimes={}, bornTime={}", 
                msg.getMsgId(), 
                msg.getTopic(), 
                msg.getTags(), 
                msg.getKeys(),
                msg.getReconsumeTimes(),
                msg.getBornTimestamp());
        
        // 可以将死信消息存储到数据库或发送告警通知
        // 这里仅记录日志，实际生产环境需要人工处理或重试
    }
}
