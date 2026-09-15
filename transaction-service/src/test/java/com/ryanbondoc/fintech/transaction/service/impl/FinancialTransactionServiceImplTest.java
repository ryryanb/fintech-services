package com.ryanbondoc.fintech.transaction.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.ryanbondoc.fintech.transaction.client.AccountServiceClient;
import com.ryanbondoc.fintech.transaction.dto.TransactionRequest;
import com.ryanbondoc.fintech.transaction.dto.TransactionResponse;
import com.ryanbondoc.fintech.transaction.entity.FinancialTransaction;
import com.ryanbondoc.fintech.transaction.entity.TransactionCategory;
import com.ryanbondoc.fintech.transaction.entity.TransactionDirection;
import com.ryanbondoc.fintech.transaction.entity.TransactionStatus;
import com.ryanbondoc.fintech.transaction.entity.TransactionType;
import com.ryanbondoc.fintech.transaction.exception.AccountNotFoundException;
import com.ryanbondoc.fintech.transaction.exception.TransactionNotFoundException;
import com.ryanbondoc.fintech.transaction.repository.FinancialTransactionRepository;
import com.ryanbondoc.fintech.transaction.security.TransactionAuthorizationService;

@ExtendWith(MockitoExtension.class)
class FinancialTransactionServiceImplTest {

        @Mock
        private FinancialTransactionRepository transactionRepository;

        @Mock
        private AccountServiceClient accountServiceClient;

        @Mock
        private TransactionAuthorizationService transactionAuthorizationService;

        /*
         * @Mock
         * private Authentication authentication;
         */

        @Mock
        private JwtAuthenticationToken authentication;

        private static final String BEARER_TOKEN = "Bearer test-token";

        @InjectMocks
        private FinancialTransactionServiceImpl transactionService;

