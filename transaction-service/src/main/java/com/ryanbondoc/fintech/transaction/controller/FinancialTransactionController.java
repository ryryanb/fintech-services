package com.ryanbondoc.fintech.transaction.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class FinancialTransactionController {

    private final FinancialTransactionService transactionService;

    @Operation(summary = "Get transactions", description = "Get transactions of a specific account of the authenticated customer.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public List<TransactionResponse> getTransactions(
            @RequestParam UUID accountId) {

        return transactionService.getTransactions(accountId);
    }

    @Operation(summary = "Create a financial transaction", description = "Creates a transaction for an account owned by the authenticated customer.", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(
            @Valid @RequestBody TransactionRequest request,
            Authentication authentication) {

        return transactionService.createTransaction(request, authentication);
    }

    @Operation(summary = "Get transaction by ID", description = """
            Retrieves a financial transaction.

            Access is restricted to the customer who owns
            the account associated with the transaction.
            """, security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Authenticated customer does not own the account"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @PathVariable UUID transactionId,
            Authentication authentication) {

        TransactionResponse response = transactionService.getTransaction(
                transactionId,
                authentication);

        return ResponseEntity.ok(response);
    }
}