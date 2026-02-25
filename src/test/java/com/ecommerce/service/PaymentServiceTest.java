package com.ecommerce.service;

import com.ecommerce.model.Order;
import com.ecommerce.model.OrderStatus;
import com.ecommerce.model.Payment;
import com.ecommerce.model.PaymentStatus;
import com.ecommerce.model.message.PaymentSuccessMessage;
import com.ecommerce.repository.PaymentRepository;
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
 * 支付服务单元测试
 * @author lvdaxianer
 * @date 2025-02-25
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private PaymentService paymentService;

    private Payment testPayment;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setPaymentId("PAY-123456");
        testPayment.setOrderId("ORD-123456");
        testPayment.setUserId("user001");
        testPayment.setAmount(new BigDecimal("7999.00"));
        testPayment.setStatus(PaymentStatus.PENDING);
        testPayment.setCreatedAt(LocalDateTime.now());

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderId("ORD-123456");
        testOrder.setStatus(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void testCreatePayment() {
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        Payment result = paymentService.createPayment(
                "ORD-123456",
                "user001",
                new BigDecimal("7999.00")
        );

        assertNotNull(result);
        assertEquals("user001", result.getUserId());
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void testProcessPaymentSuccess() {
        when(paymentRepository.findByPaymentId("PAY-123456")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(orderService.updateOrderStatus("ORD-123456", OrderStatus.PAID)).thenReturn(testOrder);

        Payment result = paymentService.processPaymentSuccess("PAY-123456", "TXN-123456");

        assertNotNull(result);
        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        assertEquals("TXN-123456", result.getTransactionId());
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(orderService, times(1)).updateOrderStatus("ORD-123456", OrderStatus.PAID);
    }

    @Test
    void testProcessPaymentFailure() {
        when(paymentRepository.findByPaymentId("PAY-123456")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        Payment result = paymentService.processPaymentFailure("PAY-123456");

        assertNotNull(result);
        assertEquals(PaymentStatus.FAILED, result.getStatus());
    }

    @Test
    void testGetPaymentById() {
        when(paymentRepository.findByPaymentId("PAY-123456")).thenReturn(Optional.of(testPayment));

        Optional<Payment> result = paymentService.getPaymentById("PAY-123456");

        assertTrue(result.isPresent());
        assertEquals("PAY-123456", result.get().getPaymentId());
    }

    @Test
    void testGetPaymentByOrderId() {
        when(paymentRepository.findByOrderId("ORD-123456")).thenReturn(Optional.of(testPayment));

        Optional<Payment> result = paymentService.getPaymentByOrderId("ORD-123456");

        assertTrue(result.isPresent());
        assertEquals("ORD-123456", result.get().getOrderId());
    }

    @Test
    void testBuildPaymentSuccessMessage() {
        testPayment.setTransactionId("TXN-123456");
        testPayment.setPaidAt(LocalDateTime.now());

        PaymentSuccessMessage message = paymentService.buildPaymentSuccessMessage(testPayment);

        assertNotNull(message);
        assertEquals(testPayment.getPaymentId(), message.getPaymentId());
        assertEquals(testPayment.getOrderId(), message.getOrderId());
        assertEquals(testPayment.getAmount(), message.getAmount());
    }
}
