package com.app.doctor_service.service;

import com.app.doctor_service.entity.Doctor;
import com.app.doctor_service.entity.TimeSlot;
import com.app.doctor_service.repository.DoctorRepository;
import com.app.doctor_service.repository.TimeSlotRepository;
// FIX: Removed 'jakarta.ws.rs.BadRequestException' — that's JAX-RS, not on the classpath.
// This project uses Spring MVC. Use Spring's ResponseStatusException instead.
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
        if (doctor.getFee().compareTo(BigDecimal.ZERO) <= 0) {
            // FIX: Use ResponseStatusException (Spring) instead of BadRequestException (JAX-RS)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Doctor fee must be positive");
        }

        if (doctor.getAddresses() != null) {
            doctor.getAddresses().forEach(address -> address.setDoctor(doctor));
        }

        return doctorRepo.save(doctor);
    }

    @Transactional
    public void deleteDoctor(Long id) {
        if (!doctorRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid Doctor Id");
        }
        doctorRepo.deleteById(id);
    }

    @Transactional
    public TimeSlot createSlot(Long doctorId, TimeSlot slot) {
        Doctor doctor = doctorRepo.findById(doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));

        slot.setDoctor(doctor);

        if (!slot.getEndTime().isAfter(slot.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }

        return slotRepo.save(slot);
    }

    public List<TimeSlot> getSlots(Long doctorId, LocalDate date) {
        return slotRepo.findByDoctorIdAndSlotDate(doctorId, date);
    }

    public Doctor getDoctor(Long doctorId) {
        return doctorRepo.findById(doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enter Valid DoctorID"));
    }
}
