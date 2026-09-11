package com.ryanbondoc.fintech.account.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(FinancialAccountNotFoundException.class)
        public ResponseEntity<Map<String, Object>> handleAccountNotFound(
                        FinancialAccountNotFoundException ex) {

                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(Map.of(
                                                "error", "ACCOUNT_NOT_FOUND",
                                                "message", ex.getMessage()));
        }

        @ExceptionHandler(EntityNotFoundException.class)
        @ResponseStatus(HttpStatus.NOT_FOUND)
        public Map<String, String> handleEntityNotFound(
                        EntityNotFoundException exception) {

                return Map.of(
                                "error", "ACCOUNT_NOT_FOUND",
                                "message", exception.getMessage());
        }

        @ExceptionHandler(ForbiddenException.class)
        @ResponseStatus(HttpStatus.FORBIDDEN)
        public Map<String, String> handleForbidden(ForbiddenException exception) {
                return Map.of(
                                "error", "FORBIDDEN",
                                "message", exception.getMessage());
        }
}
