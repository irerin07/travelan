package com.irerin.travelan.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.irerin.travelan.auth.dto.LoginCommand;
import com.irerin.travelan.auth.dto.LoginRequest;
import com.irerin.travelan.auth.entity.RefreshToken;
import com.irerin.travelan.auth.dto.LoginTokens;
import com.irerin.travelan.auth.jwt.JwtProperties;
import com.irerin.travelan.auth.jwt.JwtProvider;
import com.irerin.travelan.auth.repository.RefreshTokenRepository;
import com.irerin.travelan.common.exception.AuthException;
import com.irerin.travelan.user.entity.User;
import com.irerin.travelan.user.entity.UserRole;
import com.irerin.travelan.user.entity.UserStatus;
import com.irerin.travelan.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtProvider jwtProvider;
    @Mock private JwtProperties jwtProperties;

    private AuthService authService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-01T12:00:00Z"), ZoneId.of("Asia/Seoul"));
        authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtProvider, jwtProperties, clock);
        activeUser = User.builder()
            .email("test@example.com")
            .password("encodedPassword")
            .name("홍길동")
            .phone("01012345678")
            .nickname("여행자")
            .build();
        ReflectionTestUtils.setField(activeUser, "id", 1L);
        ReflectionTestUtils.setField(activeUser, "role", UserRole.USER);
        ReflectionTestUtils.setField(activeUser, "status", UserStatus.ACTIVE);
    }

    @Test
    void login_성공_LoginTokens_반환() {
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(activeUser));
        given(passwordEncoder.matches("password", "encodedPassword")).willReturn(true);
        given(jwtProvider.generateAccessToken(1L, UserRole.USER)).willReturn("access-token");
        given(jwtProvider.generateRefreshToken(1L, UserRole.USER)).willReturn("refresh-token");
        given(jwtProperties.getRefreshTokenExpiry()).willReturn(2592000L);
        given(jwtProperties.getAccessTokenExpiry()).willReturn(3600L);

        LoginTokens tokens = authService.login(loginCommand("test@example.com", "password"));

        assertThat(tokens.getAccessToken()).isEqualTo("access-token");
        assertThat(tokens.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(tokens.getAccessTokenExpiry()).isEqualTo(3600L);
    }

    @Test
    void login_이메일_미존재_AuthException() {
        given(userRepository.findByEmail("notfound@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginCommand("notfound@example.com", "password")))
            .isInstanceOf(AuthException.class)
            .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다");
    }

    @Test
    void login_비밀번호_불일치_AuthException() {
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(activeUser));
        given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

        assertThatThrownBy(() -> authService.login(loginCommand("test@example.com", "wrongPassword")))
            .isInstanceOf(AuthException.class)
            .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다");
    }

    @Test
    void login_SUSPENDED_계정_AuthException() {
        ReflectionTestUtils.setField(activeUser, "status", UserStatus.SUSPENDED);
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.login(loginCommand("test@example.com", "password")))
            .isInstanceOf(AuthException.class)
            .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다");
    }

    @Test
    void login_WITHDRAWN_계정_AuthException() {
        ReflectionTestUtils.setField(activeUser, "status", UserStatus.WITHDRAWN);
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.login(loginCommand("test@example.com", "password")))
            .isInstanceOf(AuthException.class)
            .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다");
    }

    @Test
    void login_성공시_RefreshToken이_저장된다() {
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(activeUser));
        given(passwordEncoder.matches("password", "encodedPassword")).willReturn(true);
        given(jwtProvider.generateAccessToken(1L, UserRole.USER)).willReturn("access-token");
        given(jwtProvider.generateRefreshToken(1L, UserRole.USER)).willReturn("refresh-token");
        given(jwtProperties.getRefreshTokenExpiry()).willReturn(2592000L);
        given(jwtProperties.getAccessTokenExpiry()).willReturn(3600L);

        authService.login(loginCommand("test@example.com", "password"));

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_성공시_기존_RefreshToken이_revoke된다() {
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(activeUser));
        given(passwordEncoder.matches("password", "encodedPassword")).willReturn(true);
        given(jwtProvider.generateAccessToken(1L, UserRole.USER)).willReturn("access-token");
        given(jwtProvider.generateRefreshToken(1L, UserRole.USER)).willReturn("refresh-token");
        given(jwtProperties.getRefreshTokenExpiry()).willReturn(2592000L);
        given(jwtProperties.getAccessTokenExpiry()).willReturn(3600L);

        authService.login(loginCommand("test@example.com", "password"));

        verify(refreshTokenRepository).revokeAllByUser(activeUser);
    }

    // ── refresh ──────────────────────────────────────────────────────────

    @Test
    void refresh_성공_새_토큰_반환() {
        RefreshToken stored = createRefreshToken(activeUser, "old-refresh-token", false);
        given(refreshTokenRepository.findByToken("old-refresh-token")).willReturn(Optional.of(stored));
        given(jwtProvider.generateAccessToken(1L, UserRole.USER)).willReturn("new-access-token");
        given(jwtProvider.generateRefreshToken(1L, UserRole.USER)).willReturn("new-refresh-token");
        given(jwtProperties.getRefreshTokenExpiry()).willReturn(2592000L);
        given(jwtProperties.getAccessTokenExpiry()).willReturn(3600L);

        LoginTokens tokens = authService.refresh("old-refresh-token");

        assertThat(tokens.getAccessToken()).isEqualTo("new-access-token");
        assertThat(tokens.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(tokens.getAccessTokenExpiry()).isEqualTo(3600L);
    }

    @Test
    void refresh_토큰_미존재_AuthException() {
        given(refreshTokenRepository.findByToken("unknown-token")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("unknown-token"))
            .isInstanceOf(AuthException.class);
    }

    @Test
    void refresh_만료된_토큰_AuthException() {
        RefreshToken expired = createRefreshToken(activeUser, "expired-token", false);
        ReflectionTestUtils.setField(expired, "expiresAt",
            java.time.LocalDateTime.of(2026, 3, 1, 0, 0)); // 과거
        given(refreshTokenRepository.findByToken("expired-token")).willReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refresh("expired-token"))
            .isInstanceOf(AuthException.class);
    }

    @Test
    void refresh_revoked된_토큰_재사용_감지_전체_revoke_후_AuthException() {
        RefreshToken revoked = createRefreshToken(activeUser, "revoked-token", true);
        given(refreshTokenRepository.findByToken("revoked-token")).willReturn(Optional.of(revoked));

        assertThatThrownBy(() -> authService.refresh("revoked-token"))
            .isInstanceOf(AuthException.class);

        verify(refreshTokenRepository).revokeAllByUser(activeUser);
    }

    @Test
    void refresh_성공시_기존토큰_revoke_후_신규토큰_저장() {
        RefreshToken stored = createRefreshToken(activeUser, "old-refresh-token", false);
        given(refreshTokenRepository.findByToken("old-refresh-token")).willReturn(Optional.of(stored));
        given(jwtProvider.generateAccessToken(1L, UserRole.USER)).willReturn("new-access-token");
        given(jwtProvider.generateRefreshToken(1L, UserRole.USER)).willReturn("new-refresh-token");
        given(jwtProperties.getRefreshTokenExpiry()).willReturn(2592000L);
        given(jwtProperties.getAccessTokenExpiry()).willReturn(3600L);

        authService.refresh("old-refresh-token");

        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    // ── logout ──────────────────────────────────────────────────────────

    @Test
    void logout_성공_해당유저_전체토큰_revoke() {
        authService.logout(1L);

        verify(refreshTokenRepository).revokeAllByUserId(1L);
    }

    @Test
    void logout_존재하지_않는_유저도_revokeAllByUserId_호출() {
        authService.logout(999L);

        verify(refreshTokenRepository).revokeAllByUserId(999L);
    }

    private RefreshToken createRefreshToken(User user, String token, boolean revoked) {
        RefreshToken refreshToken = RefreshToken.of(user, token,
            java.time.LocalDateTime.of(2026, 5, 1, 0, 0)); // 미래 = 유효
        if (revoked) {
            refreshToken.revoke();
        }
        return refreshToken;
    }

    private LoginCommand loginCommand(String email, String password) {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", password);
        return LoginCommand.from(request);
    }
}
