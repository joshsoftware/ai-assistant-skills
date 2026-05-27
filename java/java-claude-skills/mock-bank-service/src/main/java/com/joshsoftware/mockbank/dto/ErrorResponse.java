package com.joshsoftware.mockbank.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String message;
    private List<FieldError> errors;
    private LocalDateTime timestamp;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String message;
    }

    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message, null, LocalDateTime.now());
    }

    public static ErrorResponse withFieldErrors(int status, String message, List<FieldError> errors) {
        return new ErrorResponse(status, message, errors, LocalDateTime.now());
    }
}
