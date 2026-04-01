package com.irerin.travelan.auth.service;

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

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), user.getRole());

        refreshTokenRepository.save(RefreshToken.of(
            user, refreshToken,
            LocalDateTime.now().plusSeconds(jwtProperties.getRefreshTokenExpiry())
        ));

        return LoginTokens.of(accessToken, refreshToken, jwtProperties.getAccessTokenExpiry());
    }

}
