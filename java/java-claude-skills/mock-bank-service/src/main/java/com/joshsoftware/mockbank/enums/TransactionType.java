package com.joshsoftware.mockbank.enums;

import lombok.Getter;

@Getter
public enum TransactionType {
    CREDIT("Credit"),
    DEBIT("Debit"),
    TRANSFER("Transfer");

    private final String value;

    TransactionType(String value) {
        this.value = value;
    }
}
