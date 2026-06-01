package org.example.slice.eventTicketBookingSystem.service;

import lombok.extern.slf4j.Slf4j;
import org.example.slice.eventTicketBookingSystem.exception.InsufficientCapacityException;
import org.example.slice.eventTicketBookingSystem.dto.SeatInventoryUpdateRequest;
import org.example.slice.eventTicketBookingSystem.model.SeatInventory;
import org.example.slice.eventTicketBookingSystem.repository.SeatInventoryRepository;
import org.example.slice.exception.BadRequestException;
import org.example.slice.exception.BusinessRuleViolationException;
import org.example.slice.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class SeatInventoryService {

    private final SeatInventoryRepository seatInventoryRepository;

    public SeatInventoryService(SeatInventoryRepository seatInventoryRepository) {
        this.seatInventoryRepository = seatInventoryRepository;
    }

    public SeatInventory getSeatInventoryById(Long id) {
        log.debug("getSeatInventoryById - id={}", id);
        return seatInventoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("getSeatInventoryById - not found id={}", id);
                    return new ResourceNotFoundException("SeatInventory", id);
                });
    }

    public List<SeatInventory> getSeatInventoryByEventId(Long eventId) {
        log.debug("getSeatInventoryByEventId - eventId={}", eventId);
        return seatInventoryRepository.findByEventId(eventId);
    }

    @Transactional
    public SeatInventory updateSeatInventory(Long id, SeatInventory patch) {
        SeatInventory existing = seatInventoryRepository.findByIdWithLock(id)
                .orElseThrow(() -> {
                    log.warn("updateSeatInventory - not found id={}", id);
                    return new ResourceNotFoundException("SeatInventory", id);
                });

        int newOriginal = patch.getOriginalCapacity();
        int newAvailable = patch.getAvailableCapacity().get();

        if (newAvailable > newOriginal) {
            throw new BusinessRuleViolationException(
                    "INVALID_CAPACITY",
                    "Available capacity (" + newAvailable + ") cannot exceed original capacity (" + newOriginal + ")"
            );
        }

        existing.setOriginalCapacity(newOriginal);
        existing.getAvailableCapacity().set(newAvailable);
        existing.setPricing(patch.getPricing());

        SeatInventory saved = seatInventoryRepository.save(existing);
        log.info("updateSeatInventory - id={} originalCapacity={} availableCapacity={} pricing={}",
                id, newOriginal, newAvailable, patch.getPricing());
        return saved;
    }

    @Transactional
    public void deleteSeatInventory(Long id) {
        log.debug("deleteSeatInventory - id={}", id);
        getSeatInventoryById(id);
        seatInventoryRepository.deleteById(id);
        log.info("deleteSeatInventory - deleted id={}", id);
    }

    // Acquires a DB row-level lock (SELECT FOR UPDATE), checks capacity, decrements atomically.
    // Updates capacity and pricing for a specific seat type on an event.
    // availableCapacity is adjusted by the delta: if originalCapacity increases by 100,
    // availableCapacity also increases by 100 (preserving already-sold seats).
    @Transactional
    public SeatInventory updateByEventIdAndSeatType(Long eventId, String seatType,
                                                    SeatInventoryUpdateRequest request) {
        log.debug("updateByEventIdAndSeatType - eventId={} seatType={}", eventId, seatType);
        SeatInventory inventory = seatInventoryRepository
                .findByIdWithLock(seatInventoryRepository
                        .findByEventIdAndSeatType(eventId, seatType)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "SeatInventory for eventId=" + eventId + ", seatType=" + seatType))
                        .getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SeatInventory for eventId=" + eventId + ", seatType=" + seatType));

        int currentOriginal = inventory.getOriginalCapacity();
        int currentAvailable = inventory.getAvailableCapacity().get();
        int newOriginal = request.getOriginalCapacity();
        int delta = newOriginal - currentOriginal;
        int newAvailable = currentAvailable + delta;

        if (newAvailable < 0) {
            int soldCount = currentOriginal - currentAvailable;
            throw new BadRequestException(
                    "Cannot reduce capacity below already-sold seats. Sold: " + soldCount
                    + ", requested originalCapacity: " + newOriginal);
        }

        inventory.setOriginalCapacity(newOriginal);
        inventory.getAvailableCapacity().set(newAvailable);
        inventory.setPricing(request.getPricing());

        SeatInventory saved = seatInventoryRepository.save(inventory);
        log.info("updateByEventIdAndSeatType - eventId={} seatType={} originalCapacity={} availableCapacity={} pricing={}",
                eventId, seatType, newOriginal, newAvailable, request.getPricing());
        return saved;
    }

    // Called by booking strategies — the lock is held only for the duration of this transaction,
    // which commits before payment processing begins.
    @Transactional
    public void reserveSeats(Long eventId, String seatType, int qty) {
        log.debug("reserveSeats - eventId={} seatType={} qty={}", eventId, seatType, qty);
        SeatInventory inventory = seatInventoryRepository
                .findByEventIdAndSeatTypeWithLock(eventId, seatType)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SeatInventory for eventId=" + eventId + ", seatType=" + seatType));

        int current = inventory.getAvailableCapacity().get();
        if (current < qty) {
            log.warn("reserveSeats - insufficient capacity eventId={} seatType={} available={} requested={}",
                    eventId, seatType, current, qty);
            throw new InsufficientCapacityException(
                    "Only " + current + " seats available for seat type '" + seatType + "', requested " + qty);
        }

        inventory.getAvailableCapacity().addAndGet(-qty);
        seatInventoryRepository.save(inventory);
        log.info("reserveSeats - reserved eventId={} seatType={} qty={} remaining={}",
                eventId, seatType, qty, inventory.getAvailableCapacity().get());
    }

    // Releases previously reserved seats — called on payment failure or ticket cancellation.
    @Transactional
    public void releaseSeats(Long eventId, String seatType, int qty) {
        log.debug("releaseSeats - eventId={} seatType={} qty={}", eventId, seatType, qty);
        SeatInventory inventory = seatInventoryRepository
                .findByEventIdAndSeatTypeWithLock(eventId, seatType)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SeatInventory for eventId=" + eventId + ", seatType=" + seatType));

        inventory.getAvailableCapacity().addAndGet(qty);
        seatInventoryRepository.save(inventory);
        log.info("releaseSeats - released eventId={} seatType={} qty={} available={}",
                eventId, seatType, qty, inventory.getAvailableCapacity().get());
    }

    // Non-locking read — used for the optimistic strategy's fast pre-check before payment.
    public int getAvailableCapacity(Long eventId, String seatType) {
        return seatInventoryRepository.findByEventIdAndSeatType(eventId, seatType)
                .map(inv -> inv.getAvailableCapacity().get())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SeatInventory for eventId=" + eventId + ", seatType=" + seatType));
    }
}
