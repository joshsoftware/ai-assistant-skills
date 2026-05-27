package com.joshsoftware.mockbank.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class PaymentResponseDTO {
    private UUID id;
    private String paymentReference;
    private String paymentMode;
    private BigDecimal amount;
    private String status;
    private UUID transactionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
