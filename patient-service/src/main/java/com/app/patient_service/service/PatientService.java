package com.app.patient_service.service;

import com.app.patient_service.entity.AppointmentView;
import com.app.patient_service.entity.Patient;
import com.app.patient_service.repository.AppointmentViewRepository;
import com.app.patient_service.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PatientService {

    private final PatientRepository patientRepo;
    private final AppointmentViewRepository appointmentRepo;

    public PatientService(PatientRepository patientRepo,
                          AppointmentViewRepository appointmentRepo) {
        this.patientRepo = patientRepo;
        this.appointmentRepo = appointmentRepo;
    }

    public Patient getProfile(Long userId) {
        return patientRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
    }

    public Patient updateProfile(Long userId, Patient patient) {
        patient.setId(userId);
        return patientRepo.save(patient);
    }

    public List<AppointmentView> myAppointments(Long userId) {
        return appointmentRepo.findByPatientId(userId);
    }
}

