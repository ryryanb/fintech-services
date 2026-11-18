package com.ryanbondoc.fintech.account.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PlaidCredentialConfigurationTest {

    private static final String KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    @Test
    void shouldAllowPlaidToBeEntirelyDisabled() {
        assertThatCode(() -> new PlaidCredentialConfiguration("", "", ""))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAllowCompletePlaidConfiguration() {
        assertThatCode(() -> new PlaidCredentialConfiguration("client-id", "secret", KEY))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectProviderCredentialsWithoutEncryptionKey() {
        assertThatThrownBy(() -> new PlaidCredentialConfiguration("client-id", "secret", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PLAID_TOKEN_ENCRYPTION_KEY");
    }

    @Test
    void shouldRejectPartiallyConfiguredProviderCredentials() {
        assertThatThrownBy(() -> new PlaidCredentialConfiguration("client-id", "", KEY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PLAID_CLIENT_ID");
    }

    @Test
    void shouldRejectEncryptionKeyWithoutProviderCredentials() {
        assertThatThrownBy(() -> new PlaidCredentialConfiguration("", "", KEY))
                .isInstanceOf(IllegalStateException.class);
    }
}
