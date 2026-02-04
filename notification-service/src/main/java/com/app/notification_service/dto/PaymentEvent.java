package com.app.notification_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentEvent(
        UUID appointmentId,
        BigDecimal amount,
        String status
) {}

