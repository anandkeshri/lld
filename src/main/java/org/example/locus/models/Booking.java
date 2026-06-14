package org.example.locus.models;

import java.time.LocalDateTime;

public class Booking {
    private Integer id;
    private Integer roomId;
    private LocalDateTime start;
    private LocalDateTime end;

    public Booking(Integer id, Integer roomId, LocalDateTime start, LocalDateTime end) {
        this.id = id;
        this.roomId = roomId;
        this.start = start;
        this.end = end;
    }

    public Booking() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public void setEnd(LocalDateTime end) {
        this.end = end;
    }
}
