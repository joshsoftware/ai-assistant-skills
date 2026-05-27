package com.joshsoftware.mockbank.enums;

import lombok.Getter;

@Getter
public enum AccountType {
    SAVINGS("Savings"),
    CURRENT("Current"),
    FIXED_DEPOSIT("Fixed Deposit");

    private final String value;

    AccountType(String value) {
        this.value = value;
    }
}
