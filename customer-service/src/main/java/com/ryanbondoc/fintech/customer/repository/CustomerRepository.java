package com.ryanbondoc.fintech.customer.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ryanbondoc.fintech.customer.entity.Customer;
import com.ryanbondoc.fintech.customer.enums.CustomerStatus;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByCustomerNumber(String customerNumber);
    Optional<Customer> findByUserId(UUID userId);
    boolean existsByEmail(String email);
    boolean existsByCustomerNumber(String customerNumber);
    boolean existsByUserId(UUID userId);
    List<Customer> findByStatus(CustomerStatus status);
    boolean existsByEmailAndIdNot(String email, UUID id);
   List<Customer> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
        String firstName,
        String lastName);
}
