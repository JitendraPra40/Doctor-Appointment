package com.app.appointment_service.event;

import java.util.UUID;

/**
 * SAGA COMPENSATING EVENT — Published when an appointment is cancelled.
 *
 * Scenarios that trigger this event:
 *   1. Patient manually cancels via DELETE /api/v1/appointments/{id}
 *   2. Payment fails → payment-service publishes PaymentFailedEvent
 *      → appointment-service listens and cancels appointment, then publishes this event
 *
 * Consumers:
 *   notification-service — sends cancellation email/SMS to patient
 */
public record AppointmentCancelledEvent(
        UUID appointmentId,
        UUID patientId,
        UUID doctorId,
        String reason           // e.g. "PAYMENT_FAILED", "PATIENT_REQUESTED", "DOCTOR_UNAVAILABLE"
) {}
