package com.group2.rms.candidate.service;

/**
 * Phạm vi đơn ứng tuyển một người được xem, dùng làm điều kiện cho query danh sách.
 * Hiring Manager: tin thuộc phòng ban mình làm trưởng phòng hoặc trực thuộc, và đơn đã được HR chuyển
 * (cùng định nghĩa với {@code RequisitionAccess.canManageDepartment}).
 *
 * @param allDepartments  {@code true}: không giới hạn phòng ban
 * @param managerUserId   phòng ban có {@code ManagerId} này nằm trong phạm vi
 * @param departmentId    phòng ban của người xem; {@code null} nếu không thuộc phòng ban nào
 * @param forwardedOnly   chỉ đơn có lượt duyệt HR "Pass"
 */
public record ApplicationScope(boolean allDepartments, Integer managerUserId, Integer departmentId,
                               boolean forwardedOnly) {

    public static final ApplicationScope ALL = new ApplicationScope(true, null, null, false);

    public static ApplicationScope hiringManager(Integer userId, Integer departmentId) {
        return new ApplicationScope(false, userId, departmentId, true);
    }
}
