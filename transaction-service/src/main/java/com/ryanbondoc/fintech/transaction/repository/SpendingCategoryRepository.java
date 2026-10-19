package com.ryanbondoc.fintech.transaction.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ryanbondoc.fintech.transaction.entity.SpendingCategory;

public interface SpendingCategoryRepository extends JpaRepository<SpendingCategory, UUID> {
    List<SpendingCategory> findByCustomerIdOrderByNameAsc(UUID customerId);
    boolean existsByCustomerIdAndNameIgnoreCase(UUID customerId, String name);
    Optional<SpendingCategory> findByIdAndCustomerId(UUID id, UUID customerId);
}
