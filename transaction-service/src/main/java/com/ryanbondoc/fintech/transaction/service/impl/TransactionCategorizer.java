package com.ryanbondoc.fintech.transaction.service.impl;

import java.util.Locale;
import java.util.regex.Pattern;

import com.ryanbondoc.fintech.transaction.entity.TransactionCategory;
import com.ryanbondoc.fintech.transaction.entity.TransactionDirection;
import com.ryanbondoc.fintech.transaction.entity.TransactionType;

/**
 * Deterministic, conservative categorization. Explicit categories are resolved
 * by the service before this class is called; unknown transactions remain OTHER.
 */
public final class TransactionCategorizer {

    private TransactionCategorizer() {
    }

    public static TransactionCategory categorize(
            String merchant,
            String description,
            TransactionType type,
            TransactionDirection direction) {

        String text = normalize((merchant == null ? "" : merchant) + " "
                + (description == null ? "" : description));

        if (type == TransactionType.TRANSFER
                || matches(text, "transfer", "remittance", "wise", "remitly", "gcash send")) {
            return TransactionCategory.TRANSFER;
        }
        if (matches(text, "grocery", "groceries", "supermarket", "puregold",
                "sm supermarket", "robinsons supermarket", "savemore", "market basket")) {
            return TransactionCategory.GROCERIES;
        }
        if (matches(text, "restaurant", "cafe", "coffee", "mcdonald", "jollibee",
                "foodpanda", "grabfood", "dining", "pizza", "kfc", "starbucks")) {
            return TransactionCategory.DINING;
        }
        if (matches(text, "grab ride", "grabcar", "angkas", "joyride", "taxi",
                "bus fare", "train fare", "lrt", "mrt", "jeepney", "fuel",
                "gas station", "shell", "petron")) {
            return TransactionCategory.TRANSPORTATION;
        }
        if (matches(text, "electric bill", "electricity", "meralco", "water bill",
                "maynilad", "manila water", "internet bill", "wifi bill", "utility")) {
            return TransactionCategory.UTILITIES;
        }
        if (matches(text, "rent", "landlord", "condominium dues", "mortgage", "housing")) {
            return TransactionCategory.HOUSING;
        }
        if (matches(text, "hospital", "clinic", "pharmacy", "medicine", "medical",
                "healthcare", "doctor", "dental")) {
            return TransactionCategory.HEALTHCARE;
        }
        if (matches(text, "airline", "airlines", "hotel", "booking.com", "airbnb",
                "flight", "travel", "resort")) {
            return TransactionCategory.TRAVEL;
        }
        if (matches(text, "netflix", "spotify", "cinema", "movie", "steam games",
                "concert", "entertainment", "youtube premium")) {
            return TransactionCategory.ENTERTAINMENT;
        }
        if (matches(text, "salary", "payroll", "payday", "wages", "monthly pay")) {
            return TransactionCategory.SALARY;
        }
        if (matches(text, "bank fee", "service fee", "late fee", "interest charge",
                "transaction fee", "annual fee")) {
            return TransactionCategory.FEES;
        }
        if (matches(text, "shopee", "lazada", "mall", "department store",
                "clothing", "apparel", "shopping")) {
            return TransactionCategory.SHOPPING;
        }
        return TransactionCategory.OTHER;
    }

    /**
     * Maps Plaid's Personal Finance Category primary taxonomy to this app's
     * smaller category vocabulary. Returns null for unknown provider values.
     */
    public static TransactionCategory fromProviderCategory(String providerCategory) {
        if (providerCategory == null || providerCategory.isBlank()) {
            return null;
        }
        return switch (providerCategory.trim().toUpperCase(Locale.ROOT)) {
            case "FOOD_AND_DRINK" -> TransactionCategory.DINING;
            case "GENERAL_MERCHANDISE" -> TransactionCategory.SHOPPING;
            case "TRANSPORTATION" -> TransactionCategory.TRANSPORTATION;
            case "TRAVEL" -> TransactionCategory.TRAVEL;
            case "ENTERTAINMENT" -> TransactionCategory.ENTERTAINMENT;
            case "MEDICAL" -> TransactionCategory.HEALTHCARE;
            case "RENT_AND_UTILITIES" -> TransactionCategory.UTILITIES;
            case "INCOME" -> TransactionCategory.SALARY;
            case "TRANSFER_IN", "TRANSFER_OUT" -> TransactionCategory.TRANSFER;
            case "LOAN_PAYMENTS" -> TransactionCategory.FEES;
            default -> null;
        };
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean matches(String text, String... phrases) {
        for (String phrase : phrases) {
            String normalizedPhrase = normalize(phrase);
            if (Pattern.compile("(^| )" + Pattern.quote(normalizedPhrase) + "( |$)")
                    .matcher(text).find()) {
                return true;
            }
        }
        return false;
    }
}
