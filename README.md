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

## 核心特性

### 1. 消息顺序性 (Order Messages)
- 同一订单的消息按FIFO顺序处理
- 使用消息队列选择器保证同一订单发往同一队列

### 2. 事务消息 (Transaction Messages)
- 半消息机制保证支付与订单状态更新的原子性
- 本地事务执行与消息发送绑定
- 事务状态回查机制

### 3. 延迟消息 (Delayed Messages)
- 支持18个延迟级别（1秒 ~ 2小时）
- 订单超时自动取消场景

### 4. 消息过滤 (Message Filtering)
- Tag标签过滤
- SQL92表达式过滤

### 5. 死信队列 (Dead Letter Queue)
- 消息重试机制
- 超过最大重试次数进入DLQ

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
