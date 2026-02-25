package com.ecommerce.service;

import com.ecommerce.model.Order;
import com.ecommerce.model.OrderStatus;
import com.ecommerce.model.message.OrderCreatedMessage;
import com.ecommerce.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 订单服务
 * @author lvdaxianer
 * @date 2025-02-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    /**
     * 创建订单
     */
    @Transactional
    public Order createOrder(String userId, String productId, String productName, 
                             Integer quantity, java.math.BigDecimal amount) {
        String orderId = generateOrderId();
        
        Order order = new Order();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setProductId(productId);
        order.setProductName(productName);
        order.setQuantity(quantity);
        order.setAmount(amount);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setCreatedAt(LocalDateTime.now());
        
        Order savedOrder = orderRepository.save(order);
        log.info("Order created: {}", savedOrder.getOrderId());
        
        return savedOrder;
    }

    /**
     * 根据订单ID查询订单
     */
    public Optional<Order> getOrderById(String orderId) {
        return orderRepository.findByOrderId(orderId);
    }

    /**
     * 根据用户ID查询订单列表
     */
    public List<Order> getOrdersByUserId(String userId) {
        return orderRepository.findByUserId(userId);
    }

    /**
     * 根据状态查询订单列表
     */
    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    /**
     * 更新订单状态
     */
    @Transactional
    public Order updateOrderStatus(String orderId, OrderStatus newStatus) {
        Optional<Order> orderOpt = orderRepository.findByOrderId(orderId);
        if (orderOpt.isEmpty()) {
            throw new RuntimeException("Order not found: " + orderId);
        }
        
        Order order = orderOpt.get();
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        
        log.info("Order status updated: {} -> {}", orderId, newStatus);
        return orderRepository.save(order);
    }

    /**
     * 取消订单
     */
    @Transactional
    public Order cancelOrder(String orderId) {
        return updateOrderStatus(orderId, OrderStatus.CANCELLED);
    }

    /**
     * 超时取消订单
     */
    @Transactional
    public Order timeoutCancelOrder(String orderId) {
        return updateOrderStatus(orderId, OrderStatus.TIMEOUT_CANCELLED);
    }

    /**
     * 生成订单ID
     */
    private String generateOrderId() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 构建订单创建消息
     */
    public OrderCreatedMessage buildOrderCreatedMessage(Order order) {
        OrderCreatedMessage message = new OrderCreatedMessage();
        message.setOrderId(order.getOrderId());
        message.setUserId(order.getUserId());
        message.setProductId(order.getProductId());
        message.setProductName(order.getProductName());
        message.setQuantity(order.getQuantity());
        message.setAmount(order.getAmount());
        message.setCreatedAt(System.currentTimeMillis());
        message.setTags("ORDER_CREATED");
        return message;
    }
}
