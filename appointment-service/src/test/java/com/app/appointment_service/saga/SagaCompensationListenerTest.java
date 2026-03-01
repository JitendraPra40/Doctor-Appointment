package com.app.appointment_service.saga;

import com.app.appointment_service.event.PaymentFailedEvent;
import com.app.appointment_service.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SagaCompensationListener Tests")
class SagaCompensationListenerTest {

    @Mock
    private AppointmentService appointmentService;

    @InjectMocks
    private SagaCompensationListener listener;

    private UUID appointmentId;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        appointmentId = UUID.randomUUID();
        patientId     = UUID.randomUUID();
    }

    @Test
    @DisplayName("onPaymentFailed — should call cancelAppointment with PAYMENT_FAILED reason")
    void onPaymentFailed_callsCancelWithCorrectReason() {
        PaymentFailedEvent event = new PaymentFailedEvent(
                appointmentId, patientId, "timeout", "trace01");

        listener.onPaymentFailed(event);

        verify(appointmentService).cancelAppointment(
                eq(appointmentId),
                contains("PAYMENT_FAILED")
        );
    }

    @Test
    @DisplayName("onPaymentFailed — reason from event is appended to cancel reason")
    void onPaymentFailed_appendsEventReason() {
        PaymentFailedEvent event = new PaymentFailedEvent(
                appointmentId, patientId, "card_declined", "trace02");

        listener.onPaymentFailed(event);

        verify(appointmentService).cancelAppointment(
                eq(appointmentId),
                eq("PAYMENT_FAILED: card_declined")
        );
    }

    @Test
    @DisplayName("onPaymentFailed — should handle null traceId gracefully")
    void onPaymentFailed_nullTraceId_doesNotThrow() {
        PaymentFailedEvent event = new PaymentFailedEvent(
                appointmentId, patientId, "timeout", null);

        assertThatCode(() -> listener.onPaymentFailed(event))
                .doesNotThrowAnyException();

        verify(appointmentService).cancelAppointment(any(), anyString());
    }

    @Test
    @DisplayName("onPaymentFailed — exception in cancelAppointment is caught, not propagated")
    void onPaymentFailed_cancelThrows_exceptionSwallowed() {
        PaymentFailedEvent event = new PaymentFailedEvent(
                appointmentId, patientId, "error", "trace03");

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"))
                .when(appointmentService).cancelAppointment(any(), anyString());

        // Should NOT propagate the exception — Kafka consumer must not die
        assertThatCode(() -> listener.onPaymentFailed(event))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("onPaymentFailed — cancelAppointment called exactly once")
    void onPaymentFailed_cancelCalledExactlyOnce() {
        PaymentFailedEvent event = new PaymentFailedEvent(
                appointmentId, patientId, "bank_error", "trace04");

        listener.onPaymentFailed(event);

        verify(appointmentService, times(1)).cancelAppointment(any(), anyString());
    }
}
