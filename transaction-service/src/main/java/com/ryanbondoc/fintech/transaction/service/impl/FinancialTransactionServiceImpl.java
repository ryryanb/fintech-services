package com.ryanbondoc.fintech.transaction.service.impl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ryanbondoc.fintech.transaction.client.AccountServiceClient;
import com.ryanbondoc.fintech.transaction.dto.TransactionRequest;
import com.ryanbondoc.fintech.transaction.dto.TransactionResponse;
import com.ryanbondoc.fintech.transaction.entity.FinancialTransaction;
import com.ryanbondoc.fintech.transaction.entity.TransactionCategory;
import com.ryanbondoc.fintech.transaction.entity.TransactionStatus;
import com.ryanbondoc.fintech.transaction.exception.AccountNotFoundException;
import com.ryanbondoc.fintech.transaction.exception.TransactionNotFoundException;
import com.ryanbondoc.fintech.transaction.repository.FinancialTransactionRepository;
import com.ryanbondoc.fintech.transaction.repository.SpendingCategoryRepository;
import com.ryanbondoc.fintech.transaction.security.TransactionAuthorizationService;
import com.ryanbondoc.fintech.transaction.service.FinancialTransactionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class FinancialTransactionServiceImpl
                implements FinancialTransactionService {

        private final FinancialTransactionRepository transactionRepository;
        private final AccountServiceClient accountServiceClient;
        private final TransactionAuthorizationService transactionAuthorizationService;
        private final SpendingCategoryRepository spendingCategoryRepository;

        @Override
        @Transactional(readOnly = true)
        public List<TransactionResponse> getTransactions(UUID accountId, Authentication authentication) {

                transactionAuthorizationService.authorizeAccountAccess(accountId, authentication);

                return transactionRepository
                                .findByAccountIdOrderByTransactionDateDesc(accountId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        @Override
        public TransactionResponse createTransaction(
                        TransactionRequest request, Authentication authentication) {

                UUID accountId = request.accountId();

                String bearerToken = extractBearerToken(authentication);

                if (!accountServiceClient.accountExists(accountId, bearerToken)) {
                        throw new AccountNotFoundException(accountId);
                }

                transactionAuthorizationService.authorizeAccountAccess(
                                accountId,
                                authentication);

                String category = resolveCategory(request, authentication);

                FinancialTransaction transaction = FinancialTransaction.builder()
                                .accountId(accountId)
                                .type(request.type())
                                .direction(request.direction())
                                .amount(request.amount())
                                .currency(request.currency())
                                .merchant(request.merchant())
                                .category(category)
                                .description(request.description())
                                .status(TransactionStatus.COMPLETED)
                                .transactionDate(
                                                request.transactionDate() != null
                                                                ? request.transactionDate()
                                                                : OffsetDateTime.now())
                                .build();

                FinancialTransaction saved = transactionRepository.save(transaction);

                return toResponse(saved);
        }


        private String resolveCategory(TransactionRequest request, Authentication authentication) {
                if (request.category() == null || request.category().isBlank()) {
                        return TransactionCategorizer.categorize(
                                        request.merchant(),
                                        request.description(),
                                        request.type(),
                                        request.direction()).name();
                }

                String category = request.category().trim().replaceAll("\\s+", " ");
                try {
                        return TransactionCategory.valueOf(category.toUpperCase(java.util.Locale.ROOT)).name();
                } catch (IllegalArgumentException ignored) {
                        // Custom categories are scoped to the authenticated customer.
                }

                if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
                        throw new AccessDeniedException("Invalid authentication");
                }
                String customerIdValue = jwtAuthentication.getToken().getClaimAsString("customerId");
                if (customerIdValue == null || customerIdValue.isBlank()) {
                        throw new AccessDeniedException("Missing customerId claim");
                }
                UUID customerId;
                try {
                        customerId = UUID.fromString(customerIdValue);
                } catch (IllegalArgumentException exception) {
                        throw new AccessDeniedException("Invalid customerId claim", exception);
                }
                if (!spendingCategoryRepository.existsByCustomerIdAndNameIgnoreCase(customerId, category)) {
                        throw new IllegalArgumentException("Choose a built-in category or create a custom category first");
                }
                return category;
        }

        private TransactionResponse toResponse(
                        FinancialTransaction transaction) {

                return new TransactionResponse(
                                transaction.getId(),
                                transaction.getAccountId(),
                                transaction.getType(),
                                transaction.getDirection(),
                                transaction.getAmount(),
                                transaction.getCurrency(),
                                transaction.getMerchant(),
                                transaction.getCategory(),
                                transaction.getDescription(),
                                transaction.getStatus(),

                                transaction.getTransactionDate());
        }

        @Override
        @Transactional(readOnly = true)
        public TransactionResponse getTransaction(UUID transactionId, Authentication authentication) {

                FinancialTransaction transaction = transactionRepository.findById(transactionId)
                                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

                transactionAuthorizationService.authorizeTransactionAccess(
                                transaction,
                                authentication);

                return toResponse(transaction);
        }

        private String extractBearerToken(
                        Authentication authentication) {

                if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
                        throw new AccessDeniedException("Invalid authentication");
                }

                return "Bearer " + jwtAuthentication.getToken().getTokenValue();
        }

}