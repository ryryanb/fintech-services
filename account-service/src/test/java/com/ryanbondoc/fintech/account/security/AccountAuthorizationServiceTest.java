package com.ryanbondoc.fintech.account.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import com.ryanbondoc.fintech.account.exception.ForbiddenException;

class AccountAuthorizationServiceTest {

    private AccountAuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AccountAuthorizationService();
    }

    @Test
    void shouldAllowAccessWhenCustomerIdMatchesJwtClaim() {
        UUID customerId = UUID.randomUUID();

        Authentication authentication = authenticationWithCustomerId(customerId);

        assertDoesNotThrow(() -> authorizationService.requireCustomerAccess(
                authentication,
                customerId));
    }

    @Test
    void shouldRejectAccessWhenCustomerIdDoesNotMatchJwtClaim() {
        UUID authenticatedCustomerId = UUID.randomUUID();
        UUID requestedCustomerId = UUID.randomUUID();

        Authentication authentication = authenticationWithCustomerId(authenticatedCustomerId);

        assertThrows(
                ForbiddenException.class,
                () -> authorizationService.requireCustomerAccess(
                        authentication,
                        requestedCustomerId));
    }

    @Test
    void shouldRejectAccessWhenJwtDoesNotContainCustomerId() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", UUID.randomUUID().toString())
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                jwt,
                null,
                Collections.emptyList());

        assertThrows(
                ForbiddenException.class,
                () -> authorizationService.requireCustomerAccess(
                        authentication,
                        UUID.randomUUID()));
    }

    private Authentication authenticationWithCustomerId(UUID customerId) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", UUID.randomUUID().toString())
                .claim("customerId", customerId.toString())
                .build();

        return new UsernamePasswordAuthenticationToken(
                jwt,
                null,
                Collections.emptyList());
    }
}