package com.ecommerce.repository;

import com.ecommerce.model.Order;
import com.ecommerce.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 订单Repository
 * @author lvdaxianer
 * @date 2025-02-25
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 根据订单ID查询
     */
    Optional<Order> findByOrderId(String orderId);

    /**
     * 根据用户ID查询订单列表
     */
    List<Order> findByUserId(String userId);

    /**
     * 根据状态查询订单列表
     */
    List<Order> findByStatus(OrderStatus status);
}
