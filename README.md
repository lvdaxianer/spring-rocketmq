# RocketMQ 百万QPS电商场景

[English](./README_EN.md)

## 概述

本项目实现了一个基于 RocketMQ 5.x + Spring Boot 3.x 的百万QPS电商场景，涵盖消息顺序性、事务消息、延迟消息、消息过滤和死信队列等核心特性。

## 技术栈

| 技术 | 版本 |
|------|------|
| Java | 17+ |
| Spring Boot | 3.2.5 |
| RocketMQ | 5.1.4 |
| H2 Database | - |

## 核心特性及代码实现

### 1. 消息顺序性 (Order Messages)

| 实现环节 | 代码位置 | 说明 |
|---------|---------|------|
| 生产端顺序发送 | `producer/OrderProducer.java:41-49` | 使用`MessageQueueSelector`通过orderId hash分配队列 |
| 消费端顺序消费 | `consumer/OrderConsumer.java:43-67` | 使用`MessageListenerOrderly`监听器 |
| 配置 | `config/RocketMQProducerConfig.java` | `DefaultMQProducer`实例 |

```java
// 生产端：使用MessageQueueSelector保证同一订单发往同一队列
defaultMQProducer.send(rocketMsg, new MessageQueueSelector() {
    @Override
    public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
        String orderId = (String) arg;
        int hash = Math.abs(orderId.hashCode()) % mqs.size();
        return mqs.get(hash);
    }
}, message.getOrderId());
```

### 2. 事务消息 (Transaction Messages)

| 实现环节 | 代码位置 | 说明 |
|---------|---------|------|
| 事务生产者 | `config/RocketMQProducerConfig.java:52-66` | `TransactionMQProducer`配置 |
| 发送事务消息 | `producer/TransactionOrderProducer.java:32-47` | `sendMessageInTransaction()` |
| 本地事务执行 | `listener/PaymentTransactionListener.java:36-60` | `executeLocalTransaction()` |
| 事务状态回查 | `listener/PaymentTransactionListener.java:66-96` | `checkLocalTransaction()` |

### 3. 延迟消息 (Delayed Messages)

| 实现环节 | 代码位置 | 说明 |
|---------|---------|------|
| 延迟消息发送 | `producer/DelayMessageProducer.java:37-52` | `setDelayTimeLevel()` |
| 延迟级别定义 | `producer/DelayMessageProducer.java:32-36` | 18个延迟级别(1秒~2小时) |
| 订单超时场景 | `producer/DelayMessageProducer.java:57-60` | 默认5级(1分钟) |

```java
// 设置延迟级别
rocketMsg.setDelayTimeLevel(delayLevel);
// 延迟级别: 1=1秒, 2=5秒, 3=10秒, 4=30秒, 5=1分钟, ... 18=2小时
```

### 4. 消息过滤 (Message Filtering)

| 过滤方式 | 代码位置 | 说明 |
|---------|---------|------|
| Tag标签过滤 | `consumer/OrderConsumer.java:41` | `subscribe(topic, "ORDER_CREATED")` |
| Tag标签过滤 | `consumer/PaymentConsumer.java:38` | `subscribe(topic, "PAYMENT_SUCCESS")` |
| SQL92表达式 | `consumer/OrderConsumer.java` | 设置`consumer.setExpressionType("SQL92")` |

```java
// Tag过滤：只订阅特定标签的消息
defaultMQPushConsumer.subscribe(orderTopic, "ORDER_CREATED");

// SQL过滤：支持属性过滤（需在消息中设置属性）
defaultMQPushConsumer.subscribe(topic, "(TAGS is not null AND TAGS LIKE 'TAG%')");
```

### 5. 死信队列 (Dead Letter Queue)

| 实现环节 | 代码位置 | 说明 |
|---------|---------|------|
| 死信消费者 | `consumer/DeadLetterConsumer.java` | 监听`%DLQ%`主题 |
| 最大重试配置 | `config/RocketMQConsumerConfig.java:29-30` | `maxReconsume-times: 3` |
| 重试次数获取 | `consumer/DeadLetterConsumer.java:78-84` | `msg.getReconsumeTimes()` |

### 6. 消息可靠性

| 可靠性保障 | 代码位置 | 说明 |
|-----------|---------|------|
| 同步发送 | `producer/OrderProducer.java:61-72` | `send()`阻塞等待结果 |
| 异步发送 | `producer/OrderProducer.java:77-99` | `SendCallback`回调 |
| 发送重试 | `config/RocketMQProducerConfig.java:38` | `retryTimesWhenSendAsyncFailed(2)` |
| 消费重试 | `config/RocketMQConsumerConfig.java:29-30` | `maxReconsumeTimes`配置 |
| 发送超时 | `config/RocketMQProducerConfig.java:23-24` | `sendTimeout: 3000` |

