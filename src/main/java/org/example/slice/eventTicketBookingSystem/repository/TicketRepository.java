package org.example.slice.eventTicketBookingSystem.repository;

import org.example.slice.eventTicketBookingSystem.model.Ticket;
import org.example.slice.eventTicketBookingSystem.model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByOrderId(Long orderId);

    List<Ticket> findByUserId(String userId);

    long countByOrderIdAndStatus(Long orderId, TicketStatus status);
}
