package com.joshsoftware.mockbank.enums;

import lombok.Getter;

@Getter
public enum AccountStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    FROZEN("Frozen");

    private final String value;

    AccountStatus(String value) {
        this.value = value;
    }
}
