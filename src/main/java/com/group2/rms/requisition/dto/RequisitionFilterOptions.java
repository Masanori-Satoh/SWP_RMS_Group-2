package com.group2.rms.requisition.dto;

import com.group2.rms.core.dto.FilterOption;

import java.util.List;

/**
 * Cho giao diện biết người đang xem được thấy những ô lọc nào, và mỗi ô có những lựa chọn gì.
 */
public record RequisitionFilterOptions(
        boolean showStatus,
        boolean showPosting,
        boolean showDepartment,
        boolean showCreator,
        boolean showCreatedDate,
        List<FilterOption> statuses,
        List<FilterOption> postings,
        List<FilterOption> departments,
        List<FilterOption> creators,
        List<FilterOption> sorts) {
}
