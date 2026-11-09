package com.ryanbondoc.fintech.account.dto;

import java.time.Instant;
import java.util.UUID;

public record PlaidConnectionResponse(
        UUID id,
        String provider,
        String institutionId,
        String institutionName,
        String status,
        Instant connectedAt) {
}
