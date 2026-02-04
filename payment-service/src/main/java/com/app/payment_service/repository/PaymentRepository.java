package com.app.payment_service.repository;

import com.app.payment_service.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentRepository
        extends JpaRepository<Payment, UUID> {

    boolean existsByRazorpayPaymentId(String paymentId);
}

