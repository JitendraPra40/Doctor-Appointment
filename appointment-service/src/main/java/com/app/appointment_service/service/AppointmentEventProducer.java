package com.app.appointment_service.service;

import com.app.appointment_service.dto.AppointmentEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class AppointmentEventProducer {

    private final KafkaTemplate<String, AppointmentEvent> kafka;

    public AppointmentEventProducer(
            KafkaTemplate<String, AppointmentEvent> kafka) {
        this.kafka = kafka;
    }

    public void publish(AppointmentEvent event) {
        kafka.send("appointment-events", event);
    }
}

