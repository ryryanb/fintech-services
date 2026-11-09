package com.ryanbondoc.fintech.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaidExchangeRequest(
        @NotBlank @Size(max = 2048) String publicToken,
        @Size(max = 100) String institutionId,
        @Size(max = 150) String institutionName) {
}
