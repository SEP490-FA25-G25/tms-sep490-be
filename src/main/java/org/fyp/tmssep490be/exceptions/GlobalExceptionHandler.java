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

import java.util.HashMap;
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
                        (existing, replacement) -> existing + "; " + replacement
                ));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseObject.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation failed")
                        .data(errors)
                        .build());
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ResponseObject<Void>> handleNullPointerException(NullPointerException e) {
        // Log full stack trace for debugging
        log.error("NullPointerException occurred: {}", e.getMessage(), e);
        
        // Return user-friendly error message
        String errorMessage = "Đã gặp giá trị null trong ứng dụng";
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
        // Map NOT_FOUND error codes to 404
        HttpStatus status = isNotFoundError(e.getErrorCode()) 
            ? HttpStatus.NOT_FOUND 
            : HttpStatus.BAD_REQUEST;
        
        return ResponseEntity.status(status)
                .body(ResponseObject.error(e.getMessage()));
    }
    
    /**
     * Check if ErrorCode represents a NOT_FOUND scenario
     */
    private boolean isNotFoundError(ErrorCode errorCode) {
        return errorCode == ErrorCode.CLASS_NOT_FOUND 
            || errorCode == ErrorCode.RESOURCE_NOT_FOUND
            || errorCode == ErrorCode.TEACHER_NOT_FOUND
            || errorCode == ErrorCode.SESSION_NOT_FOUND
            || errorCode == ErrorCode.STUDENT_NOT_FOUND
            || errorCode == ErrorCode.CENTER_NOT_FOUND
            || errorCode == ErrorCode.COURSE_NOT_FOUND
            || errorCode == ErrorCode.SUBJECT_NOT_FOUND
            || errorCode == ErrorCode.USER_NOT_FOUND;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ResponseObject<Void>> handleEntityNotFoundException(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseObject.error(e.getMessage()));
    }

    // Authentication and Security Exceptions

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ResponseObject<Void>> handleBadCredentialsException(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseObject.error("Tên đăng nhập hoặc mật khẩu không hợp lệ"));
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
                .body(ResponseObject.error("Truy cập bị từ chối - không đủ quyền hạn"));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ResponseObject<Void>> handleInvalidTokenException(InvalidTokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ResponseObject.error(e.getMessage()));
    }

    // Student Request Business Rule Exceptions

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ResponseObject<Void>> handleBusinessRuleException(BusinessRuleException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseObject.error(e.getMessage()));
    }

    @ExceptionHandler(DuplicateRequestException.class)
    public ResponseEntity<ResponseObject<Void>> handleDuplicateRequestException(DuplicateRequestException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ResponseObject.error(e.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ResponseObject<Void>> handleResourceNotFoundException(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseObject.error(e.getMessage()));
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ResponseObject<Void>> handleUnsupportedOperationException(UnsupportedOperationException e) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(ResponseObject.error(e.getMessage()));
    }
}
