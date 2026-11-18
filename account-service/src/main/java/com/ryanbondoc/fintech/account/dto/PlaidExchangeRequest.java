package com.ryanbondoc.fintech.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaidExchangeRequest(
        @NotBlank @Size(max = 2048) String publicToken,
        @Size(max = 100) String institutionId,
        @Size(max = 150) String institutionName) {

    /**
     * Public tokens are short-lived credentials and must not appear in logs
     * if this request object is accidentally included in a diagnostic message.
     */
    @Override
    public String toString() {
        return "PlaidExchangeRequest[publicToken=REDACTED, institutionId="
                + institutionId + ", institutionName=" + institutionName + "]";
    }
}
