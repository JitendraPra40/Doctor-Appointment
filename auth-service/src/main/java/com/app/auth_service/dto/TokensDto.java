package com.app.auth_service.dto;

import lombok.Data;

@Data
public class TokensDto {
    private String accessToken;
    private String refreshToken;

    public TokensDto(String access, String refresh) {
        this.accessToken = access;
        this.refreshToken = refresh;
    }
}
