package org.example.locus.strategy;

import org.example.locus.models.Booking;
import org.example.locus.models.Room;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public interface BookingStrategy {
    Booking bookARoom(LocalDateTime start, LocalDateTime end, int capacity, ConcurrentHashMap<Integer, List<Booking>> bookings, HashMap<Integer, Room> rooms, int id);
}
