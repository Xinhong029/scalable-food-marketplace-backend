package com.lastcalleats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Starts the LastCall Eats Spring Boot application.
 * This class serves as the main entry point for bootstrapping the API module and loading the application context.
 */
@SpringBootApplication
@EnableScheduling
public class LastCallEatsApplication {

    public static void main(String[] args) {
        SpringApplication.run(LastCallEatsApplication.class, args);
    }
}
