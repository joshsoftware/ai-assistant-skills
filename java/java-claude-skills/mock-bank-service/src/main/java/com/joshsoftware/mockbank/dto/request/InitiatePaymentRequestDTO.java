package com.joshsoftware.mockbank.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class InitiatePaymentRequestDTO {

    @NotNull(message = "From account ID is required")
    private UUID fromAccountId;

    @NotNull(message = "To account ID is required")
    private UUID toAccountId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be a positive value")
    private BigDecimal amount;

    @NotBlank(message = "Payment mode is required")
    private String paymentMode;

    private String description;
}
