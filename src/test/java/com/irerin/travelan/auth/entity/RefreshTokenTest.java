package com.irerin.travelan.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RefreshTokenTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private Clock clockAt(String instant) {
        return Clock.fixed(Instant.parse(instant), ZONE);
    }

    private RefreshToken buildToken(LocalDateTime expiresAt) {
        RefreshToken token = RefreshToken.of(null, "test-token", expiresAt);
        ReflectionTestUtils.setField(token, "id", 1L);
        return token;
    }

    @Test
    void isExpired_만료전이면_false() {
        Clock clock = clockAt("2026-04-01T12:00:00Z");
        RefreshToken token = buildToken(LocalDateTime.of(2026, 4, 2, 0, 0));

        assertThat(token.isExpired(clock)).isFalse();
    }

    @Test
    void isExpired_만료후이면_true() {
        Clock clock = clockAt("2026-04-03T12:00:00Z");
        RefreshToken token = buildToken(LocalDateTime.of(2026, 4, 2, 0, 0));

        assertThat(token.isExpired(clock)).isTrue();
    }

    @Test
    void isUsable_유효하고_revoke안됨이면_true() {
        Clock clock = clockAt("2026-04-01T12:00:00Z");
        RefreshToken token = buildToken(LocalDateTime.of(2026, 4, 2, 0, 0));

        assertThat(token.isUsable(clock)).isTrue();
    }

    @Test
    void isUsable_revoke되면_false() {
        Clock clock = clockAt("2026-04-01T12:00:00Z");
        RefreshToken token = buildToken(LocalDateTime.of(2026, 4, 2, 0, 0));

        token.revoke();

        assertThat(token.isUsable(clock)).isFalse();
    }

    @Test
    void isUsable_만료되면_false() {
        Clock clock = clockAt("2026-04-03T12:00:00Z");
        RefreshToken token = buildToken(LocalDateTime.of(2026, 4, 2, 0, 0));

        assertThat(token.isUsable(clock)).isFalse();
    }
}
