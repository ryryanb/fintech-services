package com.ryanbondoc.fintech.transaction.controller;

import com.ryanbondoc.fintech.transaction.dto.TransactionResponse;
import com.ryanbondoc.fintech.transaction.service.FinancialTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class FinancialTransactionController {

    private final FinancialTransactionService transactionService;

    @GetMapping
    public List<TransactionResponse> getTransactions(
            @RequestParam UUID accountId) {

        return transactionService.getTransactions(accountId);
    }
}