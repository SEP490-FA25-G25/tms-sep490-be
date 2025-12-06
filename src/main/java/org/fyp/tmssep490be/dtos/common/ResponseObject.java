package org.fyp.tmssep490be.dtos.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseObject<T> {

    private boolean success;
    private String message;
    private T data;

    public static <T> ResponseObject<T> success(String message, T data) {
        return ResponseObject.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ResponseObject<T> success(T data) {
        return success("Operation successful", data);
    }

    public static <T> ResponseObject<T> error(String message) {
        return ResponseObject.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}