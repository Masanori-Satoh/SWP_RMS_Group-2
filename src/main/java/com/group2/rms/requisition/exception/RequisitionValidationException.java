package com.group2.rms.requisition.exception;

import com.group2.rms.core.exception.BaseBusinessException;

import java.util.Map;
public class RequisitionValidationException extends BaseBusinessException {
    private final Map<String,String> errors;
    // Tạo lỗi kiểm tra dữ liệu yêu cầu.
    public RequisitionValidationException(Map<String,String> errors) { super(String.join(" ", errors.values())); this.errors=errors; }
    // Lấy lỗi kiểm tra theo từng trường.
    public Map<String,String> getErrors() { return errors; }
}
