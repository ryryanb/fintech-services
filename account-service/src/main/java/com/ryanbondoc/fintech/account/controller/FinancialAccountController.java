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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class FinancialAccountController {

        private final FinancialAccountService financialAccountService;
        private final AccountAuthorizationService accountAuthorizationService;

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
