# Tasks: RocketMQ百万QPS电商场景

**Input**: Design documents from `/specs/001-rocketmq-ecommerce/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: This project includes test tasks as specified in the feature requirements

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [X] T001 Create Maven project structure per implementation plan
- [X] T002 Initialize Spring Boot 3.x project with RocketMQ 5.x dependencies in pom.xml
- [X] T003 [P] Configure application.yml with RocketMQ name-server and producer/consumer settings
- [X] T004 [P] Create Docker Compose configuration for RocketMQ cluster in docker/docker-compose.yml

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T005 Create Order entity model in src/main/java/com/ecommerce/model/Order.java
- [X] T006 Create OrderStatus enum in src/main/java/com/ecommerce/model/OrderStatus.java
- [X] T007 Create Payment entity model in src/main/java/com/ecommerce/model/Payment.java
- [X] T008 Create PaymentStatus enum in src/main/java/com/ecommerce/model/PaymentStatus.java
- [X] T009 [P] Configure RocketMQ Producer configuration in src/main/java/com/ecommerce/config/RocketMQProducerConfig.java
- [X] T010 [P] Configure RocketMQ Consumer configuration in src/main/java/com/ecommerce/config/RocketMQConsumerConfig.java
- [X] T011 Setup H2 database configuration in src/main/resources/application.yml

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 6 - Docker本地RocketMQ环境 (Priority: P1) 🎯 MVP

**Goal**: 提供可本地运行的Docker Compose配置，快速搭建RocketMQ开发测试环境

**Independent Test**: 通过执行Docker命令启动服务并验证NameServer、Broker、Console正常运行

### Implementation for User Story 6

- [X] T012 [P] [US6] Create RocketMQ docker-compose.yml with NameServer, Broker, Console in docker/docker-compose.yml
- [X] T013 [US6] Create Broker configuration file in docker/broker.conf
- [X] T014 [US6] Create startup script in docker/start.sh
- [X] T015 [US6] Verify RocketMQ services start correctly
- [X] T016 [US6] Verify message can be sent and consumed via RocketMQ Console

**Checkpoint**: Docker environment ready - can proceed with other user stories

---

## Phase 4: User Story 1 - 订单创建场景 (Priority: P1)

**Goal**: 用户通过电商平台创建订单，系统需要保证同一订单的消息严格按照创建顺序处理

**Independent Test**: 模拟单用户下单流程验证订单创建成功、库存扣减、订单状态更新完整链路

### Implementation for User Story 1

- [X] T017 [P] [US1] Create OrderCreatedMessage model in src/main/java/com/ecommerce/model/message/OrderCreatedMessage.java
- [X] T018 [US1] Create OrderService in src/main/java/com/ecommerce/service/OrderService.java
- [X] T019 [US1] Create OrderProducer in src/main/java/com/ecommerce/producer/OrderProducer.java (supports ordered messages)
- [X] T020 [US1] Create OrderController in src/main/java/com/ecommerce/controller/OrderController.java
- [X] T021 [US1] Create OrderConsumer in src/main/java/com/ecommerce/consumer/OrderConsumer.java (ordered consumption)
- [ ] T022 [US1] Configure ordered message listener in src/main/java/com/ecommerce/config/OrderlyListenerContainerFactory.java
- [ ] T023 [US1] Add unit tests for OrderService in src/test/java/com/ecommerce/service/OrderServiceTest.java

**Checkpoint**: 订单创建和消息顺序性功能完成

---

## Phase 5: User Story 2 - 支付交易场景 (Priority: P1)

**Goal**: 用户完成支付，系统需要通过事务消息确保支付与订单状态更新的原子性

**Independent Test**: 模拟支付成功/失败场景验证订单状态与支付状态的一致性

### Implementation for User Story 2

- [X] T024 [P] [US2] Create PaymentSuccessMessage model in src/main/java/com/ecommerce/model/message/PaymentSuccessMessage.java
- [X] T025 [US2] Create PaymentService in src/main/java/com/ecommerce/service/PaymentService.java
- [X] T026 [US2] Create TransactionOrderProducer in src/main/java/com/ecommerce/producer/TransactionOrderProducer.java
- [X] T027 [US2] Create TransactionListener implementation in src/main/java/com/ecommerce/listener/PaymentTransactionListener.java
- [X] T028 [US2] Create PaymentController in src/main/java/com/ecommerce/controller/PaymentController.java
- [X] T029 [US2] Create PaymentConsumer in src/main/java/com/ecommerce/consumer/PaymentConsumer.java
- [ ] T030 [US2] Add unit tests for PaymentService in src/test/java/com/ecommerce/service/PaymentServiceTest.java
- [ ] T031 [US2] Add transaction rollback test in src/test/java/com/ecommerce/integration/TransactionRollbackTest.java

**Checkpoint**: 支付和事务消息功能完成

---

## Phase 6: User Story 3 - 订单超时取消场景 (Priority: P2)

**Goal**: 系统通过延迟消息实现订单超时自动取消功能，提高库存释放效率

**Independent Test**: 模拟订单创建后超时场景验证订单自动取消和库存恢复

### Implementation for User Story 3

- [X] T032 [P] [US3] Create OrderTimeoutMessage model in src/main/java/com/ecommerce/model/message/OrderTimeoutMessage.java
- [X] T033 [US3] Create DelayMessageProducer in src/main/java/com/ecommerce/producer/DelayMessageProducer.java
- [X] T034 [US3] Create OrderTimeoutConsumer in src/main/java/com/ecommerce/consumer/OrderTimeoutConsumer.java
- [ ] T035 [US3] Integrate delay message with OrderService for timeout cancellation
- [ ] T036 [US3] Add unit tests for delay message in src/test/java/com/ecommerce/service/DelayMessageTest.java

**Checkpoint**: 延迟消息功能完成

---

## Phase 7: User Story 4 - 订单状态查询场景 (Priority: P2)

**Goal**: 用户或管理员通过标签过滤查询特定状态的订单，实现精准的订单管理

**Independent Test**: 模拟查询特定状态订单验证过滤功能正确性

### Implementation for User Story 4

- [ ] T037 [P] [US4] Configure Tag-based message filtering in RocketMQ consumer
- [ ] T038 [US4] Configure SQL92 message filtering in RocketMQ consumer
- [ ] T039 [US4] Add query endpoint for filtering orders by status in OrderController.java
- [ ] T040 [US4] Add query endpoint for filtering orders by SQL expression

**Checkpoint**: 消息过滤功能完成

---

## Phase 8: User Story 5 - 消息可靠投递与死信队列场景 (Priority: P3)

**Goal**: 系统确保消息在消费失败后进入重试队列，多次失败后进入死信队列，保证消息不丢失且可追溯

**Independent Test**: 模拟消息消费失败场景验证重试和死信队列机制

### Implementation for User Story 5

- [ ] T041 [P] [US5] Configure message retry mechanism with maxReconsumeTimes in application.yml
- [ ] T042 [US5] Configure Dead Letter Queue (DLQ) topic in RocketMQ
- [ ] T043 [US5] Create DLQ consumer in src/main/java/com/ecommerce/consumer/DeadLetterConsumer.java
- [ ] T044 [US5] Add unit tests for retry and DLQ in src/test/java/com/ecommerce/integration/RetryDLQTest.java

**Checkpoint**: 消息重试和死信队列功能完成

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T045 [P] Add message trace support for all message types
- [ ] T046 [P] Add Prometheus metrics for message monitoring
- [ ] T047 Performance optimization: tune Producer and Consumer batch settings
- [ ] T048 Add integration tests for end-to-end message flow
- [ ] T049 Update README.md with quickstart instructions
- [ ] T050 Run quickstart.md validation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 6 (P1)**: Can start after Foundational - No dependencies on other stories (MVP!)
- **User Story 1 (P1)**: Can start after Foundational - No dependencies on other stories
- **User Story 2 (P1)**: Can start after Foundational - No dependencies on other stories
- **User Story 3 (P2)**: Can start after Foundational - May integrate with US1 but should be independently testable
- **User Story 4 (P2)**: Can start after Foundational - May integrate with US1/US2 but should be independently testable
- **User Story 5 (P3)**: Can start after Foundational - May integrate with any story

### Within Each User Story

- Models before services
- Services before producers
- Producers before consumers
- Core implementation before integration tests

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- US6, US1, US2 are all P1 and can be worked on in parallel
- Models within a story marked [P] can run in parallel

---

## Parallel Example: User Story 1 & 2 (P1 Stories)

```bash
# Launch US1 and US2 in parallel:
Task: "Create OrderService and OrderProducer for ordered messages"
Task: "Create TransactionOrderProducer and PaymentService for transaction messages"
```

---

## Implementation Strategy

### MVP First (User Story 6 - Docker环境)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 6 (Docker环境)
4. **STOP and VALIDATE**: Docker environment works
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 6 (Docker) → Test → Ready for development
3. Add User Story 1 (订单创建) → Test independently → Deploy/Demo
4. Add User Story 2 (支付交易) → Test independently → Deploy/Demo
5. Add User Story 3 (延迟消息) → Test independently → Deploy/Demo
6. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 6 (Docker)
   - Developer B: User Story 1 (订单创建)
   - Developer C: User Story 2 (支付交易)
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence

## Task Summary

| Phase | Description | Tasks |
|-------|-------------|-------|
| Phase 1 | Setup | T001-T004 |
| Phase 2 | Foundational | T005-T011 |
| Phase 3 | US6 Docker环境 (P1) | T012-T016 |
| Phase 4 | US1 订单创建 (P1) | T017-T023 |
| Phase 5 | US2 支付交易 (P1) | T024-T031 |
| Phase 6 | US3 订单超时取消 (P2) | T032-T036 |
| Phase 7 | US4 订单状态查询 (P2) | T037-T040 |
| Phase 8 | US5 消息可靠投递 (P3) | T041-T044 |
| Phase 9 | Polish | T045-T050 |

**Total Tasks**: 50
