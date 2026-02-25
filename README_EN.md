# RocketMQ Million QPS E-commerce Demo

[中文](./README.md)

## Overview

This project implements a million QPS e-commerce scenario based on RocketMQ 5.x + Spring Boot 3.x, covering core features including message ordering, transaction messages, delayed messages, message filtering, and dead letter queues.

## Tech Stack

| Technology | Version |
|------------|---------|
| Java | 17+ |
| Spring Boot | 3.2.5 |
| RocketMQ | 5.1.4 |
| H2 Database | - |

## Core Features

### 1. Ordered Messages
- Messages for the same order are processed in FIFO order
- Message queue selector ensures messages for the same order go to the same queue

### 2. Transaction Messages
- Half-message mechanism ensures atomicity of payment and order status updates
- Local transaction execution bound with message sending
- Transaction state check mechanism

### 3. Delayed Messages
- Supports 18 delay levels (1 second ~ 2 hours)
- Order timeout auto-cancel scenario

### 4. Message Filtering
- Tag-based filtering
- SQL92 expression filtering

### 5. Dead Letter Queue
- Message retry mechanism
- Messages enter DLQ after max retry attempts

## Quick Start

### 1. Start RocketMQ Environment

```bash
cd docker
docker-compose up -d
```

Verify services:
```bash
docker ps
# Should see NameServer, Broker, Console containers
```

Console: http://localhost:8080

### 2. Build Project

```bash
mvn clean package -DskipTests
```

### 3. Run Application

```bash target/spring-rocket
java -jarmq-1.0.0-SNAPSHOT.jar
```

## API Endpoints

### Order APIs

#### Create Order
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

#### Get Order
```bash
curl http://localhost:8080/api/orders/{orderId}
```

#### Get Orders by Status
```bash
curl "http://localhost:8080/api/orders?status=PENDING_PAYMENT"
```

### Payment APIs

#### Create Payment
```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "<orderId>",
    "userId": "user001",
    "amount": 7999.00
  }'
```

## Project Structure

```
src/main/java/com/ecommerce/
├── controller/          # REST API controllers
├── service/            # Business services
├── model/              # Data models
├── config/             # RocketMQ configuration
├── consumer/           # Message consumers
├── producer/           # Message producers
├── listener/           # Transaction listeners
└── repository/         # Data access layer

docker/
├── docker-compose.yml  # Docker Compose configuration
├── broker.conf          # Broker configuration
└── start.sh            # Startup script
```

## Testing

```bash
# Run unit tests
mvn test
```

## Performance Metrics

| Metric | Target |
|--------|--------|
| Message processing latency | < 100ms |
| Throughput | 1M QPS |
| Transaction message success rate | ≥ 99.99% |
| Delayed message accuracy | ±1 second |

## License

MIT
