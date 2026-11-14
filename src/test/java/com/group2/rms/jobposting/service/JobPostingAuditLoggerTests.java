package com.group2.rms.jobposting.service;

import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.admin.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * AuditLogRepository là mock; Clock cố định để kiểm tra đúng thời điểm ghi.
 *
 * Quy ước (docs/members/hoangnh/CLAUDE.md §23): hàm_shouldKếtQuả_whenĐiềuKiện.
 */
@ExtendWith(MockitoExtension.class)
class JobPostingAuditLoggerTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-18T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock
    private AuditLogRepository audit;

    private JobPostingAuditLogger auditLogger() {
        return new JobPostingAuditLogger(audit, new ObjectMapper(), CLOCK);
    }

    private static JobPosting posting() {
        return JobPosting.builder().jobPostingId(7).postingTitle("Java Dev").jobDescription("Mô tả")
                .jobRequirements("Yêu cầu").postingStatus(JobPostingAccess.STATUS_DRAFT).build();
    }

    @Test
    @DisplayName("Tạm dừng ghi một dòng JSON trạng thái với người thực hiện và thời điểm")
    void logStatusChange_shouldSaveOnlyStatus() throws Exception {
        // Arrange
        User hr = User.builder().userId(1).build();

        // Act
        JobPosting posting = posting();
        posting.setPostingStatus(JobPostingAccess.STATUS_PAUSED);
        auditLogger().logStatusChange(posting, hr, JobPostingAuditLogger.ACTION_UNPUBLISH,
                JobPostingAccess.STATUS_PUBLISHED);

        // Assert
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(audit).save(captor.capture());
        AuditLog log = captor.getValue();
        assertEquals(JobPostingAuditLogger.ENTITY_NAME, log.getEntityName());
        assertEquals("7", log.getEntityId());
        assertEquals(hr, log.getUser());
        assertEquals(JobPostingAuditLogger.ACTION_UNPUBLISH, log.getAction());
        assertEquals("Published", new ObjectMapper().readTree(log.getOldValue()).get("Status").asText());
        assertEquals("Paused", new ObjectMapper().readTree(log.getNewValue()).get("Status").asText());
        assertEquals(NOW, log.getTimestamp());
    }

    @Test
    @DisplayName("Đổi tiêu đề -> OldValue và NewValue chỉ có Title")
    void logChanges_shouldStoreOnlyChangedFields() throws Exception {
        // Arrange
        JobPosting posting = posting();
        Map<String, String> before = auditLogger().snapshot(posting);
        posting.setPostingTitle("Java Dev (Spring)");
        Map<String, String> after = auditLogger().snapshot(posting);
        List<String> changed = FieldChanges.changedKeys(before, after);

        // Act
        auditLogger().logChanges(posting, null, JobPostingAuditLogger.ACTION_UPDATE, changed, before, after);

        // Assert
        assertEquals(List.of("Title"), changed);
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(audit).save(captor.capture());
        assertEquals("Java Dev", new ObjectMapper().readTree(captor.getValue().getOldValue()).get("Title").asText());
        assertEquals("Java Dev (Spring)", new ObjectMapper().readTree(captor.getValue().getNewValue()).get("Title").asText());
        assertEquals(1, new ObjectMapper().readTree(captor.getValue().getNewValue()).size());
        assertTrue(before.containsKey("ExpiresAt"));
    }

    @Test
    @DisplayName("Tạo tin lưu JSON nội dung, OldValue NULL; số lượng là số")
    void logCreate_shouldStoreJsonSnapshot() throws Exception {
        JobPosting posting = posting();
        posting.setNumberOfPositionsDisplay(2);
        auditLogger().logCreate(posting, User.builder().userId(1).build());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(audit).save(captor.capture());
        AuditLog log = captor.getValue();
        assertEquals(JobPostingAuditLogger.ACTION_CREATE, log.getAction());
        assertEquals(null, log.getOldValue());
        assertEquals(2, new ObjectMapper().readTree(log.getNewValue()).get("NumberOfPositions").asInt());
        assertEquals("Java Dev", new ObjectMapper().readTree(log.getNewValue()).get("Title").asText());
    }

    @Test
    @DisplayName("Trường dài đổi thành NULL vẫn nằm trong JSON thay đổi")
    void logChanges_shouldKeepNullAndLongField() throws Exception {
        JobPosting posting = posting();
        Map<String, String> before = auditLogger().snapshot(posting);
        posting.setJobDescription(null);
        Map<String, String> after = auditLogger().snapshot(posting);

        auditLogger().logChanges(posting, User.builder().userId(1).build(),
                JobPostingAuditLogger.ACTION_UPDATE, FieldChanges.changedKeys(before, after), before, after);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(audit).save(captor.capture());
        assertEquals("Mô tả", new ObjectMapper().readTree(captor.getValue().getOldValue())
                .get("JobDescription").asText());
        assertTrue(new ObjectMapper().readTree(captor.getValue().getNewValue())
                .get("JobDescription").isNull());
    }
}
