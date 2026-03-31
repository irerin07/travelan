package com.irerin.travelan.user.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.irerin.travelan.auth.dto.SignupResponse;
import com.irerin.travelan.common.exception.DuplicateException;
import com.irerin.travelan.user.dto.SignupCommand;
import com.irerin.travelan.user.entity.User;
import com.irerin.travelan.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignupResponse signup(SignupCommand request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateException("이미 사용 중인 이메일입니다");
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateException("이미 사용 중인 휴대폰 번호입니다");
        }

        if (userRepository.existsByNicknameIgnoreCase(request.getNickname())) {
            throw new DuplicateException("이미 사용 중인 닉네임입니다");
        }

        User user = User.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .name(request.getName())
            .phone(request.getPhone())
            .nickname(request.getNickname())
            .build();

        List<String> regions = request.getInterestRegions();
        if (regions != null) {
            regions.forEach(user::addInterestRegion);
        }

        return new SignupResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean isPhoneAvailable(String phone) {
        return !userRepository.existsByPhone(phone);
    }

    @Transactional(readOnly = true)
    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNicknameIgnoreCase(nickname);
    }

}
