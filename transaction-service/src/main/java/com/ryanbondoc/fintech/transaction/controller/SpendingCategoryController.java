package com.ryanbondoc.fintech.transaction.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ryanbondoc.fintech.transaction.dto.SpendingCategoryRequest;
import com.ryanbondoc.fintech.transaction.dto.SpendingCategoryResponse;
import com.ryanbondoc.fintech.transaction.service.SpendingCategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class SpendingCategoryController {
    private final SpendingCategoryService categoryService;

    @GetMapping
    public List<SpendingCategoryResponse> getCategories(Authentication authentication) {
        return categoryService.getCategories(authentication);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpendingCategoryResponse createCategory(
            @Valid @RequestBody SpendingCategoryRequest request,
            Authentication authentication) {
        return categoryService.createCategory(request, authentication);
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable UUID categoryId, Authentication authentication) {
        categoryService.deleteCategory(categoryId, authentication);
    }
}
