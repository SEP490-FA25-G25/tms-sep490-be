package org.fyp.tmssep490be.exceptions;

import lombok.Getter;

@Getter
public enum ErrorCode {
    CLASS_NOT_FOUND(4000, "Không tìm thấy lớp học"),;

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
