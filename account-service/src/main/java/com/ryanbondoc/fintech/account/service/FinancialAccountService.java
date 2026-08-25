package com.ryanbondoc.fintech.account.service;

import com.ryanbondoc.fintech.account.dto.CreateFinancialAccountRequest;
import com.ryanbondoc.fintech.account.dto.FinancialAccountResponse;

public interface FinancialAccountService {

    FinancialAccountResponse createAccount(
            CreateFinancialAccountRequest request
    );
}
