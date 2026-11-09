package com.ryanbondoc.fintech.account.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import org.junit.jupiter.api.Test;

class PlaidTokenCipherTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void shouldEncryptAndDecryptAccessToken() {
        PlaidTokenCipher cipher = new PlaidTokenCipher(KEY);
        String accessToken = "access-sandbox-test-token";

        String encrypted = cipher.encrypt(accessToken);

        assertThat(encrypted).isNotEqualTo(accessToken);
        assertThat(cipher.decrypt(encrypted)).isEqualTo(accessToken);
    }

    @Test
    void shouldUseDifferentInitializationVectorForEachEncryption() {
        PlaidTokenCipher cipher = new PlaidTokenCipher(KEY);

        String first = cipher.encrypt("same-token");
        String second = cipher.encrypt("same-token");

        assertThat(first).isNotEqualTo(second);
        assertThat(cipher.decrypt(first)).isEqualTo("same-token");
        assertThat(cipher.decrypt(second)).isEqualTo("same-token");
    }

    @Test
    void shouldRejectEncryptionWhenKeyIsNotConfigured() {
        PlaidTokenCipher cipher = new PlaidTokenCipher("");

        assertThatThrownBy(() -> cipher.encrypt("access-token"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PLAID_TOKEN_ENCRYPTION_KEY");
    }

    @Test
    void shouldRejectInvalidEncryptionKeyLength() {
        String invalidKey = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> new PlaidTokenCipher(invalidKey))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 bytes");
    }
}
