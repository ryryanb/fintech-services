package com.ryanbondoc.fintech.customer.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

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
        var authentication = authentication(UUID.randomUUID(), UUID.randomUUID());

        assertThrows(AccessDeniedException.class, () ->
                authorizationService.requireCustomerAccess(authentication, UUID.randomUUID()));
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
        var authentication = authentication(UUID.randomUUID(), UUID.randomUUID());

        assertThrows(AccessDeniedException.class, () ->
                authorizationService.requireUserAccess(authentication, UUID.randomUUID()));
    }

    private JwtAuthenticationToken authentication(UUID userId, UUID customerId) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(userId.toString())
                .claim("customerId", customerId.toString())
                .claim("roles", java.util.List.of("USER"))
                .build();

        return new JwtAuthenticationToken(jwt);
    }
}