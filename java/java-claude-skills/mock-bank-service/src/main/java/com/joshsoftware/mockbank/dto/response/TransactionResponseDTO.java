package com.joshsoftware.mockbank.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class TransactionResponseDTO {
    private UUID id;
    private String transactionId;
    private String type;
    private BigDecimal amount;
    private String description;
    private String status;
    private UUID fromAccountId;
    private UUID toAccountId;
    private LocalDateTime timestamp;
    private LocalDateTime createdAt;
}
