package com.app.notification_service.notification;

import com.app.notification_service.dto.PaymentEvent;
import com.app.notification_service.service.EmailService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentNotificationConsumer {

    private final EmailService email;

    public PaymentNotificationConsumer(EmailService email) {
        this.email = email;
    }

    @KafkaListener(topics = "payment-events")
    public void onPayment(PaymentEvent event) {

        String body = """
                Payment Status: %s
                Amount Paid: ₹%s
                """.formatted(
                event.status(),
                event.amount()
        );

        email.send(
                "patient@email.com",
                "Payment Confirmation",
                body
        );
    }
}

