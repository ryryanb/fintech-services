package com.ryanbondoc.fintech.auth.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ryanbondoc.fintech.auth.dto.CustomerCreateRequest;
import com.ryanbondoc.fintech.auth.dto.CustomerResponse;

@Component
public class CustomerServiceClient {

    private final RestClient restClient;

    public CustomerServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${services.customer-service.url}") String customerServiceUrl) {

        this.restClient = restClientBuilder
                .baseUrl(customerServiceUrl)
                .build();
    }

    public CustomerResponse createCustomer(
            UUID userId,
            String firstName,
            String lastName,
            String email) {

        CustomerCreateRequest request =
                new CustomerCreateRequest(
                        userId,
                        firstName,
                        lastName,
                        email,
                        "ACTIVE",
                        null
                );

        return restClient.post()
                .uri("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(CustomerResponse.class);
    }
}