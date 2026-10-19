package com.ryanbondoc.fintech.transaction.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidSpendingCategoryException extends RuntimeException {
    public InvalidSpendingCategoryException(String message) {
        super(message);
    }
}
