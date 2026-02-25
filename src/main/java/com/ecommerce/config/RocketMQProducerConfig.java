package com.ecommerce.config;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ生产者配置
 * @author lvdaxianer
 * @date 2025-02-25
 */
@Configuration
public class RocketMQProducerConfig {

    @Value("${spring.rocketmq.producer.group}")
    private String producerGroup;

    @Value("${spring.rocketmq.name-server}")
    private String nameServer;

    @Value("${spring.rocketmq.producer.send-timeout:3000}")
    private int sendTimeout;

    @Value("${spring.rocketmq.producer.max-message-size:4194304}")
    private int maxMessageSize;

    /**
     * 创建普通消息生产者
     */
    @Bean
    public DefaultMQProducer defaultMQProducer() {
        DefaultMQProducer producer = new DefaultMQProducer(producerGroup);
        producer.setNamesrvAddr(nameServer);
        producer.setSendMsgTimeout(sendTimeout);
        producer.setMaxMessageSize(maxMessageSize);
        producer.setRetryTimesWhenSendAsyncFailed(2);
        producer.setRetryNextServer(true);
        
        try {
            producer.start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start RocketMQ producer", e);
        }
        
        return producer;
    }

    /**
     * 创建事务消息生产者
     */
    @Bean
    public TransactionMQProducer transactionMQProducer() {
        TransactionMQProducer producer = new TransactionMQProducer(producerGroup + "-transaction");
        producer.setNamesrvAddr(nameServer);
        producer.setSendMsgTimeout(sendTimeout);
        producer.setMaxMessageSize(maxMessageSize);
        
        try {
            producer.start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start RocketMQ transaction producer", e);
        }
        
        return producer;
    }
}
