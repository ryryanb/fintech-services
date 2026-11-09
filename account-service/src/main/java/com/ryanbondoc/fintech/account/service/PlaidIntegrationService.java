package com.ryanbondoc.fintech.account.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import com.ryanbondoc.fintech.account.dto.PlaidTransactionSyncResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import com.ryanbondoc.fintech.account.dto.FinancialAccountResponse;
import com.ryanbondoc.fintech.account.dto.PlaidConnectionResponse;
import com.ryanbondoc.fintech.account.dto.PlaidExchangeRequest;
import com.ryanbondoc.fintech.account.dto.PlaidExchangeResponse;
import com.ryanbondoc.fintech.account.dto.PlaidLinkTokenResponse;
import com.ryanbondoc.fintech.account.entity.FinancialAccount;
import com.ryanbondoc.fintech.account.entity.PlaidConnection;
import com.ryanbondoc.fintech.account.enums.AccountStatus;
import com.ryanbondoc.fintech.account.enums.AccountType;
import com.ryanbondoc.fintech.account.repository.FinancialAccountRepository;
import com.ryanbondoc.fintech.account.repository.PlaidConnectionRepository;
import com.ryanbondoc.fintech.account.security.PlaidTokenCipher;

import tools.jackson.databind.JsonNode;

@Service
public class PlaidIntegrationService {
    private final FinancialAccountRepository accountRepository;
    private final PlaidConnectionRepository connectionRepository;
    private final PlaidTokenCipher tokenCipher;
    private final String clientId;
    private final String secret;
    private final String baseUrl;
    private final String transactionServiceBaseUrl;
    private final RestClient transactionClient = RestClient.create();
    private final RestClient restClient = RestClient.create();

    public PlaidIntegrationService(
            FinancialAccountRepository accountRepository,
            PlaidConnectionRepository connectionRepository,
            PlaidTokenCipher tokenCipher,
            @Value("${plaid.client-id:}") String clientId,
            @Value("${plaid.secret:}") String secret,
            @Value("${plaid.base-url:https://sandbox.plaid.com}") String baseUrl,
            @Value("${plaid.environment:sandbox}") String environment,
            @Value("${transactions.service.base-url:http://localhost:8087}") String transactionServiceBaseUrl) {
        this.accountRepository = accountRepository;
        this.connectionRepository = connectionRepository;
        this.tokenCipher = tokenCipher;
        this.clientId = clientId;
        this.secret = secret;
        this.baseUrl = baseUrl;
        this.transactionServiceBaseUrl = transactionServiceBaseUrl;
        if (!"sandbox".equalsIgnoreCase(environment)) {
            throw new IllegalStateException("This ACC-008 integration is restricted to Plaid Sandbox.");
        }
        if (!"https://sandbox.plaid.com".equalsIgnoreCase(baseUrl)) {
            throw new IllegalStateException("This ACC-008 integration must use https://sandbox.plaid.com.");
        }
    }

    public PlaidLinkTokenResponse createLinkToken(UUID customerId) {
        ensureConfigured();
        JsonNode response = plaidPost("/link/token/create", Map.of(
                "client_name", "Fintech Services",
                "language", "en",
                "country_codes", List.of("US"),
                "user", Map.of("client_user_id", customerId.toString()),
                "products", List.of("transactions")));
        return new PlaidLinkTokenResponse(
                requiredText(response, "link_token"),
                response.path("expiration").asText(null));
    }

