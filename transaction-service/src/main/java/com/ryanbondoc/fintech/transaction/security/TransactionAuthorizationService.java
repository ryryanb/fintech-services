package com.ryanbondoc.fintech.transaction.security;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import com.ryanbondoc.fintech.transaction.client.AccountOwnershipResponse;
import com.ryanbondoc.fintech.transaction.client.AccountServiceClient;
import com.ryanbondoc.fintech.transaction.entity.FinancialTransaction;

@Service
public class TransactionAuthorizationService {

    private final AccountServiceClient accountServiceClient;

    public TransactionAuthorizationService(
            AccountServiceClient accountServiceClient) {
        this.accountServiceClient = accountServiceClient;
    }

    /*
     * public UUID authenticatedCustomerId(
     * Authentication authentication) {
     * if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
     * throw new AccessDeniedException("Invalid authentication");
     * }
     * 
     * String customerId = jwtAuthentication.getToken()
     * .getClaimAsString("customerId");
     * 
     * if (customerId == null || customerId.isBlank()) {
     * throw new AccessDeniedException(
     * "Missing customerId claim");
     * }
     * 
     * try {
     * return UUID.fromString(customerId);
     * } catch (IllegalArgumentException exception) {
     * throw new AccessDeniedException(
     * "Invalid customerId claim",
     * exception);
     * }
     * }
     */

    public void authorizeTransactionAccess(
            FinancialTransaction transaction,
            Authentication authentication) {

        UUID authenticatedCustomerId = extractCustomerId(authentication);

        String bearerToken = extractBearerToken(authentication);

        UUID accountId = transaction.getAccountId();

        if (accountId == null) {
            throw new AccessDeniedException(
                    "Transaction has no associated account");
        }

        AccountOwnershipResponse account = accountServiceClient.getAccountOwnership(
                accountId,
                bearerToken);

        if (account == null || account.customerId() == null) {
            throw new AccessDeniedException(
                    "Unable to verify account ownership");
        }

        if (!authenticatedCustomerId.equals(account.customerId())) {
            throw new AccessDeniedException(
                    "Transaction does not belong to authenticated customer");
        }
    }

    private UUID extractCustomerId(
            Authentication authentication) {

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new AccessDeniedException("Invalid authentication");
        }

        String customerId = jwtAuthentication.getToken()
                .getClaimAsString("customerId");

        if (customerId == null || customerId.isBlank()) {
            throw new AccessDeniedException(
                    "Missing customerId claim");
        }

        try {
            return UUID.fromString(customerId);
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException(
                    "Invalid customerId claim",
                    exception);
        }
    }

    private String extractBearerToken(
            Authentication authentication) {

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new AccessDeniedException("Invalid authentication");
        }

        return "Bearer " + jwtAuthentication.getToken().getTokenValue();
    }

    public void authorizeAccountAccess(
            UUID accountId,
            Authentication authentication) {

        UUID authenticatedCustomerId = extractCustomerId(authentication);

        String bearerToken = extractBearerToken(authentication);

        AccountOwnershipResponse account = accountServiceClient.getAccountOwnership(
                accountId,
                bearerToken);

        if (account == null || account.customerId() == null) {
            throw new AccessDeniedException(
                    "Unable to verify account ownership");
        }

        if (!authenticatedCustomerId.equals(account.customerId())) {
            throw new AccessDeniedException(
                    "Account does not belong to authenticated customer");
        }
    }
}