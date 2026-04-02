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
