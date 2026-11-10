package com.group2.rms.notification.service;

import com.group2.rms.offer.entity.OfferProposal;
import com.group2.rms.requisition.entity.JobRequisition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/*
 * UNIT TEST: NotificationMessages — nội dung thông báo duyệt / từ chối yêu cầu và Offer không vượt cột Content (1000 ký tự).
 */
class NotificationMessagesTests {

    private static final int CONTENT_LIMIT = 1000;
    private static final LocalDateTime DECIDED_AT = LocalDateTime.of(2026, 11, 2, 9, 0);

    // Yêu cầu có tiêu đề dài tối đa 200 ký tự để thử trường hợp xấu nhất
    private JobRequisition longTitleRequisition() {
        return JobRequisition.builder().requisitionId(3093).title("T".repeat(200)).build();
    }

    @Test
    @DisplayName("Ý kiến ngắn -> Giữ nguyên trong nội dung thông báo")
    void shortComment_shouldKeepText_whenCommentIsShort() {
        assertEquals("Cần bổ sung mức lương", NotificationMessages.shortComment("  Cần bổ sung mức lương  "));
    }

    @Test
    @DisplayName("Ý kiến dài hơn 300 ký tự -> Cắt còn 300 ký tự và thêm dấu …")
    void shortComment_shouldCutAndAddEllipsis_whenCommentIsLong() {
        String result = NotificationMessages.shortComment("x".repeat(301));

        assertEquals("x".repeat(NotificationMessages.COMMENT_PREVIEW_LIMIT) + "…", result);
    }

    @Test
    @DisplayName("Từ chối với ý kiến 1000 ký tự -> Nội dung thông báo vẫn <= 1000 ký tự")
    void requisitionRejectedContent_shouldFitContentColumn_whenCommentIsAtLimit() {
        String content = NotificationMessages.requisitionRejectedContent(longTitleRequisition(), "Giám đốc",
                DECIDED_AT, "x".repeat(1000));

        assertTrue(content.length() <= CONTENT_LIMIT, "Độ dài: " + content.length());
        assertTrue(content.contains("…"));
    }

    @Test
    @DisplayName("TC-NOTIF-DECIDE-02: lý do đã có dấu chấm -> chỉ còn một dấu chấm; lý do toàn dấu câu -> câu mặc định")
    void requisitionRejectedContent_shouldNotDoublePunctuation() {
        String content = NotificationMessages.requisitionRejectedContent(longTitleRequisition(), "Giám đốc",
                DECIDED_AT, "Thiếu ngân sách.");
        assertTrue(content.contains("Lý do: Thiếu ngân sách.\n"), content);
        assertTrue(!content.contains(".."), content);

        String onlyDots = NotificationMessages.requisitionRejectedContent(longTitleRequisition(), "Giám đốc",
                DECIDED_AT, "...");
        assertTrue(onlyDots.contains("Lý do: Không có lý do cụ thể.\n"), onlyDots);
    }

    @Test
    @DisplayName("Phê duyệt với ý kiến 1000 ký tự -> Nội dung thông báo vẫn <= 1000 ký tự")
    void requisitionApprovedContent_shouldFitContentColumn_whenCommentIsAtLimit() {
        String content = NotificationMessages.requisitionApprovedContent(longTitleRequisition(), "Giám đốc",
                DECIDED_AT, "x".repeat(1000));

        assertTrue(content.length() <= CONTENT_LIMIT, "Độ dài: " + content.length());
    }

    // Offer có tên vị trí dài 200 ký tự để thử trường hợp xấu nhất
    private OfferProposal longTitleOffer() {
        return OfferProposal.builder().offerId(7).offeredPositionTitle("P".repeat(200)).build();
    }

    @Test
    @DisplayName("Giám đốc duyệt Offer với ghi chú 1000 ký tự -> Nội dung thông báo vẫn <= 1000 ký tự")
    void offerApprovedContent_shouldFitContentColumn_whenCommentIsAtLimit() {
        String content = NotificationMessages.offerApprovedContent(longTitleOffer(), "Giám đốc", "x".repeat(1000));

        assertTrue(content.length() <= CONTENT_LIMIT, "Độ dài: " + content.length());
    }

    @Test
    @DisplayName("Giám đốc từ chối Offer với lý do 1000 ký tự -> Nội dung thông báo vẫn <= 1000 ký tự")
    void offerRejectedContent_shouldFitContentColumn_whenCommentIsAtLimit() {
        String content = NotificationMessages.offerRejectedContent(longTitleOffer(), "Giám đốc", "x".repeat(1000));

        assertTrue(content.length() <= CONTENT_LIMIT, "Độ dài: " + content.length());
        assertTrue(content.contains("…"));
    }

    @Test
    @DisplayName("Ứng viên từ chối Offer với lý do 1000 ký tự -> Phần lý do bị cắt còn 300 ký tự")
    void declineReasonText_shouldCutReason_whenReasonIsAtLimit() {
        String text = NotificationMessages.declineReasonText("x".repeat(1000));

        assertEquals(" Lý do: \"" + "x".repeat(NotificationMessages.COMMENT_PREVIEW_LIMIT) + "…\".", text);
    }
}
