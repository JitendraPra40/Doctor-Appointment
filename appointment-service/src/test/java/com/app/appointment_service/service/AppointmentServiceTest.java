package com.app.appointment_service.service;

import com.app.appointment_service.entity.Appointment;
import com.app.appointment_service.event.AppointmentBookedEvent;
import com.app.appointment_service.event.AppointmentCancelledEvent;
import com.app.appointment_service.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentService Tests")
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository repo;

    @Mock
    private KafkaTemplate<String, Object> kafka;

    @InjectMocks
    private AppointmentService service;

    // ── Shared test data ──────────────────────────────────────────────────────
    private UUID doctorId;
    private UUID patientId;
    private UUID appointmentId;
    private LocalDate date;
    private LocalTime time;
    private Appointment savedAppointment;

    @BeforeEach
    void setUp() {
        doctorId      = UUID.randomUUID();
        patientId     = UUID.randomUUID();
        appointmentId = UUID.randomUUID();
        date          = LocalDate.of(2026, 3, 15);
        time          = LocalTime.of(10, 0);

        savedAppointment = new Appointment();
        savedAppointment.setId(appointmentId);
        savedAppointment.setDoctorId(doctorId);
        savedAppointment.setPatientId(patientId);
        savedAppointment.setAppointmentDate(date);
        savedAppointment.setStartTime(time);
        savedAppointment.setStatus(Appointment.Status.BOOKED);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // book()
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("book()")
    class BookTests {

        @Test
        @DisplayName("should save appointment and publish Kafka event on success")
        void book_success_savesAndPublishes() {
            // Arrange
            when(repo.existsByDoctorIdAndAppointmentDateAndStartTime(doctorId, date, time))
                    .thenReturn(false);
            when(repo.save(any(Appointment.class))).thenReturn(savedAppointment);

            // Act
            Appointment result = service.book(doctorId, patientId, date, time);

            // Assert — returned appointment is correct
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(appointmentId);
            assertThat(result.getStatus()).isEqualTo(Appointment.Status.BOOKED);
            assertThat(result.getDoctorId()).isEqualTo(doctorId);
            assertThat(result.getPatientId()).isEqualTo(patientId);

            // Assert — repo.save was called with correct fields
            ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
            verify(repo).save(captor.capture());
            Appointment toSave = captor.getValue();
            assertThat(toSave.getDoctorId()).isEqualTo(doctorId);
            assertThat(toSave.getPatientId()).isEqualTo(patientId);
            assertThat(toSave.getAppointmentDate()).isEqualTo(date);
            assertThat(toSave.getStartTime()).isEqualTo(time);
            assertThat(toSave.getStatus()).isEqualTo(Appointment.Status.BOOKED);

            // Assert — Kafka event published
            verify(kafka).send(eq("appointment-events"), eq(appointmentId.toString()), any(AppointmentBookedEvent.class));
        }

        @Test
        @DisplayName("should throw 409 CONFLICT when slot already booked")
        void book_slotAlreadyBooked_throwsConflict() {
            // Arrange
            when(repo.existsByDoctorIdAndAppointmentDateAndStartTime(doctorId, date, time))
                    .thenReturn(true);

            // Act & Assert
            ResponseStatusException ex = catchThrowableOfType(
                    () -> service.book(doctorId, patientId, date, time),
                    ResponseStatusException.class
            );

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(ex.getReason()).contains("already booked");

            // repo.save must NOT be called
            verify(repo, never()).save(any());
            // Kafka must NOT be called
            verify(kafka, never()).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("should NOT publish Kafka event if repo.save throws")
        void book_saveFails_doesNotPublish() {
            // Arrange
            when(repo.existsByDoctorIdAndAppointmentDateAndStartTime(doctorId, date, time))
                    .thenReturn(false);
            when(repo.save(any(Appointment.class))).thenThrow(new RuntimeException("DB error"));

            // Act & Assert
            assertThatThrownBy(() -> service.book(doctorId, patientId, date, time))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("DB error");

            verify(kafka, never()).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("published AppointmentBookedEvent carries correct fields")
        void book_publishedEvent_hasCorrectFields() {
            // Arrange
            when(repo.existsByDoctorIdAndAppointmentDateAndStartTime(doctorId, date, time))
                    .thenReturn(false);
            when(repo.save(any())).thenReturn(savedAppointment);

            // Act
            service.book(doctorId, patientId, date, time);

            // Assert event payload
            ArgumentCaptor<AppointmentBookedEvent> eventCaptor =
                    ArgumentCaptor.forClass(AppointmentBookedEvent.class);
            verify(kafka).send(eq("appointment-events"), anyString(), eventCaptor.capture());

            AppointmentBookedEvent event = eventCaptor.getValue();
            assertThat(event.appointmentId()).isEqualTo(appointmentId);
            assertThat(event.doctorId()).isEqualTo(doctorId);
            assertThat(event.patientId()).isEqualTo(patientId);
            assertThat(event.appointmentDate()).isEqualTo(date);
            assertThat(event.startTime()).isEqualTo(time);
            assertThat(event.traceId()).isNotBlank();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // cancelAppointment()
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("cancelAppointment()")
    class CancelTests {

        @Test
        @DisplayName("should set status to CANCELLED and publish cancelled event")
        void cancel_success_setsStatusAndPublishes() {
            // Arrange
            when(repo.findById(appointmentId)).thenReturn(Optional.of(savedAppointment));
            when(repo.save(any())).thenReturn(savedAppointment);

            // Act
            service.cancelAppointment(appointmentId, "PATIENT_REQUESTED");

            // Assert — status updated
            ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
            verify(repo).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(Appointment.Status.CANCELLED);

            // Assert — cancelled event published
            verify(kafka).send(eq("appointment-cancelled-events"),
                    eq(appointmentId.toString()),
                    any(AppointmentCancelledEvent.class));
        }

        @Test
        @DisplayName("should throw 404 NOT_FOUND when appointment does not exist")
        void cancel_notFound_throws404() {
            // Arrange
            when(repo.findById(appointmentId)).thenReturn(Optional.empty());

            // Act & Assert
            ResponseStatusException ex = catchThrowableOfType(
                    () -> service.cancelAppointment(appointmentId, "PATIENT_REQUESTED"),
                    ResponseStatusException.class
            );

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(ex.getReason()).contains("not found");

            verify(repo, never()).save(any());
            verify(kafka, never()).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("published AppointmentCancelledEvent carries correct reason")
        void cancel_publishedEvent_hasCorrectReason() {
            // Arrange
            when(repo.findById(appointmentId)).thenReturn(Optional.of(savedAppointment));
            when(repo.save(any())).thenReturn(savedAppointment);
            String reason = "PAYMENT_FAILED: timeout";

            // Act
            service.cancelAppointment(appointmentId, reason);

            // Assert event payload
            ArgumentCaptor<AppointmentCancelledEvent> captor =
                    ArgumentCaptor.forClass(AppointmentCancelledEvent.class);
            verify(kafka).send(eq("appointment-cancelled-events"), anyString(), captor.capture());

            AppointmentCancelledEvent event = captor.getValue();
            assertThat(event.appointmentId()).isEqualTo(appointmentId);
            assertThat(event.patientId()).isEqualTo(patientId);
            assertThat(event.reason()).isEqualTo(reason);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // publishBookedEvent() fallback
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Fallback methods")
    class FallbackTests {

        @Test
        @DisplayName("publishBookedFallback should not throw — graceful degradation")
        void publishBookedFallback_doesNotThrow() {
            Throwable cause = new RuntimeException("Kafka down");
            // Should complete without exception
            assertThatCode(() ->
                    service.publishBookedFallback(savedAppointment, "trace123", cause)
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("publishCancelledFallback should not throw — graceful degradation")
        void publishCancelledFallback_doesNotThrow() {
            Throwable cause = new RuntimeException("Kafka down");
            assertThatCode(() ->
                    service.publishCancelledFallback(savedAppointment, "PATIENT_REQUESTED", cause)
            ).doesNotThrowAnyException();
        }
    }
}
