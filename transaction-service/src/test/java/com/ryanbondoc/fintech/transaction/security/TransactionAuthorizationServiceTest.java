package com.ryanbondoc.fintech.transaction.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.ryanbondoc.fintech.transaction.client.AccountOwnershipResponse;
import com.ryanbondoc.fintech.transaction.client.AccountServiceClient;
import com.ryanbondoc.fintech.transaction.entity.FinancialTransaction;
import com.ryanbondoc.fintech.transaction.entity.TransactionDirection;
import com.ryanbondoc.fintech.transaction.entity.TransactionStatus;
import com.ryanbondoc.fintech.transaction.entity.TransactionType;

class TransactionAuthorizationServiceTest {

    private AccountServiceClient accountServiceClient;
    private TransactionAuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        accountServiceClient = mock(AccountServiceClient.class);
        authorizationService = new TransactionAuthorizationService(accountServiceClient);
    }

    @Test
    void allowsAccessWhenAccountBelongsToAuthenticatedCustomer() {
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        JwtAuthenticationToken authentication = authentication(customerId, "test-token");

        when(accountServiceClient.getAccountOwnership(accountId, "Bearer test-token"))
                .thenReturn(new AccountOwnershipResponse(accountId, customerId));

        assertDoesNotThrow(() -> authorizationService.authorizeAccountAccess(accountId, authentication));
        verify(accountServiceClient).getAccountOwnership(accountId, "Bearer test-token");
    }

    @Test
    void deniesAccountAccessWhenAccountBelongsToAnotherCustomer() {
        UUID accountId = UUID.randomUUID();
        JwtAuthenticationToken authentication = authentication(UUID.randomUUID(), "test-token");

        when(accountServiceClient.getAccountOwnership(accountId, "Bearer test-token"))
                .thenReturn(new AccountOwnershipResponse(accountId, UUID.randomUUID()));

        assertThatThrownBy(() -> authorizationService.authorizeAccountAccess(accountId, authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Account does not belong to authenticated customer");
    }

    @Test
    void deniesAccountAccessWhenOwnershipCannotBeVerified() {
        UUID accountId = UUID.randomUUID();
        JwtAuthenticationToken authentication = authentication(UUID.randomUUID(), "test-token");

        when(accountServiceClient.getAccountOwnership(accountId, "Bearer test-token"))
                .thenReturn(null);

        assertThatThrownBy(() -> authorizationService.authorizeAccountAccess(accountId, authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Unable to verify account ownership");
    }

    @Test
    void deniesAccountAccessWhenAuthenticationIsNotJwtBased() {
        UUID accountId = UUID.randomUUID();
        Authentication authentication = mock(Authentication.class);

        assertThatThrownBy(() -> authorizationService.authorizeAccountAccess(accountId, authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Invalid authentication");
    }

    @Test
    void deniesTransactionAccessWhenItsAccountBelongsToAnotherCustomer() {
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        FinancialTransaction transaction = FinancialTransaction.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .type(TransactionType.PAYMENT)
                .direction(TransactionDirection.DEBIT)
                .amount(new BigDecimal("100.00"))
                .currency("PHP")
                .status(TransactionStatus.COMPLETED)
                .transactionDate(OffsetDateTime.now())
                .build();
        JwtAuthenticationToken authentication = authentication(customerId, "test-token");

        when(accountServiceClient.getAccountOwnership(accountId, "Bearer test-token"))
                .thenReturn(new AccountOwnershipResponse(accountId, UUID.randomUUID()));

        assertThatThrownBy(() -> authorizationService.authorizeTransactionAccess(transaction, authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Transaction does not belong to authenticated customer");
    }

    @Test
    void deniesTransactionAccessWhenTransactionHasNoAccount() {
        FinancialTransaction transaction = FinancialTransaction.builder()
                .id(UUID.randomUUID())
                .type(TransactionType.PAYMENT)
                .direction(TransactionDirection.DEBIT)
                .amount(new BigDecimal("100.00"))
                .currency("PHP")
                .status(TransactionStatus.COMPLETED)
                .transactionDate(OffsetDateTime.now())
                .build();

        assertThatThrownBy(() -> authorizationService.authorizeTransactionAccess(
                transaction, authentication(UUID.randomUUID(), "test-token")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Transaction has no associated account");
    }

    private JwtAuthenticationToken authentication(UUID customerId, String tokenValue) {
        Jwt jwt = Jwt.withTokenValue(tokenValue)
                .header("alg", "RS256")
                .subject(UUID.randomUUID().toString())
                .claim("customerId", customerId.toString())
                .claim("roles", List.of("USER"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        return new JwtAuthenticationToken(jwt);
    }
}
