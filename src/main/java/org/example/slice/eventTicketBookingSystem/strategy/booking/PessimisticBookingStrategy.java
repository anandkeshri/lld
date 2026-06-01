package org.example.slice.eventTicketBookingSystem.strategy.booking;

import lombok.extern.slf4j.Slf4j;
import org.example.slice.eventTicketBookingSystem.exception.InsufficientCapacityException;
import org.example.slice.eventTicketBookingSystem.model.OrderStatus;
import org.example.slice.eventTicketBookingSystem.model.Ticket;
import org.example.slice.eventTicketBookingSystem.payment.MockPaymentProcessor;
import org.example.slice.eventTicketBookingSystem.service.OrderService;
import org.example.slice.eventTicketBookingSystem.service.TicketService;
import org.example.slice.eventTicketBookingSystem.service.SeatInventoryService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Lock-first strategy: acquires a DB row-level lock and decrements seat capacity
 * BEFORE initiating payment. Payment is invoked after the lock-holding transaction
 * commits, so no DB lock is held during the payment network call.
 *
 * Trade-off: seats are held during payment processing. If payment fails, they are
 * released. Prevents overselling at the cost of temporarily reducing visible capacity.
 */
@Slf4j
@Component
public class PessimisticBookingStrategy implements BookingStrategy {

    private final SeatInventoryService seatInventoryService;
    private final OrderService orderService;
    private final TicketService ticketService;
    private final MockPaymentProcessor paymentProcessor;

    public PessimisticBookingStrategy(SeatInventoryService seatInventoryService,
                                      OrderService orderService,
                                      TicketService ticketService,
                                      MockPaymentProcessor paymentProcessor) {
        this.seatInventoryService = seatInventoryService;
        this.orderService = orderService;
        this.ticketService = ticketService;
        this.paymentProcessor = paymentProcessor;
    }

    @Override
    public BookingResult execute(BookingContext context) {
        Long orderId = context.getOrder().getId();
        log.info("PessimisticBookingStrategy | START | orderId={} eventId={} seatType={} qty={}",
                orderId, context.getEventId(), context.getSeatType(), context.getQuantity());

        // Step 1: Acquire SELECT FOR UPDATE lock, verify capacity, decrement.
        // reserveSeats() is @Transactional — commits immediately, releasing the lock
        // before payment is called. Payment must NOT be inside a transaction that holds
        // a DB lock or it will serialize all concurrent bookings for this seat type.
        try {
            seatInventoryService.reserveSeats(context.getEventId(), context.getSeatType(), context.getQuantity());
            orderService.updateStatus(orderId, OrderStatus.SEATS_RESERVED);
        } catch (InsufficientCapacityException e) {
            log.warn("PessimisticBookingStrategy | SEATS_UNAVAILABLE | orderId={} reason={}", orderId, e.getMessage());
            orderService.updateStatus(orderId, OrderStatus.SEATS_UNAVAILABLE, e.getMessage());
            return BookingResult.builder()
                    .order(orderService.getById(orderId))
                    .tickets(List.of())
                    .success(false)
                    .failureReason(e.getMessage())
                    .build();
        }

        // Step 2: Payment — called outside any @Transactional scope.
        orderService.updateStatus(orderId, OrderStatus.PAYMENT_PROCESSING);
        boolean paymentSuccess = paymentProcessor.processPayment(context.getOrder());

        if (!paymentSuccess) {
            log.warn("PessimisticBookingStrategy | PAYMENT_FAILED | orderId={} — releasing seats", orderId);
            seatInventoryService.releaseSeats(context.getEventId(), context.getSeatType(), context.getQuantity());
            orderService.updateStatus(orderId, OrderStatus.PAYMENT_FAILED, "Payment declined by processor");
            return BookingResult.builder()
                    .order(orderService.getById(orderId))
                    .tickets(List.of())
                    .success(false)
                    .failureReason("Payment declined")
                    .build();
        }

        orderService.updateStatus(orderId, OrderStatus.PAYMENT_SUCCESS);

        // Step 3: Issue tickets.
        try {
            List<Ticket> tickets = ticketService.issueTickets(orderService.getById(orderId));
            orderService.updateStatus(orderId, OrderStatus.TICKETS_ISSUED);
            log.info("PessimisticBookingStrategy | SUCCESS | orderId={} tickets={}", orderId, tickets.size());
            return BookingResult.builder()
                    .order(orderService.getById(orderId))
                    .tickets(tickets)
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("PessimisticBookingStrategy | TICKET_ISSUE_FAILED | orderId={}", orderId, e);
            orderService.updateStatus(orderId, OrderStatus.TICKET_ISSUE_FAILED, e.getMessage());
            paymentProcessor.initiateRefund(context.getOrder(), context.getOrder().getTotalAmount());
            orderService.updateStatus(orderId, OrderStatus.REFUND_INITIATED);
            orderService.updateStatus(orderId, OrderStatus.REFUND_COMPLETED);
            return BookingResult.builder()
                    .order(orderService.getById(orderId))
                    .tickets(List.of())
                    .success(false)
                    .failureReason("Ticket issuance failed; refund initiated")
                    .build();
        }
    }

    @Override
    public String strategyName() {
        return "PESSIMISTIC";
    }
}
