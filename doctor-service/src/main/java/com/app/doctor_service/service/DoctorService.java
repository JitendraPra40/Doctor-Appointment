package com.app.doctor_service.service;


import com.app.doctor_service.entity.Doctor;
import com.app.doctor_service.entity.TimeSlot;
import com.app.doctor_service.repository.DoctorRepository;
import com.app.doctor_service.repository.TimeSlotRepository;
import jakarta.ws.rs.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class DoctorService {

    private final DoctorRepository doctorRepo;
    private final TimeSlotRepository slotRepo;
    private final DoctorEventProducer producer;

    public DoctorService(DoctorRepository doctorRepo,
                         TimeSlotRepository slotRepo,
                         DoctorEventProducer producer) {
        this.doctorRepo = doctorRepo;
        this.slotRepo = slotRepo;
        this.producer = producer;
    }

    @Transactional
    public Doctor createDoctor(Doctor doctor) {

        // 1. Basic validation
        if (doctor.getFee().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Doctor fee must be positive");
        }

        // 2. Initialize & link addresses
        if (doctor.getAddresses() != null) {
            doctor.getAddresses().forEach(address ->
                    address.setDoctor(doctor)
            );
        }

//        // 3. Initialize & link time slots
//        if (doctor.getTimeSlots() != null) {
//            doctor.getTimeSlots().forEach(slot ->
//                    slot.setDoctor(doctor)
//            );
//        }

        // 4. Save aggregate root
        return doctorRepo.save(doctor);
    }

    @Transactional
    public void deleteDoctor(Long id) {

        if (!doctorRepo.existsById(id)) {
            throw new RuntimeException("Invalid Doctor Id");
        }
        doctorRepo.deleteById(id);
    }

    @Transactional
    public TimeSlot createSlot(Long doctorId, TimeSlot slot) {

        Doctor doctor = doctorRepo.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        slot.setDoctor(doctor);

        if (!slot.getEndTime().isAfter(slot.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        TimeSlot saved =  slotRepo.save(slot);
       // producer.publishAvailability(doctorId);
        return saved;
    }


    public List<TimeSlot> getSlots(Long doctorId, LocalDate date) {
        return slotRepo.findByDoctorIdAndSlotDate(doctorId, date);
    }

    public Doctor getDoctor(Long doctorId) {
        return doctorRepo.findById(doctorId).orElseThrow(() -> new RuntimeException("Enter Valid DoctorID"));
    }
}

