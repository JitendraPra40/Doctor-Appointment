package com.app.appointment_service.controller;

import com.app.appointment_service.dto.AppointmentRequest;
import com.app.appointment_service.entity.Appointment;
import com.app.appointment_service.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Appointments", description = "Appointment booking and management (Saga pattern)")
@SecurityRequirement(name = "Bearer Authentication")
public class AppointmentController {

    private static final Logger log = LoggerFactory.getLogger(AppointmentController.class);

    private final AppointmentService service;

    public AppointmentController(AppointmentService service) {
        this.service = service;
    }

    @Operation(
            summary = "Book an appointment",
            description = "Books a time slot with a doctor. Triggers the Appointment Booking Saga — " +
                    "publishes AppointmentBookedEvent to Kafka for downstream payment processing."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Appointment booked successfully"),
            @ApiResponse(responseCode = "401", description = "JWT token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Only PATIENT role can book"),
            @ApiResponse(responseCode = "409", description = "Slot already booked — choose a different time")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PATIENT')")
    public Appointment book(
            Authentication auth,
            @RequestBody AppointmentRequest req) {

        UUID patientId = UUID.fromString(auth.getName());
        log.info("📥 Book appointment request | patient={} doctor={} date={} time={}",
                patientId, req.doctorId(), req.date(), req.time());

        return service.book(req.doctorId(), patientId, req.date(), req.time());
    }

    @Operation(
            summary = "Cancel an appointment",
            description = "Cancels a booked appointment. This is a compensating transaction in the Saga — " +
                    "publishes AppointmentCancelledEvent to trigger notifications."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Appointment cancelled"),
            @ApiResponse(responseCode = "404", description = "Appointment not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('PATIENT')")
    public void cancel(
            @Parameter(description = "Appointment UUID") @PathVariable UUID id,
            Authentication auth) {

        log.info("📥 Cancel appointment request | id={} patient={}", id, auth.getName());
        service.cancelAppointment(id, "PATIENT_REQUESTED");
    }
}
