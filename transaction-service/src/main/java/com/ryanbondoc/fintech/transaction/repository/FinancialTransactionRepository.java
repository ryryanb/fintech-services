package com.ryanbondoc.fintech.transaction.repository;

import com.ryanbondoc.fintech.transaction.entity.FinancialTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FinancialTransactionRepository
        extends JpaRepository<FinancialTransaction, UUID> {

    List<FinancialTransaction> findByAccountIdOrderByTransactionDateDesc(
            UUID accountId
    );
}