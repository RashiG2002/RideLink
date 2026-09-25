package com.ridelink.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
// 1. Discover and register the application's Feign HTTP clients.
@EnableFeignClients
public class PaymentApplication {

    public static void main(String[] args) {
        // 2. Start Spring Boot, which loads the application context and runs the service.
        SpringApplication.run(PaymentApplication.class, args);
    }

}