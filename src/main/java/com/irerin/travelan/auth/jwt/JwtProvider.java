package com.irerin.travelan.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.irerin.travelan.user.entity.UserRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, UserRole role) {
        return buildToken(userId, role, jwtProperties.getAccessTokenExpiry());
    }

    public String generateRefreshToken(Long userId, UserRole role) {
        return buildToken(userId, role, jwtProperties.getRefreshTokenExpiry());
    }

    public Long getUserId(String token) {
        return Long.valueOf(getClaims(token).getSubject());
    }

    public UserRole getRole(String token) {
        return UserRole.valueOf(getClaims(token).get("role", String.class));
    }

    public boolean isValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private String buildToken(Long userId, UserRole role, long expirySeconds) {
        Date now = new Date();
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("role", role.name())
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expirySeconds * 1000))
            .signWith(secretKey)
            .compact();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
