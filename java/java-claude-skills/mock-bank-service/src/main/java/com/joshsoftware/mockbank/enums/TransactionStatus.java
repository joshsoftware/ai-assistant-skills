package com.joshsoftware.mockbank.enums;

import lombok.Getter;

@Getter
public enum TransactionStatus {
    PENDING("Pending"),
    SUCCESS("Success"),
    FAILED("Failed");

    private final String value;

    TransactionStatus(String value) {
        this.value = value;
    }
}
