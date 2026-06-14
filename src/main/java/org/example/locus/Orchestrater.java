package org.example.locus;

import org.example.locus.models.Booking;
import org.example.locus.models.Room;
import org.example.locus.service.BookingManager;
import org.example.locus.service.RoomManager;
import org.example.locus.strategy.BookingStrategy;

import java.time.LocalDateTime;

public class Orchestrater {

    BookingManager bookingManager;
    RoomManager roomManager;
    BookingStrategy strategy;

    public Orchestrater(BookingStrategy strategy) {
        this.strategy = strategy;
        roomManager = new RoomManager();
        bookingManager = new BookingManager(roomManager);
    }

    public Orchestrater() {
        roomManager = new RoomManager();
        bookingManager = new BookingManager(roomManager);
    }

    public Integer bookRoom(LocalDateTime start, LocalDateTime end, int capacity){
        Integer id = bookingManager.bookRoom(start, end, capacity);
        if(id > 0){
            System.out.printf("room booked with id : %d\n", id);
            return id;
        }
        else{
            System.out.printf("sorry! No rooms available \n");
            return -1;
        }
    }

    public Integer createARoom(int cap){
        Room room = roomManager.create(cap);
        if(room == null){
            System.out.println("room cannot be created");
            return -1;
        }
        else {
            System.out.printf("room created with id : %d \n", room.getId());
            return room.getId();
        }
    }
}
