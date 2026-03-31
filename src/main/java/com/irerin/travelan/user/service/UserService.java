package com.irerin.travelan.user.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.irerin.travelan.admin.dto.UserSummaryResponse;
import com.irerin.travelan.auth.dto.SignupResponse;
import com.irerin.travelan.common.exception.DuplicateException;
import com.irerin.travelan.user.dto.SignupCommand;
import com.irerin.travelan.user.entity.User;
import com.irerin.travelan.user.entity.UserStatus;
import com.irerin.travelan.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignupResponse signup(SignupCommand request) {
        if (userRepository.existsByEmailAndStatusNot(request.getEmail(), UserStatus.WITHDRAWN)) {
            throw new DuplicateException("이미 사용 중인 이메일입니다");
        }

        if (userRepository.existsByPhoneAndStatusNot(request.getPhone(), UserStatus.WITHDRAWN)) {
            throw new DuplicateException("이미 사용 중인 휴대폰 번호입니다");
        }

        if (userRepository.existsByNicknameIgnoreCaseAndStatusNot(request.getNickname(), UserStatus.WITHDRAWN)) {
            throw new DuplicateException("이미 사용 중인 닉네임입니다");
        }

        userRepository.findByEmail(request.getEmail()).ifPresent(userRepository::delete);
        userRepository.findByPhone(request.getPhone()).ifPresent(userRepository::delete);
        userRepository.findByNicknameIgnoreCase(request.getNickname()).ifPresent(userRepository::delete);

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

        return SignupResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmailAndStatusNot(email, UserStatus.WITHDRAWN);
    }

    @Transactional(readOnly = true)
    public boolean isPhoneAvailable(String phone) {
        return !userRepository.existsByPhoneAndStatusNot(phone, UserStatus.WITHDRAWN);
    }

    @Transactional(readOnly = true)
    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNicknameIgnoreCaseAndStatusNot(nickname, UserStatus.WITHDRAWN);
    }

    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> findUsers(int page, int size) {
        return userRepository.findAll(PageRequest.of(page - 1, size)).map(UserSummaryResponse::from);
    }

}
