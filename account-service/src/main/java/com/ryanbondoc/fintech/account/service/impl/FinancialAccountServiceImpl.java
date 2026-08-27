package com.ryanbondoc.fintech.account.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ryanbondoc.fintech.account.dto.CreateFinancialAccountRequest;
import com.ryanbondoc.fintech.account.dto.FinancialAccountResponse;
import com.ryanbondoc.fintech.account.entity.FinancialAccount;
import com.ryanbondoc.fintech.account.enums.AccountStatus;
import com.ryanbondoc.fintech.account.repository.FinancialAccountRepository;
import com.ryanbondoc.fintech.account.service.FinancialAccountService;

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

    @Override
@Transactional(readOnly = true)
public List<FinancialAccountResponse> getAccounts(
        UUID customerId) {

    return repository.findByCustomerId(customerId)
            .stream()
            .map(this::toResponse)
            .toList();
}
}
