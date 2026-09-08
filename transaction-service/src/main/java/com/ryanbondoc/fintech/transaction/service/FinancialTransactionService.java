package com.ryanbondoc.fintech.transaction.service;

import java.util.List;
import java.util.UUID;

import com.ryanbondoc.fintech.transaction.dto.TransactionRequest;
import com.ryanbondoc.fintech.transaction.dto.TransactionResponse;

public interface FinancialTransactionService {

    List<TransactionResponse> getTransactions(UUID accountId);
    TransactionResponse createTransaction(TransactionRequest request);
}