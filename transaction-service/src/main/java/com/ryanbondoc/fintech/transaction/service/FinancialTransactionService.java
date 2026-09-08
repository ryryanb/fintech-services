package com.ryanbondoc.fintech.transaction.service;

import com.ryanbondoc.fintech.transaction.dto.TransactionResponse;

import java.util.List;
import java.util.UUID;

public interface FinancialTransactionService {

    List<TransactionResponse> getTransactions(UUID accountId);
}