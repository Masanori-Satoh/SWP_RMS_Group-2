package com.group2.rms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO chuẩn hóa kết quả phản hồi chung từ API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDto<T> {

    private boolean success;
    private String message;
    private T data;

    public ApiResponseDto(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
