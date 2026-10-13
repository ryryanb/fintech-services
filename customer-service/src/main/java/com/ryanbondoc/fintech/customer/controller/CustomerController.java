package com.ryanbondoc.fintech.customer.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ryanbondoc.fintech.customer.dto.AddressRequest;
import com.ryanbondoc.fintech.customer.dto.AddressResponse;
import com.ryanbondoc.fintech.customer.dto.CustomerCreateRequest;
import com.ryanbondoc.fintech.customer.dto.CustomerRequest;
import com.ryanbondoc.fintech.customer.dto.CustomerResponse;
import com.ryanbondoc.fintech.customer.security.CustomerAuthorizationService;
import com.ryanbondoc.fintech.customer.service.CustomerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerAuthorizationService authorizationService;

    /**
     * Auth Service calls this endpoint during registration, before the user has
     * credentials for an access token. The endpoint is intentionally permitAll
     * in SecurityConfig; deployments must keep this service endpoint private to
     * trusted internal callers.
     */
    @Operation(summary = "Create customer", description = "Internal registration operation called by Auth Service.")
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CustomerCreateRequest customer) {
        CustomerResponse createdCustomer = customerService.createCustomer(customer);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCustomer);
    }

    @Operation(summary = "List customers", description = "Requires an administrator role.", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @Operation(summary = "Get customer for user", description = "Retrieves the authenticated user's customer profile.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<CustomerResponse> getCustomerByUserId(
            @PathVariable UUID userId,
            Authentication authentication) {
        authorizationService.requireUserAccess(authentication, userId);
        return ResponseEntity.ok(customerService.getCustomerByUserId(userId));
    }

    @Operation(summary = "Get customer", description = "Retrieves the authenticated customer's profile.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById(
            @PathVariable UUID id,
            Authentication authentication) {
        authorizationService.requireCustomerAccess(authentication, id);
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @Operation(summary = "Update customer", description = "Updates the authenticated customer's profile.", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable UUID id,
            @Valid @RequestBody CustomerRequest customer,
            Authentication authentication) {
        authorizationService.requireCustomerAccess(authentication, id);
        return ResponseEntity.ok(customerService.updateCustomer(id, customer));
    }

    @Operation(summary = "Delete customer", description = "Deletes the authenticated customer's profile.", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(
            @PathVariable String id,
            Authentication authentication) {
        authorizationService.requireCustomerNumberAccess(authentication, id);
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search customers", description = "Requires an administrator role.", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<List<CustomerResponse>> searchCustomers(
            @RequestParam String query) {
        return ResponseEntity.ok(customerService.searchCustomers(query));
    }

    @Operation(summary = "Get customer addresses", description = "Retrieves addresses belonging to the authenticated customer.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{id}/addresses")
    public ResponseEntity<List<AddressResponse>> getCustomerAddresses(
            @PathVariable UUID id,
            Authentication authentication) {
        authorizationService.requireCustomerAccess(authentication, id);
        return ResponseEntity.ok(customerService.getCustomerAddresses(id));
    }

    @Operation(summary = "Add customer address", description = "Adds an address to the authenticated customer's profile.", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{id}/addresses")
    public ResponseEntity<AddressResponse> addCustomerAddress(
            @PathVariable UUID id,
            @Valid @RequestBody AddressRequest address,
            Authentication authentication) {
        authorizationService.requireCustomerAccess(authentication, id);
        AddressResponse createdAddress = customerService.addCustomerAddress(id, address);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAddress);
    }
}