### 7. 消费者并发配置

| 配置项 | 代码位置 | 说明 |
|-------|---------|------|
| 最小线程数 | `config/RocketMQConsumerConfig.java:32-33` | `consumeThreadMin: 20` |
| 最大线程数 | `config/RocketMQConsumerConfig.java:35-36` | `consumeThreadMax: 64` |
| 批量消费大小 | `config/RocketMQConsumerConfig.java:38-39` | `consumeMessageBatchMaxSize: 1` |

---

## 架构师面试要点：RocketMQ 20个核心问题

### 1. 消息顺序性

| 面试问题 | 答案要点 | 代码实现 |
|---------|---------|---------|
| 如何保证消息顺序？ | 生产端：MessageQueueSelector按orderId hash分配到同一队列<br>消费端：MessageListenerOrderly顺序消费 | `producer/OrderProducer.java:41-49`<br>`consumer/OrderConsumer.java:43-67` |
| 全局顺序vs局部顺序？ | 全局顺序：单队列单消费者，吞吐量低<br>局部顺序：同一业务key（如orderId）顺序，主流方案 | - |

### 2. 消息重复消费

| 面试问题 | 答案要点 | 代码实现 |
|---------|---------|---------|
| 消息重复消费的原因？ | 1.消费成功但ack失败<br>2.消费者重启<br>3.消息投递重试 | - |
| 如何实现幂等？ | 1.唯一键去重<br>2.业务状态检查<br>3.分布式锁 | `consumer/IdempotenceConsumer.java` |

```java
// 幂等性检查实现
private boolean checkAndRecord(String msgId) {
    Long previous = processedCache.get(msgId);
    if (previous != null) return false;
    processedCache.put(msgId, System.currentTimeMillis());
    return true;
}
```

### 3. 消息可靠性

| 面试问题 | 答案要点 | 代码实现 |
|---------|---------|---------|
| 如何保证消息不丢失？ | 生产端：同步发送+重试<br>Broker：同步刷盘<br>消费端：手动ack | `producer/OrderProducer.java:61-72`<br>`config/RocketMQProducerConfig.java` |
| 消息发送有几种方式？ | 1.同步send()<br>2.异步send()<br>3.单向sendOneway() | `producer/OrderProducer.java` |

### 4. 事务消息

| 面试问题 | 答案要点 | 代码实现 |
|---------|---------|---------|
| 事务消息原理？ | 半消息+本地事务+回查机制 | `producer/TransactionOrderProducer.java:32-47` |
| 事务消息三状态？ | COMMIT/ROLLBACK/UNKNOWN | `listener/PaymentTransactionListener.java` |

### 5. 延迟消息

| 面试问题 | 答案要点 | 代码实现 |
|---------|---------|---------|
| 延迟消息实现？ | 设置delayTimeLevel，消息进入延迟队列 | `producer/DelayMessageProducer.java:43` |
| 延迟级别有哪些？ | 18个级别：1s~2h | `producer/DelayMessageProducer.java:32-36` |

### 6. 消息积压

| 面试问题 | 答案要点 | 代码实现 |
|---------|---------|---------|
| 消息积压原因？ | 消费能力不足、突发流量 | - |
| 如何处理积压？ | 1.扩容消费者<br>2.批量消费<br>3.消息压缩 | `producer/BackPressureProducer.java` |

### 7. 消费者负载均衡

| 面试问题 | 答案要点 | 代码实现 |
|---------|---------|---------|
| 负载均衡策略？ | 1.平均分配<br>2.环形分配<br>3.一致性哈希<br>4.自定义 | `consumer/LoadBalanceConsumer.java:68-90` |
| rebalance触发时机？ | 消费者增减、队列数变化 | - |

```java
// 自定义负载均衡策略
defaultMQPushConsumer.setAllocateMessageQueueStrategy((consumerGroup, currentCID, mqAll, consumerAll) -> {
    int index = consumerAll.indexOf(currentCID);
    // 自定义分配逻辑
    return mqAll.subList(startIndex, startIndex + count);
});
```

### 8. 消息过滤

| 面试问题 | 答案要点 | 代码实现 |
|---------|---------|---------|
| Tag过滤？ | subscribe(topic, "TAG1 \|\| TAG2") | `consumer/OrderConsumer.java:41` |
| SQL92过滤？ | 设置expressionType为SQL92 | `consumer/OrderConsumer.java` |

### 9. 死信队列

