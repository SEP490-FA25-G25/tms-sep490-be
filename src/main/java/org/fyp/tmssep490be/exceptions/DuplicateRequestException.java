package org.fyp.tmssep490be.exceptions;

/**
 * Exception thrown when a duplicate request is detected
 */
public class DuplicateRequestException extends BusinessRuleException {

    public DuplicateRequestException(String message) {
        super("DUPLICATE_REQUEST", message);
    }
}