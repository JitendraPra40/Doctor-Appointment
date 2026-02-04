package com.app.payment_service.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = @UniqueConstraint(columnNames = "razorpay_payment_id")
)
public class Payment {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID appointmentId;

    private String razorpayOrderId;

    private String razorpayPaymentId;

    private String status;

    private BigDecimal amount;

    private Instant createdAt = Instant.now();
}

