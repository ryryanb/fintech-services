package com.ryanbondoc.fintech.transaction.repository;

import com.ryanbondoc.fintech.transaction.entity.FinancialTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, UUID> {

    List<FinancialTransaction> findByAccountIdOrderByTransactionDateDesc(UUID accountId);

    List<FinancialTransaction> findByAccountIdInAndTransactionDateGreaterThanEqualOrderByTransactionDateDesc(
            List<UUID> accountIds, OffsetDateTime from);

    java.util.Optional<FinancialTransaction> findByProviderTransactionId(String providerTransactionId);
}
