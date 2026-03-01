package com.app.appointment_service;

import com.app.appointment_service.entity.Appointment;
import com.app.appointment_service.event.PaymentFailedEvent;
import com.app.appointment_service.repository.AppointmentRepository;
import com.app.appointment_service.saga.SagaCompensationListener;
import com.app.appointment_service.service.AppointmentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Integration test: full Saga choreography flow.
 *
 * Tests the complete round-trip:
 *   book appointment → publish event → receive PaymentFailedEvent → cancel appointment
 *
 * Requires awaitility dependency:
 * <dependency>
 *     <groupId>org.awaitility</groupId>
 *     <artifactId>awaitility</artifactId>
 *     <scope>test</scope>
 * </dependency>
 */
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {"appointment-events", "appointment-cancelled-events", "payment-failed-events"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:19092", "port=19092"}
)
@DisplayName("Appointment Saga Integration Tests")
class AppointmentSagaIntegrationTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private SagaCompensationListener sagaCompensationListener;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private UUID doctorId;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        doctorId  = UUID.randomUUID();
        patientId = UUID.randomUUID();
        appointmentRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        appointmentRepository.deleteAll();
    }

    @Test
    @DisplayName("Saga happy path: book appointment saves to DB with BOOKED status")
    void saga_happyPath_appointmentSavedAsBooked() {
        Appointment result = appointmentService.book(
                doctorId, patientId,
                LocalDate.of(2026, 4, 1),
                LocalTime.of(9, 0)
        );

        assertThat(result.getId()).isNotNull();
        assertThat(result.getStatus()).isEqualTo(Appointment.Status.BOOKED);

        Appointment fromDb = appointmentRepository.findById(result.getId()).orElseThrow();
        assertThat(fromDb.getStatus()).isEqualTo(Appointment.Status.BOOKED);
    }

    @Test
    @DisplayName("Saga compensation: PaymentFailedEvent → appointment status changes to CANCELLED")
    void saga_compensating_paymentFailed_appointmentCancelled() {
        // Step 1: Book an appointment
        Appointment appt = appointmentService.book(
                doctorId, patientId,
                LocalDate.of(2026, 4, 2),
                LocalTime.of(11, 0)
        );
        assertThat(appt.getStatus()).isEqualTo(Appointment.Status.BOOKED);

        // Step 2: Simulate payment failure event (as if payment-service published it)
        PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                appt.getId(), patientId, "card_declined", "trace-test-01"
        );
        sagaCompensationListener.onPaymentFailed(failedEvent);

        // Step 3: Verify appointment is CANCELLED in DB
        await().atMost(5, SECONDS).untilAsserted(() -> {
            Appointment updated = appointmentRepository.findById(appt.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(Appointment.Status.CANCELLED);
        });
    }

    @Test
    @DisplayName("Saga: duplicate slot booking is rejected with 409")
    void saga_duplicateSlot_rejected() {
        LocalDate date = LocalDate.of(2026, 4, 3);
        LocalTime time = LocalTime.of(14, 0);

        // First booking succeeds
        appointmentService.book(doctorId, patientId, date, time);

        // Second booking for same slot should fail
        assertThat(
                org.junit.jupiter.api.Assertions.assertThrows(
                        org.springframework.web.server.ResponseStatusException.class,
                        () -> appointmentService.book(doctorId, UUID.randomUUID(), date, time)
                ).getStatusCode()
        ).isEqualTo(org.springframework.http.HttpStatus.CONFLICT);
    }
}
