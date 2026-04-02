package com.irerin.travelan.auth.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.irerin.travelan.auth.dto.LoginCommand;
import com.irerin.travelan.auth.dto.LoginTokens;
import com.irerin.travelan.auth.entity.RefreshToken;
import com.irerin.travelan.auth.jwt.JwtProperties;
import com.irerin.travelan.auth.jwt.JwtProvider;
import com.irerin.travelan.auth.repository.RefreshTokenRepository;
import com.irerin.travelan.common.exception.AuthException;
import com.irerin.travelan.user.entity.User;
import com.irerin.travelan.user.entity.UserStatus;
import com.irerin.travelan.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    @Transactional
    public LoginTokens login(LoginCommand command) {
        User user = userRepository.findByEmail(command.getEmail())
            .orElseThrow(() -> new AuthException("이메일 또는 비밀번호가 올바르지 않습니다"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthException("이메일 또는 비밀번호가 올바르지 않습니다");
        }
        if (!passwordEncoder.matches(command.getPassword(), user.getPassword())) {
            throw new AuthException("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        refreshTokenRepository.revokeAllByUser(user);

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getRole());

        refreshTokenRepository.save(RefreshToken.of(
            user, refreshToken,
            LocalDateTime.now(clock).plusSeconds(jwtProperties.getRefreshTokenExpiry())
        ));

        return LoginTokens.of(accessToken, refreshToken, jwtProperties.getAccessTokenExpiry());
    }

    @Transactional
    public LoginTokens refresh(String rawToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(rawToken)
            .orElseThrow(() -> new AuthException("유효하지 않은 리프레시 토큰입니다"));

        if (stored.isRevoked()) {
            refreshTokenRepository.revokeAllByUser(stored.getUser());
            throw new AuthException("토큰 재사용이 감지되었습니다");
        }

        if (stored.isExpired(clock)) {
            throw new AuthException("만료된 리프레시 토큰입니다");
        }

        stored.revoke();

        User user = stored.getUser();
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole());
        String newRefreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getRole());

        refreshTokenRepository.save(RefreshToken.of(
            user, newRefreshToken,
            LocalDateTime.now(clock).plusSeconds(jwtProperties.getRefreshTokenExpiry())
        ));

        return LoginTokens.of(accessToken, newRefreshToken, jwtProperties.getAccessTokenExpiry());
    }

    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException("존재하지 않는 회원입니다"));

        refreshTokenRepository.revokeAllByUser(user);
    }

}
