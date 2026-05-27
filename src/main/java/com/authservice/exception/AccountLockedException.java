package com.authservice.exception;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class AccountLockedException extends AuthenticationException {
    private final LocalDateTime lockedUntil;

    public AccountLockedException(LocalDateTime lockedUntil) {
        super("Account temporarily locked due to multiple failed login attempts", "AUTH_006");
        this.lockedUntil = lockedUntil;
    }
}
