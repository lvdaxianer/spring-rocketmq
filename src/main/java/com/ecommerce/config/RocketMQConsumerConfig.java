package com.ecommerce.config;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * RocketMQ消费者配置
 * @author lvdaxianer
 * @date 2025-02-25
 */
@Configuration
public class RocketMQConsumerConfig {

    @Value("${spring.rocketmq.consumer.group}")
    private String consumerGroup;

    @Value("${spring.rocketmq.name-server}")
    private String nameServer;

    @Value("${spring.rocketmq.consumer.max-reconsume-times:3}")
    private int maxReconsumeTimes;

    @Value("${spring.rocketmq.consumer.consume-thread-min:20}")
    private int consumeThreadMin;

    @Value("${spring.rocketmq.consumer.consume-thread-max:64}")
    private int consumeThreadMax;

    @Value("${spring.rocketmq.consumer.consume-message-batch-max-size:1}")
    private int consumeMessageBatchMaxSize;

    /**
     * 创建普通消费者
     */
    @Bean
    public DefaultMQPushConsumer defaultMQPushConsumer() {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(nameServer);
        consumer.setConsumeThreadMin(consumeThreadMin);
        consumer.setConsumeThreadMax(consumeThreadMax);
        consumer.setConsumeMessageBatchMaxSize(consumeMessageBatchMaxSize);
        consumer.setMaxReconsumeTimes(maxReconsumeTimes);
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        
        return consumer;
    }
}
