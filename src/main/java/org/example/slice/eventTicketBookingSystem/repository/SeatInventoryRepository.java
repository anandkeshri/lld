package org.example.slice.eventTicketBookingSystem.repository;

import jakarta.persistence.LockModeType;
import org.example.slice.eventTicketBookingSystem.model.SeatInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatInventoryRepository extends JpaRepository<SeatInventory, Long> {

    List<SeatInventory> findByEventId(Long eventId);

    Optional<SeatInventory> findByEventIdAndSeatType(Long eventId, String seatType);

    @Modifying
    @Query("DELETE FROM SeatInventory s WHERE s.event.id = :eventId")
    void deleteByEventId(@Param("eventId") Long eventId);

    // Row-level pessimistic write lock — issues SELECT ... FOR UPDATE in MySQL
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SeatInventory s WHERE s.event.id = :eventId AND s.seatType = :seatType")
    Optional<SeatInventory> findByEventIdAndSeatTypeWithLock(
            @Param("eventId") Long eventId,
            @Param("seatType") String seatType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SeatInventory s WHERE s.id = :id")
    Optional<SeatInventory> findByIdWithLock(@Param("id") Long id);
}
