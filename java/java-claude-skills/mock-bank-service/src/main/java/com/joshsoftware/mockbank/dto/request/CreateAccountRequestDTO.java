package com.joshsoftware.mockbank.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class CreateAccountRequestDTO {

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotBlank(message = "Account type is required")
    private String accountType;

    @NotNull(message = "Initial balance is required")
    @Positive(message = "Balance must be a positive value")
    private BigDecimal balance;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    private String currency;
}
