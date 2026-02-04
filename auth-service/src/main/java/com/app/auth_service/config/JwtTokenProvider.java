package com.app.auth_service.config;

import com.app.auth_service.entity.Admin;
import com.app.auth_service.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;
import java.security.interfaces.RSAPrivateKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final RSAPrivateKey privateKey;

    public JwtTokenProvider(RSAPrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public String generateAccessToken(User user, long expiryMs) {

        Date now = new Date();

        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(String.valueOf(user.getId()))   // ✅ userId
                .claim("role", "ROLE_" + user.getRole().name())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expiryMs))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }


}
