package com.ecommerce.service;

import com.ecommerce.model.Order;
import com.ecommerce.model.OrderStatus;
import com.ecommerce.model.message.OrderCreatedMessage;
import com.ecommerce.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 订单服务单元测试
 * @author lvdaxianer
 * @date 2025-02-25
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderId("ORD-123456");
        testOrder.setUserId("user001");
        testOrder.setProductId("product001");
        testOrder.setProductName("iPhone 15");
        testOrder.setQuantity(1);
        testOrder.setAmount(new BigDecimal("7999.00"));
        testOrder.setStatus(OrderStatus.PENDING_PAYMENT);
        testOrder.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testCreateOrder() {
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        Order result = orderService.createOrder(
                "user001",
                "product001",
                "iPhone 15",
                1,
                new BigDecimal("7999.00")
        );

        assertNotNull(result);
        assertEquals("user001", result.getUserId());
        assertEquals(OrderStatus.PENDING_PAYMENT, result.getStatus());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testGetOrderById() {
        when(orderRepository.findByOrderId("ORD-123456")).thenReturn(Optional.of(testOrder));

        Optional<Order> result = orderService.getOrderById("ORD-123456");

        assertTrue(result.isPresent());
        assertEquals("ORD-123456", result.get().getOrderId());
    }

    @Test
    void testUpdateOrderStatus() {
        when(orderRepository.findByOrderId("ORD-123456")).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        Order result = orderService.updateOrderStatus("ORD-123456", OrderStatus.PAID);

        assertNotNull(result);
        assertEquals(OrderStatus.PAID, result.getStatus());
    }

    @Test
    void testCancelOrder() {
        when(orderRepository.findByOrderId("ORD-123456")).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        Order result = orderService.cancelOrder("ORD-123456");

        assertNotNull(result);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testTimeoutCancelOrder() {
        when(orderRepository.findByOrderId("ORD-123456")).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        Order result = orderService.timeoutCancelOrder("ORD-123456");

        assertNotNull(result);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testBuildOrderCreatedMessage() {
        OrderCreatedMessage message = orderService.buildOrderCreatedMessage(testOrder);

        assertNotNull(message);
        assertEquals(testOrder.getOrderId(), message.getOrderId());
        assertEquals(testOrder.getUserId(), message.getUserId());
        assertEquals(testOrder.getAmount(), message.getAmount());
    }
}
