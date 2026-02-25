# API Contracts: RocketMQ电商场景

## 1. 订单接口

### 1.1 创建订单

**Endpoint**: `POST /api/orders`

**Request**:
```json
{
  "userId": "user001",
  "productId": "product001",
  "productName": "iPhone 15",
  "quantity": 1,
  "amount": 7999.00
}
```

**Response** (201 Created):
```json
{
  "orderId": "ORD-20250225-001",
  "userId": "user001",
  "productId": "product001",
  "productName": "iPhone 15",
  "quantity": 1,
  "amount": 7999.00,
  "status": "PENDING_PAYMENT",
  "createdAt": "2025-02-25T10:00:00"
}
```

---

### 1.2 查询订单

**Endpoint**: `GET /api/orders/{orderId}`

**Response** (200 OK):
```json
{
  "orderId": "ORD-20250225-001",
  "userId": "user001",
  "productId": "product001",
  "productName": "iPhone 15",
  "quantity": 1,
  "amount": 7999.00,
  "status": "PAID",
  "createdAt": "2025-02-25T10:00:00",
  "updatedAt": "2025-02-25T10:05:00"
}
```

---

### 1.3 按状态查询订单列表

**Endpoint**: `GET /api/orders?status={status}`

**Query Parameters**:
- status: PENDING_PAYMENT | PAID | CANCELLED | TIMEOUT_CANCELLED

**Response** (200 OK):
```json
{
  "orders": [
    {
      "orderId": "ORD-20250225-001",
      "userId": "user001",
      "status": "PENDING_PAYMENT",
      "amount": 7999.00,
      "createdAt": "2025-02-25T10:00:00"
    }
  ],
  "total": 1
}
```

---

## 2. 支付接口

### 2.1 发起支付 (事务消息)

**Endpoint**: `POST /api/payments`

**Request**:
```json
{
  "orderId": "ORD-20250225-001",
  "userId": "user001",
  "amount": 7999.00
}
```

**Response** (200 OK):
```json
{
  "paymentId": "PAY-20250225-001",
  "orderId": "ORD-20250225-001",
  "userId": "user001",
  "amount": 7999.00,
  "status": "SUCCESS",
  "transactionId": "TXN-123456789",
  "paidAt": "2025-02-25T10:05:00"
}
```

---

## 3. 消息管理接口

### 3.1 发送延迟消息

**Endpoint**: `POST /api/messages/delayed`

**Request**:
```json
{
  "topic": "order-timeout-topic",
  "orderId": "ORD-20250225-001",
  "userId": "user001",
  "delayLevel": 5
}
```

**Response** (202 Accepted):
```json
{
  "messageId": "MSG-20250225-001",
  "status": "SCHEDULED"
}
```

---

### 3.2 查询消息轨迹

**Endpoint**: `GET /api/messages/trace/{messageId}`

**Response** (200 OK):
```json
{
  "messageId": "MSG-20250225-001",
  "topic": "order-topic",
  "tags": "ORDER_CREATED",
  "sendTime": "2025-02-25T10:00:00",
  "consumeTime": "2025-02-25T10:00:01",
  "status": "CONSUMED"
}
```

---

## 4. 错误响应

**4xx/5xx Error Response**:
```json
{
  "error": "INVALID_REQUEST",
  "message": "订单金额必须大于0",
  "timestamp": "2025-02-25T10:00:00"
}
```
