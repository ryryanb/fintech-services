package com.ryanbondoc.fintech.account.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ryanbondoc.fintech.account.entity.PlaidConnection;

public interface PlaidConnectionRepository extends JpaRepository<PlaidConnection, UUID> {
    Optional<PlaidConnection> findByItemId(String itemId);
    Optional<PlaidConnection> findByIdAndCustomerId(UUID id, UUID customerId);
    List<PlaidConnection> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
