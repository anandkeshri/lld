package org.example.slice.eventTicketBookingSystem.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EventPatchRequest {
    private String name;
    private String description;
    private LocalDateTime scheduledTime;
    private String venue;
    private List<String> seatTypes;
    private LocalDateTime saleStartTime;
    private LocalDateTime saleEndTime;
}
