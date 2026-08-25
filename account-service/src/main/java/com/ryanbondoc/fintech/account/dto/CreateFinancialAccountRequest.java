package com.ryanbondoc.fintech.account.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.ryanbondoc.fintech.account.enums.AccountType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateFinancialAccountRequest(

    @NotNull
    UUID customerId,

    @NotBlank
    @Size(max = 100)
    String name,

    @NotNull
    AccountType type,

    @NotBlank
    @Pattern(regexp = "^[A-Za-z]{3}$")
    String currency,

    @NotNull
    @DecimalMin(value = "0.0")
    BigDecimal balance,

    @Size(max = 100)
    String institutionName
) {
}
