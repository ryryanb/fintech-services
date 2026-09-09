package com.ryanbondoc.fintech.transaction.exception;

import java.util.UUID;


public class AccountNotFoundException extends RuntimeException {


public AccountNotFoundException(UUID accountId) {
    super("Financial account not found: " + accountId);
}

    
  

}