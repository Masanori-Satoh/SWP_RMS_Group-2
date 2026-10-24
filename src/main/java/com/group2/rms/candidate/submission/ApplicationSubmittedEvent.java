package com.group2.rms.candidate.submission;

import com.group2.rms.candidate.screening.AiScreeningListener;

/**
 * Phát ra khi một đơn ứng tuyển vừa được lưu. {@link AiScreeningListener} nghe sự kiện này
 * sau khi transaction commit để chấm đơn ở luồng nền.
 */
public record ApplicationSubmittedEvent(Integer applicationId) {
}
