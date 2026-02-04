package com.app.appointment_service.repository;

import com.app.appointment_service.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public interface AppointmentRepository
        extends JpaRepository<Appointment, UUID> {

    boolean existsByDoctorIdAndAppointmentDateAndStartTime(
            UUID doctorId,
            LocalDate date,
            LocalTime time
    );
}

