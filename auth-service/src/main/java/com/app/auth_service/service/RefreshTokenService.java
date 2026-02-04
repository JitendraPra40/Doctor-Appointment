package com.app.auth_service.service;

import com.app.auth_service.propertiesConfig.JwtProperties;
import com.app.auth_service.dto.TokensDto;
import com.app.auth_service.entity.RefreshToken;
import com.app.auth_service.entity.User;
import com.app.auth_service.repository.RefreshTokenRepository;
import com.app.auth_service.config.JwtTokenProvider;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private JwtTokenProvider jwtTokenProvider;

    private long accessExpiry;
    private long refreshExpiry;
    @PostConstruct
    void init() {
        this.accessExpiry = jwtProperties.getAccess().getExpiration();
        this.refreshExpiry = jwtProperties.getRefresh().getExpiration();
    }

    public RefreshTokenService(JwtProperties jwtProperties, RefreshTokenRepository refreshTokenRepository, JwtTokenProvider jwtTokenProvider) {
        this.jwtProperties = jwtProperties;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public String createRefreshToken(User user){
        RefreshToken token = new RefreshToken();
        token.setToken(UUID.randomUUID().toString());
        token.setExpiry(Instant.now().plusMillis(refreshExpiry));
        token.setUser(user);
        token.setRevoked(false);
        RefreshToken saved = refreshTokenRepository.save(token);
        return(saved.getToken());
    }

    public TokensDto refresh(String token){
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.isRevoked()){
            throw new RuntimeException("Refresh token reuse detected");
        }

        if (refreshToken.getExpiry().isBefore(Instant.now())){
            throw new RuntimeException("Refresh token expired");
        }
        refreshToken.setRevoked(true);

        User user = refreshToken.getUser();
        String newAccessToken = jwtTokenProvider.generateAccessToken(user, accessExpiry);
        String newRefreshToken = createRefreshToken(user);

        return new TokensDto(newAccessToken, newRefreshToken);
    }

    public void revokeRefreshToken(String refreshToken){
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(token -> token.setRevoked(true));
    }
}
