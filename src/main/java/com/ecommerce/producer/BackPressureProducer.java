package com.ecommerce.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息积压处理生产者
 * 面试要点：消息积压处理、消息批量发送
 * 应对大量消息积压的策略：
 * 1. 批量发送减少网络开销
 * 2. 增加生产者数量
 * 3. 消息压缩
 * 4. 异步发送提高吞吐量
 * @author lvdaxianer
 * @date 2025-02-25
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BackPressureProducer {

    private final DefaultMQProducer defaultMQProducer;
    private final ObjectMapper objectMapper;

    @Value("${rocketmq.topic.order:order-topic}")
    private String orderTopic;

    /**
     * 批量发送消息
     * 适用于消息积压场景，减少网络开销
     *
     * @param messages 消息列表
     */
    public void batchSendMessages(List<Object> messages) {
        try {
            List<Message> rocketMsgs = new ArrayList<>();

            for (Object message : messages) {
                String json = objectMapper.writeValueAsString(message);
                Message rocketMsg = new Message(orderTopic, "BATCH", json.getBytes());
                rocketMsgs.add(rocketMsg);
            }

            var results = defaultMQProducer.send(rocketMsgs);

            log.info("Batch messages sent: count={}, success={}",
                    messages.size(), results.size());

        } catch (Exception e) {
            log.error("Failed to batch send messages", e);
            throw new RuntimeException("Failed to batch send messages", e);
        }
    }

    /**
     * 批量发送消息（带回调）
     */
    public void batchSendMessagesAsync(List<Object> messages) {
        try {
            List<Message> rocketMsgs = new ArrayList<>();

            for (Object message : messages) {
                String json = objectMapper.writeValueAsString(message);
                Message rocketMsg = new Message(orderTopic, "BATCH", json.getBytes());
                rocketMsgs.add(rocketMsg);
            }

            defaultMQProducer.send(rocketMsgs, new org.apache.rocketmq.client.producer.SendCallback() {
                @Override
                public void onSuccess(org.apache.rocketmq.client.producer.SendResult sendResult) {
                    log.info("Batch messages sent successfully: count={}, msgIds={}",
                            messages.size(), sendResult.getMsgIdList());
                }

                @Override
                public void onException(Throwable e) {
                    log.error("Batch messages sent failed: count={}", messages.size(), e);
                }
            });

        } catch (Exception e) {
            log.error("Failed to batch send messages async", e);
            throw new RuntimeException("Failed to batch send messages async", e);
        }
    }

    /**
     * 发送压缩消息
     * 适用于大消息或消息体较大的场景
     */
    public void sendCompressedMessage(Object message) {
        try {
            String json = objectMapper.writeValueAsString(message);

            // 实际生产中可以使用GZIP压缩
            byte[] body = json.getBytes();
            Message rocketMsg = new Message(orderTopic, "COMPRESSED", body);

            // 设置消息属性标记为压缩消息
            rocketMsg.putUserProperty("compressed", "true");

            defaultMQProducer.send(rocketMsg);
            log.info("Compressed message sent: originalSize={}, compressedSize={}",
                    json.length(), body.length);

        } catch (Exception e) {
            log.error("Failed to send compressed message", e);
            throw new RuntimeException("Failed to send compressed message", e);
        }
    }

    /**
     * 发送大消息（分片）
     * 适用于超长消息场景
     */
    public void sendLargeMessage(Object message, int maxSize) {
        try {
            String json = objectMapper.writeValueAsString(message);
            byte[] body = json.getBytes();

            if (body.length <= maxSize) {
                Message rocketMsg = new Message(orderTopic, "LARGE", body);
                defaultMQProducer.send(rocketMsg);
            } else {
                // 分片发送
                int totalChunks = (body.length + maxSize - 1) / maxSize;
                String msgId = java.util.UUID.randomUUID().toString();

                for (int i = 0; i < totalChunks; i++) {
                    int start = i * maxSize;
                    int end = Math.min(start + maxSize, body.length);
                    byte[] chunk = new byte[end - start];

                    System.arraycopy(body, start, chunk, 0, chunk.length);

                    Message rocketMsg = new Message(orderTopic, "CHUNK", chunk);
                    rocketMsg.putUserProperty("msgId", msgId);
                    rocketMsg.putUserProperty("chunkIndex", String.valueOf(i));
                    rocketMsg.putUserProperty("totalChunks", String.valueOf(totalChunks));

                    defaultMQProducer.send(rocketMsg);
                }

                log.info("Large message chunked: msgId={}, totalChunks={}, originalSize={}",
                        msgId, totalChunks, body.length);
            }

        } catch (Exception e) {
            log.error("Failed to send large message", e);
            throw new RuntimeException("Failed to send large message", e);
        }
    }

    /**
     * 高并发场景下的消息发送
     * 使用异步发送提高吞吐量
     */
    public void sendHighConcurrencyMessage(Object message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            Message rocketMsg = new Message(orderTopic, "HIGH_CONCURRENCY", json.getBytes());

            defaultMQProducer.send(rocketMsg, new org.apache.rocketmq.client.producer.SendCallback() {
                @Override
                public void onSuccess(org.apache.rocketmq.client.producer.SendResult sendResult) {
                    log.debug("High concurrency message sent: msgId={}", sendResult.getMsgId());
                }

                @Override
                public void onException(Throwable e) {
                    log.error("High concurrency message failed", e);
                }
            });

        } catch (Exception e) {
            log.error("Failed to send high concurrency message", e);
        }
    }
}
