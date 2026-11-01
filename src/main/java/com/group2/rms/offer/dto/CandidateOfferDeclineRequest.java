package com.group2.rms.offer.dto;

import jakarta.validation.constraints.Size;

/**
 * Yêu cầu từ chối Thư mời nhận việc từ phía ứng viên.
 * Hạn chế độ dài tối đa 1000 ký tự khớp với trường DeclineReason trong CSDL để tránh lỗi DataTruncation.
 */
public record CandidateOfferDeclineRequest(
        @Size(max = 1000, message = "Lý do từ chối không được vượt quá 1000 ký tự.")
        String declineReason,
        Boolean fromDetail
) {
    public CandidateOfferDeclineRequest {
        if (fromDetail == null) {
            fromDetail = Boolean.FALSE;
        }
    }

    public CandidateOfferDeclineRequest(String declineReason) {
        this(declineReason, false);
    }

    public boolean isFromDetail() {
        return Boolean.TRUE.equals(fromDetail);
    }
}
