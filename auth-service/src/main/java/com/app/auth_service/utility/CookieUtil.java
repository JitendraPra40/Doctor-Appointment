package com.app.auth_service.utility;

import com.app.auth_service.propertiesConfig.JwtProperties;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class CookieUtil {

    private final JwtProperties jwtProperties;
    private long refreshExpiry;

    @PostConstruct
    void init() {
        this.refreshExpiry = jwtProperties.getRefresh().getExpiration();
    }

    public CookieUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String getRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        return Arrays.stream(request.getCookies())
                .filter(c -> "refreshToken".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    public void setRefreshToken(
            HttpServletResponse response,
            String token
    ) {
        Cookie cookie = new Cookie("refreshToken", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/v1/auth/refresh");
        cookie.setMaxAge((int)refreshExpiry);
        response.addCookie(cookie);
    }

    public void clearRefreshToken(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/v1/auth/refresh");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
