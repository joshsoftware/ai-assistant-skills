package com.joshsoftware.mockbank.enums;

import lombok.Getter;

@Getter
public enum PaymentMode {
    UPI("UPI"),
    NEFT("NEFT"),
    IMPS("IMPS"),
    RTGS("RTGS");

    private final String value;

    PaymentMode(String value) {
        this.value = value;
    }
}
