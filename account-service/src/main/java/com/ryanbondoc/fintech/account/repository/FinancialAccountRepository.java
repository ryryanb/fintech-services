package com.ryanbondoc.fintech.account.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ryanbondoc.fintech.account.entity.FinancialAccount;

public interface FinancialAccountRepository
        extends JpaRepository<FinancialAccount, UUID> {

    List<FinancialAccount> findByCustomerId(UUID customerId);

    Optional<FinancialAccount> findByIdAndCustomerId(UUID accountId, UUID customerId);

    boolean existsByIdAndCustomerId(UUID accountId, UUID customerId);
}
