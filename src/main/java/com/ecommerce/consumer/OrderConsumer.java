package com.ecommerce.consumer;

import com.ecommerce.model.OrderStatus;
import com.ecommerce.model.message.OrderCreatedMessage;
import com.ecommerce.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;

/**
 * 订单消息消费者 - 顺序消费
 * @author lvdaxianer
 * @date 2025-02-25
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConsumer {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;
    private final DefaultMQPushConsumer defaultMQPushConsumer;

    @Value("${rocketmq.topic.order:order-topic}")
    private String orderTopic;

    @PostConstruct
    public void start() {
        try {
            defaultMQPushConsumer.subscribe(orderTopic, "ORDER_CREATED");
            
            defaultMQPushConsumer.registerMessageListener(new MessageListenerOrderly() {
                @Override
                public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs, 
                                                          ConsumeOrderlyContext context) {
                    context.setAutoCommit(true);
                    
                    for (MessageExt msg : msgs) {
                        try {
                            String body = new String(msg.getBody());
                            OrderCreatedMessage orderMessage = objectMapper.readValue(body, OrderCreatedMessage.class);
                            
                            log.info("Received order created message: orderId={}, msgId={}", 
                                    orderMessage.getOrderId(), msg.getMsgId());
                            
                            // 处理订单创建消息
                            processOrderCreatedMessage(orderMessage);
                            
                        } catch (Exception e) {
                            log.error("Failed to process order message: msgId={}", msg.getMsgId(), e);
                            return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
                        }
                    }
                    
                    return ConsumeOrderlyStatus.SUCCESS;
                }
            });
            
            defaultMQPushConsumer.start();
            log.info("Order consumer started, topic: {}", orderTopic);
            
        } catch (Exception e) {
            log.error("Failed to start order consumer", e);
            throw new RuntimeException("Failed to start order consumer", e);
        }
    }

    @PreDestroy
    public void stop() {
        if (defaultMQPushConsumer != null) {
            defaultMQPushConsumer.shutdown();
            log.info("Order consumer stopped");
        }
    }

    /**
     * 处理订单创建消息
     */
    private void processOrderCreatedMessage(OrderCreatedMessage message) {
        log.info("Processing order: orderId={}, userId={}, amount={}", 
                message.getOrderId(), message.getUserId(), message.getAmount());
        
        // 业务处理逻辑：更新订单状态、发送通知等
        // 这里可以添加库存扣减、优惠券计算等业务逻辑
        
        log.info("Order processed successfully: orderId={}", message.getOrderId());
    }
}
