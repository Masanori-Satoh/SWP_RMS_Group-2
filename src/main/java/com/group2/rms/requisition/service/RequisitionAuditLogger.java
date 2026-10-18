package com.group2.rms.requisition.service;

import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.admin.repository.AuditLogRepository;
import com.group2.rms.core.util.FieldChanges;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.ScreeningCriteria;
import com.group2.rms.user.entity.User;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ghi và đọc audit log (bảng AuditLog) của Yêu cầu tuyển dụng.
 *
 * Gom về một chỗ để: tên entity "JobRequisition" chỉ viết một lần, và cách mô tả thay đổi
 * ("Số lượng cần tuyển: 2 → 3") giống nhau ở mọi thao tác.
 */
@Component
@RequiredArgsConstructor
public class RequisitionAuditLogger {

    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";

    private static final String ENTITY_NAME = "JobRequisition";

    private final AuditLogRepository audit;

    /** Ghi một dòng audit log cho yêu cầu. */
    public void log(JobRequisition jobRequisition, User currentUser, String action, String oldValue, String newValue) {
        audit.save(AuditLog.builder()
                .user(currentUser)
                .action(action)
                .entityName(ENTITY_NAME)
                .entityId(jobRequisition.getRequisitionId().toString())
                .oldValue(oldValue)
                .newValue(newValue)
                .build());
    }

    /** Ghi audit khi sửa: chỉ liệt kê các trường đã đổi ("tên: cũ → mới"). */
    public void logChanges(JobRequisition jobRequisition, User currentUser, List<String> changedKeys,
            Map<String, String> before, Map<String, String> after) {
        log(jobRequisition, currentUser, ACTION_UPDATE,
                FieldChanges.describeBefore(changedKeys, before),
                FieldChanges.describeChange(changedKeys, before, after));
    }

    /** Lịch sử hoạt động của một yêu cầu, mới nhất trước (hiện ở màn chi tiết). */
    public List<AuditLog> history(Integer requisitionId) {
        return audit.findByEntityNameAndEntityIdOrderByTimestampDesc(ENTITY_NAME, requisitionId.toString());
    }

    /**
     * "Ảnh chụp" các trường người dùng nhìn thấy, để so sánh trước / sau khi sửa.
     * Key là tên trường tiếng Việt, value là giá trị dạng chữ (null = chưa nhập).
     */
    public Map<String, String> snapshot(JobRequisition jobRequisition) {
        Map<String, String> snapshot = new LinkedHashMap<>();
        snapshot.put("Chức danh công việc", jobRequisition.getTitle());
        String departmentName = null;
        if (jobRequisition.getDepartment() != null) {
            departmentName = jobRequisition.getDepartment().getDepartmentName();
        }
        snapshot.put("Phòng ban", departmentName);
        snapshot.put("Đợt tuyển dụng", FieldChanges.display(jobRequisition.getRecruitmentRound()));
        snapshot.put("Số lượng cần tuyển", FieldChanges.display(jobRequisition.getNumberOfPositions()));
        snapshot.put("Hình thức làm việc", jobRequisition.getEmploymentType());
        snapshot.put("Mức lương tối thiểu", formatNumber(jobRequisition.getMinSalary()));
        snapshot.put("Mức lương tối đa", formatNumber(jobRequisition.getMaxSalary()));
        snapshot.put("Yêu cầu giới tính", jobRequisition.getGender());
        snapshot.put("Địa điểm làm việc", jobRequisition.getWorkLocation());
        snapshot.put("Mô hình làm việc", jobRequisition.getWorkModel());
        String probation = null;
        if (jobRequisition.getProbationDays() != null) {
            probation = jobRequisition.getProbationDays() + " ngày";
        }
        snapshot.put("Thời gian thử việc", probation);
        snapshot.put("Ngày bắt đầu dự kiến", FieldChanges.display(jobRequisition.getExpectedStartDate()));
        snapshot.put("Lý do tuyển dụng", jobRequisition.getReasonForHiring());
        snapshot.put("Mô tả công việc", jobRequisition.getJobDescription());
        snapshot.put("Yêu cầu ứng viên", jobRequisition.getRequirementDetails());
        snapshot.put("Trạng thái phê duyệt", jobRequisition.getApprovalStatus());

        int unnamedIndex = 0;
        for (ScreeningCriteria criterion : jobRequisition.getScreeningCriteria()) {
            String criterionName = criterion.getCriteriaName();
            if (criterionName == null) {
                unnamedIndex++;
                criterionName = "Tiêu chí " + unnamedIndex;
            }
            snapshot.put("Tiêu chí: " + criterionName,
                    FieldChanges.display(criterion.getCriteriaType()) + "; "
                            + FieldChanges.display(criterion.getRequiredValue()) + "; trọng số "
                            + FieldChanges.display(formatNumber(criterion.getWeight())) + "%; bắt buộc "
                            + Boolean.TRUE.equals(criterion.getIsMandatory()));
        }
        return snapshot;
    }

    /** 20000000.00 → "20000000" (bỏ số 0 thừa để so sánh trước / sau không bị lệch). */
    private String formatNumber(BigDecimal number) {
        if (number == null) {
            return null;
        }
        return number.stripTrailingZeros().toPlainString();
    }
}
