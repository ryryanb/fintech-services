package com.ryanbondoc.fintech.account.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ryanbondoc.fintech.account.dto.PlaidConnectionResponse;
import com.ryanbondoc.fintech.account.dto.PlaidExchangeRequest;
import com.ryanbondoc.fintech.account.dto.PlaidExchangeResponse;
import com.ryanbondoc.fintech.account.dto.PlaidLinkTokenResponse;
import com.ryanbondoc.fintech.account.dto.PlaidTransactionSyncResponse;
import com.ryanbondoc.fintech.account.security.AccountAuthorizationService;
import com.ryanbondoc.fintech.account.service.PlaidIntegrationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/accounts/connections")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PlaidConnectionController {
    private final PlaidIntegrationService plaidIntegrationService;
    private final AccountAuthorizationService authorizationService;

    @Operation(summary = "Create a Plaid Link token")
    @PostMapping("/plaid/link-token")
    public ResponseEntity<PlaidLinkTokenResponse> createLinkToken(Authentication authentication) {
        UUID customerId = authorizationService.getAuthenticatedCustomerId(authentication);
        return ResponseEntity.ok(plaidIntegrationService.createLinkToken(customerId));
    }

    @Operation(summary = "Exchange a Plaid public token and import linked accounts")
    @PostMapping("/plaid")
    public ResponseEntity<PlaidExchangeResponse> exchangePublicToken(
            @Valid @RequestBody PlaidExchangeRequest request, Authentication authentication) {
        UUID customerId = authorizationService.getAuthenticatedCustomerId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(plaidIntegrationService.exchangePublicToken(customerId, request));
    }

    @Operation(summary = "List the authenticated customer's external account connections")
    @GetMapping
    public ResponseEntity<List<PlaidConnectionResponse>> getConnections(Authentication authentication) {
        UUID customerId = authorizationService.getAuthenticatedCustomerId(authentication);
        return ResponseEntity.ok(plaidIntegrationService.getConnections(customerId));
    }

    @Operation(summary = "Import new and changed transactions from a Plaid connection")
    @PostMapping("/{connectionId}/transactions/sync")
    public ResponseEntity<PlaidTransactionSyncResponse> syncTransactions(
            @PathVariable UUID connectionId, Authentication authentication) {
        UUID customerId = authorizationService.getAuthenticatedCustomerId(authentication);
        return ResponseEntity.ok(plaidIntegrationService.syncTransactions(customerId, connectionId, authentication));
    }

    @Operation(summary = "Disconnect an external Plaid account")
    @DeleteMapping("/{connectionId}")
    public ResponseEntity<Void> disconnect(@PathVariable UUID connectionId, Authentication authentication) {
        UUID customerId = authorizationService.getAuthenticatedCustomerId(authentication);
        plaidIntegrationService.disconnect(customerId, connectionId);
        return ResponseEntity.noContent().build();
    }
}
