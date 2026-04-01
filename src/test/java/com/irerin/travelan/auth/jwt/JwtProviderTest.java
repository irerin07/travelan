package com.irerin.travelan.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.irerin.travelan.user.entity.UserRole;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("travelan-test-secret-key-must-be-at-least-256-bits!!");
        props.setAccessTokenExpiry(3600L);
        props.setRefreshTokenExpiry(2592000L);
        jwtProvider = new JwtProvider(props);
    }

    @Test
    void generateAccessToken_유효한_토큰_생성() {
        String token = jwtProvider.generateAccessToken(1L, UserRole.USER);

        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtProvider.isValid(token)).isTrue();
    }

    @Test
    void generateRefreshToken_유효한_토큰_생성() {
        String token = jwtProvider.generateRefreshToken(1L, UserRole.USER);

        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtProvider.isValid(token)).isTrue();
    }

    @Test
    void getUserId_토큰에서_userId_추출() {
        String token = jwtProvider.generateAccessToken(42L, UserRole.USER);

        assertThat(jwtProvider.getUserId(token)).isEqualTo(42L);
    }

    @Test
    void getRole_토큰에서_role_추출() {
        String token = jwtProvider.generateAccessToken(1L, UserRole.ADMIN);

        assertThat(jwtProvider.getRole(token)).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void isValid_유효한_토큰_true() {
        String token = jwtProvider.generateAccessToken(1L, UserRole.USER);

        assertThat(jwtProvider.isValid(token)).isTrue();
    }

    @Test
    void isValid_만료된_토큰_false() {
        JwtProperties expiredProps = new JwtProperties();
        expiredProps.setSecret("travelan-test-secret-key-must-be-at-least-256-bits!!");
        expiredProps.setAccessTokenExpiry(0L);
        expiredProps.setRefreshTokenExpiry(2592000L);
        JwtProvider expiredProvider = new JwtProvider(expiredProps);

        String token = expiredProvider.generateAccessToken(1L, UserRole.USER);

        assertThat(jwtProvider.isValid(token)).isFalse();
    }

    @Test
    void isValid_잘못된_서명_false() {
        JwtProperties otherProps = new JwtProperties();
        otherProps.setSecret("completely-different-secret-key-for-testing-purposes!!");
        otherProps.setAccessTokenExpiry(3600L);
        otherProps.setRefreshTokenExpiry(2592000L);
        JwtProvider otherProvider = new JwtProvider(otherProps);

        String token = otherProvider.generateAccessToken(1L, UserRole.USER);

        assertThat(jwtProvider.isValid(token)).isFalse();
    }
}