| 面试问题 | 答案要点 | 代码实现 |
|---------|---------|---------|
| DLQ触发条件？ | 超过最大重试次数 | `consumer/DeadLetterConsumer.java` |
| 如何处理死信？ | 人工处理/重新投递 | - |

### 10. 消息存储机制

| 面试问题 | 答案要点 |
|---------|---------|
| 存储结构？ | CommitLog(物理存储)+ConsumeQueue(逻辑队列)+IndexFile(索引) |
| 消息写入方式？ | 顺序写入+mmap内存映射 |

### 11. 刷盘机制

| 面试问题 | 答案要点 |
|---------|---------|
| 同步刷盘？ | 消息写入内存后立即刷到磁盘，可靠性高，吞吐量低 |
| 异步刷盘？ | 批量写入内存，定时刷盘，吞吐量高，可能丢消息 |

### 12. 主从复制

| 面试问题 | 答案要点 |
|---------|---------|
| 主从模式？ | Master负责读写，Slave同步/异步复制 |
| 复制模式？ | 同步复制（可靠性高）、异步复制（延迟低） |

### 13. 高可用架构

| 面试问题 | 答案要点 |
|---------|---------|
| NameServer集群？ | 无状态，多节点部署 |
| Broker集群？ | 多Master、多Master-Slave |

### 14. 消息优先级

| 面试问题 | 答案要点 | 代码实现 |
|---------|---------|---------|
| RocketMQ支持优先级？ | 不支持内置优先级，需通过多队列模拟 | `producer/PriorityMessageProducer.java` |

```java
// 优先级实现：通过不同队列
int queueIndex = priority.getLevel();
defaultMQProducer.send(rocketMsg, selector, queueIndex);
```

### 15. 消息轨迹追踪

| 面试问题 | 答案要点 | 代码实现 |
|---------|---------|---------|
| 如何追踪消息？ | 1.消息ID+traceId<br>2.消息属性记录<br>3.独立trace系统 | `producer/MessageTraceProducer.java` |

### 16. 消息消费模式

| 面试问题 | 答案要点 |
|---------|---------|
| Push vs Pull？ | Push：实时性好，复杂度高<br>Pull：可控性好，延迟稍高 |

### 17. 消息幂等性

| 面试问题 | 答案要点 | 代码实现 |
|---------|---------|---------|
| 幂等方案？ | 1.数据库唯一键<br>2.去重表<br>3.分布式锁 | `consumer/IdempotenceConsumer.java` |

### 18. 消息堆积处理

| 面试问题 | 答案要点 |
|---------|---------|
| 堆积处理策略？ | 1.临时扩容消费者<br>2.跳过积压消息<br>3.清理过期消息 |

### 19. 消费者并发配置

| 面试问题 | 答案要点 | 代码实现 |
|---------|---------|---------|
| 如何调整并发？ | consumeThreadMin/Max配置 | `config/RocketMQConsumerConfig.java:32-36` |

### 20. 生产环境注意事项

| 注意事项 | 说明 |
|---------|------|
| 集群部署 | 多Broker+多NameServer |
| 监控告警 | RocketMQ Exporter+Prometheus+Grafana |
| 容量规划 | 根据QPS估算Broker和磁盘IO |
| 消息清理 | 合理配置消息保留时间 |
| 权限控制 | 启用ACL访问控制 |

---

## 架构师视角：RocketMQ 关键问题与实现

作为架构师，在生产环境中使用RocketMQ时需要关注以下核心问题：

### 1. 消息可靠性保障

| 问题 | 解决方案 | 实现位置 |
|------|---------|---------|
| 消息发送失败 | 同步发送 + 重试机制 | `OrderProducer.sendOrderCreatedMessageSync()` |
| 消息发送超时 | 设置合理超时时间 | `application.yml: send-timeout: 3000` |
| 消费失败重试 | 消息重试队列 | `RocketMQConsumerConfig: maxReconsume-times: 3` |
| 消息积压监控 | 监控消费延迟 | Console监控 + 告警 |

### 2. 消息顺序性保证

| 问题 | 解决方案 | 实现位置 |
|------|---------|---------|
| 全局顺序 | 单队列 + 单消费者 | 特定场景使用 |
| 局部顺序 | Hash取模分配队列 | `OrderProducer.MessageQueueSelector` |
| 消费乱序 | 顺序消费者 | `OrderConsumer.MessageListenerOrderly` |

```java
// 关键实现：使用MessageQueueSelector保证同一订单发往同一队列
defaultMQProducer.send(rocketMsg, new MessageQueueSelector() {
    @Override
    public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
        String orderId = (String) arg;
        int hash = Math.abs(orderId.hashCode()) % mqs.size();
        return mqs.get(hash);
    }
}, message.getOrderId());
```

