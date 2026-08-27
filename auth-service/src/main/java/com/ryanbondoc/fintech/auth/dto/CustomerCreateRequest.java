package com.ryanbondoc.fintech.auth.dto;

import java.util.UUID;

public record CustomerCreateRequest(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String status,
        String customerNumber
) {
}