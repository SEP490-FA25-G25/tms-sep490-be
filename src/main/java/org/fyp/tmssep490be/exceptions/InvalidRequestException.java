package org.fyp.tmssep490be.exceptions;

import lombok.Getter;

@Getter
public class InvalidRequestException extends RuntimeException {
    private final String errorCode;

    public InvalidRequestException(String message) {
        super(message);
        this.errorCode = "INVALID_REQUEST";
    }

    public InvalidRequestException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "INVALID_REQUEST";
    }
}
