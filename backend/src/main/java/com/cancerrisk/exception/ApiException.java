package com.cancerrisk.exception;

import com.cancerrisk.dto.ErrorResponse.FieldViolation;
import java.util.List;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Base type for errors that map directly to an HTTP status. */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final List<FieldViolation> details;

    public ApiException(HttpStatus status, String message) {
        this(status, message, List.of());
    }

    public ApiException(HttpStatus status, String message, List<FieldViolation> details) {
        super(message);
        this.status = status;
        this.details = details;
    }
}
