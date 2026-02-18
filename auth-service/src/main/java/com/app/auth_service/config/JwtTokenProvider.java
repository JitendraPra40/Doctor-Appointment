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
        return Jwts.builder()
                .setHeaderParam("kid", "my-key-id")
                .setIssuer("http://localhost:8081") // Match this to your YAML
                .setSubject(String.valueOf(user.getId()))
                .claim("role", "ROLE_" + user.getRole().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

}
