package com.app.payment_service.event;

import java.util.UUID;

/**
 * SAGA EVENT — Published by payment-service on successful payment.
 * Consumed by notification-service to send receipt email/SMS.
 */
public record PaymentSuccessEvent(
        UUID paymentId,
        UUID appointmentId,
        UUID patientId,
        Double amount,
        String traceId
) {}
