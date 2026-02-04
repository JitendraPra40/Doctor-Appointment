package com.app.payment_service.service;

import com.app.payment_service.entity.Payment;
import com.app.payment_service.repository.PaymentRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository repo;
    private final KafkaTemplate<String, Object> kafka;

    public PaymentService(PaymentRepository repo,
                          KafkaTemplate<String, Object> kafka) {
        this.repo = repo;
        this.kafka = kafka;
    }

    public void confirmPayment(String orderId,
                               String paymentId,
                               BigDecimal amount,
                               UUID appointmentId) {

        if (repo.existsByRazorpayPaymentId(paymentId)) {
            return; // idempotent
        }

        Payment payment = new Payment();
        payment.setAppointmentId(appointmentId);
        payment.setRazorpayOrderId(orderId);
        payment.setRazorpayPaymentId(paymentId);
        payment.setAmount(amount);
        payment.setStatus("SUCCESS");

        repo.save(payment);

        kafka.send("payment-events", payment);
    }
}

