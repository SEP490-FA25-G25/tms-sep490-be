package org.fyp.tmssep490be.exceptions;

/**
 * Exception thrown when a token is invalid or expired
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
