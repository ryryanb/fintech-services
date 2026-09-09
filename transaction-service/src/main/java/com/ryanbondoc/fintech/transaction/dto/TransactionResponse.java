package com.ryanbondoc.fintech.transaction.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.ryanbondoc.fintech.transaction.entity.TransactionCategory;
import com.ryanbondoc.fintech.transaction.entity.TransactionDirection;
import com.ryanbondoc.fintech.transaction.entity.TransactionStatus;
import com.ryanbondoc.fintech.transaction.entity.TransactionType;

public record TransactionResponse(

        UUID id,

        UUID accountId,

        TransactionType type,

        TransactionDirection direction,

        BigDecimal amount,

        String currency,

        String merchant,

        TransactionCategory category,

        String description,

        TransactionStatus status,

        OffsetDateTime transactionDate
) {
}