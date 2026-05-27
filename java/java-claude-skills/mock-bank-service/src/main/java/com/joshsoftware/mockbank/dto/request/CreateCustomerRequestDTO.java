package com.joshsoftware.mockbank.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCustomerRequestDTO {

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    @Size(max = 200, message = "Email must not exceed 200 characters")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[+]?[0-9]{7,15}$", message = "Phone must be a valid number (7-15 digits)")
    private String phone;

    @Size(max = 1000, message = "Address must not exceed 1000 characters")
    private String address;
}
