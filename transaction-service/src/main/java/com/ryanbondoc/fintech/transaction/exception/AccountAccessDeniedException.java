package com.ryanbondoc.fintech.transaction.exception;

public class AccountAccessDeniedException
        extends RuntimeException {

    public AccountAccessDeniedException(String message) {
        super(message);
    }

    public AccountAccessDeniedException(
            String message,
            Throwable cause) {
        super(message, cause);
    }
}