package com.ryanbondoc.fintech.auth.dto;

public record ErrorResponse(
        String code,
        String message
) {
}
