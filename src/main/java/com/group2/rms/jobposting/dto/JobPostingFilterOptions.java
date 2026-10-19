package com.group2.rms.jobposting.dto;

import com.group2.rms.core.dto.FilterOption;

import java.util.List;

/**
 * Người đang xem được thấy những ô lọc nào, mỗi ô có những lựa chọn gì (mỗi vai trò một bộ, đúng mockup).
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
