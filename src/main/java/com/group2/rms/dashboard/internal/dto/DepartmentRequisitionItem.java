package com.group2.rms.dashboard.internal.dto;

/*
 * DTO: một cột chồng của biểu đồ "Yêu cầu đang mở theo phòng ban" (Director): phần chờ duyệt + phần đã duyệt.
 */
public record DepartmentRequisitionItem(
        String departmentName,
        long pending,
        long approved) {
}
