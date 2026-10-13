package com.ryanbondoc.fintech.customer.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.ryanbondoc.fintech.customer.exception.ForbiddenException;
import com.ryanbondoc.fintech.customer.repository.CustomerRepository;

class CustomerAuthorizationServiceTest {

    private CustomerAuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new CustomerAuthorizationService(mock(CustomerRepository.class));
    }

    @Test
    void allowsAccessToAuthenticatedCustomersOwnProfile() {
        UUID customerId = UUID.randomUUID();
        var authentication = authentication(UUID.randomUUID(), customerId);

        assertDoesNotThrow(() ->
                authorizationService.requireCustomerAccess(authentication, customerId));
    }

    @Test
    void rejectsAccessToAnotherCustomersProfile() {
        UUID requestedCustomerId = UUID.randomUUID();
        UUID anotherCustomerId = UUID.randomUUID();
        var authentication = authentication(UUID.randomUUID(), anotherCustomerId);

        assertThrows(ForbiddenException.class, () ->
                authorizationService.requireCustomerAccess(authentication, requestedCustomerId));
    }

    @Test
    void allowsAccessToAuthenticatedUsersOwnCustomerProfile() {
        UUID userId = UUID.randomUUID();
        var authentication = authentication(userId, UUID.randomUUID());

        assertDoesNotThrow(() ->
                authorizationService.requireUserAccess(authentication, userId));
    }

    @Test
    void rejectsAccessToAnotherUsersCustomerProfile() {
        UUID requestedUserId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();
        var authentication = authentication(anotherUserId, UUID.randomUUID());

        assertThrows(ForbiddenException.class, () ->
                authorizationService.requireUserAccess(authentication, requestedUserId));
    }

    private JwtAuthenticationToken authentication(UUID userId, UUID customerId) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(userId.toString())
                .claim("customerId", customerId.toString())
                .claim("roles", List.of("USER"))
                .build();

        return new JwtAuthenticationToken(jwt);
    }
}