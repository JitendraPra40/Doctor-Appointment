package com.app.doctor_service.service;

import com.app.doctor_service.entity.Doctor;
import com.app.doctor_service.entity.TimeSlot;
import com.app.doctor_service.repository.DoctorRepository;
import com.app.doctor_service.repository.TimeSlotRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class DoctorService {

    private static final Logger log = LoggerFactory.getLogger(DoctorService.class);
    private static final String KAFKA_CB = "kafkaPublisher";

    private final DoctorRepository doctorRepo;
    private final TimeSlotRepository slotRepo;
    private final KafkaTemplate<String, Object> kafka;

    public DoctorService(DoctorRepository doctorRepo, TimeSlotRepository slotRepo,
                         KafkaTemplate<String, Object> kafka) {
        this.doctorRepo = doctorRepo;
        this.slotRepo   = slotRepo;
        this.kafka      = kafka;
    }

    @Transactional
    public Doctor createDoctor(Doctor doctor) {
        log.info("👨‍⚕️ Creating doctor | name={} specialization={}", doctor.getName(), doctor.getSpecialization());

        if (doctor.getFee().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("⚠️ Invalid fee for doctor | fee={}", doctor.getFee());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Doctor fee must be positive");
        }
        if (doctor.getAddresses() != null) {
            doctor.getAddresses().forEach(address -> address.setDoctor(doctor));
        }

        Doctor saved = doctorRepo.save(doctor);
        log.info("✅ Doctor created | id={} name={}", saved.getId(), saved.getName());

        publishDoctorCreatedEvent(saved);
        return saved;
    }

    @Transactional
    public void deleteDoctor(Long id) {
        log.info("🗑️ Deleting doctor | id={}", id);
        if (!doctorRepo.existsById(id)) {
            log.warn("⚠️ Doctor not found | id={}", id);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid Doctor Id");
        }
        doctorRepo.deleteById(id);
        log.info("✅ Doctor deleted | id={}", id);
    }

    @Transactional
    public TimeSlot createSlot(Long doctorId, TimeSlot slot) {
        log.info("🕐 Creating time slot | doctorId={} date={}", doctorId, slot.getSlotDate());
        Doctor doctor = doctorRepo.findById(doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));

        slot.setDoctor(doctor);
        if (!slot.getEndTime().isAfter(slot.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }

        TimeSlot saved = slotRepo.save(slot);
        log.info("✅ Time slot created | slotId={} doctorId={}", saved.getId(), doctorId);
        return saved;
    }

    public List<TimeSlot> getSlots(Long doctorId, LocalDate date) {
        log.debug("🔍 Fetching slots | doctorId={} date={}", doctorId, date);
        return slotRepo.findByDoctorIdAndSlotDate(doctorId, date);
    }

    public Doctor getDoctor(Long doctorId) {
        log.debug("🔍 Fetching doctor | id={}", doctorId);
        return doctorRepo.findById(doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enter Valid DoctorID"));
    }

    public List<Doctor> getAllDoctors() {
        log.debug("🔍 Fetching all doctors");
        return doctorRepo.findAll();
    }

    @CircuitBreaker(name = KAFKA_CB, fallbackMethod = "publishDoctorCreatedFallback")
    @Retry(name = KAFKA_CB)
    public void publishDoctorCreatedEvent(Doctor doctor) {
        kafka.send("doctor-events", doctor.getId().toString(), doctor);
        log.info("📨 Published DoctorCreatedEvent | doctorId={}", doctor.getId());
    }

    public void publishDoctorCreatedFallback(Doctor doctor, Throwable t) {
        log.error("🔴 [CIRCUIT-OPEN] Cannot publish DoctorCreatedEvent | doctorId={} error={}",
                doctor.getId(), t.getMessage());
    }
}
