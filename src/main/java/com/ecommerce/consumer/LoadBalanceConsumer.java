package com.ecommerce.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.rebalance.AllocateMessageQueueByMachineRoom;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;

/**
 * 消费者负载均衡演示
 * 面试要点：消费者负载均衡策略
 * RocketMQ支持多种分配策略：
 * 1. AllocateMessageQueueByMachineRoom: 按机房分配
 * 2. AllocateMessageQueueAveragely: 平均分配(默认)
 * 3. AllocateMessageQueueAveragelyByCircle: 环形分配
 * 4. AllocateMessageQueueByConfig: 配置分配
 * 5. AllocateMessageQueueConsistentHash: 一致性哈希
 * @author lvdaxianer
 * @date 2025-02-25
 */
@Slf4j
@Component
public class LoadBalanceConsumer {

    private final DefaultMQPushConsumer defaultMQPushConsumer;
    private final ObjectMapper objectMapper;

    @Value("${rocketmq.topic.order:order-topic}")
    private String orderTopic;

    @Value("${spring.rocketmq.consumer.group}")
    private String consumerGroup;

    public LoadBalanceConsumer(DefaultMQPushConsumer defaultMQPushConsumer, ObjectMapper objectMapper) {
        this.defaultMQPushConsumer = defaultMQPushConsumer;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void start() {
        try {
            defaultMQPushConsumer.subscribe(orderTopic, "*");

            // 设置消费起点
            defaultMQPushConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);

            // 设置负载均衡策略
            // 1. 平均分配（默认）
            // defaultMQPushConsumer.setAllocateMessageQueueStrategy(new AllocateMessageQueueAveragely());

            // 2. 环形分配 - 消费者依次分配队列
            // defaultMQPushConsumer.setAllocateMessageQueueStrategy(new AllocateMessageQueueAveragelyByCircle());

            // 3. 一致性哈希分配 - 相同消费者分配到相同队列
            // defaultMQPushConsumer.setAllocateMessageQueueStrategy(new AllocateMessageQueueConsistentHash());

            // 4. 按机房分配
            // defaultMQPushConsumer.setAllocateMessageQueueStrategy(new AllocateMessageQueueByMachineRoom());

            // 5. 自定义分配策略
            defaultMQPushConsumer.setAllocateMessageQueueStrategy((consumerGroup, currentCID, mqAll, consumerAll) -> {
                log.info("Custom load balance: consumerGroup={}, currentCID={}, mqAll.size={}, consumerAll.size={}",
                        consumerGroup, currentCID, mqAll.size(), consumerAll.size());

                // 自定义分配逻辑：将队列平均分配给消费者
                int index = consumerAll.indexOf(currentCID);
                int mod = mqAll.size() % consumerAll.size();

                int startIndex = index * (mqAll.size() / consumerAll.size());
                int count = mqAll.size() / consumerAll.size();

                if (index < mod) {
                    startIndex += index;
                    count += 1;
                } else if (index == mod) {
                    startIndex += mod;
                }

                for (int i = 0; i < count; i++) {
                    if (startIndex + i < mqAll.size()) {
                        log.debug("Allocated queue: cid={}, queue={}", currentCID, mqAll.get(startIndex + i));
                    }
                }

                return mqAll.subList(startIndex, startIndex + count);
            });

            defaultMQPushConsumer.registerMessageListener(new MessageListenerConcurrently() {
                @Override
                public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                                                             ConsumeConcurrentlyContext context) {
                    context.setAckIndex(0);

                    for (MessageExt msg : msgs) {
                        try {
                            String body = new String(msg.getBody());
                            log.info("Load balance consumer received: msgId={}, queueId={}, topic={}",
                                    msg.getMsgId(), msg.getQueueId(), msg.getTopic());

                        } catch (Exception e) {
                            log.error("Failed to process message: msgId={}", msg.getMsgId(), e);
                            return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                        }
                    }
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
            });

            defaultMQPushConsumer.start();
            log.info("Load balance consumer started, topic: {}, group: {}", orderTopic, consumerGroup);

        } catch (Exception e) {
            log.error("Failed to start load balance consumer", e);
            throw new RuntimeException("Failed to start load balance consumer", e);
        }
    }

    @PreDestroy
    public void stop() {
        if (defaultMQPushConsumer != null) {
            defaultMQPushConsumer.shutdown();
            log.info("Load balance consumer stopped");
        }
    }
}
