package com.group2.rms.jobposting.dto;

import com.group2.rms.core.dto.FilterOption;

import java.util.List;

/**
 * Người đang xem được thấy những ô lọc nào, mỗi ô có những lựa chọn gì (mỗi vai
 * trò một bộ, đúng mockup).
 *
 * @param showDepartment có ô "Phòng ban" không
 * @param showDeadline   có ô "Hạn nộp" không
 * @param showCreator    có ô "Người tạo" không
 * @param showPostedDate có ô "Ngày đăng (từ – đến)" không
 * @param statuses       các trạng thái được chọn (HM, Director không có "Bản
 *                       nháp")
 * @param departments    các phòng ban
 * @param creators       các người tạo tin
 * @param deadlines      các nhóm hạn nộp
 * @param sorts          các cách sắp xếp
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
