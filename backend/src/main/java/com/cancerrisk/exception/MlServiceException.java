package com.cancerrisk.exception;

import org.springframework.http.HttpStatus;

/** Raised when the ML service cannot be reached or keeps failing after retries. */
public class MlServiceException extends ApiException {

    public MlServiceException(HttpStatus status, String message) {
        super(status, message);
    }

    public static MlServiceException unavailable() {
        return new MlServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                "Prediction service is temporarily unavailable, please try again shortly");
    }

    public static MlServiceException rejected(String reason) {
        return new MlServiceException(HttpStatus.UNPROCESSABLE_ENTITY, "Prediction service rejected the input: " + reason);
    }
}
