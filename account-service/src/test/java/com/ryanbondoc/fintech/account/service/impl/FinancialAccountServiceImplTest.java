package com.ryanbondoc.fintech.account.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ryanbondoc.fintech.account.dto.CreateFinancialAccountRequest;
import com.ryanbondoc.fintech.account.dto.FinancialAccountResponse;
import com.ryanbondoc.fintech.account.entity.FinancialAccount;
import com.ryanbondoc.fintech.account.enums.AccountStatus;
import com.ryanbondoc.fintech.account.enums.AccountType;
import com.ryanbondoc.fintech.account.repository.FinancialAccountRepository;

@ExtendWith(MockitoExtension.class)
class FinancialAccountServiceImplTest {

    @Mock
    private FinancialAccountRepository financialAccountRepository;

    @InjectMocks
    private FinancialAccountServiceImpl financialAccountService;

    @Test
    void shouldCreateFinancialAccount() {
        // Given
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        CreateFinancialAccountRequest request =
                new CreateFinancialAccountRequest(
                        customerId,
                        "BPI Savings",
                        AccountType.BANK_ACCOUNT,
                        "PHP",
                        new BigDecimal("50000.00"),
                        "BPI"
                );

        FinancialAccount savedAccount = FinancialAccount.builder()
                .id(accountId)
                .customerId(customerId)
                .name("BPI Savings")
                .type(AccountType.BANK_ACCOUNT)
                .currency("PHP")
                .balance(new BigDecimal("50000.00"))
                .institutionName("BPI")
                .status(AccountStatus.ACTIVE)
                .build();

        when(financialAccountRepository.save(any(FinancialAccount.class)))
                .thenReturn(savedAccount);

        // When
        FinancialAccountResponse response =
                financialAccountService.createAccount(request);

        // Then
        verify(financialAccountRepository, times(1))
                .save(any(FinancialAccount.class));

        assertThat(response.id())
                .isEqualTo(accountId);

        assertThat(response.customerId())
                .isEqualTo(customerId);

        assertThat(response.name())
                .isEqualTo("BPI Savings");

        assertThat(response.type())
                .isEqualTo(AccountType.BANK_ACCOUNT);

        assertThat(response.currency())
                .isEqualTo("PHP");

        assertThat(response.balance())
                .isEqualByComparingTo("50000.00");

        assertThat(response.institutionName())
                .isEqualTo("BPI");

        assertThat(response.status())
                .isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void shouldNormalizeCurrencyToUppercase() {
        // Given
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        CreateFinancialAccountRequest request =
                new CreateFinancialAccountRequest(
                        customerId,
                        "BPI Savings",
                        AccountType.BANK_ACCOUNT,
                        "php",
                        new BigDecimal("50000.00"),
                        "BPI"
                );

        FinancialAccount savedAccount = FinancialAccount.builder()
                .id(accountId)
                .customerId(customerId)
                .name("BPI Savings")
                .type(AccountType.BANK_ACCOUNT)
                .currency("PHP")
                .balance(new BigDecimal("50000.00"))
                .institutionName("BPI")
                .status(AccountStatus.ACTIVE)
                .build();

        when(financialAccountRepository.save(any(FinancialAccount.class)))
                .thenReturn(savedAccount);

        // When
        financialAccountService.createAccount(request);

        // Then
        ArgumentCaptor<FinancialAccount> accountCaptor =
                ArgumentCaptor.forClass(FinancialAccount.class);

        verify(financialAccountRepository)
                .save(accountCaptor.capture());

        FinancialAccount account = accountCaptor.getValue();

        assertThat(account.getCurrency())
                .isEqualTo("PHP");
    }

    @Test
    void shouldAllowAccountWithoutInstitutionName() {
        // Given
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        CreateFinancialAccountRequest request =
                new CreateFinancialAccountRequest(
                        customerId,
                        "Cash",
                        AccountType.CASH,
                        "PHP",
                        new BigDecimal("10000.00"),
                        null
                );

        FinancialAccount savedAccount = FinancialAccount.builder()
                .id(accountId)
                .customerId(customerId)
                .name("Cash")
                .type(AccountType.CASH)
                .currency("PHP")
                .balance(new BigDecimal("10000.00"))
                .institutionName(null)
                .status(AccountStatus.ACTIVE)
                .build();

        when(financialAccountRepository.save(any(FinancialAccount.class)))
                .thenReturn(savedAccount);

        // When
        FinancialAccountResponse response =
                financialAccountService.createAccount(request);

        // Then
        verify(financialAccountRepository)
                .save(any(FinancialAccount.class));

        assertThat(response.id())
                .isEqualTo(accountId);

        assertThat(response.institutionName())
                .isNull();

        assertThat(response.status())
                .isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void shouldAssociateAccountWithCorrectCustomer() {
        // Given
        UUID customerId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        CreateFinancialAccountRequest request =
                new CreateFinancialAccountRequest(
                        customerId,
                        "BPI Savings",
                        AccountType.BANK_ACCOUNT,
                        "PHP",
                        new BigDecimal("50000.00"),
                        "BPI"
                );

        FinancialAccount savedAccount = FinancialAccount.builder()
                .id(accountId)
                .customerId(customerId)
                .name("BPI Savings")
                .type(AccountType.BANK_ACCOUNT)
                .currency("PHP")
                .balance(new BigDecimal("50000.00"))
                .institutionName("BPI")
                .status(AccountStatus.ACTIVE)
                .build();

        when(financialAccountRepository.save(any(FinancialAccount.class)))
                .thenReturn(savedAccount);

        // When
        financialAccountService.createAccount(request);

        // Then
        ArgumentCaptor<FinancialAccount> accountCaptor =
                ArgumentCaptor.forClass(FinancialAccount.class);

        verify(financialAccountRepository)
                .save(accountCaptor.capture());

        FinancialAccount account = accountCaptor.getValue();

        assertThat(account.getCustomerId())
                .isEqualTo(customerId);
    }

    @Test
void shouldReturnCustomersAccounts() {
    // Given
    UUID customerId = UUID.randomUUID();

    UUID accountId1 = UUID.randomUUID();
    UUID accountId2 = UUID.randomUUID();

    FinancialAccount account1 = FinancialAccount.builder()
            .id(accountId1)
            .customerId(customerId)
            .name("BPI Savings")
            .type(AccountType.BANK_ACCOUNT)
            .currency("PHP")
            .balance(new BigDecimal("50000.00"))
            .institutionName("BPI")
            .status(AccountStatus.ACTIVE)
            .build();

    FinancialAccount account2 = FinancialAccount.builder()
            .id(accountId2)
            .customerId(customerId)
            .name("Cash")
            .type(AccountType.CASH)
            .currency("PHP")
            .balance(new BigDecimal("10000.00"))
            .institutionName(null)
            .status(AccountStatus.ACTIVE)
            .build();

    when(financialAccountRepository.findByCustomerId(customerId))
            .thenReturn(List.of(account1, account2));

    // When
    List<FinancialAccountResponse> responses =
            financialAccountService.getAccounts(customerId);

    // Then
    assertThat(responses)
            .hasSize(2);

    assertThat(responses.get(0).id())
            .isEqualTo(accountId1);

    assertThat(responses.get(0).customerId())
            .isEqualTo(customerId);

    assertThat(responses.get(0).name())
            .isEqualTo("BPI Savings");

    assertThat(responses.get(1).id())
            .isEqualTo(accountId2);

    assertThat(responses.get(1).customerId())
            .isEqualTo(customerId);

    assertThat(responses.get(1).name())
            .isEqualTo("Cash");
}

@Test
void shouldReturnEmptyListWhenNoAccounts() {
    // Given
    UUID customerId = UUID.randomUUID();

    when(financialAccountRepository.findByCustomerId(customerId))
            .thenReturn(List.of());

    // When
    List<FinancialAccountResponse> responses =
            financialAccountService.getAccounts(customerId);

    // Then
    assertThat(responses)
            .isEmpty();

    verify(financialAccountRepository)
            .findByCustomerId(customerId);
}

@Test
void shouldQueryByCustomerId() {
    // Given
    UUID customerId = UUID.randomUUID();

    when(financialAccountRepository.findByCustomerId(customerId))
            .thenReturn(List.of());

    // When
    financialAccountService.getAccounts(customerId);

    // Then
    verify(financialAccountRepository)
            .findByCustomerId(customerId);

    verify(financialAccountRepository, never())
            .findAll();
}


}