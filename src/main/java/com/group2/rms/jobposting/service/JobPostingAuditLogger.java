package com.group2.rms.jobposting.service;

import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.admin.repository.AuditLogRepository;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.user.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * AUDIT: mỗi thao tác ghi một dòng; OldValue/NewValue là JSON các trường thay đổi.
 */
@Component
@RequiredArgsConstructor
public class JobPostingAuditLogger {

    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";
    public static final String ACTION_PUBLISH = "PUBLISH";
    public static final String ACTION_UNPUBLISH = "UNPUBLISH";
    public static final String ACTION_RESTORE = "RESTORE";
    /** Tên entity trong cột AuditLog.EntityName. */
    public static final String ENTITY_NAME = "JobPosting";

    private final AuditLogRepository audit;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public void logCreate(JobPosting posting, User actor) {
        save(posting.getJobPostingId(), actor, ACTION_CREATE, null, json(snapshot(posting), false));
    }

    public void logDelete(JobPosting posting, User actor) {
        save(posting.getJobPostingId(), actor, ACTION_DELETE, json(snapshot(posting), false), null);
    }

    public void logStatusChange(JobPosting posting, User actor, String action, String oldStatus) {
        save(posting.getJobPostingId(), actor, action,
                json(Map.of("Status", oldStatus), true),
                json(Map.of("Status", posting.getPostingStatus()), true));
    }

    /** Một lần lưu, một dòng, chỉ gồm các trường có giá trị thay đổi. */
    public void logChanges(JobPosting posting, User actor, String action, List<String> changedKeys,
            Map<String, String> before, Map<String, String> after) {
        if (changedKeys.isEmpty()) return;
        Map<String, String> oldValues = new LinkedHashMap<>();
        Map<String, String> newValues = new LinkedHashMap<>();
        for (String key : changedKeys) {
            oldValues.put(key, before.get(key));
            newValues.put(key, after.get(key));
        }
        save(posting.getJobPostingId(), actor, action, json(oldValues, true), json(newValues, true));
    }

    /** Lịch sử hoạt động của một tin, mới nhất trước. */
    public List<AuditLog> history(Integer jobPostingId) {
        return audit.findByEntityNameAndEntityIdOrderByTimestampDesc(ENTITY_NAME, String.valueOf(jobPostingId));
    }

    /** Tên trường ổn định trong JSON và giá trị trước/sau để so sánh. */
    public Map<String, String> snapshot(JobPosting posting) {
        Map<String, String> snapshot = new LinkedHashMap<>();
        snapshot.put("Title", posting.getPostingTitle());
        // Seed data stores CRLF while the form saves LF; compare text, not line endings.
        snapshot.put("JobDescription", lf(posting.getJobDescription()));
        snapshot.put("JobRequirements", lf(posting.getJobRequirements()));
        snapshot.put("Benefits", lf(posting.getBenefits()));
        snapshot.put("SalaryDisplay", posting.getSalaryDisplay());
        snapshot.put("NumberOfPositions", posting.getNumberOfPositionsDisplay() == null
                ? null : posting.getNumberOfPositionsDisplay().toString());
        snapshot.put("WorkLocation", posting.getWorkLocation());
        snapshot.put("WorkingHours", posting.getWorkingHours());
        // NULL means "same as the requisition", so audit the value the posting actually shows.
        snapshot.put("EmploymentType", posting.effectiveEmploymentType());
        snapshot.put("WorkModel", posting.effectiveWorkModel());
        snapshot.put("ExpiresAt", posting.getApplicationDeadline() == null
                ? null : posting.getApplicationDeadline().toLocalDate().toString());
        snapshot.put("Status", posting.getPostingStatus());
        return snapshot;
    }

    private static String lf(String text) {
        return text == null ? null : text.replace("\r\n", "\n");
    }

    private String json(Map<String, String> fields, boolean includeNulls) {
        Map<String, Object> values = new LinkedHashMap<>();
        fields.forEach((key, value) -> {
            if (value != null || includeNulls) {
                values.put(key, "NumberOfPositions".equals(key) && value != null
                        ? Integer.valueOf(value) : value);
            }
        });
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không thể ghi JSON lịch sử tin tuyển dụng.", e);
        }
    }

    private void save(Integer jobPostingId, User actor, String action, String oldValue, String newValue) {
        audit.save(AuditLog.builder()
                .user(actor)
                .action(action)
                .entityName(ENTITY_NAME)
                .entityId(String.valueOf(jobPostingId))
                .oldValue(oldValue)
                .newValue(newValue)
                .timestamp(LocalDateTime.now(clock))
                .build());
    }

}
