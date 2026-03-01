package com.app.appointment_service.event;

import java.util.UUID;

/**
 * Received from payment-service when payment processing fails.
 * Triggers the saga compensating transaction in appointment-service.
 */
public record PaymentFailedEvent(
        UUID appointmentId,
        UUID patientId,
        String reason,
        String traceId
) {}
