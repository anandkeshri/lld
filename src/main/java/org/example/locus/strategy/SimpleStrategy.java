package org.example.locus.strategy;

import org.example.locus.models.Booking;
import org.example.locus.models.Room;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleStrategy implements BookingStrategy {

    @Override
    public Booking bookARoom(LocalDateTime start, LocalDateTime end, int capacity, ConcurrentHashMap<Integer, List<Booking>> roomBookings, HashMap<Integer, Room> rooms, int id) {

        for(Map.Entry<Integer, Room> entry : rooms.entrySet()){
            Room room = entry.getValue();
            if(room.getCapacity() >= capacity){
                List<Booking> bookings = roomBookings.get(room.getId());
                if(bookings == null || bookings.isEmpty()){
                    Booking booking = new Booking(id, room.getId(), start, end);
                    List<Booking> books = new ArrayList<>();
                    books.add(booking);
                    roomBookings.put(room.getId(), books);
                    return booking;
                }
                else {
                    Collections.sort(bookings, (a, b) -> a.getStart().compareTo(b.getStart()));

                    int n = bookings.size();
                    if(bookings.get(0).getStart().isAfter(end) || bookings.get(n-1).getEnd().isBefore(start)){

                    }
                    Booking booking = null;
                    for(int i=0;i<n-1;i++){
                        if(bookings.get(i).getEnd().isBefore(start) && bookings.get(i+1).getStart().isAfter(end)){
                            booking = new Booking(id, room.getId(), start, end);
                            break;
                        }
                    }
                    if(booking != null){
                        bookings.add(booking);
                        return booking;
                    }
                }
            }
        }

        return null;
    }
}
