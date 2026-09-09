package com.ryanbondoc.fintech.transaction.client.impl;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.ryanbondoc.fintech.transaction.client.AccountServiceClient;


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
public boolean accountExists(UUID accountId) {

    try {
        restClient
                .get()
                .uri("/accounts/{accountId}", accountId)
                .retrieve()
                .toBodilessEntity();

        return true;

    } catch (RestClientResponseException exception) {

        if (exception.getStatusCode()
                .equals(HttpStatus.NOT_FOUND)) {

            return false;
        }

        throw exception;
    }
}

    
  

}