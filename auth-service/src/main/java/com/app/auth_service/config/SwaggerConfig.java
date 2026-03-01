package com.app.auth_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Service API")
                        .description("""
                                Handles registration, OTP verification, login and JWT token issuance.
                                
                                **Flow:**
                                1. POST /register → sends OTP to email
                                2. POST /verify-otp → verifies OTP
                                3. POST /login → returns JWT access token + refresh token
                                4. POST /refresh → exchange refresh token for new access token
                                """)
                        .version("1.0.0"))
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Direct"),
                        new Server().url("http://localhost:8080").description("Via API Gateway")))
                // Auth endpoints are public but we still allow testing with Bearer token
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components().addSecuritySchemes("Bearer Authentication",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Only needed for /refresh and admin endpoints")));
    }
}