### 3. 事务消息核心问题

| 问题 | 解决方案 | 实现位置 |
|------|---------|---------|
| 本地事务失败 | 事务回滚 | `PaymentTransactionListener.executeLocalTransaction()` |
| 事务状态未知 | 定时回查 | `PaymentTransactionListener.checkLocalTransaction()` |
| 事务消息堆积 | 限制并发 | `TransactionMQProducer`配置 |

### 4. 消息重复消费

| 问题 | 解决方案 | 实现位置 |
|------|---------|---------|
| 消费端幂等 | 唯一键去重 | 业务层实现 |
| 消息去重 | 消息ID+业务状态检查 | `OrderConsumer` |
| 分布式锁 | Redis/ZK分布式锁 | 扩展实现 |

### 5. 消息积压与背压

| 问题 | 解决方案 | 实现位置 |
|------|---------|---------|
| 消费能力不足 | 动态调整消费线程 | `RocketMQConsumerConfig` |
| 突发流量 | 限流 + 削峰 | 扩展实现 |
| 消息堆积 | 扩容消费节点 | Docker集群部署 |

### 6. 高可用架构

| 场景 | 方案 | 配置 |
|------|------|------|
| Broker主从 | 主从复制 | `broker.conf: brokerRole=SLAVE` |
| NameServer集群 | 多节点部署 | `docker-compose.yml` |
| Producer高可用 | 失败重试+重试下一服务器 | `setRetryTimesWhenSendAsyncFailed(2)` |

### 7. 性能优化参数

```yaml
# Producer优化
spring:
  rocketmq:
    producer:
      send-timeout: 3000              # 发送超时3秒
      max-message-size: 4194304       # 最大消息4MB
      retry-times-when-send-async-failed: 2  # 异步失败重试2次
      
# Consumer优化
spring:
  rocketmq:
    consumer:
      consume-thread-min: 20           # 最小消费线程
      consume-thread-max: 64           # 最大消费线程
      max-reconsume-times: 3           # 最大重试次数
      consume-message-batch-max-size: 1 # 批量消费大小
```

---

## 快速开始

### 1. 启动RocketMQ环境

```bash
cd docker
docker-compose up -d
```

验证服务启动：
```bash
docker ps
# 应该看到 NameServer, Broker, Console 三个容器
```

访问控制台：http://localhost:8080

### 2. 构建项目

```bash
mvn clean package -DskipTests
```

### 3. 运行应用

```bash
java -jar target/spring-rocketmq-1.0.0-SNAPSHOT.jar
```

## API接口

### 订单接口

#### 创建订单
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user001",
    "productId": "product001",
    "productName": "iPhone 15",
    "quantity": 1,
    "amount": 7999.00
  }'
```

#### 查询订单
```bash
curl http://localhost:8080/api/orders/{orderId}
```

#### 按状态查询订单
```bash
curl "http://localhost:8080/api/orders?status=PENDING_PAYMENT"
```

### 支付接口

#### 发起支付
```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "<orderId>",
    "userId": "user001",
    "amount": 7999.00
  }'
```

## 项目结构

```
src/main/java/com/ecommerce/
├── controller/          # REST API控制器
├── service/            # 业务服务层
├── model/              # 数据模型
├── config/             # RocketMQ配置
├── consumer/           # 消息消费者
├── producer/           # 消息生产者
├── listener/           # 事务监听器
└── repository/         # 数据访问层

docker/
├── docker-compose.yml  # Docker Compose配置
├── broker.conf          # Broker配置
└── start.sh            # 启动脚本

src/test/java/com/ecommerce/
├── service/            # 单元测试
└── integration/        # 高并发集成测试
```

## 测试

```bash
# 运行所有测试
mvn test

# 运行高并发测试
mvn test -Dtest=HighConcurrencyTest
```

### 高并发测试结果

| 测试场景 | 结果 |
|---------|------|
| 全链路压测 | 6600+ QPS |
| 突发流量 | 20000 QPS |
| 订单创建吞吐 | 19000+ QPS |

## 性能指标

| 指标 | 目标值 |
|------|--------|
| 消息处理延迟 | < 100ms |
| 吞吐量 | 100万QPS |
| 事务消息成功率 | ≥ 99.99% |
| 延迟消息精度 | ±1秒 |

## 生产环境注意事项

1. **集群部署**：使用多Broker + 多NameServer架构
2. **监控告警**：配置RocketMQ Exporter + Prometheus + Grafana
3. **容量规划**：根据QPS估算Broker和磁盘IO
4. **消息清理**：配置合理的消息保留时间
5. **权限控制**：启用ACL访问控制

## License

MIT
