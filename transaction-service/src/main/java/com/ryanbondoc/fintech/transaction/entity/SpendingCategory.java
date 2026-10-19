package com.ryanbondoc.fintech.transaction.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "spending_categories", uniqueConstraints = {
        @UniqueConstraint(name = "uk_spending_categories_customer_name", columnNames = {"customer_id", "name"})
})
@Getter
@Setter
@NoArgsConstructor
public class SpendingCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false, length = 60)
    private String name;

    public SpendingCategory(UUID customerId, String name) {
        this.customerId = customerId;
        this.name = name;
    }
}
