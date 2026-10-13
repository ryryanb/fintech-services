package com.ryanbondoc.fintech.customer.exception;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ryanbondoc.fintech.core.exception.ErrorResponse;

@RestControllerAdvice
public class CustomerAuthorizationExceptionHandler {

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException exception) {
        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                HttpStatus.FORBIDDEN.value(),
                exception.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
}