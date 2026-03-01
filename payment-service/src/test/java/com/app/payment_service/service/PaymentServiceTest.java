package com.app.payment_service.service;

import com.app.payment_service.entity.Payment;
import com.app.payment_service.entity.Payment.PaymentStatus;
import com.app.payment_service.event.PaymentFailedEvent;
import com.app.payment_service.event.PaymentSuccessEvent;
import com.app.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Tests")
class PaymentServiceTest {

    @Mock
    private PaymentRepository repo;

    @Mock
    private KafkaTemplate<String, Object> kafka;

    @InjectMocks
    private PaymentService service;

    private UUID appointmentId;
    private UUID patientId;
    private UUID paymentId;
    private String orderId;
    private String razorpayPaymentId;
    private BigDecimal amount;
    private Payment savedPayment;

    @BeforeEach
    void setUp() {
        appointmentId    = UUID.randomUUID();
        patientId        = UUID.randomUUID();
        paymentId        = UUID.randomUUID();
        orderId          = "order_test_001";
        razorpayPaymentId = "pay_test_001";
        amount           = new BigDecimal("500.00");

        savedPayment = new Payment();
        savedPayment.setId(paymentId);
        savedPayment.setAppointmentId(appointmentId);
        savedPayment.setRazorpayOrderId(orderId);
        savedPayment.setRazorpayPaymentId(razorpayPaymentId);
        savedPayment.setAmount(amount.doubleValue());
        savedPayment.setStatus(PaymentStatus.SUCCESS);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // confirmPayment()
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("confirmPayment()")
    class ConfirmPaymentTests {

        @Test
        @DisplayName("should save payment with SUCCESS status and publish success event")
        void confirmPayment_success_savesAndPublishes() {
            // Arrange
            when(repo.existsByRazorpayPaymentId(razorpayPaymentId)).thenReturn(false);
            when(repo.save(any(Payment.class))).thenReturn(savedPayment);

            // Act
            Payment result = service.confirmPayment(orderId, razorpayPaymentId, amount, appointmentId);

            // Assert — returned payment
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(paymentId);
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

            // Assert — saved entity has correct fields
            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(repo).save(captor.capture());
            Payment toSave = captor.getValue();
            assertThat(toSave.getAppointmentId()).isEqualTo(appointmentId);
            assertThat(toSave.getRazorpayOrderId()).isEqualTo(orderId);
            assertThat(toSave.getRazorpayPaymentId()).isEqualTo(razorpayPaymentId);
            assertThat(toSave.getAmount()).isEqualTo(amount.doubleValue());
            assertThat(toSave.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

            // Assert — Kafka success event published
            verify(kafka).send(eq("payment-events"), anyString(), any(PaymentSuccessEvent.class));
        }

        @Test
        @DisplayName("should return existing payment without saving when payment ID is duplicate (idempotency)")
        void confirmPayment_duplicate_returnsExistingWithoutSaving() {
            // Arrange
            when(repo.existsByRazorpayPaymentId(razorpayPaymentId)).thenReturn(true);
            when(repo.findByRazorpayPaymentId(razorpayPaymentId)).thenReturn(Optional.of(savedPayment));

            // Act
            Payment result = service.confirmPayment(orderId, razorpayPaymentId, amount, appointmentId);

            // Assert — existing record returned
            assertThat(result.getId()).isEqualTo(paymentId);

            // Assert — repo.save and Kafka NOT called
            verify(repo, never()).save(any());
            verify(kafka, never()).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("published PaymentSuccessEvent has correct appointment and amount")
        void confirmPayment_publishedEvent_hasCorrectFields() {
            // Arrange
            when(repo.existsByRazorpayPaymentId(razorpayPaymentId)).thenReturn(false);
            when(repo.save(any())).thenReturn(savedPayment);

            // Act
            service.confirmPayment(orderId, razorpayPaymentId, amount, appointmentId);

            // Assert event
            ArgumentCaptor<PaymentSuccessEvent> captor = ArgumentCaptor.forClass(PaymentSuccessEvent.class);
            verify(kafka).send(eq("payment-events"), anyString(), captor.capture());

            PaymentSuccessEvent event = captor.getValue();
            assertThat(event.paymentId()).isEqualTo(paymentId);
            assertThat(event.appointmentId()).isEqualTo(appointmentId);
            assertThat(event.amount()).isEqualTo(amount.doubleValue());
        }

        @Test
        @DisplayName("should store amount as double value of BigDecimal")
        void confirmPayment_amountStoredCorrectly() {
            BigDecimal bigDecimalAmount = new BigDecimal("1299.99");
            when(repo.existsByRazorpayPaymentId(any())).thenReturn(false);
            when(repo.save(any())).thenReturn(savedPayment);

            service.confirmPayment(orderId, razorpayPaymentId, bigDecimalAmount, appointmentId);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(repo).save(captor.capture());
            assertThat(captor.getValue().getAmount()).isEqualTo(1299.99);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // handlePaymentFailure()
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("handlePaymentFailure()")
    class HandlePaymentFailureTests {

        @Test
        @DisplayName("should save FAILED payment and publish PaymentFailedEvent")
        void handlePaymentFailure_savesAndPublishesFailed() {
            // Arrange
            when(repo.save(any())).thenReturn(new Payment());

            // Act
            service.handlePaymentFailure(appointmentId, patientId, "card_declined", "trace01");

            // Assert — payment saved with FAILED status
            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(repo).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(captor.getValue().getAppointmentId()).isEqualTo(appointmentId);
            assertThat(captor.getValue().getAmount()).isEqualTo(0.0);

            // Assert — failure event published
            verify(kafka).send(eq("payment-failed-events"), eq(appointmentId.toString()), any(PaymentFailedEvent.class));
        }

        @Test
        @DisplayName("published PaymentFailedEvent contains correct appointmentId and reason")
        void handlePaymentFailure_eventHasCorrectFields() {
            when(repo.save(any())).thenReturn(new Payment());

            service.handlePaymentFailure(appointmentId, patientId, "insufficient_funds", "trace02");

            ArgumentCaptor<PaymentFailedEvent> captor = ArgumentCaptor.forClass(PaymentFailedEvent.class);
            verify(kafka).send(anyString(), anyString(), captor.capture());

            PaymentFailedEvent event = captor.getValue();
            assertThat(event.appointmentId()).isEqualTo(appointmentId);
            assertThat(event.patientId()).isEqualTo(patientId);
            assertThat(event.reason()).isEqualTo("insufficient_funds");
        }

        @Test
        @DisplayName("should handle null traceId without throwing")
        void handlePaymentFailure_nullTraceId_doesNotThrow() {
            when(repo.save(any())).thenReturn(new Payment());

            assertThatCode(() ->
                    service.handlePaymentFailure(appointmentId, patientId, "error", null)
            ).doesNotThrowAnyException();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Fallback methods
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Fallback methods")
    class FallbackTests {

        @Test
        @DisplayName("publishSuccessFallback does not throw")
        void publishSuccessFallback_doesNotThrow() {
            assertThatCode(() ->
                    service.publishSuccessFallback(savedPayment, "trace", new RuntimeException("Kafka down"))
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("publishFailedFallback does not throw")
        void publishFailedFallback_doesNotThrow() {
            assertThatCode(() ->
                    service.publishFailedFallback(appointmentId, patientId,
                            "error", "trace", new RuntimeException("Kafka down"))
            ).doesNotThrowAnyException();
        }
    }
}
