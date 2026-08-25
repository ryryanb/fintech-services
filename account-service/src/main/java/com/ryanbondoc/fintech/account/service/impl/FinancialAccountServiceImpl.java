package com.ryanbondoc.fintech.account.service.impl;

import org.springframework.stereotype.Service;

import com.ryanbondoc.fintech.account.dto.CreateFinancialAccountRequest;
import com.ryanbondoc.fintech.account.dto.FinancialAccountResponse;
import com.ryanbondoc.fintech.account.entity.FinancialAccount;
import com.ryanbondoc.fintech.account.enums.AccountStatus;
import com.ryanbondoc.fintech.account.repository.FinancialAccountRepository;
import com.ryanbondoc.fintech.account.service.FinancialAccountService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class FinancialAccountServiceImpl
        implements FinancialAccountService {

    private final FinancialAccountRepository repository;

    @Override
    public FinancialAccountResponse createAccount(
            CreateFinancialAccountRequest request) {

        FinancialAccount account = FinancialAccount.builder()
                .customerId(request.customerId())
                .name(request.name().trim())
                .type(request.type())
                .currency(request.currency().toUpperCase())
                .balance(request.balance())
                .institutionName(
                        request.institutionName() == null
                                ? null
                                : request.institutionName().trim()
                )
                .status(AccountStatus.ACTIVE)
                .build();

        FinancialAccount saved = repository.save(account);

        return toResponse(saved);
    }

    private FinancialAccountResponse toResponse(
            FinancialAccount account) {

        return new FinancialAccountResponse(
                account.getId(),
                account.getCustomerId(),
                account.getName(),
                account.getType(),
                account.getCurrency(),
                account.getBalance(),
                account.getInstitutionName(),
                account.getStatus()
        );
    }
}
