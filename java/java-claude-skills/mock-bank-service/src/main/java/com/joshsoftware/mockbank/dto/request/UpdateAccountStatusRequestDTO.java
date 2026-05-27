package com.joshsoftware.mockbank.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAccountStatusRequestDTO {

    @NotBlank(message = "Account status is required")
    private String status;
}
