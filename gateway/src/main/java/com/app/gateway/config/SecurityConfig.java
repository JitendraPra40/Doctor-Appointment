package com.app.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwksUri;

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwksUri).build();
    }

    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex
                        .pathMatchers(
                                "/api/v1/auth/**",
                                "/actuator/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/webjars/**",
                                // Per-service swagger docs proxied through gateway
                                "/auth-service/api-docs",
                                "/doctor-service/api-docs",
                                "/patient-service/api-docs",
                                "/appointment-service/api-docs",
                                "/payment-service/api-docs",
                                "/sso-auth-server/.well-known/jwks.json",
                                "/.well-known/jwks.json"
                        ).permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> jwt.jwtDecoder(jwtDecoder()))
                )
                .build();
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()

                // ── Auth Service ───────────────────────────────────────────
                .route("auth-service", r -> r
                        .path("/api/v1/auth/**", "/sso-auth-server/.well-known/jwks.json")
                        .uri("lb://AUTH-SERVICE")
                )
                // Proxy auth-service Swagger docs through gateway
                .route("auth-service-docs", r -> r
                        .path("/auth-service/api-docs")
                        .filters(f -> f.rewritePath("/auth-service/api-docs", "/api-docs"))
                        .uri("lb://AUTH-SERVICE")
                )

                // ── Doctor Service ─────────────────────────────────────────
                .route("doctor-service", r -> r
                        .path("/api/v1/doctors/**")
                        // Circuit breaker: if doctor-service is down, return 503 immediately
                        .filters(f -> f.circuitBreaker(c -> c
                                .setName("doctorService")
                                .setFallbackUri("forward:/fallback/doctor-service")))
                        .uri("lb://DOCTOR-SERVICE")
                )
                .route("doctor-service-docs", r -> r
                        .path("/doctor-service/api-docs")
                        .filters(f -> f.rewritePath("/doctor-service/api-docs", "/api-docs"))
                        .uri("lb://DOCTOR-SERVICE")
                )

                // ── Patient Service ────────────────────────────────────────
                .route("patient-service", r -> r
                        .path("/api/v1/patients/**")
                        .filters(f -> f.circuitBreaker(c -> c
                                .setName("patientService")
                                .setFallbackUri("forward:/fallback/patient-service")))
                        .uri("lb://PATIENT-SERVICE")
                )
                .route("patient-service-docs", r -> r
                        .path("/patient-service/api-docs")
                        .filters(f -> f.rewritePath("/patient-service/api-docs", "/api-docs"))
                        .uri("lb://PATIENT-SERVICE")
                )

                // ── Appointment Service ────────────────────────────────────
                .route("appointment-service", r -> r
                        .path("/api/v1/appointments/**")
                        .filters(f -> f.circuitBreaker(c -> c
                                .setName("appointmentService")
                                .setFallbackUri("forward:/fallback/appointment-service")))
                        .uri("lb://APPOINTMENT-SERVICE")
                )
                .route("appointment-service-docs", r -> r
                        .path("/appointment-service/api-docs")
                        .filters(f -> f.rewritePath("/appointment-service/api-docs", "/api-docs"))
                        .uri("lb://APPOINTMENT-SERVICE")
                )

                // ── Payment Service ────────────────────────────────────────
                .route("payment-service", r -> r
                        .path("/api/v1/payments/**")
                        .filters(f -> f.circuitBreaker(c -> c
                                .setName("paymentService")
                                .setFallbackUri("forward:/fallback/payment-service")))
                        .uri("lb://PAYMENT-SERVICE")
                )
                .route("payment-service-docs", r -> r
                        .path("/payment-service/api-docs")
                        .filters(f -> f.rewritePath("/payment-service/api-docs", "/api-docs"))
                        .uri("lb://PAYMENT-SERVICE")
                )

                .build();
    }
}