        @Test
        void shouldReturnTransactionsForAccount() {
                UUID accountId = UUID.randomUUID();

                FinancialTransaction transaction = FinancialTransaction.builder()
                                .id(UUID.randomUUID())
                                .accountId(accountId)
                                .type(TransactionType.PAYMENT)
                                .direction(TransactionDirection.DEBIT)
                                .amount(new BigDecimal("1250.00"))
                                .currency("PHP")
                                .description("Utility payment")
                                .status(TransactionStatus.COMPLETED)
                                .transactionDate(OffsetDateTime.now())
                                .build();

                when(transactionRepository
                                .findByAccountIdOrderByTransactionDateDesc(accountId))
                                .thenReturn(List.of(transaction));

                List<TransactionResponse> result = transactionService.getTransactions(accountId);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).accountId()).isEqualTo(accountId);
                assertThat(result.get(0).amount())
                                .isEqualByComparingTo("1250.00");
        }

        @Test
        void shouldReturnEmptyListWhenAccountHasNoTransactions() {
                UUID accountId = UUID.randomUUID();

                when(transactionRepository
                                .findByAccountIdOrderByTransactionDateDesc(accountId))
                                .thenReturn(List.of());

                List<TransactionResponse> result = transactionService.getTransactions(accountId);

                assertThat(result).isEmpty();
        }

        @Test
        void shouldCreateTransaction() {

                UUID accountId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();

                JwtAuthenticationToken authentication = createAuthentication();

                String bearerToken = "Bearer " + authentication.getToken().getTokenValue();

                when(accountServiceClient.accountExists(
                                eq(accountId),
                                eq(bearerToken)))
                                .thenReturn(true);

                OffsetDateTime transactionDate = OffsetDateTime.parse("2026-09-08T14:30:00Z");

                TransactionRequest request = new TransactionRequest(
                                accountId,
                                TransactionType.PAYMENT,
                                TransactionDirection.DEBIT,
                                new BigDecimal("1250.00"),
                                "PHP",
                                "Utility payment",
                                "merchant",
                                TransactionCategory.FEES,
                                transactionDate);

                FinancialTransaction savedTransaction = FinancialTransaction.builder()
                                .id(transactionId)
                                .accountId(accountId)
                                .type(TransactionType.PAYMENT)
                                .direction(TransactionDirection.DEBIT)
                                .amount(new BigDecimal("1250.00"))
                                .currency("PHP")
                                .description("Utility payment")
                                .status(TransactionStatus.COMPLETED)
                                .transactionDate(transactionDate)
                                .build();

                when(transactionRepository.save(any(FinancialTransaction.class)))
                                .thenReturn(savedTransaction);

                TransactionResponse result = transactionService.createTransaction(request, authentication);

                assertThat(result.id()).isEqualTo(transactionId);
                assertThat(result.accountId()).isEqualTo(accountId);
                assertThat(result.type()).isEqualTo(TransactionType.PAYMENT);
                assertThat(result.direction()).isEqualTo(TransactionDirection.DEBIT);
                assertThat(result.amount())
                                .isEqualByComparingTo("1250.00");
                assertThat(result.currency()).isEqualTo("PHP");
                assertThat(result.description()).isEqualTo("Utility payment");
                assertThat(result.status())
                                .isEqualTo(TransactionStatus.COMPLETED);
                assertThat(result.transactionDate())
                                .isEqualTo(transactionDate);
        }

        @Test
        void shouldUseCurrentTimeWhenTransactionDateIsNotProvided() {

                UUID accountId = UUID.randomUUID();

                JwtAuthenticationToken authentication = createAuthentication();

                String bearerToken = "Bearer " + authentication.getToken().getTokenValue();

                when(accountServiceClient.accountExists(
                                eq(accountId),
                                eq(bearerToken)))
                                .thenReturn(true);

                TransactionRequest request = new TransactionRequest(
                                accountId,
                                TransactionType.DEPOSIT,
                                TransactionDirection.CREDIT,
                                new BigDecimal("5000.00"),
                                "PHP",
                                "Cash deposit",
                                "merchant",
                                TransactionCategory.FEES,
                                null);

                when(transactionRepository.save(any(FinancialTransaction.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                OffsetDateTime before = OffsetDateTime.now();

                TransactionResponse result = transactionService.createTransaction(request, authentication);

                OffsetDateTime after = OffsetDateTime.now();

                assertThat(result.transactionDate())
                                .isBetween(before, after);

                assertThat(result.status())
                                .isEqualTo(TransactionStatus.COMPLETED);
        }

        @Test
        void shouldPersistTransactionWithCompletedStatus() {

                UUID accountId = UUID.randomUUID();

                JwtAuthenticationToken authentication = createAuthentication();

                String bearerToken = "Bearer " + authentication.getToken().getTokenValue();

                when(accountServiceClient.accountExists(
                                eq(accountId),
                                eq(bearerToken)))
                                .thenReturn(true);

                TransactionRequest request = new TransactionRequest(
                                accountId,
                                TransactionType.TRANSFER,
                                TransactionDirection.DEBIT,
                                new BigDecimal("1500.00"),
                                "PHP",
                                "Transfer to savings",
                                "merchant",
                                TransactionCategory.FEES,
                                null);

                when(transactionRepository.save(any(FinancialTransaction.class)))
                                .thenAnswer(invocation -> {
                                        FinancialTransaction transaction = invocation.getArgument(0);

                                        transaction.setId(UUID.randomUUID());
                                        return transaction;
                                });

                transactionService.createTransaction(request, authentication);

                ArgumentCaptor<FinancialTransaction> captor = ArgumentCaptor.forClass(FinancialTransaction.class);

                verify(transactionRepository).save(captor.capture());

                FinancialTransaction persisted = captor.getValue();

                assertThat(persisted.getAccountId()).isEqualTo(accountId);
                assertThat(persisted.getType())
                                .isEqualTo(TransactionType.TRANSFER);
                assertThat(persisted.getDirection())
                                .isEqualTo(TransactionDirection.DEBIT);
                assertThat(persisted.getAmount())
                                .isEqualByComparingTo("1500.00");
                assertThat(persisted.getCurrency()).isEqualTo("PHP");
                assertThat(persisted.getStatus())
                                .isEqualTo(TransactionStatus.COMPLETED);
        }

        @Test
        void shouldAssociateTransactionWithExistingAccount() {

                UUID accountId = UUID.randomUUID();
                UUID transactionId = UUID.randomUUID();

                OffsetDateTime transactionDate = OffsetDateTime.parse("2026-09-08T14:30:00Z");

                TransactionRequest request = new TransactionRequest(
                                accountId,
                                TransactionType.PAYMENT,
                                TransactionDirection.DEBIT,
                                new BigDecimal("1250.00"),
                                "PHP",
                                "Utility payment",
                                "merchant",
                                TransactionCategory.FEES,
                                transactionDate);

                FinancialTransaction savedTransaction = FinancialTransaction.builder()
                                .id(transactionId)
                                .accountId(accountId)
                                .type(TransactionType.PAYMENT)
                                .direction(TransactionDirection.DEBIT)
                                .amount(new BigDecimal("1250.00"))
                                .currency("PHP")
                                .description("Utility payment")
                                .status(TransactionStatus.COMPLETED)
                                .transactionDate(transactionDate)
                                .build();

                JwtAuthenticationToken authentication = createAuthentication();

                String bearerToken = "Bearer " + authentication.getToken().getTokenValue();

                when(accountServiceClient.accountExists(
                                eq(accountId),
                                eq(bearerToken)))
                                .thenReturn(true);

                when(transactionRepository.save(any(FinancialTransaction.class)))
                                .thenReturn(savedTransaction);

                TransactionResponse result = transactionService.createTransaction(request, authentication);

                assertThat(result.accountId())
                                .isEqualTo(accountId);

                verify(accountServiceClient)
                                .accountExists(accountId, bearerToken);

                verify(transactionRepository)
                                .save(any(FinancialTransaction.class));

        }

        @Test
        void shouldRejectTransactionWhenAccountDoesNotExist() {

                UUID accountId = UUID.randomUUID();

                TransactionRequest request = new TransactionRequest(
                                accountId,
                                TransactionType.PAYMENT,
                                TransactionDirection.DEBIT,
                                new BigDecimal("1250.00"),
                                "PHP",
                                "Utility payment",
                                "merchant",
                                TransactionCategory.FEES,
                                null);

                JwtAuthenticationToken authentication = createAuthentication();

                String bearerToken = "Bearer " + authentication.getToken().getTokenValue();

                when(accountServiceClient.accountExists(
                                eq(accountId),
                                eq(bearerToken)))
                                .thenReturn(false);

                assertThatThrownBy(() -> transactionService.createTransaction(
                                request,
                                authentication))
                                .isInstanceOf(AccountNotFoundException.class);

                verify(accountServiceClient)
                                .accountExists(accountId, bearerToken);

                verify(transactionRepository, never())
                                .save(any(FinancialTransaction.class));

        }

        @Test
        void shouldGetTransactionById() {

                UUID transactionId = UUID.randomUUID();
                UUID accountId = UUID.randomUUID();

                OffsetDateTime transactionDate = OffsetDateTime.parse("2026-09-09T14:30:00Z");

                FinancialTransaction transaction = FinancialTransaction.builder()
                                .id(transactionId)
                                .accountId(accountId)
                                .type(TransactionType.PAYMENT)
                                .direction(TransactionDirection.DEBIT)
                                .amount(new BigDecimal("1250.00"))
                                .currency("PHP")
                                .merchant("SM Supermarket")
                                .category(TransactionCategory.GROCERIES)
                                .description("Weekly groceries")
                                .status(TransactionStatus.COMPLETED)
                                .transactionDate(transactionDate)
                                .build();

                when(transactionRepository.findById(transactionId))
                                .thenReturn(Optional.of(transaction));

                Authentication authentication = org.mockito.Mockito.mock(Authentication.class);

                doNothing().when(transactionAuthorizationService)
                                .authorizeTransactionAccess(
                                                eq(transaction),
                                                eq(authentication));
                TransactionResponse result = transactionService.getTransaction(transactionId, authentication);

                assertThat(result.id()).isEqualTo(transactionId);
                assertThat(result.accountId()).isEqualTo(accountId);
                assertThat(result.merchant()).isEqualTo("SM Supermarket");
                assertThat(result.amount())
                                .isEqualByComparingTo("1250.00");
                assertThat(result.transactionDate())
                                .isEqualTo(transactionDate);
                assertThat(result.category())
                                .isEqualTo(TransactionCategory.GROCERIES);

                verify(transactionRepository).findById(transactionId);
                verify(transactionAuthorizationService)
                                .authorizeTransactionAccess(
                                                eq(transaction),
                                                eq(authentication));
        }

        @Test
        void shouldThrowWhenTransactionDoesNotExist() {

                UUID transactionId = UUID.randomUUID();

                when(transactionRepository.findById(transactionId))
                                .thenReturn(Optional.empty());

                Authentication authentication = org.mockito.Mockito.mock(Authentication.class);

                assertThatThrownBy(() -> transactionService.getTransaction(transactionId, authentication))
                                .isInstanceOf(TransactionNotFoundException.class)
                                .hasMessage("Transaction not found: " + transactionId);

                verify(transactionRepository).findById(transactionId);
        }

        private JwtAuthenticationToken createAuthentication() {

                Jwt jwt = Jwt.withTokenValue("test-token")
                                .header("alg", "RS256")
                                .claim("sub", UUID.randomUUID().toString())
                                .claim("customerId", UUID.randomUUID().toString())
                                .claim("roles", List.of("USER"))
                                .issuedAt(java.time.Instant.now())
                                .expiresAt(java.time.Instant.now().plusSeconds(3600))
                                .build();

                return new JwtAuthenticationToken(jwt);
        }

}
