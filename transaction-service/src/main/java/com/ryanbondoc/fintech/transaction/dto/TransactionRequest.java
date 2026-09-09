package com.ryanbondoc.fintech.transaction.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.ryanbondoc.fintech.transaction.entity.TransactionCategory;
import com.ryanbondoc.fintech.transaction.entity.TransactionDirection;
import com.ryanbondoc.fintech.transaction.entity.TransactionType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TransactionRequest(

        @NotNull(message = "Account ID is required")
        UUID accountId,

        @NotNull(message = "Transaction type is required")
        TransactionType type,

        @NotNull(message = "Transaction direction is required")
        TransactionDirection direction,

        @NotNull(message = "Amount is required")
        @DecimalMin(
                value = "0.01",
                message = "Amount must be greater than zero"
        )
        BigDecimal amount,

        @NotNull(message = "Currency is required")
        @Pattern(
                regexp = "^[A-Z]{3}$",
                message = "Currency must be a 3-letter uppercase ISO currency code"
        )
        String currency,

        @Size(
                max = 255,
                message = "Description must not exceed 255 characters"
        )
        String description,

        @Size(max = 150)
String merchant,

TransactionCategory category,

        OffsetDateTime transactionDate
        
        
) {
}
