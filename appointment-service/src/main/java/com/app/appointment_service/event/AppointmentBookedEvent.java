package com.app.appointment_service.event;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * SAGA EVENT — Step 1 of the Appointment Booking Saga.
 *
 * Flow:
 *   appointment-service  →  [appointment-events]  →  payment-service
 *
 * On receipt, payment-service initiates the Razorpay order.
 * If payment fails, payment-service publishes PaymentFailedEvent
 * which triggers the compensating transaction in appointment-service.
 */
public record AppointmentBookedEvent(
        UUID appointmentId,
        UUID doctorId,
        UUID patientId,
        LocalDate appointmentDate,
        LocalTime startTime,
        String traceId          // correlation ID for distributed tracing across saga steps
) {}
