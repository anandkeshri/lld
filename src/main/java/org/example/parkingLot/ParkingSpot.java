package org.example.parkingLot;

import java.util.concurrent.atomic.AtomicBoolean;

public class ParkingSpot {
    private int id;
    AtomicBoolean occupied;
    SpotType type;
}
