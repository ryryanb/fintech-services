package com.ryanbondoc.fintech.transaction.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ryanbondoc.fintech.transaction.dto.SpendingCategoryRequest;
import com.ryanbondoc.fintech.transaction.dto.SpendingCategoryResponse;
import com.ryanbondoc.fintech.transaction.entity.SpendingCategory;
import com.ryanbondoc.fintech.transaction.exception.InvalidSpendingCategoryException;
import com.ryanbondoc.fintech.transaction.repository.SpendingCategoryRepository;
import com.ryanbondoc.fintech.transaction.service.SpendingCategoryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SpendingCategoryServiceImpl implements SpendingCategoryService {
    private final SpendingCategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SpendingCategoryResponse> getCategories(Authentication authentication) {
        UUID customerId = customerId(authentication);
        return categoryRepository.findByCustomerIdOrderByNameAsc(customerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public SpendingCategoryResponse createCategory(SpendingCategoryRequest request, Authentication authentication) {
        UUID customerId = customerId(authentication);
        String name = request.name().trim().replaceAll("\\s+", " ");
        if (name.isBlank()) {
            throw new InvalidSpendingCategoryException("Category name is required");
        }
        if (isBuiltInCategory(name)) {
            throw new InvalidSpendingCategoryException("That name is already available as a built-in category");
        }
        if (categoryRepository.existsByCustomerIdAndNameIgnoreCase(customerId, name)) {
            throw new InvalidSpendingCategoryException("You already have a category with that name");
        }
        return toResponse(categoryRepository.save(new SpendingCategory(customerId, name)));
    }

    @Override
    public void deleteCategory(UUID categoryId, Authentication authentication) {
        UUID customerId = customerId(authentication);
        SpendingCategory category = categoryRepository.findByIdAndCustomerId(categoryId, customerId)
                .orElseThrow(() -> new InvalidSpendingCategoryException("Spending category was not found"));
        categoryRepository.delete(category);
    }

    private boolean isBuiltInCategory(String name) {
        try {
            com.ryanbondoc.fintech.transaction.entity.TransactionCategory.valueOf(
                    name.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private UUID customerId(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new AccessDeniedException("Invalid authentication");
        }
        String value = jwtAuthentication.getToken().getClaimAsString("customerId");
        if (value == null || value.isBlank()) {
            throw new AccessDeniedException("Missing customerId claim");
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException("Invalid customerId claim", exception);
        }
    }

    private SpendingCategoryResponse toResponse(SpendingCategory category) {
        return new SpendingCategoryResponse(category.getId(), category.getName());
    }
}
