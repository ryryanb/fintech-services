package com.ryanbondoc.fintech.account.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ryanbondoc.fintech.account.dto.AccountBalanceResponse;
import com.ryanbondoc.fintech.account.dto.FinancialAccountResponse;
import com.ryanbondoc.fintech.account.entity.FinancialAccount;
import com.ryanbondoc.fintech.account.enums.AccountStatus;
import com.ryanbondoc.fintech.account.enums.AccountType;
import com.ryanbondoc.fintech.account.exception.FinancialAccountNotFoundException;
import com.ryanbondoc.fintech.account.repository.FinancialAccountRepository;

@ExtendWith(MockitoExtension.class)
class FinancialAccountServiceImplTest {

    @Mock
    private FinancialAccountRepository repository;

    @InjectMocks
    private FinancialAccountServiceImpl service;

    @Test
    void shouldReturnOnlyAccountsForRequestedCustomer() {
        UUID customerId = UUID.randomUUID();
        FinancialAccount account = account(UUID.randomUUID(), customerId);
        when(repository.findByCustomerId(customerId)).thenReturn(List.of(account));

        List<FinancialAccountResponse> result = service.getAccounts(customerId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().customerId()).isEqualTo(customerId);
        verify(repository).findByCustomerId(customerId);
        verify(repository, never()).findAll();
    }

    @Test
    void shouldReturnAccountWhenItBelongsToCustomer() {
        UUID accountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(repository.findByIdAndCustomerId(accountId, customerId))
                .thenReturn(Optional.of(account(accountId, customerId)));

        FinancialAccountResponse result = service.getAccount(accountId, customerId);

        assertThat(result.id()).isEqualTo(accountId);
        assertThat(result.customerId()).isEqualTo(customerId);
        verify(repository).findByIdAndCustomerId(accountId, customerId);
        verify(repository, never()).findById(accountId);
    }

    @Test
    void shouldNotReturnAccountOwnedByAnotherCustomer() {
        UUID accountId = UUID.randomUUID();
        UUID authenticatedCustomerId = UUID.randomUUID();
        UUID accountOwnerId = UUID.randomUUID();
        when(repository.findByIdAndCustomerId(accountId, authenticatedCustomerId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAccount(accountId, authenticatedCustomerId))
                .isInstanceOf(FinancialAccountNotFoundException.class);

        verify(repository).findByIdAndCustomerId(accountId, authenticatedCustomerId);
        verify(repository, never()).findById(accountId);
    }

    @Test
    void shouldReturnBalanceOnlyForOwningCustomer() {
        UUID accountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        FinancialAccount account = account(accountId, customerId);
        when(repository.findByIdAndCustomerId(accountId, customerId))
                .thenReturn(Optional.of(account));

        AccountBalanceResponse result = service.getAccountBalance(accountId, customerId);

        assertThat(result.accountId()).isEqualTo(accountId);
        assertThat(result.balance()).isEqualByComparingTo("125.00");
        verify(repository).findByIdAndCustomerId(accountId, customerId);
    }

    @Test
    void shouldNotReturnBalanceForAnotherCustomersAccount() {
        UUID accountId = UUID.randomUUID();
        UUID authenticatedCustomerId = UUID.randomUUID();
        when(repository.findByIdAndCustomerId(accountId, authenticatedCustomerId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAccountBalance(accountId, authenticatedCustomerId))
                .isInstanceOf(FinancialAccountNotFoundException.class);

        verify(repository).findByIdAndCustomerId(accountId, authenticatedCustomerId);
    }

    private FinancialAccount account(UUID accountId, UUID customerId) {
        return FinancialAccount.builder()
                .id(accountId)
                .customerId(customerId)
                .name("Primary")
                .type(AccountType.BANK_ACCOUNT)
                .currency("PHP")
                .balance(new BigDecimal("125.00"))
                .institutionName("Example Bank")
                .status(AccountStatus.ACTIVE)
                .build();
    }
}
