package org.example.slice.eventTicketBookingSystem.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    @Column(nullable = false)
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Scheduled time is required")
    @Column(nullable = false)
    private LocalDateTime scheduledTime;

    @NotBlank(message = "Venue is required")
    @Size(max = 300, message = "Venue must not exceed 300 characters")
    @Column(nullable = false)
    private String venue;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "event_seat_types", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "seat_type")
    @NotEmpty(message = "At least one seat type is required")
    private List<String> seatTypes;

    @NotNull(message = "Sale start time is required")
    @Column(nullable = false)
    private LocalDateTime saleStartTime;

    @NotNull(message = "Sale end time is required")
    @Column(nullable = false)
    private LocalDateTime saleEndTime;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
