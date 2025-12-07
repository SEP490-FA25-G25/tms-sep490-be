package org.fyp.tmssep490be.exceptions;

import lombok.Getter;

@Getter
public class BusinessRuleException extends RuntimeException {
    private final String errorCode;

    public BusinessRuleException(String message) {
        super(message);
        this.errorCode = "BUSINESS_RULE_VIOLATION";
    }
    public BusinessRuleException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    public BusinessRuleException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "BUSINESS_RULE_VIOLATION";
    }
}