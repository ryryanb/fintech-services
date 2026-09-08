package com.ryanbondoc.fintech.account.exception;

import java.util.UUID;

public class FinancialAccountNotFoundException extends RuntimeException {

    public FinancialAccountNotFoundException(UUID accountId) {
        super("Financial account not found: " + accountId);
    }
}