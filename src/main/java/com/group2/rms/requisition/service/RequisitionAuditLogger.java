package com.group2.rms.requisition.service;

import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.admin.repository.AuditLogRepository;
import com.group2.rms.core.util.FieldChanges;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.user.entity.User;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Component
@RequiredArgsConstructor
public class RequisitionAuditLogger {

    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";

    private static final String ENTITY_NAME = "JobRequisition";

    private final AuditLogRepository audit;


    // Records a requisition audit event.
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


    // Records the changed requisition fields.
    public void logChanges(JobRequisition jobRequisition, User currentUser, List<String> changedKeys,
            Map<String, String> before, Map<String, String> after) {
        log(jobRequisition, currentUser, ACTION_UPDATE,
                FieldChanges.describeBefore(changedKeys, before),
                FieldChanges.describeChange(changedKeys, before, after));
    }


    // Returns the requisition audit history.
    public List<AuditLog> history(Integer requisitionId) {
        return audit.findByEntityNameAndEntityIdOrderByTimestampDesc(ENTITY_NAME, requisitionId.toString());
    }


    // Returns a snapshot of user-visible requisition fields.
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
        snapshot.put("Thời gian thử việc", jobRequisition.getProbationPeriod());
        snapshot.put("Lý do tuyển dụng", jobRequisition.getReasonForHiring());
        snapshot.put("Mô tả công việc", jobRequisition.getJobDescription());
        snapshot.put("Yêu cầu ứng viên", jobRequisition.getRequirementDetails());
        snapshot.put("Trạng thái phê duyệt", jobRequisition.getApprovalStatus());
        return snapshot;
    }

    // Formats a salary amount.
    private String formatNumber(BigDecimal number) {
        if (number == null) {
            return null;
        }
        return number.stripTrailingZeros().toPlainString();
    }
}
