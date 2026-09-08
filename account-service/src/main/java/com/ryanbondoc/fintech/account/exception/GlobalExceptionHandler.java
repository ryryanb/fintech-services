package com.ryanbondoc.fintech.account.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FinancialAccountNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAccountNotFound(
            FinancialAccountNotFoundException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", "ACCOUNT_NOT_FOUND",
                        "message", ex.getMessage()
                ));
    }
}
