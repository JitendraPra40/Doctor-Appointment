package com.app.doctor_service.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class DoctorEventProducer {

    private final KafkaTemplate<String, Object> kafka;

    public DoctorEventProducer(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    public void publishAvailability(Long doctorId) {
        kafka.send("doctor-availability-topic", doctorId.toString());
    }
}

