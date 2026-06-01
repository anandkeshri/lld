package org.example.slice.eventTicketBookingSystem.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.slice.eventTicketBookingSystem.dto.SeatCapacityResponse;
import org.example.slice.eventTicketBookingSystem.service.BookingService;
import org.example.slice.eventTicketBookingSystem.dto.EventPatchRequest;
import org.example.slice.eventTicketBookingSystem.dto.SeatInventoryUpdateRequest;
import org.example.slice.eventTicketBookingSystem.model.Event;
import org.example.slice.eventTicketBookingSystem.model.SeatInventory;
import org.example.slice.eventTicketBookingSystem.service.EventService;
import org.example.slice.eventTicketBookingSystem.service.SeatInventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final SeatInventoryService seatInventoryService;
    private final BookingService bookingService;

    public EventController(EventService eventService,
                           SeatInventoryService seatInventoryService,
                           BookingService bookingService) {
        this.eventService = eventService;
        this.seatInventoryService = seatInventoryService;
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<Event> createEvent(@Valid @RequestBody Event event) {
        log.info("POST /api/events - name={}", event.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.createEvent(event));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable Long id) {
        log.info("GET /api/events/{}", id);
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Event> patchEvent(@PathVariable Long id, @RequestBody EventPatchRequest patch) {
        log.info("PATCH /api/events/{}", id);
        return ResponseEntity.ok(eventService.patchEvent(id, patch));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(@PathVariable Long id, @Valid @RequestBody Event event) {
        log.info("PUT /api/events/{}", id);
        return ResponseEntity.ok(eventService.updateEvent(id, event));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        log.info("DELETE /api/events/{}", id);
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{eventId}/capacity")
    public ResponseEntity<List<SeatCapacityResponse>> getSeatCapacity(@PathVariable Long eventId) {
        log.info("GET /api/events/{}/capacity", eventId);
        return ResponseEntity.ok(bookingService.getSeatCapacity(eventId));
    }

    @PutMapping("/{eventId}/inventory/{seatType}")
    public ResponseEntity<SeatInventory> updateSeatInventory(
            @PathVariable Long eventId,
            @PathVariable String seatType,
            @Valid @RequestBody SeatInventoryUpdateRequest request) {
        log.info("PUT /api/events/{}/inventory/{}", eventId, seatType);
        return ResponseEntity.ok(seatInventoryService.updateByEventIdAndSeatType(eventId, seatType, request));
    }
}
