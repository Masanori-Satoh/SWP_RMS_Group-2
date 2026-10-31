package com.group2.rms.jobposting.dto;

import com.group2.rms.core.dto.FilterOption;

import java.util.List;

/*
 * DTO: các ô lọc người xem được thấy và lựa chọn của từng ô (mỗi vai trò một bộ).
 */
public record JobPostingFilterOptions(
                boolean showDepartment,
                boolean showDeadline,
                boolean showCreator,
                boolean showPostedDate,
                List<FilterOption> statuses,
                List<FilterOption> departments,
                List<FilterOption> creators,
                List<FilterOption> deadlines,
                List<FilterOption> sorts) {
}
