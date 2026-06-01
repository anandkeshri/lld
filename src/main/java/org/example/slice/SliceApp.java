package org.example.slice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SliceApp {
    public static void main(String[] args) {
        SpringApplication.run(SliceApp.class, args);
    }
}
