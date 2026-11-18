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

    @Test
    void doesNotClassifyAnUnidentifiedCreditAsSalary() {
        assertThat(TransactionCategorizer.categorize(
                "Acme", "Incoming transfer", TransactionType.DEPOSIT, TransactionDirection.CREDIT))
                .isEqualTo(TransactionCategory.TRANSFER);
        assertThat(TransactionCategorizer.categorize(
                "Acme", "Cash deposit", TransactionType.DEPOSIT, TransactionDirection.CREDIT))
                .isEqualTo(TransactionCategory.OTHER);
    }

    @Test
    void mapsPlaidFoodAndDrinkCategory() {
        assertThat(TransactionCategorizer.fromProviderCategory("FOOD_AND_DRINK"))
                .isEqualTo(TransactionCategory.DINING);
    }

    @Test
    void mapsPlaidMedicalAndTransportCategories() {
        assertThat(TransactionCategorizer.fromProviderCategory("MEDICAL"))
                .isEqualTo(TransactionCategory.HEALTHCARE);
        assertThat(TransactionCategorizer.fromProviderCategory("TRANSPORTATION"))
                .isEqualTo(TransactionCategory.TRANSPORTATION);
    }

    @Test
    void returnsNullForUnknownProviderCategorySoMerchantRulesCanBeUsed() {
        assertThat(TransactionCategorizer.fromProviderCategory("SOMETHING_NEW")).isNull();
        assertThat(TransactionCategorizer.fromProviderCategory(null)).isNull();
    }

    @Test
    void matchesMerchantPhrasesWithoutMatchingUnrelatedSubstrings() {
        assertThat(TransactionCategorizer.categorize(
                "Shell", "Fuel purchase", TransactionType.PAYMENT, TransactionDirection.DEBIT))
                .isEqualTo(TransactionCategory.TRANSPORTATION);
        assertThat(TransactionCategorizer.categorize(
                "Michelle's Crafts", "Handmade gift", TransactionType.PAYMENT, TransactionDirection.DEBIT))
                .isEqualTo(TransactionCategory.OTHER);
    }
}
