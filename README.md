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
```

## 测试

```bash
# 运行单元测试
mvn test
```

## 性能指标

| 指标 | 目标值 |
|------|--------|
| 消息处理延迟 | < 100ms |
| 吞吐量 | 100万QPS |
| 事务消息成功率 | ≥ 99.99% |
| 延迟消息精度 | ±1秒 |

## License

MIT
