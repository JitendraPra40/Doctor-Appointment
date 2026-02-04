package com.app.appointment_service.contoller;

import com.app.appointment_service.entity.Appointment;
import com.app.appointment_service.service.AppointmentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@PreAuthorize("hasRole('PATIENT')")
public class AppointmentController {

    private final AppointmentService service;

    public AppointmentController(AppointmentService service) {
        this.service = service;
    }

    @PostMapping
    public Appointment book(Authentication auth,
                            @RequestBody AppointmentRequest req) {

        UUID patientId = UUID.fromString(auth.getName());

        return service.book(
                req.doctorId(),
                patientId,
                req.date(),
                req.time()
        );
    }
}

