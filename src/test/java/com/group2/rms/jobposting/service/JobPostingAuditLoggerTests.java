package com.group2.rms.jobposting.service;

import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.admin.repository.AuditLogRepository;
import com.group2.rms.core.util.FieldChanges;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

/*
 * UNIT TEST: JobPostingAuditLogger (ghi audit log và "ảnh chụp" trước / sau khi sửa tin).
 */
@ExtendWith(MockitoExtension.class)
class JobPostingAuditLoggerTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-18T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock
    private AuditLogRepository audit;

    private JobPostingAuditLogger auditLogger() {
        return new JobPostingAuditLogger(audit, CLOCK);
    }

    private static JobPosting posting() {
        return JobPosting.builder().jobPostingId(7).postingTitle("Java Dev").jobDescription("Mô tả")
                .jobRequirements("Yêu cầu").postingStatus(JobPostingAccess.STATUS_DRAFT).build();
    }

    @Test
    @DisplayName("Ghi một dòng -> EntityName 'JobPosting', id dạng chữ, mô tả kèm tiêu đề, thời điểm theo Clock")
    void log_shouldSaveDescriptionWithTitle() {
        // Arrange
        User hr = User.builder().userId(1).build();

        // Act
        auditLogger().log(posting(), hr, JobPostingAuditLogger.ACTION_UPDATE, JobPostingLabels.LOG_PAUSE);

        // Assert
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(audit).save(captor.capture());
        AuditLog log = captor.getValue();
        assertEquals(JobPostingAuditLogger.ENTITY_NAME, log.getEntityName());
        assertEquals("7", log.getEntityId());
        assertEquals(JobPostingLabels.LOG_PAUSE + ": Java Dev", log.getNewValue());
        assertEquals(NOW, log.getTimestamp());
    }

    @Test
    @DisplayName("Đổi tiêu đề -> Snapshot chỉ khác ở 'Tiêu đề tin'; audit ghi 'Tiêu đề tin: cũ → mới', ô trống hiện 'Chưa xác định'")
    void logChanges_shouldDescribeOnlyChangedFields() {
        // Arrange
        JobPosting posting = posting();
        Map<String, String> before = auditLogger().snapshot(posting);
        posting.setPostingTitle("Java Dev (Spring)");
        Map<String, String> after = auditLogger().snapshot(posting);
        List<String> changed = FieldChanges.changedKeys(before, after);

        // Act
        auditLogger().logChanges(posting, null, JobPostingLabels.LOG_UPDATE, changed, before, after);

        // Assert
        assertEquals(List.of("Tiêu đề tin"), changed);
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(audit).save(captor.capture());
        assertEquals(JobPostingLabels.LOG_UPDATE + "\nTiêu đề tin: Java Dev → Java Dev (Spring)", captor.getValue().getNewValue());
        assertTrue(before.containsKey("Ngày đăng"));
        assertEquals(FieldChanges.EMPTY_VALUE, FieldChanges.display(before.get("Ngày đăng")));
    }
}
