package com.app.gateway.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@WebFluxTest(GatewayFallbackController.class)
@DisplayName("GatewayFallbackController Tests")
class GatewayFallbackControllerTest {

    @Autowired
    private WebTestClient webClient;

    // ═════════════════════════════════════════════════════════════════════════
    // Each fallback endpoint returns 503 with correct JSON
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Service fallback endpoints")
    class FallbackEndpointTests {

        @Test
        @DisplayName("GET /fallback/doctor-service returns 503 with service name in body")
        void doctorServiceFallback_returns503() {
            webClient.get().uri("/fallback/doctor-service")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(503)
                    .jsonPath("$.service").isEqualTo("doctor-service")
                    .jsonPath("$.error").isEqualTo("Service Temporarily Unavailable")
                    .jsonPath("$.message").value(msg ->
                            assertThat(msg.toString()).contains("doctor-service"))
                    .jsonPath("$.timestamp").isNotEmpty();
        }

        @Test
        @DisplayName("POST /fallback/doctor-service returns 503")
        void doctorServiceFallback_post_returns503() {
            webClient.post().uri("/fallback/doctor-service")
                    .contentType(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        }

        @Test
        @DisplayName("GET /fallback/patient-service returns 503 with service name")
        void patientServiceFallback_returns503() {
            webClient.get().uri("/fallback/patient-service")
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                    .expectBody()
                    .jsonPath("$.service").isEqualTo("patient-service");
        }

        @Test
        @DisplayName("GET /fallback/appointment-service returns 503 with service name")
        void appointmentServiceFallback_returns503() {
            webClient.get().uri("/fallback/appointment-service")
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                    .expectBody()
                    .jsonPath("$.service").isEqualTo("appointment-service");
        }

        @Test
        @DisplayName("GET /fallback/payment-service returns 503 with service name")
        void paymentServiceFallback_returns503() {
            webClient.get().uri("/fallback/payment-service")
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                    .expectBody()
                    .jsonPath("$.service").isEqualTo("payment-service");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Common response structure
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Response body structure")
    class ResponseStructureTests {

        @Test
        @DisplayName("response body always includes all required fields: status, error, service, message, timestamp")
        void fallback_responseBodyHasAllRequiredFields() {
            webClient.get().uri("/fallback/doctor-service")
                    .exchange()
                    .expectBody()
                    .jsonPath("$.status").exists()
                    .jsonPath("$.error").exists()
                    .jsonPath("$.service").exists()
                    .jsonPath("$.message").exists()
                    .jsonPath("$.timestamp").exists();
        }

        @Test
        @DisplayName("status field in body is always 503")
        void fallback_bodyStatusFieldAlways503() {
            webClient.get().uri("/fallback/payment-service")
                    .exchange()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(503);
        }

        @Test
        @DisplayName("message contains the service name")
        void fallback_messageContainsServiceName() {
            webClient.get().uri("/fallback/appointment-service")
                    .exchange()
                    .expectBody()
                    .jsonPath("$.message").value(msg ->
                            assertThat(msg.toString()).contains("appointment-service"));
        }
    }
}
