package com.ryanbondoc.fintech.transaction.service.impl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ryanbondoc.fintech.transaction.client.AccountServiceClient;
import com.ryanbondoc.fintech.transaction.dto.TransactionRequest;
import com.ryanbondoc.fintech.transaction.dto.TransactionResponse;
import com.ryanbondoc.fintech.transaction.entity.FinancialTransaction;
import com.ryanbondoc.fintech.transaction.entity.TransactionStatus;
import com.ryanbondoc.fintech.transaction.exception.AccountNotFoundException;
import com.ryanbondoc.fintech.transaction.repository.FinancialTransactionRepository;
import com.ryanbondoc.fintech.transaction.service.FinancialTransactionService;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
@Transactional
public class FinancialTransactionServiceImpl
implements FinancialTransactionService {


private final FinancialTransactionRepository transactionRepository;
private final AccountServiceClient accountServiceClient;

@Override
@Transactional(readOnly = true)
public List<TransactionResponse> getTransactions(UUID accountId) {

    return transactionRepository
            .findByAccountIdOrderByTransactionDateDesc(accountId)
            .stream()
            .map(this::toResponse)
            .toList();
}

@Override
public TransactionResponse createTransaction(
        TransactionRequest request) {

    UUID accountId = request.accountId();

    if (!accountServiceClient.accountExists(accountId)) {
        throw new AccountNotFoundException(accountId);
    }

    FinancialTransaction transaction =
            FinancialTransaction.builder()
                    .accountId(accountId)
                    .type(request.type())
                    .direction(request.direction())
                    .amount(request.amount())
                    .currency(request.currency())
                    .description(request.description())
                    .status(TransactionStatus.COMPLETED)
                    .transactionDate(
                            request.transactionDate() != null
                                    ? request.transactionDate()
                                    : OffsetDateTime.now()
                    )
                    .build();

    FinancialTransaction saved =
            transactionRepository.save(transaction);

    return toResponse(saved);
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
            transaction.getDescription(),
            transaction.getStatus(),
            transaction.getTransactionDate()
    );
}

    
  

}