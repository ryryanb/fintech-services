package com.ryanbondoc.fintech.account.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ryanbondoc.fintech.account.entity.FinancialAccount;

public interface FinancialAccountRepository
        extends JpaRepository<FinancialAccount, UUID> {
}
