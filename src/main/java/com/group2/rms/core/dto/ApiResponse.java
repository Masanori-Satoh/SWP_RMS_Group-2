package com.group2.rms.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chuẩn hóa kết quả phản hồi chung từ API cho toàn bộ hệ thống RMS.
 * Tuân thủ ARCHITECTURE_GUIDE.md (không dùng hậu tố Dto).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
