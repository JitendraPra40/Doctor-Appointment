package com.app.patient_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "appointments_view")
public class AppointmentView {

    @Id
    private Long id;

    private Long patientId;

    private Long doctorId;

    private LocalDate date;

    private LocalTime startTime;

    private String status;
}

