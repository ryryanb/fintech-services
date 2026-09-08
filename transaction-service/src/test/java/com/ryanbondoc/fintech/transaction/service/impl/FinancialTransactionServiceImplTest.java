package com.ryanbondoc.fintech.transaction.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ryanbondoc.fintech.transaction.dto.TransactionResponse;
import com.ryanbondoc.fintech.transaction.entity.FinancialTransaction;
import com.ryanbondoc.fintech.transaction.entity.TransactionDirection;
import com.ryanbondoc.fintech.transaction.entity.TransactionStatus;
import com.ryanbondoc.fintech.transaction.entity.TransactionType;
import com.ryanbondoc.fintech.transaction.repository.FinancialTransactionRepository;



@ExtendWith(MockitoExtension.class)
class FinancialTransactionServiceImplTest {

    @Mock
    private FinancialTransactionRepository transactionRepository;

    @InjectMocks
    private FinancialTransactionServiceImpl transactionService;

    @Test
void shouldReturnTransactionsForAccount() {
    UUID accountId = UUID.randomUUID();

    FinancialTransaction transaction =
            FinancialTransaction.builder()
                    .id(UUID.randomUUID())
                    .accountId(accountId)
                    .type(TransactionType.PAYMENT)
                    .direction(TransactionDirection.DEBIT)
                    .amount(new BigDecimal("1250.00"))
                    .currency("PHP")
                    .description("Utility payment")
                    .status(TransactionStatus.COMPLETED)
                    .transactionDate(OffsetDateTime.now())
                    .build();

    when(transactionRepository
            .findByAccountIdOrderByTransactionDateDesc(accountId))
            .thenReturn(List.of(transaction));

    List<TransactionResponse> result =
            transactionService.getTransactions(accountId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).accountId()).isEqualTo(accountId);
    assertThat(result.get(0).amount())
            .isEqualByComparingTo("1250.00");
}

@Test
void shouldReturnEmptyListWhenAccountHasNoTransactions() {
    UUID accountId = UUID.randomUUID();

    when(transactionRepository
            .findByAccountIdOrderByTransactionDateDesc(accountId))
            .thenReturn(List.of());

    List<TransactionResponse> result =
            transactionService.getTransactions(accountId);

    assertThat(result).isEmpty();
}

}
