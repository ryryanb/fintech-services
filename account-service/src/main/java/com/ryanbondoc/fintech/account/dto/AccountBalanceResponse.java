package com.ryanbondoc.fintech.account.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountBalanceResponse(
        UUID accountId,
        String currency,
        BigDecimal balance
) {
}
