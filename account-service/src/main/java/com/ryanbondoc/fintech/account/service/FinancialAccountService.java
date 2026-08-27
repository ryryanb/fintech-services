package com.ryanbondoc.fintech.account.service;

import java.util.List;
import java.util.UUID;

import com.ryanbondoc.fintech.account.dto.CreateFinancialAccountRequest;
import com.ryanbondoc.fintech.account.dto.FinancialAccountResponse;

public interface FinancialAccountService {

    FinancialAccountResponse createAccount(
            CreateFinancialAccountRequest request
    );

    List<FinancialAccountResponse> getAccounts(
            UUID customerId);
}
