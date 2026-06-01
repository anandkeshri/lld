package org.example.slice.eventTicketBookingSystem.service;

import lombok.extern.slf4j.Slf4j;
import org.example.slice.eventTicketBookingSystem.dto.BookingHistoryResponse;
import org.example.slice.eventTicketBookingSystem.dto.BookingRequest;
import org.example.slice.eventTicketBookingSystem.dto.BookingResponse;
import org.example.slice.eventTicketBookingSystem.dto.SeatCapacityResponse;
import org.example.slice.eventTicketBookingSystem.exception.SaleNotActiveException;
import org.example.slice.eventTicketBookingSystem.exception.TicketCancellationException;
import org.example.slice.eventTicketBookingSystem.model.*;
import org.example.slice.eventTicketBookingSystem.payment.MockPaymentProcessor;
import org.example.slice.eventTicketBookingSystem.repository.TicketRepository;
import org.example.slice.eventTicketBookingSystem.strategy.booking.BookingContext;
import org.example.slice.eventTicketBookingSystem.strategy.booking.BookingResult;
import org.example.slice.eventTicketBookingSystem.strategy.booking.BookingStrategy;
import org.example.slice.eventTicketBookingSystem.strategy.pricing.DynamicPricingStrategy;
import org.example.slice.eventTicketBookingSystem.strategy.pricing.PricingStrategy;
import org.example.slice.eventTicketBookingSystem.model.Event;
import org.example.slice.eventTicketBookingSystem.model.SeatInventory;
import org.example.slice.eventTicketBookingSystem.repository.SeatInventoryRepository;
import org.example.slice.eventTicketBookingSystem.service.EventService;
import org.example.slice.eventTicketBookingSystem.service.SeatInventoryService;
import org.example.slice.exception.BadRequestException;
import org.example.slice.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BookingService {

    private final Map<String, BookingStrategy> bookingStrategies;
    private final Map<String, PricingStrategy> pricingStrategies;
    private final DynamicPricingStrategy dynamicPricingStrategy;
    private final EventService eventService;
    private final SeatInventoryService seatInventoryService;
    private final SeatInventoryRepository seatInventoryRepository;
    private final OrderService orderService;
    private final TicketService ticketService;
    private final TicketRepository ticketRepository;
    private final MockPaymentProcessor paymentProcessor;

    @Value("${booking.default.strategy:PESSIMISTIC}")
    private String defaultBookingStrategy;

    @Value("${booking.default.pricing-strategy:FIXED}")
    private String defaultPricingStrategy;

    public BookingService(List<BookingStrategy> bookingStrategyList,
                          List<PricingStrategy> pricingStrategyList,
                          DynamicPricingStrategy dynamicPricingStrategy,
                          EventService eventService,
                          SeatInventoryService seatInventoryService,
                          SeatInventoryRepository seatInventoryRepository,
                          OrderService orderService,
                          TicketService ticketService,
                          TicketRepository ticketRepository,
                          MockPaymentProcessor paymentProcessor) {
        this.bookingStrategies = bookingStrategyList.stream()
                .collect(Collectors.toMap(BookingStrategy::strategyName, Function.identity()));
        this.pricingStrategies = pricingStrategyList.stream()
                .collect(Collectors.toMap(PricingStrategy::strategyName, Function.identity()));
        this.dynamicPricingStrategy = dynamicPricingStrategy;
        this.eventService = eventService;
        this.seatInventoryService = seatInventoryService;
        this.seatInventoryRepository = seatInventoryRepository;
        this.orderService = orderService;
        this.ticketService = ticketService;
        this.ticketRepository = ticketRepository;
        this.paymentProcessor = paymentProcessor;
    }

    public BookingResponse bookTicket(BookingRequest request) {
        log.info("bookTicket | userId={} eventId={} seatType={} qty={} bookingStrategy={} pricingStrategy={}",
                request.getUserId(), request.getEventId(), request.getSeatType(),
                request.getQuantity(), request.getBookingStrategy(), request.getPricingStrategy());

        Event event = eventService.getEventById(request.getEventId());
        validateSaleWindow(event);
        validateSeatTypeAllowed(event, request.getSeatType());

        SeatInventory inventory = seatInventoryRepository
                .findByEventIdAndSeatType(request.getEventId(), request.getSeatType())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SeatInventory for eventId=" + request.getEventId()
                        + ", seatType=" + request.getSeatType()));

        String pricingStrategyName = resolveStrategyName(request.getPricingStrategy(), defaultPricingStrategy);
        PricingStrategy pricingStrategy = resolvePricingStrategy(pricingStrategyName);
        BigDecimal unitPrice = pricingStrategy.calculateUnitPrice(inventory);
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

        String bookingStrategyName = resolveStrategyName(request.getBookingStrategy(), defaultBookingStrategy);
        Order order = orderService.createOrder(
                request.getUserId(), request.getEventId(), request.getSeatType(),
                OrderType.BOOK, request.getQuantity(), unitPrice, totalAmount,
                bookingStrategyName, pricingStrategyName, null
        );

        BookingContext context = BookingContext.builder()
                .userId(request.getUserId())
                .eventId(request.getEventId())
                .seatType(request.getSeatType())
                .quantity(request.getQuantity())
                .order(order)
                .inventory(inventory)
                .unitPrice(unitPrice)
                .build();

        BookingStrategy strategy = resolveBookingStrategy(bookingStrategyName);
        BookingResult result = strategy.execute(context);

        return toBookingResponse(result);
    }

    @Transactional
    public void cancelTicket(Long ticketId) {
        log.info("cancelTicket | ticketId={}", ticketId);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> {
                    log.warn("cancelTicket | ticket not found ticketId={}", ticketId);
                    return new ResourceNotFoundException("Ticket", ticketId);
                });

        if (ticket.getStatus() != TicketStatus.ISSUED) {
            log.warn("cancelTicket | not cancellable ticketId={} status={}", ticketId, ticket.getStatus());
            throw new TicketCancellationException(
                    "Ticket " + ticketId + " cannot be cancelled — current status is " + ticket.getStatus());
        }

        Order originalOrder = orderService.getById(ticket.getOrderId());

        // Create a CANCEL order to keep a full audit trail of the cancellation.
        Order cancelOrder = orderService.createOrder(
                ticket.getUserId(), ticket.getEventId(), ticket.getSeatType(),
                OrderType.CANCEL, 1, ticket.getPrice(), ticket.getPrice(),
                null, null, originalOrder.getId()
        );

        // Release the seat back to inventory under a DB lock.
        seatInventoryService.releaseSeats(ticket.getEventId(), ticket.getSeatType(), 1);

        ticket.setStatus(TicketStatus.CANCELLED);
        ticketRepository.save(ticket);
        log.info("cancelTicket | ticketId={} marked CANCELLED", ticketId);

        paymentProcessor.initiateRefund(cancelOrder, ticket.getPrice());
        orderService.updateStatus(cancelOrder.getId(), OrderStatus.REFUND_INITIATED);
        orderService.updateStatus(cancelOrder.getId(), OrderStatus.REFUND_COMPLETED);

        // If no ISSUED tickets remain on the original order, mark it CANCELLED.
        long stillIssued = ticketRepository.countByOrderIdAndStatus(originalOrder.getId(), TicketStatus.ISSUED);
        if (stillIssued == 0) {
            orderService.updateStatus(originalOrder.getId(), OrderStatus.CANCELLED);
            log.info("cancelTicket | all tickets cancelled for orderId={} — order marked CANCELLED",
                    originalOrder.getId());
        }

        log.info("cancelTicket | done ticketId={} cancelOrderId={}", ticketId, cancelOrder.getId());
    }

    public List<BookingHistoryResponse> getBookingHistory(String userId) {
        log.debug("getBookingHistory | userId={}", userId);
        return orderService.getHistoryByUserId(userId).stream()
                .map(this::toHistoryResponse)
                .collect(Collectors.toList());
    }

    public List<SeatCapacityResponse> getSeatCapacity(Long eventId) {
        log.debug("getSeatCapacity | eventId={}", eventId);
        eventService.getEventById(eventId); // verify event exists
        return seatInventoryService.getSeatInventoryByEventId(eventId).stream()
                .map(inv -> SeatCapacityResponse.builder()
                        .eventId(eventId)
                        .seatType(inv.getSeatType())
                        .originalCapacity(inv.getOriginalCapacity())
                        .availableCapacity(inv.getAvailableCapacity().get())
                        .soldCount(inv.getOriginalCapacity() - inv.getAvailableCapacity().get())
                        .basePrice(inv.getPricing())
                        .currentPrice(dynamicPricingStrategy.calculateUnitPrice(inv))
                        .build())
                .collect(Collectors.toList());
    }

    private void validateSaleWindow(Event event) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(event.getSaleStartTime()) || now.isAfter(event.getSaleEndTime())) {
            log.warn("validateSaleWindow | eventId={} sale not active now={} window=[{}, {}]",
                    event.getId(), now, event.getSaleStartTime(), event.getSaleEndTime());
            throw new SaleNotActiveException(
                    "Ticket sales for event " + event.getId() + " are not currently active. "
                    + "Sale window: " + event.getSaleStartTime() + " to " + event.getSaleEndTime());
        }
    }

    private void validateSeatTypeAllowed(Event event, String seatType) {
        if (!event.getSeatTypes().contains(seatType)) {
            throw new BadRequestException(
                    "Seat type '" + seatType + "' is not available for event " + event.getId()
                    + ". Available types: " + event.getSeatTypes());
        }
    }

    private String resolveStrategyName(String requested, String defaultName) {
        return (requested != null && !requested.isBlank()) ? requested.toUpperCase() : defaultName;
    }

    private BookingStrategy resolveBookingStrategy(String name) {
        BookingStrategy strategy = bookingStrategies.get(name);
        if (strategy == null) {
            log.warn("resolveBookingStrategy | unknown '{}', falling back to {}", name, defaultBookingStrategy);
            strategy = bookingStrategies.get(defaultBookingStrategy);
        }
        return strategy;
    }

    private PricingStrategy resolvePricingStrategy(String name) {
        PricingStrategy strategy = pricingStrategies.get(name);
        if (strategy == null) {
            log.warn("resolvePricingStrategy | unknown '{}', falling back to {}", name, defaultPricingStrategy);
            strategy = pricingStrategies.get(defaultPricingStrategy);
        }
        return strategy;
    }

    private BookingResponse toBookingResponse(BookingResult result) {
        Order order = result.getOrder();
        List<BookingResponse.TicketInfo> ticketInfos = result.getTickets().stream()
                .map(t -> BookingResponse.TicketInfo.builder()
                        .ticketId(t.getId())
                        .ticketNumber(t.getTicketNumber())
                        .price(t.getPrice())
                        .status(t.getStatus())
                        .build())
                .collect(Collectors.toList());

        return BookingResponse.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .eventId(order.getEventId())
                .seatType(order.getSeatType())
                .orderType(order.getOrderType())
                .quantity(order.getQuantity())
                .unitPrice(order.getUnitPrice())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .bookingStrategy(order.getBookingStrategy())
                .pricingStrategy(order.getPricingStrategy())
                .tickets(ticketInfos)
                .build();
    }

    private BookingHistoryResponse toHistoryResponse(Order order) {
        List<BookingHistoryResponse.TicketInfo> ticketInfos = List.of();
        if (order.getOrderType() == OrderType.BOOK) {
            ticketInfos = ticketService.getByOrderId(order.getId()).stream()
                    .map(t -> BookingHistoryResponse.TicketInfo.builder()
                            .ticketId(t.getId())
                            .ticketNumber(t.getTicketNumber())
                            .price(t.getPrice())
                            .status(t.getStatus())
                            .build())
                    .collect(Collectors.toList());
        }
        return BookingHistoryResponse.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .eventId(order.getEventId())
                .seatType(order.getSeatType())
                .orderType(order.getOrderType())
                .quantity(order.getQuantity())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .referenceOrderId(order.getReferenceOrderId())
                .createdAt(order.getCreatedAt())
                .tickets(ticketInfos)
                .build();
    }
}
