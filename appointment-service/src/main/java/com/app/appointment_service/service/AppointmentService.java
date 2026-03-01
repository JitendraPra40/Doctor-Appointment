package com.app.appointment_service.service;

import com.app.appointment_service.entity.Appointment;
import com.app.appointment_service.event.AppointmentBookedEvent;
import com.app.appointment_service.event.AppointmentCancelledEvent;
import com.app.appointment_service.repository.AppointmentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Service
@Transactional
public class AppointmentService {

    // ✅ SLF4J Logger — use LoggerFactory, NOT @Slf4j here so it's explicit
    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private static final String KAFKA_CB       = "kafkaPublisher";
    private static final String TOPIC_BOOKED   = "appointment-events";
    private static final String TOPIC_CANCELLED = "appointment-cancelled-events";

    private final AppointmentRepository repo;
    private final KafkaTemplate<String, Object> kafka;

    public AppointmentService(AppointmentRepository repo, KafkaTemplate<String, Object> kafka) {
        this.repo  = repo;
        this.kafka = kafka;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SAGA STEP 1 — Book appointment
    // Publishes AppointmentBookedEvent → triggers PaymentService (next saga step)
    // If payment fails, PaymentService publishes AppointmentCancelledCommand
    // and this service listens and executes compensateBooking() below.
    // ─────────────────────────────────────────────────────────────────────────
    public Appointment book(UUID doctorId, UUID patientId, LocalDate date, LocalTime time) {
        // Add traceId to MDC for structured logging across the saga
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);

        log.info("📅 [SAGA-START] Booking appointment | doctor={} patient={} date={} time={} traceId={}",
                doctorId, patientId, date, time, traceId);

        // Check slot availability
        if (repo.existsByDoctorIdAndAppointmentDateAndStartTime(doctorId, date, time)) {
            log.warn("⚠️ Slot already booked | doctor={} date={} time={}", doctorId, date, time);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This slot is already booked. Please choose a different time.");
        }

        // Persist appointment
        Appointment appt = new Appointment();
        appt.setDoctorId(doctorId);
        appt.setPatientId(patientId);
        appt.setAppointmentDate(date);
        appt.setStartTime(time);
        appt.setStatus(Appointment.Status.BOOKED);
        Appointment saved = repo.save(appt);

        log.info("✅ [SAGA-STEP-1] Appointment saved | id={} status={}", saved.getId(), saved.getStatus());

        // SAGA: Publish event — payment-service listens and initiates payment
        publishBookedEvent(saved, traceId);

        MDC.clear();
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SAGA COMPENSATING TRANSACTION — Cancel appointment
    // Called when payment fails (via Kafka) or by patient directly.
    // This is the "rollback" step of the saga.
    // ─────────────────────────────────────────────────────────────────────────
    public void cancelAppointment(UUID appointmentId, String reason) {
        MDC.put("traceId", appointmentId.toString().substring(0, 8));

        log.info("🔄 [SAGA-COMPENSATE] Cancelling appointment | id={} reason={}", appointmentId, reason);

        Appointment appt = repo.findById(appointmentId)
                .orElseThrow(() -> {
                    log.error("❌ Appointment not found for cancellation | id={}", appointmentId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found");
                });

        appt.setStatus(Appointment.Status.CANCELLED);
        repo.save(appt);

        log.info("✅ [SAGA-COMPENSATE] Appointment cancelled | id={}", appointmentId);

        // Notify other services of cancellation
        publishCancelledEvent(appt, reason);

        MDC.clear();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Kafka publish with Circuit Breaker + Retry
    // If Kafka is down: retries 3x, then opens circuit, falls back gracefully.
    // The appointment is STILL saved — notification is best-effort.
    // ─────────────────────────────────────────────────────────────────────────
    @CircuitBreaker(name = KAFKA_CB, fallbackMethod = "publishBookedFallback")
    @Retry(name = KAFKA_CB)
    public void publishBookedEvent(Appointment appt, String traceId) {
        AppointmentBookedEvent event = new AppointmentBookedEvent(
                appt.getId(),
                appt.getDoctorId(),
                appt.getPatientId(),
                appt.getAppointmentDate(),
                appt.getStartTime(),
                traceId
        );
        kafka.send(TOPIC_BOOKED, appt.getId().toString(), event);
        log.info("📨 [SAGA-EVENT] Published AppointmentBookedEvent | appointmentId={} topic={}",
                appt.getId(), TOPIC_BOOKED);
    }

    @CircuitBreaker(name = KAFKA_CB, fallbackMethod = "publishCancelledFallback")
    @Retry(name = KAFKA_CB)
    public void publishCancelledEvent(Appointment appt, String reason) {
        AppointmentCancelledEvent event = new AppointmentCancelledEvent(
                appt.getId(),
                appt.getPatientId(),
                appt.getDoctorId(),
                reason
        );
        kafka.send(TOPIC_CANCELLED, appt.getId().toString(), event);
        log.info("📨 [SAGA-EVENT] Published AppointmentCancelledEvent | appointmentId={}", appt.getId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fallback methods — called when circuit is OPEN or retries exhausted
    // ─────────────────────────────────────────────────────────────────────────
    public void publishBookedFallback(Appointment appt, String traceId, Throwable t) {
        log.error("🔴 [CIRCUIT-OPEN] Kafka unavailable for BookedEvent | appointmentId={} error={} " +
                        "→ Appointment saved but notification skipped. Will retry when circuit closes.",
                appt.getId(), t.getMessage());
        // In production: store in outbox table for later retry
    }

    public void publishCancelledFallback(Appointment appt, String reason, Throwable t) {
        log.error("🔴 [CIRCUIT-OPEN] Kafka unavailable for CancelledEvent | appointmentId={} error={}",
                appt.getId(), t.getMessage());
    }
}
