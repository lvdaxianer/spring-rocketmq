# Feature Specification: RocketMQ百万QPS电商场景

**Feature Branch**: `001-rocketmq-ecommerce`  
**Created**: 2025-02-25  
**Status**: Draft  
**Input**: User description: "要实现一个百万qps的电商场景,核心业务是下单,以及支付. 下单是为了模拟出 mq的消息顺序性 支付为了模拟mq的事务消息 但是根据rocketmq的特性 可能需要别的场景 同时 需要编写一个docker 能让我本地直接跑起来 rocketmq 服务"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 订单创建场景 (Priority: P1)

用户通过电商平台创建订单，系统需要保证同一订单的消息严格按照创建顺序处理，确保订单状态一致性。

**Why this priority**: 订单是电商交易的核心，消息顺序性直接影响订单状态准确性，是交易链路的最基础环节

**Independent Test**: 可以通过模拟单用户下单流程验证订单创建成功、库存扣减、订单状态更新完整链路

**Acceptance Scenarios**:

1. **Given** 用户选择商品并提交订单, **When** 系统创建订单并发送消息, **Then** 消息按FIFO顺序被消费者处理，订单状态正确更新
2. **Given** 同一用户的多个订单, **When** 并发下单, **Then** 每个订单的消息被顺序处理，无乱序
3. **Given** 订单消息处理失败, **When** 重试机制触发, **Then** 消息被重新投递，保证最终一致性

---

### User Story 2 - 支付交易场景 (Priority: P1)

用户完成支付，系统需要通过事务消息确保支付与订单状态更新的原子性，避免出现支付成功但订单未更新的情况。

**Why this priority**: 支付是资金交易的核心场景，事务消息能保证支付与业务操作的原子性，是金融级应用的关键要求

**Independent Test**: 可以通过模拟支付成功/失败场景验证订单状态与支付状态的一致性

**Acceptance Scenarios**:

1. **Given** 用户发起支付请求, **When** 支付成功, **Then** 本地事务提交，订单状态更新为已支付，消息被标记为已处理
2. **Given** 用户发起支付请求, **When** 支付失败或超时, **Then** 本地事务回滚，订单状态保持不变，无不一致数据
3. **Given** 事务消息提交后, **When** 消费者处理失败, **Then** 系统进行事务状态检查，确保最终一致性

---

### User Story 3 - 订单超时取消场景 (Priority: P2)

系统通过延迟消息实现订单超时自动取消功能，提高库存释放效率。

**Why this priority**: 提升用户体验和库存周转率，避免用户长时间占用库存但不付款

**Independent Test订单创建后超时**: 可以通过模拟场景验证订单自动取消和库存恢复

**Acceptance Scenarios**:

1. **Given** 用户创建订单后未支付, **When** 超过设定时间（如15分钟）, **Then** 系统发送延迟消息，订单被自动取消，库存被释放
2. **Given** 订单支付成功, **When** 延迟消息到达, **Then** 延迟消息被忽略，订单状态保持不变

---

### User Story 4 - 订单状态查询场景 (Priority: P2)

用户或管理员通过标签过滤查询特定状态的订单，实现精准的订单管理。

**Why this priority**: 支持订单状态筛选和批量处理，提高运营效率

**Independent Test**: 可以通过模拟查询特定状态订单验证过滤功能正确性

**Acceptance Scenarios**:

1. **Given** 存在多种状态的订单, **When** 用户查询待支付订单, **Then** 只返回状态为待支付的订单
2. **Given** 存在多种状态的订单, **When** 用户使用SQL表达式查询复杂条件, **Then** 满足条件的订单被正确返回

---

### User Story 5 - 消息可靠投递与死信队列场景 (Priority: P3)

系统确保消息在消费失败后进入重试队列，多次失败后进入死信队列，保证消息不丢失且可追溯。

**Why this priority**: 保证消息处理的可靠性，支持失败消息的追溯和人工处理

**Independent Test**: 可以通过模拟消息消费失败场景验证重试和死信队列机制

**Acceptance Scenarios**:

1. **Given** 消息处理首次失败, **When** 触发重试机制, **Then** 消息进入重试队列，按照配置的时间间隔重新投递
2. **Given** 消息重试次数超过上限, **When** 再次失败, **Then** 消息进入死信队列，不再被正常消费
3. **Given** 死信队列中存在消息, **When** 运维人员查询, **Then** 可以查看失败原因并进行人工处理

---

### User Story 6 - Docker本地RocketMQ环境 (Priority: P1)

提供可本地运行的Docker Compose配置，快速搭建RocketMQ开发测试环境。

**Why this priority**: 降低开发环境搭建门槛，支持本地开发和调试

**Independent Test**: 可以通过执行Docker命令启动服务并验证各组件正常运行

**Acceptance Scenarios**:

1. **Given** 用户执行Docker启动命令, **When** 环境启动, **Then** NameServer、Broker、Console服务均正常运行
2. **Given** RocketMQ服务正常运行, **When** 用户发送测试消息, **Then** 消息可以正常发送和消费

---

### Edge Cases

- NameServer或Broker宕机时，消息发送失败但不能丢失
- 高并发情况下消息积压的监控和告警
- 消费者实例数量变化时的负载均衡
- 网络分区时的消息重复消费风险控制

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 系统 MUST 支持消息顺序性，确保同一订单的消息按创建顺序处理
- **FR-002**: 系统 MUST 支持事务消息，保证支付与订单状态更新的原子性
- **FR-003**: 系统 MUST 支持延迟消息，实现订单超时自动取消功能
- **FR-004**: 系统 MUST 支持消息过滤（Tag和SQL92），实现精准的订单状态查询
- **FR-005**: 系统 MUST 支持消息重试机制和死信队列，保证消息可靠投递
- **FR-006**: 系统 MUST 提供Docker Compose配置，支持本地一键启动RocketMQ服务
- **FR-007**: 系统 MUST 支持百万QPS的消息收发，满足高并发场景需求
- **FR-008**: 系统 MUST 提供消息轨迹追踪能力，支持消息全链路监控

### Key Entities *(include if feature involves data)*

- **订单(Order)**: 包含订单ID、用户ID、商品信息、订单状态、创建时间等
- **支付(Payment)**: 包含支付ID、订单ID、支付金额、支付状态、支付时间等
- **消息(Message)**: 包含消息ID、Topic、Tag、消息体、发送时间、重试次数等

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 订单创建消息的处理延迟控制在100ms以内
- **SC-002**: 系统支持每秒处理100万条消息的吞吐量
- **SC-003**: 事务消息的成功率达到99.99%以上，确保支付与订单状态一致
- **SC-004**: 延迟消息的投递精度在±1秒以内
- **SC-005**: Docker环境启动时间不超过2分钟
- **SC-006**: 消息重复消费率控制在0.01%以下
