package com.app.patient_service.contoller;

import com.app.patient_service.entity.AppointmentView;
import com.app.patient_service.entity.Patient;
import com.app.patient_service.service.PatientService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients")
@PreAuthorize("hasRole('PATIENT')")
public class PatientController {

    private final PatientService service;

    public PatientController(PatientService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public Patient profile(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return service.getProfile(userId);
    }

    @PutMapping("/me")
    public Patient update(Authentication auth,
                          @RequestBody Patient patient) {
        UUID userId = UUID.fromString(auth.getName());
        return service.updateProfile(userId, patient);
    }

    @GetMapping("/me/appointments")
    public List<AppointmentView> appointments(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return service.myAppointments(userId);
    }
}

