package com.app.appointment_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

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
    // FIX: MySQL doesn't have a native UUID column type.
    // @UuidGenerator ensures Hibernate generates a UUID and stores it as CHAR(36) or BINARY(16).
    @UuidGenerator
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID doctorId;

    @Column(nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID patientId;

    private LocalDate appointmentDate;

    private LocalTime startTime;

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        BOOKED, CANCELLED
    }
}
