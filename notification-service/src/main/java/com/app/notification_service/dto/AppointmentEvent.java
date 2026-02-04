package com.app.notification_service.dto;

import java.util.UUID;

public record AppointmentEvent(
        UUID appointmentId,
        UUID doctorId,
        UUID patientId,
        String status
) {}

