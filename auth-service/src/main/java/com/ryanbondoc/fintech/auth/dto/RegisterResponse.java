package com.ryanbondoc.fintech.auth.dto;

import java.time.Instant;
import java.util.UUID;

import com.ryanbondoc.fintech.auth.enums.UserStatus;

public record RegisterResponse(
        UUID id,
        String email,
        UserStatus status,
        Instant createdAt
) {
}
