package com.group2.rms.candidate.talentpool.exception;
import com.group2.rms.core.exception.BaseBusinessException;
public class RecruitmentEmailException extends BaseBusinessException {
    public RecruitmentEmailException() {
        super("Đã lưu dữ liệu nhưng hệ thống gửi mail đang gián đoạn. HR có thể bấm Gửi lại Email.", "RECRUITMENT_EMAIL_FAILED");
    }
}

