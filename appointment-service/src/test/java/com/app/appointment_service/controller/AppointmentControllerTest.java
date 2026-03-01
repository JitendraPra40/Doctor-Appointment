package com.app.appointment_service.controller;

import com.app.appointment_service.entity.Appointment;
import com.app.appointment_service.service.AppointmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = com.app.appointment_service.controller.AppointmentController.class)
@DisplayName("AppointmentController Tests")
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppointmentService service;

    private ObjectMapper mapper;
    private UUID patientId;
    private UUID doctorId;
    private UUID appointmentId;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        patientId     = UUID.randomUUID();
        doctorId      = UUID.randomUUID();
        appointmentId = UUID.randomUUID();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // POST /api/v1/appointments
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("POST /api/v1/appointments")
    class BookEndpointTests {

        @Test
        @DisplayName("returns 201 and appointment body on valid request")
        @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "PATIENT")
        void book_validRequest_returns201() throws Exception {
            // Arrange
            Appointment appt = new Appointment();
            appt.setId(appointmentId);
            appt.setStatus(Appointment.Status.BOOKED);
            appt.setDoctorId(doctorId);

            when(service.book(any(), any(), any(), any())).thenReturn(appt);

            String body = mapper.writeValueAsString(Map.of(
                    "doctorId", doctorId.toString(),
                    "date", "2026-03-15",
                    "time", "10:00:00"
            ));

            // Act & Assert
            mockMvc.perform(post("/api/v1/appointments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(appointmentId.toString()))
                    .andExpect(jsonPath("$.status").value("BOOKED"));
        }

        @Test
        @DisplayName("returns 401 when no JWT token provided")
        void book_noAuth_returns401() throws Exception {
            mockMvc.perform(post("/api/v1/appointments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("returns 403 when user is ADMIN role (not PATIENT)")
        @WithMockUser(username = "admin-id", roles = "ADMIN")
        void book_adminRole_returns403() throws Exception {
            mockMvc.perform(post("/api/v1/appointments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 409 when slot already booked")
        @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "PATIENT")
        void book_slotConflict_returns409() throws Exception {
            when(service.book(any(), any(), any(), any()))
                    .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT,
                            "This slot is already booked."));

            String body = mapper.writeValueAsString(Map.of(
                    "doctorId", doctorId.toString(),
                    "date", "2026-03-15",
                    "time", "10:00:00"
            ));

            mockMvc.perform(post("/api/v1/appointments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // DELETE /api/v1/appointments/{id}
    // ═════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("DELETE /api/v1/appointments/{id}")
    class CancelEndpointTests {

        @Test
        @DisplayName("returns 204 on successful cancellation")
        @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "PATIENT")
        void cancel_success_returns204() throws Exception {
            doNothing().when(service).cancelAppointment(any(), anyString());

            mockMvc.perform(delete("/api/v1/appointments/{id}", appointmentId)
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(service).cancelAppointment(eq(appointmentId), eq("PATIENT_REQUESTED"));
        }

        @Test
        @DisplayName("returns 404 when appointment not found")
        @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "PATIENT")
        void cancel_notFound_returns404() throws Exception {
            doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"))
                    .when(service).cancelAppointment(any(), anyString());

            mockMvc.perform(delete("/api/v1/appointments/{id}", appointmentId)
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }
}
