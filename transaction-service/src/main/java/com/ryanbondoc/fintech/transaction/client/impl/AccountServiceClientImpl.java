package com.ryanbondoc.fintech.transaction.client.impl;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.ryanbondoc.fintech.transaction.client.AccountOwnershipResponse;
import com.ryanbondoc.fintech.transaction.client.AccountServiceClient;
import com.ryanbondoc.fintech.transaction.exception.AccountAccessDeniedException;

@Component
public class AccountServiceClientImpl
        implements AccountServiceClient {

    private final RestClient restClient;

    public AccountServiceClientImpl(
            RestClient.Builder restClientBuilder,
            AccountServiceProperties properties) {

        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Override
    public boolean accountExists(
            UUID accountId,
            String bearerToken) {

        try {
            restClient
                    .get()
                    .uri("/accounts/{accountId}", accountId)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            bearerToken)
                    .retrieve()
                    .toBodilessEntity();

            return true;

        } catch (RestClientResponseException exception) {

            if (exception.getStatusCode()
                    .equals(HttpStatus.NOT_FOUND)) {

                return false;
            }

            if (exception.getStatusCode()
                    .equals(HttpStatus.FORBIDDEN)) {

                throw new AccountAccessDeniedException(
                        "Account does not belong to authenticated customer",
                        exception);
            }

            throw exception;
        }
    }

    @Override
    public AccountOwnershipResponse getAccountOwnership(
            UUID accountId,
            String bearerToken) {

        return restClient
                .get()
                .uri("/accounts/{accountId}", accountId)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .retrieve()
                .body(AccountOwnershipResponse.class);
    }

}