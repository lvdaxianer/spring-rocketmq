# Quickstart: RocketMQ百万QPS电商场景

## 环境要求

| 组件 | 最低版本 |
|------|----------|
| Java | 17 |
| Maven | 3.9 |
| Docker | 24 |
| Docker Compose | 2 |

---

## 快速启动

### 步骤1: 启动RocketMQ本地环境

```bash
cd docker
docker-compose up -d
```

验证服务启动:
```bash
docker ps
# 应该看到 NameServer, Broker, Console 三个容器
```

访问控制台: http://localhost:8080

---

### 步骤2: 构建项目

```bash
mvn clean package -DskipTests
```

---

### 步骤3: 运行应用

```bash
java -jar target/rocketmq-ecommerce-demo.jar
```

---

## 功能测试

### 1. 创建订单 (消息顺序性测试)

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

预期: 订单创建成功，消息按顺序发送

---

### 2. 支付订单 (事务消息测试)

```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "<orderId>",
    "userId": "user001",
    "amount": 7999.00
  }'
```

预期: 支付成功，订单状态更新为已支付

---

### 3. 订单超时取消 (延迟消息测试)

订单创建后15分钟，系统自动取消未支付订单

查看延迟消息投递:
```bash
# 在RocketMQ Console观察消息投递延迟
```

---

### 4. 订单状态查询 (消息过滤测试)

按状态查询:
```bash
curl http://localhost:8080/api/orders?status=PENDING_PAYMENT
```

---

### 5. 消息重试与DLQ测试

模拟消费失败，观察重试和DLQ机制:
```bash
# 配置消费失败触发重试
# 超过最大重试次数后进入DLQ
```

---

## 性能测试

### 发送100万消息

```bash
# 使用压测脚本
./benchmark/send benchmark -t order-topic -c 1000000
```

监控指标:
- 吞吐量: 观察QPS
- 延迟: P99延迟
- 消息堆积: 积压数量

---

## 配置说明

### application.yml关键配置

```yaml
spring:
  rocketmq:
    name-server: localhost:9876
    producer:
      group: producer-group
      send-timeout: 3000
      max-message-size: 4194304
    consumer:
      group: consumer-group
      max-reconsume-times: 3
```

### Docker环境变量

```yaml
ROCKETMQ_NAME_SERVER: nameserver:9876
ROCKETMQ_BROKER_NAME: broker-a
ROCKETMQ_BROKER_PORT: 10911
```

---

## 常见问题

### 1. 消息发送失败

- 检查NameServer地址是否正确
- 检查Broker是否正常运行

### 2. 消息消费乱序

- 确认使用顺序消费者
- 同一订单ID的消息发往同一队列

### 3. 事务消息未提交

- 检查TransactionListener实现
- 确认本地事务执行成功
