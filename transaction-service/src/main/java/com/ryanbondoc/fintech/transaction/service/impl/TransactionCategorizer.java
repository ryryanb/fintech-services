package com.ryanbondoc.fintech.transaction.service.impl;

import java.util.Locale;

import com.ryanbondoc.fintech.transaction.entity.TransactionCategory;
import com.ryanbondoc.fintech.transaction.entity.TransactionDirection;
import com.ryanbondoc.fintech.transaction.entity.TransactionType;

/**
 * Conservative, deterministic categorization for transactions without a
 * user-selected category. Explicit user choices always take precedence.
 */
public final class TransactionCategorizer {

    private TransactionCategorizer() {
    }

    public static TransactionCategory categorize(
            String merchant,
            String description,
            TransactionType type,
            TransactionDirection direction) {

        String text = ((merchant == null ? "" : merchant) + " "
                + (description == null ? "" : description))
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (type == TransactionType.TRANSFER || containsAny(text, "transfer", "remittance", "wise", "remitly", "gcash send")) {
            return TransactionCategory.TRANSFER;
        }
        if (containsAny(text, "grocery", "groceries", "supermarket", "market basket", "puregold", "sm supermarket", "robinsons supermarket", "savemore")) {
            return TransactionCategory.GROCERIES;
        }
        if (containsAny(text, "restaurant", "cafe", "coffee", "mcdonald", "jollibee", "foodpanda", "grabfood", "dining", "pizza", "kfc", "starbucks")) {
            return TransactionCategory.DINING;
        }
        if (containsAny(text, "grab ride", "grabcar", "angkas", "joyride", "taxi", "bus fare", "train fare", "lrt", "mrt", "jeepney", "fuel", "gas station", "shell", "petron")) {
            return TransactionCategory.TRANSPORTATION;
        }
        if (containsAny(text, "electric bill", "electricity", "meralco", "water bill", "maynilad", "manila water", "internet bill", "wifi bill", "utility")) {
            return TransactionCategory.UTILITIES;
        }
        if (containsAny(text, "rent", "landlord", "condominium dues", "mortgage", "housing")) {
            return TransactionCategory.HOUSING;
        }
        if (containsAny(text, "hospital", "clinic", "pharmacy", "medicine", "medical", "healthcare", "doctor", "dental")) {
            return TransactionCategory.HEALTHCARE;
        }
        if (containsAny(text, "airline", "airlines", "hotel", "booking.com", "airbnb", "flight", "travel", "resort")) {
            return TransactionCategory.TRAVEL;
        }
        if (containsAny(text, "netflix", "spotify", "cinema", "movie", "steam games", "concert", "entertainment", "youtube premium")) {
            return TransactionCategory.ENTERTAINMENT;
        }
        if (containsAny(text, "salary", "payroll", "payday", "wages", "monthly pay")) {
            return TransactionCategory.SALARY;
        }
        if (containsAny(text, "bank fee", "service fee", "late fee", "interest charge", "transaction fee", "annual fee")) {
            return TransactionCategory.FEES;
        }
        if (containsAny(text, "shopee", "lazada", "mall", "department store", "clothing", "apparel", "shopping")) {
            return TransactionCategory.SHOPPING;
        }
        if (type == TransactionType.DEPOSIT && direction == TransactionDirection.CREDIT) {
            return TransactionCategory.SALARY;
        }
        return TransactionCategory.OTHER;
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
