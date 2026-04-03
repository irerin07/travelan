package com.irerin.travelan.user.service;

import java.time.Clock;
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
import com.irerin.travelan.user.entity.UserAction;
import com.irerin.travelan.user.entity.UserHistory;
import com.irerin.travelan.user.repository.UserHistoryRepository;
import com.irerin.travelan.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserHistoryRepository userHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

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

        User user = User.of(
            request.getEmail(),
            passwordEncoder.encode(request.getPassword()),
            request.getName(),
            request.getPhone(),
            request.getNickname()
        );

        List<String> regions = request.getInterestRegions();
        if (regions != null) {
            regions.stream().distinct().forEach(user::addInterestRegion);
        }

        User savedUser = userRepository.save(user);
        userHistoryRepository.save(UserHistory.ofEvent(savedUser, UserAction.SIGNUP));

        return SignupResponse.from(savedUser);
    }

    @Transactional
    public void withdraw(User user) {
        user.withdraw(clock);

        userHistoryRepository.save(UserHistory.ofEvent(user, UserAction.WITHDRAWAL));
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

    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> findUsers(int page, int size) {
        return userRepository.findAll(PageRequest.of(page - 1, size)).map(UserSummaryResponse::from);
    }

}
