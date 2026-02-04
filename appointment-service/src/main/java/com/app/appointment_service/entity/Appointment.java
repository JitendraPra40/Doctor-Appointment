package com.app.appointment_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(
        name = "appointments",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"doctor_id", "appointment_date", "start_time"}
        )
)
public class Appointment {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID doctorId;

    @Column(nullable = false)
    private UUID patientId;

    private LocalDate appointmentDate;

    private LocalTime startTime;

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        BOOKED, CANCELLED
    }
}

