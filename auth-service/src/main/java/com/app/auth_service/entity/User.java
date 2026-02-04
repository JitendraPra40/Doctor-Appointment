package com.app.auth_service.entity;

import com.app.auth_service.roles.Role;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Getter
    @Setter
    @Id
    @GeneratedValue
    private UUID id;

    @Column(unique = true, nullable = false)
    private String mobile;

    @Column(unique = true, nullable = true)
    private String email;

    @Enumerated(EnumType.STRING)
    private Role role;

    public User(Object obj, String mobile, Role role) {
        this.mobile = mobile;
        this.role = role;
    }
}

