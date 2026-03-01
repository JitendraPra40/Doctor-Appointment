package com.app.patient_service.repository;

import com.app.patient_service.entity.AppointmentView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AppointmentViewRepository
        extends JpaRepository<AppointmentView, Long> {

    List<AppointmentView> findByPatientId(Long patientId);
}

