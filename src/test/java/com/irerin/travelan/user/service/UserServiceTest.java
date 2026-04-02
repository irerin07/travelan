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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import org.springframework.data.domain.Page;

import com.irerin.travelan.admin.dto.UserSummaryResponse;
import com.irerin.travelan.auth.dto.SignupResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import com.irerin.travelan.common.exception.DuplicateException;
import com.irerin.travelan.user.dto.SignupCommand;
import com.irerin.travelan.user.entity.User;
import com.irerin.travelan.user.entity.UserAction;
import com.irerin.travelan.user.entity.UserHistory;
import com.irerin.travelan.user.entity.UserRole;
import com.irerin.travelan.user.entity.UserStatus;
import com.irerin.travelan.user.repository.UserHistoryRepository;
import com.irerin.travelan.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserHistoryRepository userHistoryRepository;
    @Mock PasswordEncoder passwordEncoder;

    private UserService userService;

    private SignupCommand command;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-01T12:00:00Z"), ZoneId.of("Asia/Seoul"));
        userService = new UserService(userRepository, userHistoryRepository, passwordEncoder, clock);
        command = SignupCommand.of(
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
        ReflectionTestUtils.setField(user, "role", UserRole.USER);
        return user;
    }

    /** 중복 없음 상태를 stub하는 헬퍼 */
    private void stubNoDuplicate() {
        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(userRepository.existsByPhone(anyString())).willReturn(false);
        given(userRepository.existsByNicknameIgnoreCase(anyString())).willReturn(false);
    }

    // ── signup ──────────────────────────────────────────────────────────────

    @Test
    void signup_성공() {
        stubNoDuplicate();
        given(passwordEncoder.encode(anyString())).willReturn("encoded");
        given(userRepository.save(any())).willReturn(buildSavedUser());

        SignupResponse response = userService.signup(command);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getNickname()).isEqualTo("여행자");
    }

    @Test
    void signup_비밀번호가_BCrypt로_저장된다() {
        stubNoDuplicate();
        given(passwordEncoder.encode("Password1!")).willReturn("encoded");
        given(userRepository.save(any())).willReturn(buildSavedUser());

        userService.signup(command);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded");
    }

    @Test
    void signup_관심지역이_저장된다() {
        stubNoDuplicate();
        given(passwordEncoder.encode(anyString())).willReturn("encoded");
        given(userRepository.save(any())).willReturn(buildSavedUser());

        userService.signup(command);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getInterestRegions()).hasSize(2);
    }

    @Test
    void signup_관심지역_없이도_가입된다() {
        SignupCommand noRegion = SignupCommand.of(
            "test@example.com", "Password1!", "홍길동", "01012345678", "여행자", null
        );
        stubNoDuplicate();
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
    void signup_WITHDRAWN_이메일은_익명화되어_중복_아님() {
        // WITHDRAWN 유저의 이메일은 "withdrawn_99@deleted"로 익명화되므로
        // 원래 이메일로 existsByEmail하면 false → 재가입 가능
        stubNoDuplicate();
        given(passwordEncoder.encode(anyString())).willReturn("encoded");
        given(userRepository.save(any())).willReturn(buildSavedUser());

        SignupResponse response = userService.signup(command);

        assertThat(response.getEmail()).isEqualTo("test@example.com");
        verify(userRepository, never()).delete(any());
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

    @Test
    void signup_중복_관심지역은_제거되어_저장된다() {
        SignupCommand dupRegion = SignupCommand.of(
            "test@example.com", "Password1!", "홍길동", "01012345678", "여행자",
            List.of("유럽", "유럽", "동남아")
        );
        stubNoDuplicate();
        given(passwordEncoder.encode(anyString())).willReturn("encoded");
        given(userRepository.save(any())).willReturn(buildSavedUser());

        userService.signup(dupRegion);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getInterestRegions()).hasSize(2);
    }

    // ── signup history ───────────────────────────────────────────────────────

    @Test
    void signup_성공시_SIGNUP_이력이_저장된다() {
        stubNoDuplicate();
        given(passwordEncoder.encode(anyString())).willReturn("encoded");
        given(userRepository.save(any())).willReturn(buildSavedUser());

        userService.signup(command);

        ArgumentCaptor<UserHistory> captor = ArgumentCaptor.forClass(UserHistory.class);
        verify(userHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(UserAction.SIGNUP);
    }

    // ── withdraw ────────────────────────────────────────────────────────────

    @Test
    void withdraw_WITHDRAWAL_이력이_1건_저장된다() {
        User user = buildSavedUser();

        userService.withdraw(user);

        ArgumentCaptor<UserHistory> captor = ArgumentCaptor.forClass(UserHistory.class);
        verify(userHistoryRepository).save(captor.capture());

        assertThat(captor.getValue().getAction()).isEqualTo(UserAction.WITHDRAWAL);
    }

    // ── isEmailAvailable ────────────────────────────────────────────────────

    @Test
    void isEmailAvailable_미사용_이메일이면_true() {
        given(userRepository.existsByEmail("new@example.com")).willReturn(false);
        assertThat(userService.isEmailAvailable("new@example.com")).isTrue();
    }

    @Test
    void isEmailAvailable_사용중_이메일이면_false() {
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
    void isPhoneAvailable_사용중_번호면_false() {
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
    void isNicknameAvailable_사용중_닉네임이면_false() {
        given(userRepository.existsByNicknameIgnoreCase("여행자")).willReturn(true);
        assertThat(userService.isNicknameAvailable("여행자")).isFalse();
    }

    // ── findUsers ───────────────────────────────────────────────────────────

    @Test
    void findUsers_목록반환() {
        User user1 = buildSavedUser();
        User user2 = User.builder()
            .email("other@example.com").password("encoded").name("김철수")
            .phone("01087654321").nickname("김닉").build();
        ReflectionTestUtils.setField(user2, "id", 2L);
        ReflectionTestUtils.setField(user2, "role", UserRole.USER);
        given(userRepository.findAll(any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(user1, user2), PageRequest.of(0, 20), 2));

        Page<UserSummaryResponse> result = userService.findUsers(1, 20);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("test@example.com");
        assertThat(result.getContent().get(1).getEmail()).isEqualTo("other@example.com");
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }

    @Test
    void findUsers_빈페이지이면_빈리스트반환() {
        given(userRepository.findAll(any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        Page<UserSummaryResponse> result = userService.findUsers(1, 20);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void findUsers_DTO_필드_매핑_확인() {
        User user = buildSavedUser();
        ReflectionTestUtils.setField(user, "status", UserStatus.ACTIVE);
        given(userRepository.findAll(any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1));

        UserSummaryResponse dto = userService.findUsers(1, 20).getContent().get(0);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getEmail()).isEqualTo("test@example.com");
        assertThat(dto.getName()).isEqualTo("홍길동");
        assertThat(dto.getPhone()).isEqualTo("01012345678");
        assertThat(dto.getNickname()).isEqualTo("여행자");
        assertThat(dto.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(dto.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    void findUsers_page_1부터_시작하여_0indexed로_변환() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        given(userRepository.findAll(any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 10));

        userService.findUsers(2, 5);

        verify(userRepository).findAll(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
    }
}
