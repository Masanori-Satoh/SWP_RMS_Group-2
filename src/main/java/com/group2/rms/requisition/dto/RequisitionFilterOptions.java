package com.group2.rms.requisition.dto;

import com.group2.rms.core.dto.FilterOption;

import java.util.List;

/*
 * DTO: các ô lọc người xem được thấy và lựa chọn của từng ô (mỗi role một bộ).
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
