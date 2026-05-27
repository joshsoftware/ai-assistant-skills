package com.joshsoftware.mockbank.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class AccountResponseDTO {
    private UUID id;
    private String accountNumber;
    private String accountType;
    private BigDecimal balance;
    private String currency;
    private String status;
    private UUID customerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
