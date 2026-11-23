package com.ryanbondoc.fintech.transaction.dto;

import java.util.List;

public record FinancialContextResponse(
        int periodDays,
        int transactionCount,
        boolean truncated,
        List<TransactionResponse> transactions) {
}
