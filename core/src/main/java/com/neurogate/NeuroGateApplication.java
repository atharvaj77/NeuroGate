package com.neurogate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NeuroGateApplication {

    public static void main(String[] args) {
        SpringApplication.run(NeuroGateApplication.class, args);
    }
}
