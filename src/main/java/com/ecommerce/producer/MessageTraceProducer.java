package com.ecommerce.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 消息轨迹追踪生产者
 * 面试要点：消息轨迹追踪、消息全链路追踪
 * 实现方式：消息发送时记录关键节点信息
 * @author lvdaxianer
 * @date 2025-02-25
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageTraceProducer {

    private final DefaultMQProducer defaultMQProducer;
    private final ObjectMapper objectMapper;

    @Value("${rocketmq.topic.order:order-topic}")
    private String orderTopic;

    /**
     * 消息轨迹记录
     */
    public static class MessageTrace {
        private String traceId;
        private String msgId;
        private String topic;
        private String tags;
        private long sendTime;
        private long consumeTime;
        private String status;
        private String errorMsg;

        public MessageTrace(String traceId, String msgId, String topic, String tags) {
            this.traceId = traceId;
            this.msgId = msgId;
            this.topic = topic;
            this.tags = tags;
            this.sendTime = System.currentTimeMillis();
        }

        public void markConsumed() {
            this.consumeTime = System.currentTimeMillis();
            this.status = "CONSUMED";
        }

        public void markFailed(String errorMsg) {
            this.status = "FAILED";
            this.errorMsg = errorMsg;
        }

        public String getTraceId() {
            return traceId;
        }

        public String getMsgId() {
            return msgId;
        }

        public String getTopic() {
            return topic;
        }

        public String getTags() {
            return tags;
        }

        public long getSendTime() {
            return sendTime;
        }

        public long getConsumeTime() {
            return consumeTime;
        }

        public String getStatus() {
            return status;
        }

        public String getErrorMsg() {
            return errorMsg;
        }

        public long getLatency() {
            if (consumeTime > 0) {
                return consumeTime - sendTime;
            }
            return 0;
        }
    }

    /**
     * 发送带轨迹追踪的消息
     *
     * @param message 消息内容
     * @param tags    标签
     * @return 轨迹ID
     */
    public String sendMessageWithTrace(Object message, String tags) {
        try {
            String traceId = UUID.randomUUID().toString();
            String json = objectMapper.writeValueAsString(message);

            Message rocketMsg = new Message(orderTopic, tags, json.getBytes());
            rocketMsg.setKeys(traceId);

            var result = defaultMQProducer.send(rocketMsg);

            log.info("Message sent with trace: traceId={}, msgId={}, topic={}, tags={}",
                    traceId, result.getMsgId(), orderTopic, tags);

            return traceId;

        } catch (Exception e) {
            log.error("Failed to send message with trace", e);
            throw new RuntimeException("Failed to send message with trace", e);
        }
    }

    /**
     * 发送订单创建消息并记录轨迹
     */
    public String sendOrderMessageWithTrace(Object message) {
        return sendMessageWithTrace(message, "ORDER_CREATED");
    }

    /**
     * 构建消息轨迹信息
     */
    public MessageTrace buildTrace(String traceId, String msgId, String topic, String tags) {
        return new MessageTrace(traceId, msgId, topic, tags);
    }

    /**
     * 记录消费完成的轨迹
     */
    public void recordConsumeSuccess(MessageTrace trace) {
        trace.markConsumed();
        log.info("Message consumed: traceId={}, latency={}ms",
                trace.getTraceId(), trace.getLatency());
    }

    /**
     * 记录消费失败的轨迹
     */
    public void recordConsumeFailed(MessageTrace trace, String errorMsg) {
        trace.markFailed(errorMsg);
        log.error("Message consume failed: traceId={}, error={}",
                trace.getTraceId(), errorMsg);
    }
}
