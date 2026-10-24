package com.group2.rms.candidate.portal.exception;

import com.group2.rms.core.exception.BaseBusinessException;

/**
 * Vi phạm nghiệp vụ khi ứng viên lưu hồ sơ ({@code /portal/profile}), ví dụ giới tính ngoài danh sách cho phép.
 * {@code field} là ô bị lỗi trên form.
 */
public class CandidateProfileException extends BaseBusinessException {

    private final String field;

    public CandidateProfileException(String field, String message) {
        super(message, "CANDIDATE_PROFILE");
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
