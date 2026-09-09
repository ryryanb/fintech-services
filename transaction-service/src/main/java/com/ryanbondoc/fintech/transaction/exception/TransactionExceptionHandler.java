package com.ryanbondoc.fintech.transaction.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class TransactionExceptionHandler {


@ExceptionHandler(AccountNotFoundException.class)
@ResponseStatus(HttpStatus.NOT_FOUND)
public Map<String, String> handleAccountNotFound(
        AccountNotFoundException exception) {

    return Map.of(
            "error", "ACCOUNT_NOT_FOUND",
            "message", exception.getMessage()
    );
}

@ExceptionHandler(TransactionNotFoundException.class)
@ResponseStatus(HttpStatus.NOT_FOUND)
public Map<String, String> handleTransactionNotFound(
        TransactionNotFoundException exception) {

    return Map.of(
            "error", "TRANSACTION_NOT_FOUND",
            "message", exception.getMessage()
    );
}

    
  

}
