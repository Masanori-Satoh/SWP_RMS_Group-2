package com.group2.rms.candidate.domain.rule;
import com.group2.rms.core.exception.BaseBusinessException;
/** Đơn chưa đủ điều kiện cho thao tác của module khác (vd. xếp lịch phỏng vấn), xem {@link ApplicationStatusService}. */
public class ApplicationStatusException extends BaseBusinessException {
    public ApplicationStatusException(String message) { super(message, "APPLICATION_STATUS_INVALID"); }
}
