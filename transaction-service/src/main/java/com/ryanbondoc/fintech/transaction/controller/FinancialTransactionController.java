package com.ryanbondoc.fintech.transaction.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ryanbondoc.fintech.transaction.dto.TransactionRequest;
import com.ryanbondoc.fintech.transaction.dto.TransactionResponse;
import com.ryanbondoc.fintech.transaction.service.FinancialTransactionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(
            @Valid @RequestBody TransactionRequest request) {

        return transactionService.createTransaction(request);
    }

    @GetMapping("/{transactionId}")
public TransactionResponse getTransaction(
        @PathVariable UUID transactionId) {

    return transactionService.getTransaction(transactionId);
}
}