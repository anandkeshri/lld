package org.example.slice.eventTicketBookingSystem.service;

import lombok.extern.slf4j.Slf4j;
import org.example.slice.eventTicketBookingSystem.model.Order;
import org.example.slice.eventTicketBookingSystem.model.OrderStatus;
import org.example.slice.eventTicketBookingSystem.model.OrderType;
import org.example.slice.eventTicketBookingSystem.repository.OrderRepository;
import org.example.slice.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Order createOrder(String userId, Long eventId, String seatType,
                             OrderType orderType, int quantity,
                             BigDecimal unitPrice, BigDecimal totalAmount,
                             String bookingStrategy, String pricingStrategy,
                             Long referenceOrderId) {
        Order order = Order.builder()
                .userId(userId)
                .eventId(eventId)
                .seatType(seatType)
                .orderType(orderType)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .totalAmount(totalAmount)
                .status(OrderStatus.CREATED)
                .bookingStrategy(bookingStrategy)
                .pricingStrategy(pricingStrategy)
                .referenceOrderId(referenceOrderId)
                .build();
        Order saved = orderRepository.save(order);
        log.info("createOrder - orderId={} type={} userId={} eventId={} seatType={} qty={} total={}",
                saved.getId(), orderType, userId, eventId, seatType, quantity, totalAmount);
        return saved;
    }

    @Transactional
    public Order updateStatus(Long orderId, OrderStatus newStatus) {
        return updateStatus(orderId, newStatus, null);
    }

    @Transactional
    public Order updateStatus(Long orderId, OrderStatus newStatus, String failureReason) {
        Order order = getById(orderId);
        log.debug("updateStatus - orderId={} {} → {}", orderId, order.getStatus(), newStatus);
        order.setStatus(newStatus);
        if (failureReason != null) {
            order.setFailureReason(failureReason);
        }
        return orderRepository.save(order);
    }

    public Order getById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("getById - order not found orderId={}", orderId);
                    return new ResourceNotFoundException("Order", orderId);
                });
    }

    public List<Order> getHistoryByUserId(String userId) {
        log.debug("getHistoryByUserId - userId={}", userId);
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
