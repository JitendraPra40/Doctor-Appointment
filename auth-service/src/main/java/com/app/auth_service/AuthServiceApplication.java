package com.app.auth_service;

import com.app.auth_service.propertiesConfig.JwtProperties;
import com.app.auth_service.propertiesConfig.TwilioProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

// FIX: Removed @EnableResourceServer — it's from legacy spring-security-oauth2-autoconfigure
// which is incompatible with Spring Boot 3.x. Spring Boot 3 uses oauth2-resource-server starter.
@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties({ JwtProperties.class, TwilioProperties.class })
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

}
