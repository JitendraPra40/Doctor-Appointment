package com.app.doctor_service.contoller;

import com.app.doctor_service.entity.Doctor;
import com.app.doctor_service.entity.TimeSlot;
import com.app.doctor_service.service.DoctorService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/doctors")
@PreAuthorize("hasAuthority('ROLE_DOCTOR') or hasAuthority('ROLE_ADMIN')")

public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @PostMapping("/add")
    public Doctor create(@RequestBody Doctor doctor) {
        return doctorService.createDoctor(doctor);
    }

    @GetMapping("/{doctorId}")
    public ResponseEntity<Doctor> getDoctor(@PathVariable Long doctorId){
        Doctor doctor = doctorService.getDoctor(doctorId);
        return ResponseEntity.ok(doctor);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDoctor(@PathVariable Long id) {
        doctorService.deleteDoctor(id);
        return ResponseEntity.ok("Doctor deleted successfully");
    }


    @PostMapping("/{doctorId}/time-slots")
    public TimeSlot createSlot(
            @PathVariable Long doctorId,
            @RequestBody TimeSlot slot) {
        return doctorService.createSlot(doctorId, slot);
    }

    @GetMapping("/{doctorId}/slots")
    public List<TimeSlot> getTimeSlots(@PathVariable Long doctorId,
                                @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date ) {
        return doctorService.getSlots(doctorId, date);
    }

    @GetMapping("/api/v1/doctors/debug")
    public String debug(Authentication auth) {
        if (auth == null) {
            return "No authentication found!";
        }
        return "Your Authorities: " + auth.getAuthorities().toString();
    }
}

