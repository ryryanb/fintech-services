package com.ryanbondoc.fintech.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SpendingCategoryRequest(
        @NotBlank(message = "Category name is required")
        @Size(max = 60, message = "Category name must be 60 characters or fewer")
        String name
) {
}
