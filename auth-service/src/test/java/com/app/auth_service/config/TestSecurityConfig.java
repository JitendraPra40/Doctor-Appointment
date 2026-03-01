package com.app.auth_service.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test-only security configuration.
 *
 * Disables JWT validation so @WebMvcTest slice tests can test
 * controller logic without needing a real JWKS endpoint.
 *
 * Usage — import in your @WebMvcTest class:
 *
 *   @WebMvcTest(MyController.class)
 *   @Import(TestSecurityConfig.class)
 *   class MyControllerTest { ... }
 *
 * For role-based tests, still use @WithMockUser(roles = "PATIENT").
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }
}
