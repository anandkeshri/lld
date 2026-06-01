package org.example.slice.eventTicketBookingSystem.service;

import lombok.extern.slf4j.Slf4j;
import org.example.slice.eventTicketBookingSystem.dto.EventPatchRequest;
import org.example.slice.eventTicketBookingSystem.model.Event;
import org.example.slice.eventTicketBookingSystem.model.SeatInventory;
import org.example.slice.eventTicketBookingSystem.repository.EventRepository;
import org.example.slice.eventTicketBookingSystem.repository.SeatInventoryRepository;
import org.example.slice.eventTicketBookingSystem.validation.EventValidator;
import org.example.slice.exception.DuplicateResourceException;
import org.example.slice.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class EventService {

    private final EventRepository eventRepository;
    private final SeatInventoryRepository seatInventoryRepository;
    private final EventValidator eventValidator;

    public EventService(EventRepository eventRepository,
                        SeatInventoryRepository seatInventoryRepository,
                        EventValidator eventValidator) {
        this.eventRepository = eventRepository;
        this.seatInventoryRepository = seatInventoryRepository;
        this.eventValidator = eventValidator;
    }

    // Creates the event and initialises a SeatInventory row (capacity=0, pricing=0) for each seatType.
    // If any step fails, the entire transaction rolls back — no partial state is persisted.
    @Transactional
    public Event createEvent(Event event) {
        log.debug("createEvent - validating event name={}", event.getName());
        eventValidator.validateForCreate(event);

        if (eventRepository.existsByNameAndScheduledTime(event.getName(), event.getScheduledTime())) {
            log.warn("createEvent - duplicate name={} scheduledTime={}", event.getName(), event.getScheduledTime());
            throw new DuplicateResourceException("Event", "name+scheduledTime",
                    event.getName() + " @ " + event.getScheduledTime());
        }

        Event saved = eventRepository.save(event);
        log.debug("createEvent - eventId={} saved, initialising {} seat inventory rows",
                saved.getId(), saved.getSeatTypes().size());

        for (String seatType : saved.getSeatTypes()) {
            SeatInventory inventory = new SeatInventory();
            inventory.setEvent(saved);
            inventory.setSeatType(seatType);
            inventory.setOriginalCapacity(0);
            inventory.setAvailableCapacity(new AtomicInteger(0));
            inventory.setPricing(BigDecimal.ZERO);
            seatInventoryRepository.save(inventory);
        }

        log.info("createEvent - eventId={} created with {} seat types", saved.getId(), saved.getSeatTypes().size());
        return saved;
    }

    public Event getEventById(Long id) {
        log.debug("getEventById - id={}", id);
        return eventRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("getEventById - not found id={}", id);
                    return new ResourceNotFoundException("Event", id);
                });
    }

    @Transactional
    public Event updateEvent(Long id, Event incoming) {
        log.debug("updateEvent - id={}", id);
        Event existing = getEventById(id);
        eventValidator.validateForUpdate(incoming);

        boolean nameOrTimeChanged = !existing.getName().equals(incoming.getName())
                || !existing.getScheduledTime().equals(incoming.getScheduledTime());

        if (nameOrTimeChanged
                && eventRepository.existsByNameAndScheduledTime(incoming.getName(), incoming.getScheduledTime())) {
            log.warn("updateEvent - duplicate name={} scheduledTime={}", incoming.getName(), incoming.getScheduledTime());
            throw new DuplicateResourceException("Event", "name+scheduledTime",
                    incoming.getName() + " @ " + incoming.getScheduledTime());
        }

        existing.setName(incoming.getName());
        existing.setDescription(incoming.getDescription());
        existing.setScheduledTime(incoming.getScheduledTime());
        existing.setVenue(incoming.getVenue());
        existing.setSeatTypes(incoming.getSeatTypes());
        existing.setSaleStartTime(incoming.getSaleStartTime());
        existing.setSaleEndTime(incoming.getSaleEndTime());

        Event saved = eventRepository.save(existing);
        log.info("updateEvent - eventId={} updated", id);
        return saved;
    }

    @Transactional
    public Event patchEvent(Long id, EventPatchRequest patch) {
        log.debug("patchEvent - id={}", id);
        Event existing = getEventById(id);

        if (patch.getName() != null)          existing.setName(patch.getName());
        if (patch.getDescription() != null)   existing.setDescription(patch.getDescription());
        if (patch.getScheduledTime() != null) existing.setScheduledTime(patch.getScheduledTime());
        if (patch.getVenue() != null)         existing.setVenue(patch.getVenue());
        if (patch.getSeatTypes() != null)     existing.setSeatTypes(patch.getSeatTypes());
        if (patch.getSaleStartTime() != null) existing.setSaleStartTime(patch.getSaleStartTime());
        if (patch.getSaleEndTime() != null)   existing.setSaleEndTime(patch.getSaleEndTime());

        eventValidator.validateForUpdate(existing);

        Event saved = eventRepository.save(existing);
        log.info("patchEvent - eventId={} patched", id);
        return saved;
    }

    @Transactional
    public void deleteEvent(Long id) {
        log.debug("deleteEvent - id={}", id);
        getEventById(id);
        seatInventoryRepository.deleteByEventId(id);
        eventRepository.deleteById(id);
        log.info("deleteEvent - eventId={} deleted along with all seat inventory", id);
    }
}
