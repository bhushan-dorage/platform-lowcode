package com.platform.studio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class StudioBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(StudioBackendApplication.class, args);
    }
}
