package com.app.doctor_service.repository;

import com.app.doctor_service.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    List<TimeSlot> findByDoctorIdAndSlotDate(Long doctorId, LocalDate slotDate);
}
