package org.example.slice.eventTicketBookingSystem.service;

import lombok.extern.slf4j.Slf4j;
import org.example.slice.eventTicketBookingSystem.model.Order;
import org.example.slice.eventTicketBookingSystem.model.Ticket;
import org.example.slice.eventTicketBookingSystem.model.TicketStatus;
import org.example.slice.eventTicketBookingSystem.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public List<Ticket> issueTickets(Order order) {
        log.debug("issueTickets - orderId={} qty={}", order.getId(), order.getQuantity());
        List<Ticket> tickets = new ArrayList<>();
        for (int i = 0; i < order.getQuantity(); i++) {
            String ticketNumber = buildTicketNumber(order);
            Ticket ticket = Ticket.builder()
                    .orderId(order.getId())
                    .eventId(order.getEventId())
                    .userId(order.getUserId())
                    .seatType(order.getSeatType())
                    .ticketNumber(ticketNumber)
                    .price(order.getUnitPrice())
                    .status(TicketStatus.ISSUED)
                    .build();
            Ticket saved = ticketRepository.save(ticket);
            log.info("issueTickets - issued ticketId={} ticketNumber={} orderId={} eventId={} seatType={}",
                    saved.getId(), ticketNumber, order.getId(), order.getEventId(), order.getSeatType());
            tickets.add(saved);
        }
        return tickets;
    }

    public List<Ticket> getByOrderId(Long orderId) {
        log.debug("getByOrderId - orderId={}", orderId);
        return ticketRepository.findByOrderId(orderId);
    }

    private String buildTicketNumber(Order order) {
        return "TKT-" + order.getEventId() + "-" + order.getSeatType() + "-" + UUID.randomUUID();
    }
}
