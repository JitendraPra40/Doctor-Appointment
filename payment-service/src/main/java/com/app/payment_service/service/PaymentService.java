package com.app.payment_service.service;

import com.app.payment_service.entity.Payment;
import com.app.payment_service.entity.Payment.PaymentStatus;
import com.app.payment_service.event.PaymentFailedEvent;
import com.app.payment_service.event.PaymentSuccessEvent;
import com.app.payment_service.repository.PaymentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private static final String KAFKA_CB              = "kafkaPublisher";
    private static final String RAZORPAY_CB           = "razorpayClient";
    private static final String TOPIC_SUCCESS         = "payment-events";
    private static final String TOPIC_FAILED          = "payment-failed-events";

    private final PaymentRepository repo;
    private final KafkaTemplate<String, Object> kafka;

    public PaymentService(PaymentRepository repo, KafkaTemplate<String, Object> kafka) {
        this.repo  = repo;
        this.kafka = kafka;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SAGA STEP 2 — Confirm payment
    // Called by patient after Razorpay checkout on frontend.
    // On success → publishes PaymentSuccessEvent (notification)
    // On failure → publishes PaymentFailedEvent (triggers appointment cancellation)
    // ─────────────────────────────────────────────────────────────────────────
    public Payment confirmPayment(String orderId, String paymentId,
                                  BigDecimal amount, UUID appointmentId) {
        String traceId = appointmentId.toString().substring(0, 8);
        MDC.put("traceId", traceId);

        log.info("💳 [SAGA-STEP-2] Payment confirmation | orderId={} paymentId={} appointmentId={}",
                orderId, paymentId, appointmentId);

        // Idempotency check — prevents duplicate payment processing
        if (repo.existsByRazorpayPaymentId(paymentId)) {
            log.warn("⚠️ Duplicate payment ignored | paymentId={}", paymentId);
            MDC.clear();
            return repo.findByRazorpayPaymentId(paymentId).orElseThrow();
        }

        Payment payment = new Payment();
        payment.setAppointmentId(appointmentId);
        payment.setRazorpayOrderId(orderId);
        payment.setRazorpayPaymentId(paymentId);
        payment.setAmount(amount.doubleValue());
        payment.setStatus(PaymentStatus.SUCCESS);
        Payment saved = repo.save(payment);

        log.info("✅ [SAGA-STEP-2] Payment saved | id={} status={}", saved.getId(), saved.getStatus());

        // Publish success event → notification-service sends receipt
        publishSuccessEvent(saved, traceId);

        MDC.clear();
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SAGA FAILURE — Called when Razorpay payment fails
    // Publishes PaymentFailedEvent → triggers appointment cancellation (compensation)
    // ─────────────────────────────────────────────────────────────────────────
    public void handlePaymentFailure(UUID appointmentId, UUID patientId,
                                     String reason, String traceId) {
        MDC.put("traceId", traceId != null ? traceId : appointmentId.toString().substring(0, 8));

        log.error("❌ [SAGA-FAILURE] Payment failed | appointmentId={} reason={}", appointmentId, reason);

        // Save failed payment record for audit
        Payment payment = new Payment();
        payment.setAppointmentId(appointmentId);
        payment.setStatus(PaymentStatus.FAILED);
        payment.setAmount(0.0);
        repo.save(payment);

        // Publish failure event → appointment-service will compensate (cancel appointment)
        publishFailedEvent(appointmentId, patientId, reason, MDC.get("traceId"));

        MDC.clear();
    }

    @CircuitBreaker(name = KAFKA_CB, fallbackMethod = "publishSuccessFallback")
    @Retry(name = KAFKA_CB)
    public void publishSuccessEvent(Payment payment, String traceId) {
        PaymentSuccessEvent event = new PaymentSuccessEvent(
                payment.getId(), payment.getAppointmentId(),
                payment.getPatientId(), payment.getAmount(), traceId
        );
        kafka.send(TOPIC_SUCCESS, payment.getId().toString(), event);
        log.info("📨 [SAGA-EVENT] Published PaymentSuccessEvent | paymentId={}", payment.getId());
    }

    @CircuitBreaker(name = KAFKA_CB, fallbackMethod = "publishFailedFallback")
    @Retry(name = KAFKA_CB)
    public void publishFailedEvent(UUID appointmentId, UUID patientId,
                                   String reason, String traceId) {
        PaymentFailedEvent event = new PaymentFailedEvent(appointmentId, patientId, reason, traceId);
        kafka.send(TOPIC_FAILED, appointmentId.toString(), event);
        log.info("📨 [SAGA-EVENT] Published PaymentFailedEvent | appointmentId={}", appointmentId);
    }

    // Fallbacks
    public void publishSuccessFallback(Payment payment, String traceId, Throwable t) {
        log.error("🔴 [CIRCUIT-OPEN] Cannot publish PaymentSuccessEvent | paymentId={} error={}",
                payment.getId(), t.getMessage());
    }

    public void publishFailedFallback(UUID appointmentId, UUID patientId,
                                      String reason, String traceId, Throwable t) {
        log.error("🔴 [CIRCUIT-OPEN] Cannot publish PaymentFailedEvent | appointmentId={} error={} " +
                        "→ CRITICAL: Appointment may remain in BOOKED state without payment",
                appointmentId, t.getMessage());
    }
}
