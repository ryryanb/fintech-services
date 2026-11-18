package com.ryanbondoc.fintech.account.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PlaidExchangeRequestTest {

    @Test
    void shouldRedactPublicTokenFromToString() {
        PlaidExchangeRequest request = new PlaidExchangeRequest(
                "public-sandbox-sensitive-token", "ins_123", "Sandbox Bank");

        assertThat(request.toString())
                .contains("REDACTED")
                .doesNotContain("public-sandbox-sensitive-token");
    }
}
