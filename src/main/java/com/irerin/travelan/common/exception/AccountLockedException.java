package com.irerin.travelan.common.exception;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class AccountLockedException extends RuntimeException {
    private final LocalDateTime lockedUntil;

    public AccountLockedException(LocalDateTime lockedUntil) {
        super("계정이 잠금되었습니다");
        this.lockedUntil = lockedUntil;
    }
}
