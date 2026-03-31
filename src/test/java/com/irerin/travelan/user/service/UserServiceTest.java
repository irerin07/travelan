package com.irerin.travelan.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.irerin.travelan.auth.dto.SignupResponse;
import com.irerin.travelan.common.exception.DuplicateException;
import com.irerin.travelan.user.dto.SignupCommand;
import com.irerin.travelan.user.entity.User;
import com.irerin.travelan.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserService userService;

    private SignupCommand command;

    @BeforeEach
    void setUp() {
        command = new SignupCommand(
            "test@example.com", "Password1!", "홍길동", "01012345678", "여행자",
            List.of("서울", "부산")
        );
    }

    private User buildSavedUser() {
        User user = User.builder()
            .email(command.getEmail())
            .password("encoded")
            .name(command.getName())
            .phone(command.getPhone())
            .nickname(command.getNickname())
            .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    // ── signup ──────────────────────────────────────────────────────────────

    @Test
    void signup_성공() {
        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(userRepository.existsByPhone(anyString())).willReturn(false);
        given(userRepository.existsByNicknameIgnoreCase(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encoded");
        given(userRepository.save(any())).willReturn(buildSavedUser());

        SignupResponse response = userService.signup(command);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getNickname()).isEqualTo("여행자");
    }

    @Test
    void signup_비밀번호가_BCrypt로_저장된다() {
        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(userRepository.existsByPhone(anyString())).willReturn(false);
        given(userRepository.existsByNicknameIgnoreCase(anyString())).willReturn(false);
        given(passwordEncoder.encode("Password1!")).willReturn("encoded");
        given(userRepository.save(any())).willReturn(buildSavedUser());

        userService.signup(command);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded");
    }

    @Test
    void signup_관심지역이_저장된다() {
        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(userRepository.existsByPhone(anyString())).willReturn(false);
        given(userRepository.existsByNicknameIgnoreCase(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encoded");
        given(userRepository.save(any())).willReturn(buildSavedUser());

        userService.signup(command);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getInterestRegions()).hasSize(2);
    }

    @Test
    void signup_관심지역_없이도_가입된다() {
        SignupCommand noRegion = new SignupCommand(
            "test@example.com", "Password1!", "홍길동", "01012345678", "여행자", null
        );
        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(userRepository.existsByPhone(anyString())).willReturn(false);
        given(userRepository.existsByNicknameIgnoreCase(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encoded");
        given(userRepository.save(any())).willReturn(buildSavedUser());

        userService.signup(noRegion);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getInterestRegions()).isEmpty();
    }

    @Test
    void signup_이메일_중복이면_DuplicateException() {
        given(userRepository.existsByEmail(command.getEmail())).willReturn(true);

        assertThatThrownBy(() -> userService.signup(command))
            .isInstanceOf(DuplicateException.class)
            .hasMessage("이미 사용 중인 이메일입니다");
        verify(userRepository, never()).save(any());
    }

    @Test
    void signup_핸드폰번호_중복이면_DuplicateException() {
        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(userRepository.existsByPhone(command.getPhone())).willReturn(true);

        assertThatThrownBy(() -> userService.signup(command))
            .isInstanceOf(DuplicateException.class)
            .hasMessage("이미 사용 중인 휴대폰 번호입니다");
        verify(userRepository, never()).save(any());
    }

    @Test
    void signup_닉네임_중복이면_DuplicateException() {
        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(userRepository.existsByPhone(anyString())).willReturn(false);
        given(userRepository.existsByNicknameIgnoreCase(command.getNickname())).willReturn(true);

        assertThatThrownBy(() -> userService.signup(command))
            .isInstanceOf(DuplicateException.class)
            .hasMessage("이미 사용 중인 닉네임입니다");
        verify(userRepository, never()).save(any());
    }

    // ── isEmailAvailable ────────────────────────────────────────────────────

    @Test
    void isEmailAvailable_미사용_이메일이면_true() {
        given(userRepository.existsByEmail("new@example.com")).willReturn(false);
        assertThat(userService.isEmailAvailable("new@example.com")).isTrue();
    }

    @Test
    void isEmailAvailable_이미_사용중이면_false() {
        given(userRepository.existsByEmail("used@example.com")).willReturn(true);
        assertThat(userService.isEmailAvailable("used@example.com")).isFalse();
    }

    // ── isPhoneAvailable ────────────────────────────────────────────────────

    @Test
    void isPhoneAvailable_미사용_번호면_true() {
        given(userRepository.existsByPhone("01099999999")).willReturn(false);
        assertThat(userService.isPhoneAvailable("01099999999")).isTrue();
    }

    @Test
    void isPhoneAvailable_이미_사용중이면_false() {
        given(userRepository.existsByPhone("01012345678")).willReturn(true);
        assertThat(userService.isPhoneAvailable("01012345678")).isFalse();
    }

    // ── isNicknameAvailable ─────────────────────────────────────────────────

    @Test
    void isNicknameAvailable_미사용_닉네임이면_true() {
        given(userRepository.existsByNicknameIgnoreCase("새닉")).willReturn(false);
        assertThat(userService.isNicknameAvailable("새닉")).isTrue();
    }

    @Test
    void isNicknameAvailable_이미_사용중이면_false() {
        given(userRepository.existsByNicknameIgnoreCase("여행자")).willReturn(true);
        assertThat(userService.isNicknameAvailable("여행자")).isFalse();
    }
}
