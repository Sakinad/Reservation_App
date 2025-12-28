package org.example.reservation_event;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ReservationEventApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReservationEventApplication.class, args);
    }
}