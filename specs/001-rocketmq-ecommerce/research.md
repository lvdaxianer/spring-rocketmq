# Research: RocketMQ百万QPS电商场景技术调研

## 技术栈决策

### 1. RocketMQ版本选择

**Decision**: RocketMQ 5.x (最新稳定版5.1.4)

**Rationale**: 
- 5.x版本相比4.x有更好的性能优化，支持POP消费模式
- 原生支持延迟消息、事务消息、顺序消息等核心特性
- 与Spring Cloud Stream集成更完善

**Alternatives considered**: 
- RocketMQ 4.x - 功能成熟但性能略低
- Apache Kafka - 吞吐量更高但事务消息支持较弱

---

### 2. Spring Boot版本选择

**Decision**: Spring Boot 3.2.x (最新LTS版本)

**Rationale**:
- 支持Java 17+新特性
- 与Spring Cloud 2023.x版本兼容
- Spring Cloud Stream 4.x支持RocketMQ 5.x

**Alternatives considered**: 
- Spring Boot 2.7.x - 兼容性好但即将EOL

---

### 3. 百万QPS实现方案

**Decision**: 采用以下优化策略实现百万QPS：
- Producer端：批量发送、异步发送、连接池复用
- Consumer端：POP消费模式、并发消费
- Broker端：多队列、高性能磁盘IO

**Rationale**: RocketMQ 5.x的POP消费模式相比传统拉取模式有显著性能提升

---

### 4. Docker环境方案

**Decision**: 使用docker-compose部署RocketMQ 5.x集群

**Components**:
- RocketMQ NameServer (1节点)
- RocketMQ Broker (主节点)
- RocketMQ Console (可视化控制台)

**Rationale**: 满足本地开发测试需求，配置简单

---

## 关键技术点实现

### 1. 消息顺序性实现

- 使用OrderlyListenerContainerFactory配置顺序消费
- 同一订单ID的消息发送到同一队列
- 消费者按队列维度顺序处理

### 2. 事务消息实现

- 使用TransactionListener进行本地事务处理
- 实现executeLocalTransaction方法
- 实现checkLocalTransaction方法用于状态回查

### 3. 延迟消息实现

- 设置消息延迟级别（1s-2h共18级）
- 订单超时取消场景使用15分钟延迟

### 4. 消息过滤实现

- Tag过滤：简单场景，如"ORDER_CREATED"、"ORDER_PAID"
- SQL92过滤：复杂条件，如"amount > 100"

### 5. 死信队列实现

- 配置maxReconsumeTimes设置重试次数
- 超过重试次数后自动进入DLQ
- DLQ独立Topic存储

---

## 依赖版本清单

| 组件 | 版本 |
|------|------|
| Java | 17 |
| Spring Boot | 3.2.x |
| Spring Cloud Stream | 4.1.x |
| RocketMQ Client | 5.1.x |
| Maven | 3.9+ |
| Docker | 24.x |
| docker-compose | 2.x |

---

## 参考资料

- Apache RocketMQ官方文档: https://rocketmq.apache.org/docs/
- Spring Cloud Stream RocketMQ文档: https://spring.io/projects/spring-cloud-stream
- RocketMQ 5.x新特性: POP消费模式、性能优化
