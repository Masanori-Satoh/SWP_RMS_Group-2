package com.group2.rms.jobposting.service;

import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.admin.repository.AuditLogRepository;
import com.group2.rms.core.util.FieldChanges;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * AUDIT: ghi và đọc audit log của tin tuyển dụng, mô tả thay đổi dạng "tên: cũ → mới".
 */
@Component
@RequiredArgsConstructor
public class JobPostingAuditLogger {

    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";
    /** Tên entity trong cột AuditLog.EntityName. */
    public static final String ENTITY_NAME = "JobPosting";

    private static final DateTimeFormatter SNAPSHOT_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AuditLogRepository audit;
    private final Clock clock;

    /** Ghi một dòng audit: "mô tả: tiêu đề tin" (ví dụ "Tạm dừng tin tuyển dụng: Java Dev"). */
    public void log(JobPosting posting, User actor, String action, String description) {
        save(posting.getJobPostingId(), actor, action, null, description + ": " + posting.getPostingTitle());
    }

    /** Audit khi sửa: dòng đầu là mô tả, mỗi dòng sau là "tên: cũ → mới". */
    public void logChanges(JobPosting posting, User actor, String description, List<String> changedKeys,
            Map<String, String> before, Map<String, String> after) {
        save(posting.getJobPostingId(), actor, ACTION_UPDATE,
                FieldChanges.describeBefore(changedKeys, before),
                description + "\n" + FieldChanges.describeChange(changedKeys, before, after));
    }

    /** Lịch sử hoạt động của một tin, mới nhất trước. */
    public List<AuditLog> history(Integer jobPostingId) {
        return audit.findByEntityNameAndEntityIdOrderByTimestampDesc(ENTITY_NAME, String.valueOf(jobPostingId));
    }

    /** Giá trị các trường người dùng thấy (key tiếng Việt), để so trước / sau khi sửa. */
    public Map<String, String> snapshot(JobPosting posting) {
        Map<String, String> snapshot = new LinkedHashMap<>();
        snapshot.put("Tiêu đề tin", posting.getPostingTitle());
        snapshot.put("Mô tả công việc", posting.getJobDescription());
        snapshot.put("Yêu cầu ứng viên", posting.getJobRequirements());
        snapshot.put("Quyền lợi", posting.getBenefits());
        snapshot.put("Mức lương hiển thị", posting.getSalaryDisplay());
        snapshot.put("Số lượng hiển thị", posting.getNumberOfPositionsDisplay());
        snapshot.put("Địa điểm làm việc", posting.getWorkLocation());
        snapshot.put("Hạn nộp hồ sơ", formatTime(posting.getApplicationDeadline()));
        snapshot.put("Ngày đăng", formatTime(posting.getPostingDate()));
        snapshot.put("Trạng thái", posting.getPostingStatus());
        return snapshot;
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

    private static String formatTime(LocalDateTime moment) {
        if (moment == null) {
            return null;
        }
        return moment.format(SNAPSHOT_TIME);
    }
}
