package com.cancerrisk.exception;

import com.cancerrisk.dto.ErrorResponse.FieldViolation;
import java.util.List;
import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

    public BadRequestException(String message, List<FieldViolation> details) {
        super(HttpStatus.BAD_REQUEST, message, details);
    }
}
