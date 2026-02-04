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

//    @Bean
//    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
//
//        return http
//                .csrf(ServerHttpSecurity.CsrfSpec::disable)
//                .authorizeExchange(ex -> ex
//                        .pathMatchers(
//                                "/api/v1/auth/**",
//                                "/actuator/**"
//                        ).permitAll()
//                        .anyExchange().authenticated()
//                )
//                .oauth2ResourceServer(oauth2 ->
//                        oauth2.jwt(jwt -> {})
//                )
//                .build();
//    }
//
//    @Configuration
//    public class GatewayRoutes {
//
//        @Bean
//        public RouteLocator routes(RouteLocatorBuilder builder) {
//            return builder.routes()
//                    .route("doctor-service", r -> r
//                            .path("/api/v1/doctors/**")
//                            .filters(f -> f
//                                    .addRequestHeader("X-GATEWAY", "API-GATEWAY"))
//                            .uri("lb://DOCTOR-SERVICE")
//                    )
//
//                    .route("auth-service", r -> r
//                            .path("/api/v1/auth/**")
//                            .uri("lb://AUTH-SERVICE")
//                    )
//                    .build();
//        }
//    }


    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex
                        .anyExchange().permitAll()
                )
                .build();
    }

}

