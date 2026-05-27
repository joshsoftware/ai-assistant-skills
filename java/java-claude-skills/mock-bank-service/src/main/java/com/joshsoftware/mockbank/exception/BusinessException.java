package com.joshsoftware.mockbank.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends AppException {

    public BusinessException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
