package com.ryanbondoc.fintech.transaction.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.ryanbondoc.fintech.transaction.entity.TransactionCategory;
import com.ryanbondoc.fintech.transaction.entity.TransactionDirection;
import com.ryanbondoc.fintech.transaction.entity.TransactionType;

class TransactionCategorizerTest {

    @Test
    void categorizesCommonGroceryMerchants() {
        assertThat(TransactionCategorizer.categorize(
                "SM Supermarket", "Weekly groceries", TransactionType.PAYMENT, TransactionDirection.DEBIT))
                .isEqualTo(TransactionCategory.GROCERIES);
    }

    @Test
    void categorizesUtilitiesFromDescription() {
        assertThat(TransactionCategorizer.categorize(
                "Meralco", "Monthly electric bill", TransactionType.PAYMENT, TransactionDirection.DEBIT))
                .isEqualTo(TransactionCategory.UTILITIES);
    }

    @Test
    void categorizesSalaryByDescription() {
        assertThat(TransactionCategorizer.categorize(
                "Acme Inc.", "Monthly salary", TransactionType.DEPOSIT, TransactionDirection.CREDIT))
                .isEqualTo(TransactionCategory.SALARY);
    }

    @Test
    void categorizesTransfersByTransactionType() {
        assertThat(TransactionCategorizer.categorize(
                "Unknown recipient", "Personal payment", TransactionType.TRANSFER, TransactionDirection.DEBIT))
                .isEqualTo(TransactionCategory.TRANSFER);
    }

    @Test
    void fallsBackToOtherWhenThereIsNotEnoughInformation() {
        assertThat(TransactionCategorizer.categorize(
                "Acme", "Payment", TransactionType.PAYMENT, TransactionDirection.DEBIT))
                .isEqualTo(TransactionCategory.OTHER);
    }
}
