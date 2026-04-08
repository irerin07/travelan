package com.irerin.travelan.auth.support;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieFactory {

    public static final String REFRESH_TOKEN_PATH = "/api/v1/auth";

    public ResponseCookie refreshTokenCookie(String value, Duration ttl) {
        return ResponseCookie.from("refreshToken", value)
            .httpOnly(true)
            .secure(true)
            .sameSite("Strict")
            .maxAge(ttl)
            .path(REFRESH_TOKEN_PATH)
            .build();
    }

    public ResponseCookie expiredRefreshTokenCookie() {
        return ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .secure(true)
            .sameSite("Strict")
            .maxAge(0)
            .path(REFRESH_TOKEN_PATH)
            .build();
    }
}
