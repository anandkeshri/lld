package org.example.locus.service;

import org.example.locus.models.Room;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RoomManager {
    private ConcurrentHashMap<Integer, Room> rooms;
    private AtomicInteger idGenerator = new AtomicInteger(0);

    public RoomManager() {
        this.rooms = new ConcurrentHashMap<Integer, Room>();
    }

    public HashMap<Integer, Room> getRooms() {
        return new HashMap<>(rooms);
    }

    public Room create(int capacity){
        Integer id = idGenerator.getAndIncrement();
        Room room = new Room(id, capacity);
        rooms.put(id, room);
        return room;
    }
}
