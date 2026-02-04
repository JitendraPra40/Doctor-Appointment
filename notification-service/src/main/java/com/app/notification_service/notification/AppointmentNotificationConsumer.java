package com.app.notification_service.notification;

import com.app.notification_service.dto.AppointmentEvent;
import com.app.notification_service.service.EmailService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AppointmentNotificationConsumer {

    private final EmailService email;

    public AppointmentNotificationConsumer(EmailService email) {
        this.email = email;
    }

    @KafkaListener(topics = "appointment-events")
    public void onAppointment(AppointmentEvent event) {

        String body = """
                Appointment %s
                Doctor ID: %s
                Status: %s
                """.formatted(
                event.appointmentId(),
                event.doctorId(),
                event.status()
        );

        email.send(
                "patient@email.com",
                "Appointment Update",
                body
        );
    }
}

