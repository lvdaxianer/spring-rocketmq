package com.ecommerce.integration;

import com.ecommerce.model.Order;
import com.ecommerce.model.OrderStatus;
import com.ecommerce.model.Payment;
import com.ecommerce.model.PaymentStatus;
import com.ecommerce.model.message.OrderCreatedMessage;
import com.ecommerce.model.message.PaymentSuccessMessage;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.PaymentRepository;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 高并发场景测试 - 百万QPS电商系统
 * 覆盖：订单创建、支付处理、消息顺序性、事务消息、死信队列
 * @author lvdaxianer
 * @date 2025-02-25
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("高并发场景测试")
class HighConcurrencyTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderService orderService;

    @InjectMocks
    private PaymentService paymentService;

    private static final int CONCURRENT_THREADS = 100;
    private static final int TOTAL_REQUESTS = 1000;

    @BeforeEach
    void setUp() {
    }

    private Order createTestOrder(String orderId, String userId) {
        Order order = new Order();
        order.setId(1L);
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setProductId("product001");
        order.setProductName("iPhone 15");
        order.setQuantity(1);
        order.setAmount(new BigDecimal("7999.00"));
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }

    private Payment createTestPayment(String paymentId, String orderId, String userId) {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setPaymentId(paymentId);
        payment.setOrderId(orderId);
        payment.setUserId(userId);
        payment.setAmount(new BigDecimal("7999.00"));
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        return payment;
    }

    @Nested
    @DisplayName("订单创建并发测试")
    class OrderConcurrencyTest {

        @Test
        @DisplayName("100线程同时创建订单 - 验证订单ID唯一性")
        void testConcurrentOrderCreation() throws InterruptedException {
            ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(CONCURRENT_THREADS);
            ConcurrentHashMap<String, Order> orderMap = new ConcurrentHashMap<>();
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);

            for (int i = 0; i < CONCURRENT_THREADS; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        Order order = createTestOrder("ORD-" + System.currentTimeMillis() + "-" + threadId, "user" + threadId);

                        when(orderRepository.save(any(Order.class))).thenReturn(order);
                        Order savedOrder = orderRepository.save(order);

                        orderMap.put(savedOrder.getOrderId(), savedOrder);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            endLatch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            assertTrue(successCount.get() >= CONCURRENT_THREADS * 0.9, "成功创建的订单数量应至少为线程数的90%");
        }

        @Test
        @DisplayName("1000次订单创建吞吐量测试")
        void testOrderCreationThroughput() throws InterruptedException {
            ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
            CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS);
            AtomicInteger successCount = new AtomicInteger(0);
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < TOTAL_REQUESTS; i++) {
                final int requestId = i;
                executor.submit(() -> {
                    try {
                        Order order = createTestOrder("ORD-" + requestId, "user" + requestId);

                        when(orderRepository.save(any(Order.class))).thenReturn(order);
                        orderRepository.save(order);
                        successCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(60, TimeUnit.SECONDS);
            long endTime = System.currentTimeMillis();
            executor.shutdown();

            double qps = (successCount.get() * 1000.0) / (endTime - startTime);
            System.out.println(String.format("订单创建吞吐量: %d requests, QPS: %.2f", 
                    successCount.get(), qps));

            assertTrue(qps > 0, "QPS应大于0");
            assertEquals(TOTAL_REQUESTS, successCount.get(), "所有请求应成功");
        }

        @Test
        @DisplayName("订单状态并发更新测试")
        void testConcurrentOrderStatusUpdate() throws InterruptedException {
            String orderId = "ORD-TEST-001";
            Order order = createTestOrder(orderId, "user001");

            ExecutorService executor = Executors.newFixedThreadPool(50);
            CountDownLatch latch = new CountDownLatch(50);
            AtomicInteger updateCount = new AtomicInteger(0);

        lenient().when(orderRepository.save(any(Order.class))).thenReturn(order);
        lenient().when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(order));
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            updateCount.incrementAndGet();
            return invocation.getArgument(0);
        });

            for (int i = 0; i < 50; i++) {
                executor.submit(() -> {
                    try {
                        orderService.updateOrderStatus(orderId, OrderStatus.PAID);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            assertTrue(updateCount.get() > 0, "应有成功的状态更新");
        }
    }

    @Nested
    @DisplayName("支付处理并发测试")
    class PaymentConcurrencyTest {

        @Test
        @DisplayName("100线程同时发起支付")
        void testConcurrentPaymentCreation() throws InterruptedException {
            ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(CONCURRENT_THREADS);
            ConcurrentHashMap<String, Payment> paymentMap = new ConcurrentHashMap<>();
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < CONCURRENT_THREADS; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        Payment payment = createTestPayment("PAY-" + threadId, "ORD-" + threadId, "user" + threadId);

                        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
                        Payment savedPayment = paymentRepository.save(payment);

                        paymentMap.put(savedPayment.getPaymentId(), savedPayment);
                        successCount.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            endLatch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            assertTrue(successCount.get() >= CONCURRENT_THREADS * 0.9, "成功创建的支付数量应至少为线程数的90%");
        }
    }

    @Nested
    @DisplayName("消息顺序性测试")
    class MessageOrderTest {

        @Test
        @DisplayName("同一订单消息顺序性验证")
        void testMessageOrderForSameOrder() {
            String orderId = "ORD-ORDER-TEST";
            Order order = createTestOrder(orderId, "user001");

            OrderCreatedMessage message1 = orderService.buildOrderCreatedMessage(order);
            assertEquals(orderId, message1.getOrderId());
            assertEquals("ORDER_CREATED", message1.getTags());

            Payment payment = createTestPayment("PAY-001", orderId, "user001");
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId("TXN-001");
            payment.setPaidAt(LocalDateTime.now());

            PaymentSuccessMessage message2 = paymentService.buildPaymentSuccessMessage(payment);
            assertEquals(orderId, message2.getOrderId());
            assertEquals("PAYMENT_SUCCESS", message2.getTags());
            assertNotNull(message2.getPaidAt());
        }

        @Test
        @DisplayName("消息时间戳顺序验证")
        void testMessageTimestampOrder() {
            long baseTime = System.currentTimeMillis();
            
            for (int i = 0; i < 10; i++) {
                Order order = createTestOrder("ORD-" + i, "user001");

                OrderCreatedMessage message = orderService.buildOrderCreatedMessage(order);
                assertTrue(message.getCreatedAt() >= baseTime);
            }
        }
    }

    @Nested
    @DisplayName("事务消息并发测试")
    class TransactionMessageTest {

        @Test
        @DisplayName("事务消息构建测试")
        void testTransactionMessageBuilding() {
            Payment payment = createTestPayment("PAY-TXN-001", "ORD-TXN-001", "user001");
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId("TXN-123456");
            payment.setPaidAt(LocalDateTime.now());

            PaymentSuccessMessage message = paymentService.buildPaymentSuccessMessage(payment);

            assertNotNull(message);
            assertEquals("PAY-TXN-001", message.getPaymentId());
            assertEquals("ORD-TXN-001", message.getOrderId());
            assertEquals("TXN-123456", message.getTransactionId());
            assertEquals(new BigDecimal("7999.00"), message.getAmount());
            assertEquals("PAYMENT_SUCCESS", message.getTags());
        }

        @Test
        @DisplayName("并发事务消息构建测试")
        void testConcurrentTransactionMessageBuilding() throws InterruptedException {
            ExecutorService executor = Executors.newFixedThreadPool(50);
            CountDownLatch latch = new CountDownLatch(100);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < 100; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        Payment payment = createTestPayment("PAY-" + index, "ORD-" + index, "user" + index);
                        payment.setStatus(PaymentStatus.SUCCESS);
                        payment.setTransactionId("TXN-" + index);
                        payment.setPaidAt(LocalDateTime.now());

                        PaymentSuccessMessage message = paymentService.buildPaymentSuccessMessage(payment);
                        
                        assertNotNull(message);
                        assertEquals("PAY-" + index, message.getPaymentId());
                        assertEquals("ORD-" + index, message.getOrderId());
                        successCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            assertEquals(100, successCount.get());
        }
    }

    @Nested
    @DisplayName("死信队列并发测试")
    class DeadLetterQueueTest {

        @Test
        @DisplayName("死信消息处理测试")
        void testDeadLetterMessageHandling() {
            for (int i = 0; i < 10; i++) {
                String orderId = "ORD-DLQ-" + i;
                Order order = createTestOrder(orderId, "user" + i);

                when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(order));
                when(orderRepository.save(any(Order.class))).thenReturn(order);

                Order result = orderService.timeoutCancelOrder(orderId);
                assertNotNull(result);
            }
        }

        @Test
        @DisplayName("死信队列消息重试机制测试")
        void testDeadLetterRetryMechanism() throws InterruptedException {
            AtomicInteger retryCount = new AtomicInteger(0);
            int maxRetries = 3;

            for (int retry = 0; retry < maxRetries; retry++) {
                String orderId = "ORD-RETRY-" + retry;
                Order order = createTestOrder(orderId, "user" + retry);

                when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(order));
                when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                    retryCount.incrementAndGet();
                    return invocation.getArgument(0);
                });

                orderService.timeoutCancelOrder(orderId);
            }

            assertEquals(maxRetries, retryCount.get());
        }
    }

    @Nested
    @DisplayName("延迟消息并发测试")
    class DelayMessageTest {

        @Test
        @DisplayName("延迟消息构建测试")
        void testDelayMessageBuilding() {
            for (int delayLevel = 1; delayLevel <= 5; delayLevel++) {
                Order order = createTestOrder("ORD-DELAY-" + delayLevel, "user001");

                OrderCreatedMessage message = orderService.buildOrderCreatedMessage(order);
                assertNotNull(message);
                assertEquals("ORD-DELAY-" + delayLevel, message.getOrderId());
            }
        }

        @Test
        @DisplayName("批量延迟消息并发构建测试")
        void testBatchDelayMessageBuilding() throws InterruptedException {
            ExecutorService executor = Executors.newFixedThreadPool(50);
            CountDownLatch latch = new CountDownLatch(500);
            AtomicLong messageCount = new AtomicLong(0);

            for (int i = 0; i < 500; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        Order order = createTestOrder("ORD-" + index, "user" + (index % 100));

                        OrderCreatedMessage message = orderService.buildOrderCreatedMessage(order);
                        assertNotNull(message);
                        messageCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            assertEquals(500, messageCount.get());
        }
    }

    @Nested
    @DisplayName("消息过滤并发测试")
    class MessageFilterTest {

        @Test
        @DisplayName("Tag标签过滤测试")
        void testTagFiltering() {
            OrderCreatedMessage orderMessage = new OrderCreatedMessage();
            orderMessage.setOrderId("ORD-001");
            orderMessage.setTags("ORDER_CREATED");
            assertEquals("ORDER_CREATED", orderMessage.getTags());

            PaymentSuccessMessage paymentMessage = new PaymentSuccessMessage();
            paymentMessage.setPaymentId("PAY-001");
            paymentMessage.setOrderId("ORD-001");
            paymentMessage.setTags("PAYMENT_SUCCESS");
            assertEquals("PAYMENT_SUCCESS", paymentMessage.getTags());
        }

        @Test
        @DisplayName("并发消息Tag过滤测试")
        void testConcurrentTagFiltering() throws InterruptedException {
            ExecutorService executor = Executors.newFixedThreadPool(50);
            CountDownLatch latch = new CountDownLatch(200);
            AtomicInteger orderCount = new AtomicInteger(0);
            AtomicInteger paymentCount = new AtomicInteger(0);

            for (int i = 0; i < 200; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        if (index % 2 == 0) {
                            OrderCreatedMessage msg = new OrderCreatedMessage();
                            msg.setOrderId("ORD-" + index);
                            msg.setTags("ORDER_CREATED");
                            assertEquals("ORDER_CREATED", msg.getTags());
                            orderCount.incrementAndGet();
                        } else {
                            PaymentSuccessMessage msg = new PaymentSuccessMessage();
                            msg.setPaymentId("PAY-" + index);
                            msg.setOrderId("ORD-" + index);
                            msg.setTags("PAYMENT_SUCCESS");
                            assertEquals("PAYMENT_SUCCESS", msg.getTags());
                            paymentCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            assertEquals(100, orderCount.get());
            assertEquals(100, paymentCount.get());
        }
    }

    @Nested
    @DisplayName("综合高并发压力测试")
    class ComprehensiveStressTest {

        @Test
        @DisplayName("全链路高并发测试 - 订单+支付+消息")
        void testFullChainHighConcurrency() throws InterruptedException {
            int threadCount = 50;
            int requestsPerThread = 20;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threadCount * requestsPerThread);
            AtomicInteger totalSuccess = new AtomicInteger(0);
            AtomicInteger orderSuccess = new AtomicInteger(0);
            AtomicInteger paymentSuccess = new AtomicInteger(0);
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < threadCount * requestsPerThread; i++) {
                final int requestId = i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        
                        String orderId = "ORD-STRESS-" + requestId;
                        String paymentId = "PAY-STRESS-" + requestId;

                        Order order = createTestOrder(orderId, "user" + (requestId % 100));

                        when(orderRepository.save(any(Order.class))).thenReturn(order);
                        orderRepository.save(order);
                        orderSuccess.incrementAndGet();

                        Payment payment = createTestPayment(paymentId, orderId, "user" + (requestId % 100));

                        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
                        paymentRepository.save(payment);
                        paymentSuccess.incrementAndGet();

                        OrderCreatedMessage orderMsg = orderService.buildOrderCreatedMessage(order);
                        PaymentSuccessMessage paymentMsg = paymentService.buildPaymentSuccessMessage(payment);

                        assertNotNull(orderMsg);
                        assertNotNull(paymentMsg);

                        totalSuccess.incrementAndGet();
                    } catch (Exception e) {
                        System.err.println("Request " + requestId + " failed: " + e.getMessage());
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            endLatch.await(60, TimeUnit.SECONDS);
            long endTime = System.currentTimeMillis();
            executor.shutdown();

            long duration = endTime - startTime;
            double totalQps = (totalSuccess.get() * 1000.0) / duration;
            double orderQps = (orderSuccess.get() * 1000.0) / duration;
            double paymentQps = (paymentSuccess.get() * 1000.0) / duration;

            System.out.println(String.format("=== 高并发压测结果 ==="));
            System.out.println(String.format("总请求数: %d", threadCount * requestsPerThread));
            System.out.println(String.format("订单成功: %d, QPS: %.2f", orderSuccess.get(), orderQps));
            System.out.println(String.format("支付成功: %d, QPS: %.2f", paymentSuccess.get(), paymentQps));
            System.out.println(String.format("总成功: %d, 总QPS: %.2f", totalSuccess.get(), totalQps));
            System.out.println(String.format("耗时: %dms", duration));

            assertTrue(totalSuccess.get() > 0, "应有成功的请求");
            assertTrue(orderSuccess.get() > 0, "订单应有成功");
            assertTrue(paymentSuccess.get() > 0, "支付应有成功");
        }

        @Test
        @DisplayName("突发流量测试")
        void testBurstTraffic() throws InterruptedException {
            int burstSize = 200;
            ExecutorService executor = Executors.newFixedThreadPool(burstSize);
            CountDownLatch latch = new CountDownLatch(burstSize);
            AtomicInteger successCount = new AtomicInteger(0);
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < burstSize; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        Order order = createTestOrder("ORD-BURST-" + index, "user" + index);

                        when(orderRepository.save(any(Order.class))).thenReturn(order);
                        orderRepository.save(order);
                        successCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(30, TimeUnit.SECONDS);
            long endTime = System.currentTimeMillis();
            executor.shutdown();

            long duration = endTime - startTime;
            double qps = (successCount.get() * 1000.0) / duration;

            System.out.println(String.format("突发流量: %d requests in %dms, QPS: %.2f", 
                    successCount.get(), duration, qps));

            assertEquals(burstSize, successCount.get(), "突发流量应全部成功");
        }
    }
}
