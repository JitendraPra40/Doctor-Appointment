package com.app.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String token;

    private Instant expiry;

    private boolean revoked;

    @ManyToOne
    private User user;

    public RefreshToken(Object obj, String refresh, Instant plus, boolean status, User user) {
            this.token = refresh;
            this.expiry = plus;
            this.revoked = status;
            this.user = user;
    }
}

