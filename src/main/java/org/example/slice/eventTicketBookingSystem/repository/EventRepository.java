package org.example.slice.eventTicketBookingSystem.repository;

import org.example.slice.eventTicketBookingSystem.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    boolean existsByNameAndScheduledTime(String name, LocalDateTime scheduledTime);
}
