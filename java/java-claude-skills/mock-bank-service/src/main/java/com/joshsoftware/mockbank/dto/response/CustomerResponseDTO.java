package com.joshsoftware.mockbank.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class CustomerResponseDTO {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String kycStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
