package org.example.slice.eventTicketBookingSystem.repository;

import org.example.slice.eventTicketBookingSystem.model.Order;
import org.example.slice.eventTicketBookingSystem.model.OrderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Order> findByUserIdAndOrderTypeOrderByCreatedAtDesc(String userId, OrderType orderType);

    List<Order> findByUserIdAndEventIdOrderByCreatedAtDesc(String userId, Long eventId);
}
