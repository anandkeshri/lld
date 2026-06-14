package org.example.locus;

import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {
        System.out.println("hello");

        Orchestrater app = new Orchestrater();

        app.bookRoom(LocalDateTime.now(), LocalDateTime.now().plusHours(1), 2);

        app.createARoom(4);
        app.bookRoom(LocalDateTime.now(), LocalDateTime.now().plusHours(1), 2);
        app.bookRoom(LocalDateTime.now(), LocalDateTime.now().plusHours(1), 2);
    }
}
