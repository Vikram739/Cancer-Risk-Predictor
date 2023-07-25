package com.cancerrisk.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldViolation> details) {

    public record FieldViolation(String field, String message) {
    }
}
