package com.ryanbondoc.fintech.transaction.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlaidTransactionImportRequest(
        @NotNull @Valid List<ImportedTransaction> added,
        @NotNull @Valid List<ImportedTransaction> modified,
        @NotNull List<@NotBlank String> removed) {
    public record ImportedTransaction(
            @NotBlank @Size(max = 150) String providerTransactionId,
            @NotNull UUID accountId,
            @NotNull BigDecimal amount,
            @NotBlank @Size(min = 3, max = 3) String currency,
            @NotBlank @Size(max = 255) String name,
            @Size(max = 150) String merchant,
            @Size(max = 60) String category,
            @NotNull OffsetDateTime transactionDate,
            boolean pending) {}
}
