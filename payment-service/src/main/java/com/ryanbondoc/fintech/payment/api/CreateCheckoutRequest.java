package com.ryanbondoc.fintech.payment.api;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCheckoutRequest(
        @NotNull
        @DecimalMin(value = "0.50", message = "Amount must be at least 0.50")
        @Digits(integer = 10, fraction = 2, message = "Amount must have no more than two decimal places")
        BigDecimal amount,
        @Pattern(regexp = "(?i)USD|PHP|EUR|GBP|SGD|AUD|CAD", message = "Currency must be one of USD, PHP, EUR, GBP, SGD, AUD, or CAD")
        String currency,
        @NotBlank
        @Size(max = 120)
        String description) {
    public String normalizedCurrency() {
        return currency == null || currency.isBlank() ? "USD" : currency.toUpperCase();
    }
}