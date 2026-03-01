package com.app.doctor_service.service;

import com.app.doctor_service.entity.Doctor;
import com.app.doctor_service.entity.TimeSlot;
import com.app.doctor_service.repository.DoctorRepository;
import com.app.doctor_service.repository.TimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DoctorService Tests")
class DoctorServiceTest {

    @Mock
    private DoctorRepository doctorRepo;

    @Mock
    private TimeSlotRepository slotRepo;

    @Mock
    private KafkaTemplate<String, Object> kafka;

    @InjectMocks
    private DoctorService service;

    private Doctor doctor;
    private TimeSlot slot;

    @BeforeEach
    void setUp() {
        doctor = new Doctor();
        doctor.setId(1L);
        doctor.setName("Dr. Smith");
        doctor.setSpecialization("Cardiology");
        doctor.setFee(new BigDecimal("500.00"));

        slot = new TimeSlot();
        slot.setId(10L);
        slot.setSlotDate(LocalDate.of(2026, 3, 15));
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(9, 30));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // createDoctor()
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("createDoctor()")
    class CreateDoctorTests {

        @Test
        @DisplayName("should save doctor and publish Kafka event on valid input")
        void createDoctor_valid_savesAndPublishes() {
            when(doctorRepo.save(any())).thenReturn(doctor);

            Doctor result = service.createDoctor(doctor);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Dr. Smith");

            verify(doctorRepo).save(doctor);
            // Kafka event published with doctorId as key
            verify(kafka).send(eq("doctor-events"), eq("1"), any(Doctor.class));
        }

        @Test
        @DisplayName("should throw 400 BAD_REQUEST when fee is zero")
        void createDoctor_zeroFee_throwsBadRequest() {
            doctor.setFee(BigDecimal.ZERO);

            ResponseStatusException ex = catchThrowableOfType(
                    () -> service.createDoctor(doctor),
                    ResponseStatusException.class
            );

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getReason()).contains("positive");
            verify(doctorRepo, never()).save(any());
        }

        @Test
        @DisplayName("should throw 400 BAD_REQUEST when fee is negative")
        void createDoctor_negativeFee_throwsBadRequest() {
            doctor.setFee(new BigDecimal("-100"));

            ResponseStatusException ex = catchThrowableOfType(
                    () -> service.createDoctor(doctor),
                    ResponseStatusException.class
            );

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            verify(doctorRepo, never()).save(any());
        }

