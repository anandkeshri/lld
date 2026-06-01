package org.example.slice.eventTicketBookingSystem.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.slice.eventTicketBookingSystem.converter.AtomicIntegerConverter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

@Entity
@Table(name = "seat_inventory",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "seat_type"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SeatInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @NotBlank(message = "Seat type is required")
    @Column(name = "seat_type", nullable = false)
    private String seatType;

    @Min(value = 0, message = "Original capacity must be non-negative")
    @Column(nullable = false)
    private int originalCapacity;

    @Convert(converter = AtomicIntegerConverter.class)
    @Column(name = "available_capacity", nullable = false)
    private AtomicInteger availableCapacity = new AtomicInteger(0);

    @NotNull(message = "Pricing is required")
    @DecimalMin(value = "0.00", message = "Pricing must be non-negative")
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal pricing;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime modifiedAt;
}
