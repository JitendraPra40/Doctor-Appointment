package com.app.auth_service.entity;

import com.app.auth_service.roles.Role;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "admin")
public class Admin {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false)
        private String username;

        @Column(unique = true, nullable = false)
        private String email;

        @Enumerated(EnumType.STRING)
        private Role role;

}
