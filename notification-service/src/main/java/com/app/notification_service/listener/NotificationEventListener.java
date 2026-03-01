package com.app.notification_service.listener;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Listens to ALL saga events and sends notifications.
 * Each email send is wrapped with Circuit Breaker + Retry
 * so if Gmail SMTP is flaky, the saga is NOT affected.
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);
    private static final String EMAIL_CB = "emailSender";

    private final JavaMailSender mailSender;

    public NotificationEventListener(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ── SAGA EVENT: Appointment Booked ────────────────────────────────────────
    @KafkaListener(topics = "appointment-events", groupId = "notification-group")
    public void onAppointmentBooked(AppointmentBookedMessage msg) {
        MDC.put("traceId", msg.traceId() != null ? msg.traceId() : "unknown");
        log.info("📬 [NOTIFICATION] Appointment booked event received | appointmentId={}", msg.appointmentId());

        sendEmail(
                msg.patientEmail(),
                "Appointment Confirmed",
                String.format("""
                        Dear Patient,
                        
                        Your appointment has been confirmed!
                        
                        Appointment ID : %s
                        Date           : %s
                        Time           : %s
                        
                        Please complete payment to confirm your slot.
                        
                        Thank you,
                        Doctor Appointment System
                        """, msg.appointmentId(), msg.appointmentDate(), msg.startTime())
        );
        MDC.clear();
    }

    // ── SAGA EVENT: Payment Success ───────────────────────────────────────────
    @KafkaListener(topics = "payment-events", groupId = "notification-group")
    public void onPaymentSuccess(PaymentSuccessMessage msg) {
        MDC.put("traceId", msg.traceId() != null ? msg.traceId() : "unknown");
        log.info("📬 [NOTIFICATION] Payment success event received | paymentId={}", msg.paymentId());

        sendEmail(
                msg.patientEmail(),
                "Payment Receipt",
                String.format("""
                        Dear Patient,
                        
                        Your payment has been received successfully!
                        
                        Payment ID     : %s
                        Appointment ID : %s
                        Amount Paid    : ₹%.2f
                        
                        Your appointment is now fully confirmed.
                        
                        Thank you,
                        Doctor Appointment System
                        """, msg.paymentId(), msg.appointmentId(), msg.amount())
        );
        MDC.clear();
    }

    // ── SAGA COMPENSATING EVENT: Appointment Cancelled ────────────────────────
    @KafkaListener(topics = "appointment-cancelled-events", groupId = "notification-group")
    public void onAppointmentCancelled(AppointmentCancelledMessage msg) {
        MDC.put("traceId", msg.appointmentId() != null ? msg.appointmentId().toString().substring(0, 8) : "unknown");
        log.info("📬 [NOTIFICATION] Appointment cancelled | appointmentId={} reason={}",
                msg.appointmentId(), msg.reason());

        String subject = msg.reason().startsWith("PAYMENT_FAILED")
                ? "Appointment Cancelled — Payment Failed"
                : "Appointment Cancelled";

        sendEmail(
                msg.patientEmail(),
                subject,
                String.format("""
                        Dear Patient,
                        
                        Your appointment has been cancelled.
                        
                        Appointment ID : %s
                        Reason         : %s
                        
                        If this was due to a payment issue, please try booking again.
                        If you need help, contact our support team.
                        
                        Thank you,
                        Doctor Appointment System
                        """, msg.appointmentId(), msg.reason())
        );
        MDC.clear();
    }

    // ── SAGA FAILURE EVENT: Payment Failed ────────────────────────────────────
    @KafkaListener(topics = "payment-failed-events", groupId = "notification-group")
    public void onPaymentFailed(PaymentFailedMessage msg) {
        MDC.put("traceId", msg.traceId() != null ? msg.traceId() : "unknown");
        log.warn("📬 [NOTIFICATION] Payment failed event received | appointmentId={}", msg.appointmentId());

        sendEmail(
                msg.patientEmail(),
                "Payment Failed — Action Required",
                String.format("""
                        Dear Patient,
                        
                        Unfortunately, your payment could not be processed.
                        
                        Appointment ID : %s
                        Reason         : %s
                        
                        Your appointment slot has been released.
                        Please try booking again.
                        
                        Thank you,
                        Doctor Appointment System
                        """, msg.appointmentId(), msg.reason())
        );
        MDC.clear();
    }

    // ── Email sending with Circuit Breaker + Retry ────────────────────────────
    @CircuitBreaker(name = EMAIL_CB, fallbackMethod = "emailFallback")
    @Retry(name = EMAIL_CB)
    public void sendEmail(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            log.warn("⚠️ Skipping email — no recipient address available");
            return;
        }
        log.debug("📧 Sending email | to={} subject={}", to, subject);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        log.info("✅ Email sent | to={} subject={}", to, subject);
    }

    public void emailFallback(String to, String subject, String body, Throwable t) {
        log.error("🔴 [CIRCUIT-OPEN] Email failed after retries | to={} subject={} error={}" +
                " → Notification dropped. Saga flow is NOT affected.", to, subject, t.getMessage());
        // In production: persist to notification_outbox table for retry job
    }

    // ── Inner message records (match Kafka event structure) ───────────────────
    // These are lightweight projections — only fields notification-service needs

    public record AppointmentBookedMessage(
            UUID appointmentId, UUID patientId, String patientEmail,
            String appointmentDate, String startTime, String traceId) {}

    public record PaymentSuccessMessage(
            UUID paymentId, UUID appointmentId, UUID patientId,
            String patientEmail, Double amount, String traceId) {}

    public record AppointmentCancelledMessage(
            UUID appointmentId, UUID patientId, String patientEmail, String reason) {}

    public record PaymentFailedMessage(
            UUID appointmentId, UUID patientId, String patientEmail,
            String reason, String traceId) {}
}