        @Test
        @DisplayName("should save doctor successfully with valid positive fee")
        void createDoctor_validFee_saves() {
            doctor.setFee(new BigDecimal("100.00"));
            when(doctorRepo.save(any())).thenReturn(doctor);

            Doctor result = service.createDoctor(doctor);

            assertThat(result).isNotNull();
            verify(doctorRepo).save(any());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // deleteDoctor()
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("deleteDoctor()")
    class DeleteDoctorTests {

        @Test
        @DisplayName("should delete doctor when ID exists")
        void deleteDoctor_exists_deletes() {
            when(doctorRepo.existsById(1L)).thenReturn(true);

            service.deleteDoctor(1L);

            verify(doctorRepo).deleteById(1L);
        }

        @Test
        @DisplayName("should throw 404 NOT_FOUND when doctor ID does not exist")
        void deleteDoctor_notFound_throws404() {
            when(doctorRepo.existsById(99L)).thenReturn(false);

            ResponseStatusException ex = catchThrowableOfType(
                    () -> service.deleteDoctor(99L),
                    ResponseStatusException.class
            );

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            verify(doctorRepo, never()).deleteById(any());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getDoctor()
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getDoctor()")
    class GetDoctorTests {

        @Test
        @DisplayName("should return doctor when found")
        void getDoctor_found_returnsDoctor() {
            when(doctorRepo.findById(1L)).thenReturn(Optional.of(doctor));

            Doctor result = service.getDoctor(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Dr. Smith");
        }

        @Test
        @DisplayName("should throw 404 when doctor not found")
        void getDoctor_notFound_throws404() {
            when(doctorRepo.findById(999L)).thenReturn(Optional.empty());

            ResponseStatusException ex = catchThrowableOfType(
                    () -> service.getDoctor(999L),
                    ResponseStatusException.class
            );

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getAllDoctors()
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getAllDoctors()")
    class GetAllDoctorsTests {

        @Test
        @DisplayName("should return list of all doctors")
        void getAllDoctors_returnsAll() {
            when(doctorRepo.findAll()).thenReturn(List.of(doctor));

            List<Doctor> result = service.getAllDoctors();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Dr. Smith");
        }

        @Test
        @DisplayName("should return empty list when no doctors")
        void getAllDoctors_empty_returnsEmptyList() {
            when(doctorRepo.findAll()).thenReturn(Collections.emptyList());

            List<Doctor> result = service.getAllDoctors();

            assertThat(result).isEmpty();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // createSlot()
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("createSlot()")
    class CreateSlotTests {

        @Test
        @DisplayName("should create time slot and save it")
        void createSlot_valid_savesSlot() {
            when(doctorRepo.findById(1L)).thenReturn(Optional.of(doctor));
            when(slotRepo.save(any())).thenReturn(slot);

            TimeSlot result = service.createSlot(1L, slot);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(10L);
            verify(slotRepo).save(slot);
        }

        @Test
        @DisplayName("should throw 404 when doctor not found for slot creation")
        void createSlot_doctorNotFound_throws404() {
            when(doctorRepo.findById(99L)).thenReturn(Optional.empty());

            ResponseStatusException ex = catchThrowableOfType(
                    () -> service.createSlot(99L, slot),
                    ResponseStatusException.class
            );

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            verify(slotRepo, never()).save(any());
        }

        @Test
        @DisplayName("should throw 400 BAD_REQUEST when end time is before start time")
        void createSlot_endBeforeStart_throwsBadRequest() {
            slot.setStartTime(LocalTime.of(10, 0));
            slot.setEndTime(LocalTime.of(9, 0));   // INVALID — end before start
            when(doctorRepo.findById(1L)).thenReturn(Optional.of(doctor));

            ResponseStatusException ex = catchThrowableOfType(
                    () -> service.createSlot(1L, slot),
                    ResponseStatusException.class
            );

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getReason()).contains("End time");
        }

        @Test
        @DisplayName("should throw 400 BAD_REQUEST when end time equals start time")
        void createSlot_endEqualsStart_throwsBadRequest() {
            slot.setStartTime(LocalTime.of(9, 0));
            slot.setEndTime(LocalTime.of(9, 0));   // INVALID — same time
            when(doctorRepo.findById(1L)).thenReturn(Optional.of(doctor));

            ResponseStatusException ex = catchThrowableOfType(
                    () -> service.createSlot(1L, slot),
                    ResponseStatusException.class
            );

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // getSlots()
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getSlots()")
    class GetSlotsTests {

        @Test
        @DisplayName("should return slots for given doctor and date")
        void getSlots_returnsSlots() {
            LocalDate date = LocalDate.of(2026, 3, 15);
            when(slotRepo.findByDoctorIdAndSlotDate(1L, date)).thenReturn(List.of(slot));

            List<TimeSlot> result = service.getSlots(1L, date);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("should return empty list when no slots available")
        void getSlots_empty_returnsEmptyList() {
            LocalDate date = LocalDate.of(2026, 3, 16);
            when(slotRepo.findByDoctorIdAndSlotDate(1L, date)).thenReturn(Collections.emptyList());

            List<TimeSlot> result = service.getSlots(1L, date);

            assertThat(result).isEmpty();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // publishDoctorCreatedFallback()
    // ═════════════════════════════════════════════════════════════════════════
    @Test
    @DisplayName("publishDoctorCreatedFallback should not throw — graceful degradation")
    void publishDoctorCreatedFallback_doesNotThrow() {
        assertThatCode(() ->
                service.publishDoctorCreatedFallback(doctor, new RuntimeException("Kafka down"))
        ).doesNotThrowAnyException();
    }
}
