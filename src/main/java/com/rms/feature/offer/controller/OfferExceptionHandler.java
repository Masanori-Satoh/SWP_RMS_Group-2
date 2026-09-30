package com.rms.feature.offer.controller;

import com.rms.feature.offer.dto.ApiResponseDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler dành riêng cho module Offer.
 */
@RestControllerAdvice(basePackages = "com.rms.feature.offer")
public class OfferExceptionHandler {

    /**
     * Xử lý lỗi không tìm thấy thực thể (Application, OfferProposal, User,...).
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleEntityNotFoundException(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDTO.error(ex.getMessage()));
    }

    /**
     * Xử lý lỗi dữ liệu không hợp lệ / vi phạm nghiệp vụ (như BR-OFF-01: lương thử việc < 85%).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDTO.error(ex.getMessage()));
    }

    /**
     * Xử lý lỗi xung đột trạng thái (như GBR-14: đã có offer active, hoặc offer chưa approved).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleIllegalStateException(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponseDTO.error(ex.getMessage()));
    }

    /**
     * Xử lý lỗi vi phạm validation (@Valid, @NotNull, @NotBlank, @Positive,...).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDTO.error("Dữ liệu gửi lên không hợp lệ. Vui lòng kiểm tra lại các trường.", errors));
    }

    /**
     * Xử lý lỗi không đủ quyền hạn (Forbidden).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponseDTO.error("Bạn không có quyền thực hiện thao tác này."));
    }

    /**
     * Xử lý lỗi trùng lặp dữ liệu từ Database (Unique constraint violation).
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleDataIntegrityViolationException(
            org.springframework.dao.DataIntegrityViolationException ex) {
        String msg = "Hồ sơ ứng tuyển này đã tồn tại đề xuất Offer trên hệ thống. Vui lòng đổi sang mã hồ sơ ứng tuyển khác.";
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponseDTO.error(msg));
    }

    /**
     * Xử lý các ngoại lệ không mong muốn khác.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO.error("Đã xảy ra lỗi hệ thống: " + ex.getMessage()));
    }
}
