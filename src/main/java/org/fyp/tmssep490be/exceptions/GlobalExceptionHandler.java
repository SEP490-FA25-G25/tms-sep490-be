package org.fyp.tmssep490be.exceptions;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<ResponseObject<Void>> handleRuntimeException(RuntimeException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ResponseObject.error(e.getMessage()));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ResponseObject<Map<String, String>>> handleMethodArgumentNotValidException(
                        MethodArgumentNotValidException e) {
                Map<String, String> errors = e.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .collect(Collectors.toMap(
                                                fieldError -> fieldError.getField(),
                                                fieldError -> fieldError.getDefaultMessage(),
                                                (existing, replacement) -> existing + "; " + replacement));

                // Build a descriptive error message from all validation errors
                String detailedMessage = errors.values().stream()
                                .collect(Collectors.joining("; "));

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ResponseObject.<Map<String, String>>builder()
                                                .success(false)
                                                .message(detailedMessage)
                                                .data(errors)
                                                .build());
        }

        @ExceptionHandler(NullPointerException.class)
        public ResponseEntity<ResponseObject<Void>> handleNullPointerException(NullPointerException e) {
                log.error("NullPointerException occurred: {}", e.getMessage(), e);

                String errorMessage = "Đã xảy ra lỗi giá trị null trong ứng dụng";
                if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                        errorMessage += ": " + e.getMessage();
                }

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ResponseObject.error(errorMessage));
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ResponseObject<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ResponseObject.error(e.getMessage()));
        }

        @ExceptionHandler(CustomException.class)
        public ResponseEntity<ResponseObject<Void>> handleCustomException(CustomException e) {
                boolean isNotFound = e.getErrorCode().name().startsWith("NOT_FOUND_");

                HttpStatus status = isNotFound
                                ? HttpStatus.NOT_FOUND
                                : HttpStatus.BAD_REQUEST;

                return ResponseEntity.status(status)
                                .body(ResponseObject.error(e.getMessage()));
        }

        @ExceptionHandler(EntityNotFoundException.class)
        public ResponseEntity<ResponseObject<Void>> handleEntityNotFoundException(EntityNotFoundException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ResponseObject.error(e.getMessage()));
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ResponseObject<Void>> handleBadCredentialsException(BadCredentialsException e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ResponseObject.error("Tên đăng nhập hoặc mật khẩu không đúng"));
        }

        @ExceptionHandler(UsernameNotFoundException.class)
        public ResponseEntity<ResponseObject<Void>> handleUsernameNotFoundException(UsernameNotFoundException e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ResponseObject.error("Không tìm thấy người dùng"));
        }

        @ExceptionHandler(DisabledException.class)
        public ResponseEntity<ResponseObject<Void>> handleDisabledException(DisabledException e) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(ResponseObject.error("Tài khoản đã bị vô hiệu hóa"));
        }

        @ExceptionHandler(LockedException.class)
        public ResponseEntity<ResponseObject<Void>> handleLockedException(LockedException e) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(ResponseObject.error("Tài khoản đã bị khóa"));
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ResponseObject<Void>> handleAccessDeniedException(AccessDeniedException e) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(ResponseObject.error("Từ chối truy cập - không đủ quyền hạn"));
        }

        @ExceptionHandler(InvalidTokenException.class)
        public ResponseEntity<ResponseObject<Void>> handleInvalidTokenException(InvalidTokenException e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(ResponseObject.error(e.getMessage()));
        }
}
