package com.ryanbondoc.fintech.transaction.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;

import com.ryanbondoc.fintech.transaction.dto.SpendingCategoryRequest;
import com.ryanbondoc.fintech.transaction.dto.SpendingCategoryResponse;

public interface SpendingCategoryService {
    List<SpendingCategoryResponse> getCategories(Authentication authentication);
    SpendingCategoryResponse createCategory(SpendingCategoryRequest request, Authentication authentication);
    void deleteCategory(UUID categoryId, Authentication authentication);
}
