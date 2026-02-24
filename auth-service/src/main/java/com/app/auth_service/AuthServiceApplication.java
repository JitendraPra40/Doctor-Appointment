package com.app.auth_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

// FIX: Removed @EnableResourceServer — it's from legacy spring-security-oauth2-autoconfigure
// which is incompatible with Spring Boot 3.x. Spring Boot 3 uses oauth2-resource-server starter.
@SpringBootApplication
@EnableDiscoveryClient
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

}
