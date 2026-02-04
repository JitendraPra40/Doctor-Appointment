package com.app.appointment_service.service;

import com.app.appointment_service.dto.AppointmentEvent;
import com.app.appointment_service.entity.Appointment;
import com.app.appointment_service.repository.AppointmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Service
@Transactional
public class AppointmentService {

    private final AppointmentRepository repo;
    private final AppointmentEventProducer producer;

    public AppointmentService(AppointmentRepository repo,
                              AppointmentEventProducer producer) {
        this.repo = repo;
        this.producer = producer;
    }

    public Appointment book(UUID doctorId,
                            UUID patientId,
                            LocalDate date,
                            LocalTime time) {

        if (repo.existsByDoctorIdAndAppointmentDateAndStartTime(
                doctorId, date, time)) {
            throw new RuntimeException("Slot already booked");
        }

        Appointment appt = new Appointment();
        appt.setDoctorId(doctorId);
        appt.setPatientId(patientId);
        appt.setAppointmentDate(date);
        appt.setStartTime(time);
        appt.setStatus(Appointment.Status.BOOKED);

        Appointment saved = repo.save(appt);

        producer.publish(new AppointmentEvent(
                saved.getId(),
                doctorId,
                patientId,
                "BOOKED"
        ));

        return saved;
    }
}

