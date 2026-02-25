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

    /**
     * FIX: Inject the JWKS URI from application.properties.
     *
     * ROOT CAUSE OF 401:
     * The gateway was calling oauth2.jwt(jwt -> {}) with NO explicit JwtDecoder.
     * Spring then tries to auto-configure a JwtDecoder using issuer-uri, which
     * performs an OIDC discovery call to:
     *   http://localhost:8081/.well-known/openid-configuration
     *
     * Your auth-service is NOT a standard OIDC provider — it doesn't expose that
     * discovery endpoint. So the gateway's JwtDecoder fails to initialize and
     * rejects ALL tokens with 401, even valid ones.
     *
     * FIX: Explicitly build a NimbusReactiveJwtDecoder pointed directly at your
     * auth-service JWKS endpoint. This bypasses OIDC discovery entirely.
     */
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwksUri;

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        // Explicitly tell gateway HOW to validate tokens:
        // fetch public keys from auth-service JWKS endpoint and verify signatures
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
                                "/sso-auth-server/.well-known/jwks.json",
                                "/.well-known/jwks.json"
                        ).permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        // FIX: Wire in our explicit JwtDecoder instead of empty jwt -> {}
                        oauth2.jwt(jwt -> jwt.jwtDecoder(jwtDecoder()))
                )
                .build();
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                // Auth service — public
                .route("auth-service", r -> r
                        .path("/api/v1/auth/**", "/sso-auth-server/.well-known/jwks.json")
                        .uri("lb://AUTH-SERVICE")
                )
                // Doctor service
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
