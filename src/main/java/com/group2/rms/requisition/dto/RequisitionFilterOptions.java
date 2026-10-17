package com.group2.rms.requisition.dto;

import java.util.List;

/**
 * Cho giao diện biết người đang xem được thấy những ô lọc nào, và mỗi ô có những lựa chọn gì.
 * Mỗi role một bộ khác nhau (xem RequisitionServiceImpl#getFilterOptions).
 *
 * @param showStatus      có ô "Trạng thái" không
 * @param showPosting     có ô "Tin tuyển dụng" không
 * @param showDepartment  có ô "Phòng ban" không
 * @param showCreator     có ô "Người tạo yêu cầu" không
 * @param showCreatedDate có ô "Ngày tạo (từ - đến)" không
 * @param statuses        các trạng thái được phép chọn
 * @param postings        các lựa chọn của ô "Tin tuyển dụng"
 * @param departments     các phòng ban
 * @param creators        các người tạo yêu cầu
 * @param sorts           các cách sắp xếp
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
