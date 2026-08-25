package com.ryanbondoc.fintech.account.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.ryanbondoc.fintech.account.enums.AccountStatus;
import com.ryanbondoc.fintech.account.enums.AccountType;

public record FinancialAccountResponse(
        UUID id,
        UUID customerId,
        String name,
        AccountType type,
        String currency,
        BigDecimal balance,
        String institutionName,
        AccountStatus status
) {
}