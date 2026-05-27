package com.joshsoftware.mockbank.entity;

/**
 * author : Apurva Rawal
 **/

import com.joshsoftware.mockbank.enums.AccountStatus;
import com.joshsoftware.mockbank.enums.AccountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "customer")
@Entity
@Table(
    name = "accounts",
    indexes = {
        @Index(name = "idx_account_number", columnList = "accountNumber", unique = true),
        @Index(name = "idx_account_customer", columnList = "customer_id")
    }
)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType accountType;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(updatable = false, length = 100)
    private String createdBy;

    @Column(length = 100)
    private String updatedBy;
}
