package com.irerin.travelan.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UserTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
        Instant.parse("2026-04-01T12:00:00Z"), ZoneId.of("Asia/Seoul")
    );

    private User buildUser() {
        User user = User.builder()
            .email("test@example.com")
            .password("encoded")
            .name("홍길동")
            .phone("01012345678")
            .nickname("여행자")
            .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    @Test
    void withdraw_상태가_WITHDRAWN으로_변경된다() {
        User user = buildUser();

        user.withdraw(FIXED_CLOCK);

        assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
    }

    @Test
    void withdraw_이메일이_익명화된다() {
        User user = buildUser();

        user.withdraw(FIXED_CLOCK);

        assertThat(user.getEmail()).isEqualTo("withdrawn_1@deleted");
    }

    @Test
    void withdraw_전화번호가_익명화된다() {
        User user = buildUser();

        user.withdraw(FIXED_CLOCK);

        assertThat(user.getPhone()).isEqualTo("del_1");
    }

    @Test
    void withdraw_닉네임이_익명화된다() {
        User user = buildUser();

        user.withdraw(FIXED_CLOCK);

        assertThat(user.getNickname()).isEqualTo("탈퇴1");
    }

    @Test
    void withdraw_대용량_ID에서_닉네임이_10자_이하여야_한다() {
        User user = buildUser();
        ReflectionTestUtils.setField(user, "id", 999_999_999L);

        user.withdraw(FIXED_CLOCK);

        assertThat(user.getNickname().length()).isLessThanOrEqualTo(10);
    }

    @Test
    void of_정적팩토리로_User_생성() {
        User user = User.of("user@example.com", "encodedPw", "홍길동", "01011112222", "트레블러");

        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(user.getPassword()).isEqualTo("encodedPw");
        assertThat(user.getName()).isEqualTo("홍길동");
        assertThat(user.getPhone()).isEqualTo("01011112222");
        assertThat(user.getNickname()).isEqualTo("트레블러");
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void withdraw_원본이메일이_보존된다() {
        User user = buildUser();

        user.withdraw(FIXED_CLOCK);

        assertThat(user.getOriginalEmail()).isEqualTo("test@example.com");
    }

    @Test
    void withdraw_탈퇴시각이_고정시간으로_설정된다() {
        User user = buildUser();

        user.withdraw(FIXED_CLOCK);

        assertThat(user.getWithdrawnAt()).isEqualTo(LocalDateTime.now(FIXED_CLOCK));
    }
}
