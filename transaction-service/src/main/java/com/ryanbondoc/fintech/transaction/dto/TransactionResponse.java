package com.ryanbondoc.fintech.transaction.dto;

import com.ryanbondoc.fintech.transaction.entity.TransactionDirection;
import com.ryanbondoc.fintech.transaction.entity.TransactionStatus;
import com.ryanbondoc.fintech.transaction.entity.TransactionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID accountId,
        TransactionType type,
        TransactionDirection direction,
        BigDecimal amount,
        String currency,
        String description,
        TransactionStatus status,
        OffsetDateTime transactionDate
) {
}