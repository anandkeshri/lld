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
 * Pay-first strategy: collects payment before acquiring a seat lock.
 * A fast non-locking capacity check is performed before payment as an optimistic
 * early-exit. After payment succeeds, a DB row-level lock is acquired and capacity
 * is re-validated under the lock — the authoritative check.
 *
 * Trade-off: no seats are blocked during payment, maximising visible inventory.
 * Risk: payment may succeed but seats could be sold out by the time the lock is
 * acquired — triggers an automatic refund in that case.
 */
@Slf4j
@Component
public class OptimisticBookingStrategy implements BookingStrategy {

    private final SeatInventoryService seatInventoryService;
    private final OrderService orderService;
    private final TicketService ticketService;
    private final MockPaymentProcessor paymentProcessor;

    public OptimisticBookingStrategy(SeatInventoryService seatInventoryService,
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
        log.info("OptimisticBookingStrategy | START | orderId={} eventId={} seatType={} qty={}",
                orderId, context.getEventId(), context.getSeatType(), context.getQuantity());

        // Step 1: Non-locking fast-check — fail before charging if obviously out of stock.
        int available = seatInventoryService.getAvailableCapacity(context.getEventId(), context.getSeatType());
        if (available < context.getQuantity()) {
            log.warn("OptimisticBookingStrategy | FAST_FAIL | orderId={} available={} requested={}",
                    orderId, available, context.getQuantity());
            orderService.updateStatus(orderId, OrderStatus.SEATS_UNAVAILABLE,
                    "Insufficient capacity (pre-payment check)");
            throw new InsufficientCapacityException(
                    "Only " + available + " seats available for '" + context.getSeatType() + "'");
        }

        // Step 2: Process payment with no seats held.
        orderService.updateStatus(orderId, OrderStatus.PAYMENT_PROCESSING);
        boolean paymentSuccess = paymentProcessor.processPayment(context.getOrder());

        if (!paymentSuccess) {
            log.warn("OptimisticBookingStrategy | PAYMENT_FAILED | orderId={}", orderId);
            orderService.updateStatus(orderId, OrderStatus.PAYMENT_FAILED, "Payment declined by processor");
            return BookingResult.builder()
                    .order(orderService.getById(orderId))
                    .tickets(List.of())
                    .success(false)
                    .failureReason("Payment declined")
                    .build();
        }

        orderService.updateStatus(orderId, OrderStatus.PAYMENT_SUCCESS);

        // Step 3: Acquire lock and perform the authoritative capacity check under the lock.
        try {
            seatInventoryService.reserveSeats(context.getEventId(), context.getSeatType(), context.getQuantity());
            orderService.updateStatus(orderId, OrderStatus.SEATS_RESERVED);
        } catch (InsufficientCapacityException e) {
            log.warn("OptimisticBookingStrategy | SEATS_GONE_POST_PAYMENT | orderId={} — initiating refund", orderId);
            orderService.updateStatus(orderId, OrderStatus.SEATS_UNAVAILABLE,
                    "Seats no longer available after payment was collected");
            paymentProcessor.initiateRefund(context.getOrder(), context.getOrder().getTotalAmount());
            orderService.updateStatus(orderId, OrderStatus.REFUND_INITIATED);
            orderService.updateStatus(orderId, OrderStatus.REFUND_COMPLETED);
            return BookingResult.builder()
                    .order(orderService.getById(orderId))
                    .tickets(List.of())
                    .success(false)
                    .failureReason("Seats sold out after payment; refund initiated")
                    .build();
        }

        // Step 4: Issue tickets.
        try {
            List<Ticket> tickets = ticketService.issueTickets(orderService.getById(orderId));
            orderService.updateStatus(orderId, OrderStatus.TICKETS_ISSUED);
            log.info("OptimisticBookingStrategy | SUCCESS | orderId={} tickets={}", orderId, tickets.size());
            return BookingResult.builder()
                    .order(orderService.getById(orderId))
                    .tickets(tickets)
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("OptimisticBookingStrategy | TICKET_ISSUE_FAILED | orderId={}", orderId, e);
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
        return "OPTIMISTIC";
    }
}
