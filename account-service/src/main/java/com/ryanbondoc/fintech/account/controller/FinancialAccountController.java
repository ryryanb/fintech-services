package com.ryanbondoc.fintech.account.controller;

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
import org.springframework.web.bind.annotation.RestController;

import com.ryanbondoc.fintech.account.dto.AccountBalanceResponse;
import com.ryanbondoc.fintech.account.dto.CreateFinancialAccountRequest;
import com.ryanbondoc.fintech.account.dto.FinancialAccountResponse;
import com.ryanbondoc.fintech.account.security.AccountAuthorizationService;
import com.ryanbondoc.fintech.account.service.FinancialAccountService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class FinancialAccountController {

    private final FinancialAccountService financialAccountService;
    private final AccountAuthorizationService accountAuthorizationService;

    @Operation(summary = "Create an account", description = "Creates an account for the authenticated customer.", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    public ResponseEntity<FinancialAccountResponse> createAccount(
            @Valid @RequestBody CreateFinancialAccountRequest request,
            Authentication authentication) {

        accountAuthorizationService.requireCustomerAccess(authentication, request.customerId());

        FinancialAccountResponse response = financialAccountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Retrieve accounts", description = "Retrieve only the financial accounts belonging to the authenticated customer. Customer identity is derived from the validated bearer token.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<List<FinancialAccountResponse>> getAccounts(
            Authentication authentication) {

        UUID customerId = accountAuthorizationService.getAuthenticatedCustomerId(authentication);
        return ResponseEntity.ok(financialAccountService.getAccounts(customerId));
    }

    @Operation(summary = "Get the account balance", description = "Retrieve account balance of the authenticated customer.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{accountId}/balance")
    public ResponseEntity<AccountBalanceResponse> getAccountBalance(
            @PathVariable UUID accountId,
            Authentication authentication) {

        UUID customerId = accountAuthorizationService.getAuthenticatedCustomerId(authentication);
        return ResponseEntity.ok(financialAccountService.getAccountBalance(accountId, customerId));
    }

    @Operation(summary = "Retrieve an account", description = "Retrieve the specified account of the authenticated customer.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{accountId}")
    public ResponseEntity<FinancialAccountResponse> getAccount(
            @PathVariable UUID accountId,
            Authentication authentication) {

        UUID customerId = accountAuthorizationService.getAuthenticatedCustomerId(authentication);
        return ResponseEntity.ok(financialAccountService.getAccount(accountId, customerId));
    }
}
