package com.group2.rms.admin.exception;

import com.group2.rms.core.exception.BaseBusinessException;

/* File Admin tải lên không hợp lệ (trống, quá lớn, không phải PDF); câu lỗi hiện cạnh ô chọn file. */
public class PolicyDocumentException extends BaseBusinessException {

    public PolicyDocumentException(String message) {
        super(message, "POLICY_DOCUMENT_INVALID");
    }
}
