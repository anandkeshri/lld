package org.example.slice.eventTicketBookingSystem.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.slice.eventTicketBookingSystem.dto.BookingHistoryResponse;
import org.example.slice.eventTicketBookingSystem.dto.BookingRequest;
import org.example.slice.eventTicketBookingSystem.dto.BookingResponse;
import org.example.slice.eventTicketBookingSystem.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/book")
    public ResponseEntity<BookingResponse> bookTicket(@Valid @RequestBody BookingRequest request) {
        log.info("POST /api/bookings/book | userId={} eventId={} seatType={} qty={}",
                request.getUserId(), request.getEventId(), request.getSeatType(), request.getQuantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.bookTicket(request));
    }

    @PostMapping("/cancel/{ticketId}")
    public ResponseEntity<Void> cancelTicket(@PathVariable Long ticketId) {
        log.info("POST /api/bookings/cancel/{}", ticketId);
        bookingService.cancelTicket(ticketId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/history")
    public ResponseEntity<List<BookingHistoryResponse>> getBookingHistory(@RequestParam String userId) {
        log.info("GET /api/bookings/history | userId={}", userId);
        return ResponseEntity.ok(bookingService.getBookingHistory(userId));
    }
}
