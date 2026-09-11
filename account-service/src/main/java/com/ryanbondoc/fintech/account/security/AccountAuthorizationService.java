package com.ryanbondoc.fintech.account.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.ryanbondoc.fintech.account.exception.ForbiddenException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountAuthorizationService {

    private static final String CUSTOMER_ID_CLAIM = "customerId";

    public void requireCustomerAccess(
            Authentication authentication,
            UUID requestedCustomerId) {

        UUID authenticatedCustomerId = getAuthenticatedCustomerId(authentication);

        if (!authenticatedCustomerId.equals(requestedCustomerId)) {
            throw new ForbiddenException(
                    "You are not authorized to access this customer's financial information.");
        }
    }

    public UUID getAuthenticatedCustomerId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new ForbiddenException("Authenticated customer identity is unavailable.");
        }

        String customerIdClaim = jwt.getClaimAsString(CUSTOMER_ID_CLAIM);

        if (customerIdClaim == null || customerIdClaim.isBlank()) {
            throw new ForbiddenException("Authenticated token does not contain customerId.");
        }

        try {
            return UUID.fromString(customerIdClaim);
        } catch (IllegalArgumentException exception) {
            throw new ForbiddenException("Authenticated token contains an invalid customerId.");
        }
    }
}