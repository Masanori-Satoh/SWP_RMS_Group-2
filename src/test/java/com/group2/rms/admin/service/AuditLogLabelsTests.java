package com.group2.rms.admin.service;

import com.group2.rms.admin.dto.AuditLogLabels;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogLabelsTests {
    private AuditLogService.Row row(String entity, String action, String old, String value) {
        return new AuditLogService.Row(null, "Lê Thị Mai Lan", action, entity, "1226", old, value);
    }

    @Test void talentPoolRecommendationExplainsTheSourceAndStatusWithoutChangingRawValues() {
        var row = row("Application", "CREATE", "135", "TalentPoolHrRecommended");
        assertEquals("Tạo mới", row.actionLabel());
        assertEquals("Hồ sơ ứng tuyển", row.entityLabel());
        assertEquals("Hồ sơ nguồn: #135", row.oldContent());
        assertEquals("Trạng thái: HR đã gửi gợi ý", row.newContent());
        assertEquals("135", row.oldValue());
        assertEquals("TalentPoolHrRecommended", row.newValue());
        assertEquals("ML", row.initials());
    }

    @Test void cancellationDescribesTheProposalRatherThanUnmarkingTheCandidate() {
        var row = row("Application", "UPDATE", "HM_Suggested", "TalentPoolCancelled");
        assertEquals("Trạng thái: Chờ xử lý đề xuất", row.oldContent());
        assertEquals("Trạng thái: Đề xuất đã bị hủy", row.newContent());
        assertEquals("danger", row.contentTone());
        assertEquals("Trạng thái: HR đã rút lại gợi ý", row("Application", "UPDATE", "HM_Suggested", "TalentPoolHrWithdrawn").newContent());
    }

    @Test void markAndUnmarkExplainBooleanValues() {
        assertEquals("Tiềm năng: Chưa đánh dấu", row("Candidate", "UPDATE", "false", "TalentPoolMarked").oldContent());
        assertEquals("Tiềm năng: Đã bỏ đánh dấu", row("Candidate", "UPDATE", "true", "TalentPoolUnmarked").newContent());
    }

    @Test void notesAndUnfamiliarContentArePreservedVerbatim() {
        assertEquals("TalentPoolHrRecommended", row("TalentPoolNote", "CREATE", null, "TalentPoolHrRecommended").newContent());
        assertEquals("Mã mới chưa biết", row("Application", "UPDATE", null, "Mã mới chưa biết").newContent());
        assertEquals("Tên: Active", row("Department", "UPDATE", null, "Tên: Active").newContent());
        assertEquals("<script>alert(1)</script>", row("TalentPoolNote", "CREATE", null, "<script>alert(1)</script>").newContent());
        assertNull(row("Department", "CREATE", null, "").oldContent());
    }

    @Test void translatesAccountRolesAndStatusOnlyInTheirOwnFields() {
        var row = row("User", "UPDATE", "Vai trò: Interviewer\nTrạng thái: Active", "Vai trò: HR\nTên: Interviewer");
        assertEquals("Vai trò: Người phỏng vấn\nTrạng thái: Hoạt động", row.oldContent());
        assertEquals("Vai trò: Nhân sự\nTên: Interviewer", row.newContent());
    }

    @Test void emailAndLoginHaveReadableLabelsAndFailureTone() {
        assertEquals("Gửi thư mời phỏng vấn thất bại", row("InterviewSchedule", "UPDATE", null, "MailFailed:InterviewInvitation").newContent());
        assertEquals("danger", row("InterviewSchedule", "UPDATE", null, "MailFailed:InterviewInvitation").contentTone());
        assertEquals("Đăng nhập vào hệ thống", row("User", "LOGIN", null, "User logged in successfully").newContent());
        assertEquals("Đăng nhập", AuditLogLabels.action("LOGIN"));
        assertEquals("CustomEntity", AuditLogLabels.entity("CustomEntity"));
    }
}
