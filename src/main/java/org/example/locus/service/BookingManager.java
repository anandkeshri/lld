package org.example.locus.service;

import org.example.locus.models.Booking;
import org.example.locus.models.Room;
import org.example.locus.strategy.BookingStrategy;
import org.example.locus.strategy.SimpleStrategy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BookingManager {
    ConcurrentHashMap<Integer, Booking> bookings;
    ConcurrentHashMap<Integer, List<Booking>> roomBookings;
    RoomManager roomManager;
    BookingStrategy strategy;
    private static final Object lock = new Object();
    AtomicInteger idGenerator = new AtomicInteger(0);

    public void setStrategy(BookingStrategy strategy) {
        this.strategy = strategy;
    }

    public BookingManager(RoomManager roomManager) {
        bookings = new ConcurrentHashMap<Integer, Booking>();
        roomBookings = new ConcurrentHashMap<Integer, List<Booking>>();
        this.roomManager = roomManager;
        strategy = new SimpleStrategy(); // default
    }

    public Integer bookRoom(LocalDateTime start, LocalDateTime end, int capacity){
        Booking booking = null;
        synchronized (lock) {
            int id = idGenerator.incrementAndGet();

            booking = strategy.bookARoom(start, end, capacity, roomBookings, roomManager.getRooms(), id);
            if(booking!=null){
                bookings.put(booking.getId(), booking);
            }
        }
        return booking!=null ? booking.getId() : -1;
    }
}
