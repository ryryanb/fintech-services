package com.ryanbondoc.fintech.account.dto;

import java.util.List;

public record PlaidExchangeResponse(
        PlaidConnectionResponse connection,
        List<FinancialAccountResponse> accounts) {
}