    @Transactional
    public PlaidExchangeResponse exchangePublicToken(UUID customerId, PlaidExchangeRequest request) {
        ensureConfigured();
        JsonNode exchange = plaidPost("/item/public_token/exchange",
                Map.of("public_token", request.publicToken()));
        String accessToken = requiredText(exchange, "access_token");
        String itemId = requiredText(exchange, "item_id");

        if (connectionRepository.findByItemId(itemId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This Plaid Item is already connected.");
        }

        Instant now = Instant.now();
        PlaidConnection connection = new PlaidConnection();
        connection.setCustomerId(customerId);
        connection.setItemId(itemId);
        connection.setInstitutionId(request.institutionId());
        connection.setInstitutionName(request.institutionName());
        connection.setEncryptedAccessToken(tokenCipher.encrypt(accessToken));
        connection.setStatus("ACTIVE");
        connection.setCreatedAt(now);
        connection.setUpdatedAt(now);
        connection = connectionRepository.save(connection);

        JsonNode accountResponse = plaidPost("/accounts/get", Map.of("access_token", accessToken));
        List<FinancialAccountResponse> imported = new ArrayList<>();
        JsonNode accounts = accountResponse.path("accounts");
        for (JsonNode plaidAccount : accounts) {
            String providerAccountId = requiredText(plaidAccount, "account_id");
            FinancialAccount account = accountRepository.findByProviderAccountId(providerAccountId)
                    .orElseGet(FinancialAccount::new);
            account.setCustomerId(customerId);
            account.setProviderAccountId(providerAccountId);
            account.setExternalConnectionId(connection.getId());
            account.setName(firstNonBlank(plaidAccount.path("name").asText(null),
                    plaidAccount.path("official_name").asText(null), "Connected account"));
            account.setType(mapAccountType(plaidAccount.path("type").asText("depository")));
            account.setCurrency(firstNonBlank(
                    plaidAccount.path("balances").path("iso_currency_code").asText(null),
                    plaidAccount.path("balances").path("unofficial_currency_code").asText(null), "USD")
                    .toUpperCase());
            JsonNode current = plaidAccount.path("balances").path("current");
            account.setBalance(current.isNumber() ? current.decimalValue() : BigDecimal.ZERO);
            account.setInstitutionName(request.institutionName());
            account.setStatus(AccountStatus.ACTIVE);
            imported.add(toResponse(accountRepository.save(account)));
        }
        return new PlaidExchangeResponse(toResponse(connection), imported);
    }


    @Transactional
    public PlaidTransactionSyncResponse syncTransactions(UUID customerId, UUID connectionId,
            Authentication authentication) {
        ensureConfigured();
        PlaidConnection connection = connectionRepository.findByIdAndCustomerId(connectionId, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plaid connection not found."));
        if (!"ACTIVE".equals(connection.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This Plaid connection is not active.");
        }
        if (!(authentication instanceof JwtAuthenticationToken jwt)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A valid bearer token is required.");
        }
        String bearer = "Bearer " + jwt.getToken().getTokenValue();
        String accessToken = tokenCipher.decrypt(connection.getEncryptedAccessToken());
        String cursor = connection.getTransactionsCursor();
        int added = 0, updated = 0, removed = 0;
        boolean hasMore;
        do {
            Map<String, Object> payload = new HashMap<>();
            payload.put("access_token", accessToken);
            payload.put("count", 500);
            if (cursor != null && !cursor.isBlank()) payload.put("cursor", cursor);
            JsonNode page = plaidPost("/transactions/sync", payload);
            List<Map<String, Object>> newTransactions = mapPlaidTransactions(page.path("added"));
            List<Map<String, Object>> changedTransactions = mapPlaidTransactions(page.path("modified"));
            List<String> removedIds = new ArrayList<>();
            for (JsonNode item : page.path("removed")) {
                String id = item.path("transaction_id").asText(null);
                if (id != null && !id.isBlank()) removedIds.add(id);
            }
            Map<String, Object> importPayload = new HashMap<>();
            importPayload.put("added", newTransactions);
            importPayload.put("modified", changedTransactions);
            importPayload.put("removed", removedIds);
            PlaidTransactionSyncResponse imported = transactionClient.post()
                    .uri(transactionServiceBaseUrl + "/transactions/import/plaid")
                    .header(HttpHeaders.AUTHORIZATION, bearer)
                    .body(importPayload)
                    .retrieve()
                    .body(PlaidTransactionSyncResponse.class);
            if (imported == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Transaction Service returned an empty import response.");
            }
            added += imported.added();
            updated += imported.updated();
            removed += imported.removed();
            cursor = requiredText(page, "next_cursor");
            hasMore = page.path("has_more").asBoolean(false);
        } while (hasMore);
        connection.setTransactionsCursor(cursor);
        connection.setUpdatedAt(Instant.now());
        connectionRepository.save(connection);
        return new PlaidTransactionSyncResponse(added, updated, removed);
    }

    private List<Map<String, Object>> mapPlaidTransactions(JsonNode transactions) {
        List<Map<String, Object>> mapped = new ArrayList<>();
        for (JsonNode transaction : transactions) {
            String providerAccountId = requiredText(transaction, "account_id");
            FinancialAccount account = accountRepository.findByProviderAccountId(providerAccountId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "Plaid returned a transaction for an account that is not linked."));
            String date = firstNonBlank(transaction.path("authorized_date").asText(null),
                    transaction.path("date").asText(null), null);
            if (date == null) date = requiredText(transaction, "date");
            String currency = firstNonBlank(transaction.path("iso_currency_code").asText(null),
                    transaction.path("unofficial_currency_code").asText(null), account.getCurrency());
            Map<String, Object> item = new HashMap<>();
            item.put("providerTransactionId", requiredText(transaction, "transaction_id"));
            item.put("accountId", account.getId());
            item.put("amount", transaction.path("amount").decimalValue());
            item.put("currency", currency.toUpperCase());
            item.put("name", firstNonBlank(transaction.path("name").asText(null), "Bank transaction", "Bank transaction"));
            item.put("merchant", transaction.path("merchant_name").asText(null));
            item.put("category", transaction.path("personal_finance_category").path("primary").asText(null));
            item.put("transactionDate", LocalDate.parse(date).atStartOfDay().atOffset(ZoneOffset.UTC).toString());
            item.put("pending", transaction.path("pending").asBoolean(false));
            mapped.add(item);
        }
        return mapped;
    }

    @Transactional(readOnly = true)
    public List<PlaidConnectionResponse> getConnections(UUID customerId) {
        return connectionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public void disconnect(UUID customerId, UUID connectionId) {
        PlaidConnection connection = connectionRepository.findByIdAndCustomerId(connectionId, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plaid connection not found."));
        if ("DISCONNECTED".equals(connection.getStatus()))
            return;
        String accessToken = tokenCipher.decrypt(connection.getEncryptedAccessToken());
        plaidPost("/item/remove", Map.of("access_token", accessToken));
        connection.setStatus("DISCONNECTED");
        connection.setUpdatedAt(Instant.now());
        connectionRepository.save(connection);
        accountRepository.findByExternalConnectionId(connectionId).forEach(account -> {
            account.setStatus(AccountStatus.INACTIVE);
            accountRepository.save(account);
        });
    }

    private JsonNode plaidPost(String path, Map<String, ?> payload) {
        try {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("client_id", clientId);
            body.put("secret", secret);
            body.putAll(payload);
            JsonNode response = restClient.post().uri(baseUrl + path).body(body).retrieve().body(JsonNode.class);
            if (response == null)
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Plaid returned an empty response.");
            return response;
        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Plaid request failed. Check the Plaid Sandbox credentials and request configuration.");
        } catch (org.springframework.web.client.ResourceAccessException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Plaid is temporarily unreachable.");
        }
    }

    private void ensureConfigured() {
        if (clientId.isBlank() || secret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Plaid is not configured. Set PLAID_CLIENT_ID and PLAID_SECRET.");
        }
        if (!tokenCipher.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Plaid token encryption is not configured. Set PLAID_TOKEN_ENCRYPTION_KEY.");
        }
    }

    private String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Plaid response is missing " + field + ".");
        }
        return value;
    }

    private String firstNonBlank(String first, String second, String fallback) {
        if (first != null && !first.isBlank())
            return first;
        if (second != null && !second.isBlank())
            return second;
        return fallback;
    }

    private AccountType mapAccountType(String type) {
        return switch (type.toLowerCase()) {
            case "credit" -> AccountType.CREDIT_CARD;
            case "investment" -> AccountType.INVESTMENT;
            case "loan" -> AccountType.LOAN;
            default -> AccountType.BANK_ACCOUNT;
        };
    }

    private PlaidConnectionResponse toResponse(PlaidConnection connection) {
        return new PlaidConnectionResponse(connection.getId(), "PLAID", connection.getInstitutionId(),
                connection.getInstitutionName(), connection.getStatus(), connection.getCreatedAt());
    }

    private FinancialAccountResponse toResponse(FinancialAccount account) {
        return new FinancialAccountResponse(account.getId(), account.getCustomerId(), account.getName(),
                account.getType(), account.getCurrency(), account.getBalance(), account.getInstitutionName(),
                account.getStatus());
    }
}
