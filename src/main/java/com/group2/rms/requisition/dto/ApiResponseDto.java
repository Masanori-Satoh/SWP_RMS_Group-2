package com.group2.rms.dto.response;

import com.group2.rms.core.dto.ApiResponse;

/**
 * @deprecated Sử dụng {@link com.group2.rms.core.dto.ApiResponse} theo quy chuẩn ARCHITECTURE_GUIDE.md.
 */
@Deprecated
public class ApiResponseDto<T> extends ApiResponse<T> {

    public ApiResponseDto() {
        super();
    }

    public ApiResponseDto(boolean success, String message) {
        super(success, message);
    }

    public ApiResponseDto(boolean success, String message, T data) {
        super(success, message, data);
    }
}
