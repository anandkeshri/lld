package org.example.parkingLot;

public class ParkingLot {
    private static volatile ParkingLot instance;
    private String name;
    private int totalFloors;
    private int totalSpots;

    private ParkingLot(){

    }

    public static ParkingLot getInstance(){
        if(instance == null){
            synchronized (ParkingLot.class) {
                if(instance == null){
                    instance = new ParkingLot();
                }
            }
        }
        return instance;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }
}
