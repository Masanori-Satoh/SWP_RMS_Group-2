package com.group2.rms.jobposting.service;

import com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse;
import com.group2.rms.jobposting.dto.JobPostingDashboardResponse;
import com.group2.rms.jobposting.dto.JobPostingCreateRequest;
import com.group2.rms.jobposting.dto.JobPostingListResponse;
import com.group2.rms.jobposting.dto.JobPostingSearchRequest;
import com.group2.rms.user.entity.User;

public interface JobPostingService {

    /**
     * Màn danh sách tin tuyển dụng nội bộ: phạm vi theo vai trò, bộ lọc đã làm
     * sạch, ô lọc theo vai trò,
     * chip bộ lọc, số hồ sơ mỗi tin và cờ thao tác. Người xem: HR, System Admin,
     * Hiring Manager, Director.
     */
    JobPostingListResponse getListPage(JobPostingSearchRequest filter, User viewer);

    /**
     * Các yêu cầu đã duyệt chưa có tin đang mở, cho ô "Đổi yêu cầu" trên form tạo
     * tin.
     * Nhãn kèm "(đã đăng N lần)" nếu yêu cầu từng có tin.
     */
    java.util.List<com.group2.rms.core.dto.FilterOption> requisitionChoices(Integer currentRequisitionId,
            User currentUser);

    /**
     * Form bị lỗi cần hiển thị lại: điền lại thông tin yêu cầu nguồn (chỉ hiển thị)
     * từ DB,
     * giữ nguyên nội dung người dùng đã nhập.
     */
    JobPostingCreateRequest restoreSourceInfo(JobPostingCreateRequest edited, User currentUser);

    /** "Đăng ngay" tin đang hẹn đăng (ngày đăng = bây giờ). */
    void publishNow(Integer id, User actor);

    /** "Hủy lịch đăng": tin hẹn đăng trở về bản nháp. */
    void unschedule(Integer id, User actor);

    /**
     * "Đăng lại" tin đã đóng: form tạo tin lần mới, nội dung lấy từ tin đã đóng.
     */
    JobPostingCreateRequest prepareRepostForm(Integer closedPostingId, User currentUser);

    /**
     * Chuẩn bị dữ liệu prefill cho form tạo tin tuyển dụng từ Requisition đã
     * Approved.
     */
    JobPostingCreateRequest prepareCreateForm(Integer requisitionId, User currentUser);

    JobPostingDashboardResponse getDashboard(User currentUser);

    /** Tạo bản nháp từ Requisition đã duyệt. */
    Integer createJobPosting(JobPostingCreateRequest request, User currentUser);

    /** Tạo và đăng trong cùng một giao dịch khi HR bấm Đăng tin trên form tạo. */
    Integer createAndPublishJobPosting(JobPostingCreateRequest request, User currentUser);

    void updateJobPosting(Integer id, JobPostingCreateRequest request, User currentUser);

    void updateAndPublishJobPosting(Integer id, JobPostingCreateRequest request, User currentUser);

    void publishJobPosting(Integer id, User currentUser);

    void pauseJobPosting(Integer id, User currentUser);

    void closeJobPosting(Integer id, User currentUser);

    /**
     * Chuẩn bị dữ liệu cho form chỉnh sửa tin tuyển dụng.
     */
    JobPostingCreateRequest prepareEditForm(Integer jobPostingId, User currentUser);

    /**
     * Xóa tin tuyển dụng (kèm audit log).
     */
    void deleteJobPosting(Integer id, User currentUser);

    /**
     * Lấy thông tin chi tiết nội bộ của một tin tuyển dụng.
     */
    InternalJobPostingDetailResponse getInternalJobPostingDetail(Integer id, User currentUser);
}
