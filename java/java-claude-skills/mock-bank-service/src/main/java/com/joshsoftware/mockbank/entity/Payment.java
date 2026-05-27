package com.joshsoftware.mockbank.entity;

/**
 * author : Apurva Rawal
 **/

import com.joshsoftware.mockbank.enums.PaymentMode;
import com.joshsoftware.mockbank.enums.PaymentStatus;
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
@ToString(exclude = "transaction")
@Entity
@Table(
    name = "payments",
    indexes = {
        @Index(name = "idx_payment_reference", columnList = "paymentReference", unique = true)
    }
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String paymentReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentMode paymentMode;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(updatable = false, length = 100)
    private String createdBy;

    @Column(length = 100)
    private String updatedBy;
}
