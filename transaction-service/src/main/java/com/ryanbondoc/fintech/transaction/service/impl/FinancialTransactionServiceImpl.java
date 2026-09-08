package com.ryanbondoc.fintech.transaction.service.impl;

import com.ryanbondoc.fintech.transaction.dto.TransactionResponse;
import com.ryanbondoc.fintech.transaction.entity.FinancialTransaction;
import com.ryanbondoc.fintech.transaction.repository.FinancialTransactionRepository;
import com.ryanbondoc.fintech.transaction.service.FinancialTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialTransactionServiceImpl
        implements FinancialTransactionService {

    private final FinancialTransactionRepository transactionRepository;

    @Override
    public List<TransactionResponse> getTransactions(UUID accountId) {

        return transactionRepository
                .findByAccountIdOrderByTransactionDateDesc(accountId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TransactionResponse toResponse(FinancialTransaction transaction) {

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