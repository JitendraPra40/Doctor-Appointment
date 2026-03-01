package com.app.notification_service.service;

import com.app.notification_service.listener.NotificationEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventListener Tests")
class NotificationEventListenerTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationEventListener listener;

    private UUID appointmentId;
    private UUID paymentId;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        appointmentId = UUID.randomUUID();
        paymentId     = UUID.randomUUID();
        patientId     = UUID.randomUUID();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // onAppointmentBooked()
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("onAppointmentBooked()")
    class AppointmentBookedTests {

        @Test
        @DisplayName("should send email with correct recipient and subject")
        void onAppointmentBooked_sendsEmailToPatient() {
            var msg = new NotificationEventListener.AppointmentBookedMessage(
                    appointmentId, patientId, "patient@example.com",
                    "2026-03-15", "10:00", "trace01");

            listener.onAppointmentBooked(msg);

            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());

            SimpleMailMessage mail = captor.getValue();
            assertThat(mail.getTo()).contains("patient@example.com");
            assertThat(mail.getSubject()).isEqualTo("Appointment Confirmed");
        }

        @Test
        @DisplayName("should include appointment ID in email body")
        void onAppointmentBooked_emailBodyContainsAppointmentId() {
            var msg = new NotificationEventListener.AppointmentBookedMessage(
                    appointmentId, patientId, "patient@example.com",
                    "2026-03-15", "10:00", "trace01");

            listener.onAppointmentBooked(msg);

            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            assertThat(captor.getValue().getText()).contains(appointmentId.toString());
        }

        @Test
        @DisplayName("should skip sending email when recipient is null")
        void onAppointmentBooked_nullEmail_skipsMailSend() {
            var msg = new NotificationEventListener.AppointmentBookedMessage(
                    appointmentId, patientId, null,
                    "2026-03-15", "10:00", "trace01");

            listener.onAppointmentBooked(msg);

            verify(mailSender, never()).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("should skip sending email when recipient is blank")
        void onAppointmentBooked_blankEmail_skipsMailSend() {
            var msg = new NotificationEventListener.AppointmentBookedMessage(
                    appointmentId, patientId, "   ",
                    "2026-03-15", "10:00", "trace01");

            listener.onAppointmentBooked(msg);

            verify(mailSender, never()).send(any(SimpleMailMessage.class));
        }

        @Test
        @DisplayName("should handle null traceId without throwing")
        void onAppointmentBooked_nullTraceId_doesNotThrow() {
            var msg = new NotificationEventListener.AppointmentBookedMessage(
                    appointmentId, patientId, "patient@example.com",
                    "2026-03-15", "10:00", null);

            assertThatCode(() -> listener.onAppointmentBooked(msg))
                    .doesNotThrowAnyException();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // onPaymentSuccess()
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("onPaymentSuccess()")
    class PaymentSuccessTests {

        @Test
        @DisplayName("should send receipt email with correct subject")
        void onPaymentSuccess_sendsReceiptEmail() {
            var msg = new NotificationEventListener.PaymentSuccessMessage(
                    paymentId, appointmentId, patientId, "patient@example.com", 500.0, "trace01");

            listener.onPaymentSuccess(msg);

            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            assertThat(captor.getValue().getSubject()).isEqualTo("Payment Receipt");
        }

        @Test
        @DisplayName("should include amount in email body")
        void onPaymentSuccess_bodyContainsAmount() {
            var msg = new NotificationEventListener.PaymentSuccessMessage(
                    paymentId, appointmentId, patientId, "patient@example.com", 1299.0, "trace01");

            listener.onPaymentSuccess(msg);

            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            assertThat(captor.getValue().getText()).contains("1299");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // onAppointmentCancelled()
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("onAppointmentCancelled()")
    class AppointmentCancelledTests {

        @Test
        @DisplayName("should send cancellation email with PAYMENT_FAILED subject when reason starts with PAYMENT_FAILED")
        void onAppointmentCancelled_paymentFailedReason_usesCorrectSubject() {
            var msg = new NotificationEventListener.AppointmentCancelledMessage(
                    appointmentId, patientId, "patient@example.com", "PAYMENT_FAILED: timeout");

            listener.onAppointmentCancelled(msg);

            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            assertThat(captor.getValue().getSubject()).contains("Payment Failed");
        }

        @Test
        @DisplayName("should use generic cancellation subject for non-payment reasons")
        void onAppointmentCancelled_patientRequestedReason_usesGenericSubject() {
            var msg = new NotificationEventListener.AppointmentCancelledMessage(
                    appointmentId, patientId, "patient@example.com", "PATIENT_REQUESTED");

            listener.onAppointmentCancelled(msg);

            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            assertThat(captor.getValue().getSubject()).isEqualTo("Appointment Cancelled");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // onPaymentFailed()
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("onPaymentFailed()")
    class PaymentFailedTests {

        @Test
        @DisplayName("should send payment failure email with correct subject")
        void onPaymentFailed_sendsFailureEmail() {
            var msg = new NotificationEventListener.PaymentFailedMessage(
                    appointmentId, patientId, "patient@example.com", "card_declined", "trace01");

            listener.onPaymentFailed(msg);

            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            assertThat(captor.getValue().getSubject()).contains("Payment Failed");
        }

        @Test
        @DisplayName("should include failure reason in email body")
        void onPaymentFailed_bodyContainsReason() {
            var msg = new NotificationEventListener.PaymentFailedMessage(
                    appointmentId, patientId, "patient@example.com", "insufficient_funds", "trace01");

            listener.onPaymentFailed(msg);

            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            assertThat(captor.getValue().getText()).contains("insufficient_funds");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // sendEmail() fallback
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("emailFallback()")
    class EmailFallbackTests {

        @Test
        @DisplayName("emailFallback should not throw — graceful degradation")
        void emailFallback_doesNotThrow() {
            assertThatCode(() ->
                    listener.emailFallback(
                            "patient@example.com",
                            "Subject",
                            "Body",
                            new RuntimeException("SMTP down"))
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("mailSender should NOT be called from fallback")
        void emailFallback_doesNotCallMailSender() {
            listener.emailFallback("p@test.com", "S", "B", new RuntimeException());

            verify(mailSender, never()).send(any(SimpleMailMessage.class));
        }
    }
}
