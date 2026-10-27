package com.group2.rms.candidate.application.portal.dto;

import java.util.List;

/**
 * Trang "Đơn của tôi": các thẻ của tab đang chọn (sự kiện sắp tới → đang xử lý → đã kết thúc), số đơn mỗi tab, và các
 * đơn gấp cho banner.
 *
 * @param applications các thẻ của tab {@code tab}
 * @param tab          tab đang chọn: {@code all}, {@code active} (Đang xử lý), {@code ended} (Đã kết thúc)
 * @param allCount     tổng số đơn
 * @param activeCount  số đơn đang xử lý
 * @param endedCount   số đơn đã kết thúc
 * @param urgent       mọi đơn gấp (không phụ thuộc tab), hạn gần nhất trước
 */
public record MyApplicationListResponse(
        List<MyApplicationResponse> applications,
        String tab,
        int allCount,
        int activeCount,
        int endedCount,
        List<MyApplicationResponse> urgent) {
}
