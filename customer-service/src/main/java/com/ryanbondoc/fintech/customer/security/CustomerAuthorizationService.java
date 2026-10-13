package com.ryanbondoc.fintech.customer.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.ryanbondoc.fintech.customer.entity.Customer;
import com.ryanbondoc.fintech.customer.exception.ForbiddenException;
import com.ryanbondoc.fintech.customer.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerAuthorizationService {

    private static final String CUSTOMER_ID_CLAIM = "customerId";

    private final CustomerRepository customerRepository;

    public void requireCustomerAccess(Authentication authentication, UUID requestedCustomerId) {
        UUID authenticatedCustomerId = getAuthenticatedCustomerId(authentication);

        if (!authenticatedCustomerId.equals(requestedCustomerId)) {
            throw new ForbiddenException(
                    "You are not authorized to access this customer's information.");
        }
    }

    public void requireUserAccess(Authentication authentication, UUID requestedUserId) {
        Jwt jwt = getAuthenticatedJwt(authentication);
        String subject = jwt.getSubject();

        if (subject == null || subject.isBlank()) {
            throw new ForbiddenException("Authenticated token does not contain a user identity.");
        }

        UUID authenticatedUserId;
        try {
            authenticatedUserId = UUID.fromString(subject);
        } catch (IllegalArgumentException exception) {
            throw new ForbiddenException("Authenticated token contains an invalid user identity.");
        }

        if (!authenticatedUserId.equals(requestedUserId)) {
            throw new ForbiddenException(
                    "You are not authorized to access this user's information.");
        }
    }

    public void requireCustomerNumberAccess(Authentication authentication, String customerNumber) {
        Customer customer = customerRepository.findByCustomerNumber(customerNumber)
                .orElseThrow(() -> new ForbiddenException(
                        "You are not authorized to access this customer's information."));
        requireCustomerAccess(authentication, customer.getId());
    }

    public UUID getAuthenticatedCustomerId(Authentication authentication) {
        Jwt jwt = getAuthenticatedJwt(authentication);
        String customerId = jwt.getClaimAsString(CUSTOMER_ID_CLAIM);

        if (customerId == null || customerId.isBlank()) {
            throw new ForbiddenException("Authenticated token does not contain customerId.");
        }

        try {
            return UUID.fromString(customerId);
        } catch (IllegalArgumentException exception) {
            throw new ForbiddenException("Authenticated token contains an invalid customerId.");
        }
    }

    private Jwt getAuthenticatedJwt(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new ForbiddenException("Authenticated customer identity is unavailable.");
        }
        return jwt;
    }
}