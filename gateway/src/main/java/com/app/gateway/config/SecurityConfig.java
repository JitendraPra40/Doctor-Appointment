package com.app.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex
                        .pathMatchers(
                                "/api/v1/auth/**",
                                "/actuator/**",
                                "/sso-auth-server/.well-known/jwks.json",
                                "/.well-known/jwks.json"
                        ).permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> {})
                )
                .build();
    }

    /**
     * FIX: Moved GatewayRoutes out of nested class into top-level @Bean.
     * Nested @Configuration classes work but can cause Spring context ordering issues.
     *
     * FIX: tokenRelay() requires oauth2-client to be configured with a registered client.
     * For local dev where we just want to pass the Bearer token through, we can use
     * a simple AddRequestHeader filter or keep tokenRelay() if oauth2-client is configured.
     * We keep tokenRelay() here but note it needs:
     *   spring.security.oauth2.client.registration.* config to function.
     * If you don't need SSO client login at gateway level, remove tokenRelay().
     *
     * FIX: Added JWKS proxy route so clients can reach auth-service JWKS from gateway.
     */
    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth service — public, no token relay needed
                .route("auth-service", r -> r
                        .path("/api/v1/auth/**", "/sso-auth-server/.well-known/jwks.json")
                        .uri("lb://AUTH-SERVICE")
                )
                // Doctor service — forward JWT from incoming request
                .route("doctor-service", r -> r
                        .path("/api/v1/doctors/**")
                        .uri("lb://DOCTOR-SERVICE")
                )
                // Patient service
                .route("patient-service", r -> r
                        .path("/api/v1/patients/**")
                        .uri("lb://PATIENT-SERVICE")
                )
                // Appointment service
                .route("appointment-service", r -> r
                        .path("/api/v1/appointments/**")
                        .uri("lb://APPOINTMENT-SERVICE")
                )
                // Payment service
                .route("payment-service", r -> r
                        .path("/api/v1/payments/**")
                        .uri("lb://PAYMENT-SERVICE")
                )
                .build();
    }
}
