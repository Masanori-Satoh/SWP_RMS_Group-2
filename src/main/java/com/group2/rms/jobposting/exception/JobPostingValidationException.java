package com.group2.rms.jobposting.exception;

import com.group2.rms.core.exception.BaseBusinessException;

import java.util.Map;

/*
 * EXCEPTION: lỗi nghiệp vụ của tin; lỗi chung là một câu, lỗi theo ô là map tên ô → câu lỗi.
 */
public class JobPostingValidationException extends BaseBusinessException {

    private final Map<String, String> fieldErrors;

    // Tạo lỗi kiểm tra tin tuyển dụng.
    public JobPostingValidationException(String message) {
        super(message);
        this.fieldErrors = Map.of();
    }

    // Tạo lỗi kiểm tra tin tuyển dụng.
    public JobPostingValidationException(Map<String, String> fieldErrors) {
        super(String.join(" ", fieldErrors.values()));
        this.fieldErrors = fieldErrors;
    }

    // Lấy lỗi kiểm tra theo từng trường.
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
