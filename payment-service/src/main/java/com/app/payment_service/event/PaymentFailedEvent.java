package com.app.payment_service.event;

import java.util.UUID;

/**
 * SAGA FAILURE EVENT — Published by payment-service when payment cannot be processed.
 *
 * Consumed by:
 *   - appointment-service → SagaCompensationListener → cancels the appointment
 *   - notification-service → sends failure notification to patient
 */
public record PaymentFailedEvent(
        UUID appointmentId,
        UUID patientId,
        String reason,
        String traceId
) {}
