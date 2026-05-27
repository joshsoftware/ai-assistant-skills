package com.joshsoftware.mockbank.enums;

import lombok.Getter;

@Getter
public enum PaymentStatus {
    PENDING("Pending"),
    SUCCESS("Success"),
    FAILED("Failed");

    private final String value;

    PaymentStatus(String value) {
        this.value = value;
    }
}
