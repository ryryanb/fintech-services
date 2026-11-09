package com.ryanbondoc.fintech.account.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Validates that Plaid provider credentials are configured as one complete,
 * protected set. Local environments may leave all Plaid values unset.
 */
@Component
public class PlaidCredentialConfiguration {

    public PlaidCredentialConfiguration(
            @Value("${plaid.client-id:}") String clientId,
            @Value("${plaid.secret:}") String secret,
            @Value("${plaid.token-encryption-key:}") String tokenEncryptionKey) {

        boolean clientIdConfigured = isConfigured(clientId);
        boolean secretConfigured = isConfigured(secret);
        boolean encryptionKeyConfigured = isConfigured(tokenEncryptionKey);

        if (!clientIdConfigured && !secretConfigured && !encryptionKeyConfigured) {
            return;
        }

        if (!clientIdConfigured || !secretConfigured || !encryptionKeyConfigured) {
            throw new IllegalStateException(
                    "Plaid configuration is incomplete. Set PLAID_CLIENT_ID, PLAID_SECRET, "
                            + "and PLAID_TOKEN_ENCRYPTION_KEY together, or leave all three unset.");
        }
    }

    private boolean isConfigured(String value) {
        return value != null && !value.isBlank();
    }
}
