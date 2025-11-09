package org.fyp.tmssep490be.exceptions;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus
@Getter
@Setter
public class CustomException extends RuntimeException {
    private ErrorCode errorCode;
    private String message;

    public CustomException() {
    }

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.message = errorCode.getMessage();
    }

}
