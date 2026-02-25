# Data Model: RocketMQ百万QPS电商场景

## 核心实体

### 1. Order (订单)

| 字段 | 类型 | 说明 |
|------|------|------|
| orderId | String | 订单唯一ID (UUID) |
| userId | String | 用户ID |
| productId | String | 商品ID |
| productName | String | 商品名称 |
| quantity | Integer | 数量 |
| amount | BigDecimal | 订单金额 |
| status | OrderStatus | 订单状态 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |

**OrderStatus枚举**:
- PENDING_PAYMENT (待支付)
- PAID (已支付)
- CANCELLED (已取消)
- TIMEOUT_CANCELLED (超时取消)

---

### 2. Payment (支付)

| 字段 | 类型 | 说明 |
|------|------|------|
| paymentId | String | 支付唯一ID |
| orderId | String | 关联订单ID |
| userId | String | 用户ID |
| amount | BigDecimal | 支付金额 |
| status | PaymentStatus | 支付状态 |
| transactionId | String | 第三方交易号 |
| paidAt | LocalDateTime | 支付时间 |
| createdAt | LocalDateTime | 创建时间 |

**PaymentStatus枚举**:
- PENDING (待支付)
- SUCCESS (支付成功)
- FAILED (支付失败)
- REFUNDED (已退款)

---

### 3. RocketMQ消息模型

#### 3.1 订单创建消息 (OrderCreatedMessage)

| 字段 | 类型 | 说明 |
|------|------|------|
| orderId | String | 订单ID |
| userId | String | 用户ID |
| amount | BigDecimal | 订单金额 |
| createdAt | Long | 创建时间戳 |
| tags | String | 消息标签 |

**Topic**: `order-topic`
**Tag**: `ORDER_CREATED`

#### 3.2 支付成功消息 (PaymentSuccessMessage)

| 字段 | 类型 | 说明 |
|------|------|------|
| paymentId | String | 支付ID |
| orderId | String | 订单ID |
| amount | BigDecimal | 支付金额 |
| transactionId | String | 交易号 |
| paidAt | Long | 支付时间戳 |

**Topic**: `payment-topic`
**Tag**: `PAYMENT_SUCCESS`

#### 3.3 订单超时消息 (OrderTimeoutMessage)

| 字段 | 类型 | 说明 |
|------|------|------|
| orderId | String | 订单ID |
| userId | String | 用户ID |
| createdAt | Long | 订单创建时间 |

**Topic**: `order-timeout-topic`

---

## 关系图

```
Order (1) ---> (1) Payment
     |
     +---> (N) OrderCreatedMessage (RocketMQ)
     |
     +---> (N) PaymentSuccessMessage (RocketMQ)
     |
     +---> (N) OrderTimeoutMessage (RocketMQ)
```

---

## 验证规则

### Order验证
- orderId: 非空，UUID格式
- userId: 非空
- amount: > 0
- quantity: > 0

### Payment验证
- paymentId: 非空，UUID格式
- orderId: 非空
- amount: > 0
- status: 有效枚举值

### 消息验证
- Topic: 非空
- 消息体: JSON序列化
- 延迟级别: 0-18 (0表示无延迟)
