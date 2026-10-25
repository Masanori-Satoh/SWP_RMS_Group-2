package com.group2.rms.candidate.application.pipeline.service;

import com.group2.rms.candidate.domain.rule.ApplicationAccess;

/**
 * Phạm vi đơn ứng tuyển một người được xem, dùng làm điều kiện cho query danh sách.
 * Hiring Manager: mọi đơn của tin thuộc phòng ban mình làm trưởng phòng hoặc trực thuộc, kể cả đơn HR chưa chuyển
 * (cùng định nghĩa với {@code RequisitionAccess.canManageDepartment}). Duyệt thì vẫn chỉ đơn HR đã chuyển
 * ({@link ApplicationAccess#canReview}).
 *
 * @param allDepartments  {@code true}: không giới hạn phòng ban
 * @param managerUserId   phòng ban có {@code ManagerId} này nằm trong phạm vi
 * @param departmentId    phòng ban của người xem; {@code null} nếu không thuộc phòng ban nào
 */
public record ApplicationScope(boolean allDepartments, Integer managerUserId, Integer departmentId) {

    public static final ApplicationScope ALL = new ApplicationScope(true, null, null);

    public static ApplicationScope hiringManager(Integer userId, Integer departmentId) {
        return new ApplicationScope(false, userId, departmentId);
    }
}
