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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ryanbondoc.fintech.account.dto.AccountBalanceResponse;
import com.ryanbondoc.fintech.account.dto.CreateFinancialAccountRequest;
import com.ryanbondoc.fintech.account.dto.FinancialAccountResponse;
import com.ryanbondoc.fintech.account.exception.ForbiddenException;
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

                accountAuthorizationService.requireCustomerAccess(
                                authentication,
                                request.customerId());

                FinancialAccountResponse response = financialAccountService.createAccount(request);

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(response);
        }

        @Operation(summary = "Retrieve accounts", description = "Retrieve accounts of the authenticated customer.", security = @SecurityRequirement(name = "bearerAuth"))
        @GetMapping
        public ResponseEntity<List<FinancialAccountResponse>> getAccounts(
                        @RequestParam UUID customerId,
                        Authentication authentication) {

                accountAuthorizationService.requireCustomerAccess(
                                authentication,
                                customerId);

                List<FinancialAccountResponse> accounts = financialAccountService.getAccounts(customerId);

                return ResponseEntity.ok(accounts);
        }

        @Operation(summary = "Get the account balance", description = "Retrieve account balance of the authenticated customer.", security = @SecurityRequirement(name = "bearerAuth"))
        @GetMapping("/{accountId}/balance")
        public ResponseEntity<AccountBalanceResponse> getAccountBalance(
                        @PathVariable UUID accountId,
                        Authentication authentication) {

                UUID authenticatedCustomerId = accountAuthorizationService.getAuthenticatedCustomerId(authentication);

                if (!financialAccountService.belongsToCustomer(
                                accountId,
                                authenticatedCustomerId)) {

                        throw new ForbiddenException(
                                        "You are not authorized to access this account.");
                }

                AccountBalanceResponse response = financialAccountService.getAccountBalance(accountId);

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Retrieve an account", description = "Retrieve the specified account of the authenticated customer.", security = @SecurityRequirement(name = "bearerAuth"))
        @GetMapping("/{accountId}")
        public ResponseEntity<FinancialAccountResponse> getAccount(
                        @PathVariable UUID accountId, Authentication authentication) {
                FinancialAccountResponse account = financialAccountService.getAccount(accountId);

                accountAuthorizationService.authorizeAccountAccess(
                                account,
                                authentication);
                return ResponseEntity.ok(
                                financialAccountService.getAccount(accountId));

        }

}
