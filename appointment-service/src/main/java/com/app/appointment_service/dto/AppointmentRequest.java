package com.app.appointment_service.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AppointmentRequest(
        UUID doctorId,
        LocalDate date,
        LocalTime time
) {}

