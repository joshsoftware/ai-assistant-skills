package com.joshsoftware.mockbank.enums;

import lombok.Getter;

@Getter
public enum KycStatus {
    PENDING("Pending"),
    VERIFIED("Verified"),
    REJECTED("Rejected"),
    UNDER_REVIEW("Under Review");

    private final String value;

    KycStatus(String value) {
        this.value = value;
    }
}
