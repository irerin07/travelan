package com.irerin.travelan.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

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

    @InjectMocks private AuthService authService;

    private User activeUser;

    @BeforeEach
    void setUp() {
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

    private LoginCommand loginCommand(String email, String password) {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", password);
        return LoginCommand.from(request);
    }
}
