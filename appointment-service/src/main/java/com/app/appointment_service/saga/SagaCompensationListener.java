package com.app.appointment_service.saga;

import com.app.appointment_service.event.PaymentFailedEvent;
import com.app.appointment_service.service.AppointmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * SAGA COMPENSATION LISTENER
 *
 * This is the heart of the Choreography-based Saga pattern.
 *
 * When payment-service cannot process a payment, it publishes a PaymentFailedEvent.
 * This listener catches it and triggers the compensating transaction:
 *   → cancels the appointment (sets status = CANCELLED)
 *   → publishes AppointmentCancelledEvent for notification-service
 *
 * Saga Flow (Happy Path):
 *   [1] appointment-service  BOOKS appointment
 *   [2] appointment-service  publishes AppointmentBookedEvent
 *   [3] payment-service      receives event, initiates Razorpay order
 *   [4] patient              completes payment
 *   [5] payment-service      publishes PaymentSuccessEvent
 *   [6] notification-service sends confirmation
 *
 * Saga Flow (Failure / Compensation):
 *   [1] appointment-service  BOOKS appointment
 *   [2] appointment-service  publishes AppointmentBookedEvent
 *   [3] payment-service      fails (timeout / Razorpay error)
 *   [4] payment-service      publishes PaymentFailedEvent  ← we listen here
 *   [5] THIS LISTENER        cancels the appointment (compensating tx)
 *   [6] THIS LISTENER        publishes AppointmentCancelledEvent
 *   [7] notification-service sends cancellation email to patient
 */
@Component
public class SagaCompensationListener {

    private static final Logger log = LoggerFactory.getLogger(SagaCompensationListener.class);

    private final AppointmentService appointmentService;

    public SagaCompensationListener(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @KafkaListener(
            topics = "payment-failed-events",
            groupId = "appointment-saga-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentFailed(PaymentFailedEvent event) {
        MDC.put("traceId", event.traceId() != null ? event.traceId() : "unknown");

        log.warn("⚠️ [SAGA-COMPENSATION] PaymentFailedEvent received | appointmentId={} reason={} traceId={}",
                event.appointmentId(), event.reason(), event.traceId());

        try {
            appointmentService.cancelAppointment(event.appointmentId(), "PAYMENT_FAILED: " + event.reason());
            log.info("✅ [SAGA-COMPENSATION] Appointment successfully rolled back | appointmentId={}",
                    event.appointmentId());
        } catch (Exception e) {
            log.error("❌ [SAGA-COMPENSATION] Failed to rollback appointment | appointmentId={} error={}",
                    event.appointmentId(), e.getMessage(), e);
            // In production: write to a dead-letter topic or outbox for manual intervention
        } finally {
            MDC.clear();
        }
    }
}
