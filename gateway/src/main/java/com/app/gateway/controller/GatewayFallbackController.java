package com.app.gateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Fallback controller for gateway circuit breakers.
 * When a downstream service circuit is OPEN, Gateway forwards here
 * instead of letting the request hang or return a cryptic 503.
 */
@RestController
@RequestMapping("/fallback")
public class GatewayFallbackController {

    private static final Logger log = LoggerFactory.getLogger(GatewayFallbackController.class);

    @GetMapping("/doctor-service")
    @PostMapping("/doctor-service")
    public ResponseEntity<Map<String, Object>> doctorServiceFallback() {
        log.warn("🔴 [CIRCUIT-OPEN] doctor-service unavailable — returning fallback");
        return fallbackResponse("doctor-service");
    }

    @GetMapping("/patient-service")
    @PostMapping("/patient-service")
    public ResponseEntity<Map<String, Object>> patientServiceFallback() {
        log.warn("🔴 [CIRCUIT-OPEN] patient-service unavailable — returning fallback");
        return fallbackResponse("patient-service");
    }

    @GetMapping("/appointment-service")
    @PostMapping("/appointment-service")
    public ResponseEntity<Map<String, Object>> appointmentServiceFallback() {
        log.warn("🔴 [CIRCUIT-OPEN] appointment-service unavailable — returning fallback");
        return fallbackResponse("appointment-service");
    }

    @GetMapping("/payment-service")
    @PostMapping("/payment-service")
    public ResponseEntity<Map<String, Object>> paymentServiceFallback() {
        log.warn("🔴 [CIRCUIT-OPEN] payment-service unavailable — returning fallback");
        return fallbackResponse("payment-service");
    }

    private ResponseEntity<Map<String, Object>> fallbackResponse(String service) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "status", 503,
                "error", "Service Temporarily Unavailable",
                "service", service,
                "message", service + " is currently down or overloaded. Please try again in a few moments.",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
