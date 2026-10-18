package com.group2.rms.jobposting.service;

import com.group2.rms.core.dto.FilterOption;
import com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse;
import com.group2.rms.jobposting.dto.JobPostingCreateRequest;
import com.group2.rms.jobposting.dto.JobPostingDashboardResponse;
import com.group2.rms.user.entity.User;

import java.util.List;

/**
 * Nghiệp vụ một tin tuyển dụng: mở form, tạo, sửa, chuyển trạng thái, xóa, xem chi tiết, bảng điều khiển HR.
 * Màn danh sách nằm ở JobPostingListService. Người thao tác luôn truyền từ phiên (JobPostingAccess#actor).
 */
public interface JobPostingService {

    JobPostingDashboardResponse getDashboard(User currentUser);

    /**
     * Các yêu cầu đã duyệt chưa có tin đang mở, cho ô "Đổi yêu cầu" trên form tạo tin.
     * Nhãn kèm "(đã đăng N lần)" nếu yêu cầu từng có tin.
     */
    List<FilterOption> requisitionChoices(User currentUser);

    /** Form tạo tin điền sẵn từ yêu cầu đã duyệt. */
    JobPostingCreateRequest prepareCreateForm(Integer requisitionId, User currentUser);

    /** "Đăng lại" tin đã đóng: form tạo tin lần mới, nội dung lấy từ tin đã đóng. */
    JobPostingCreateRequest prepareRepostForm(Integer closedPostingId, User currentUser);

    /** Form chỉnh sửa tin đang nháp / hẹn đăng / đang đăng / tạm dừng. */
    JobPostingCreateRequest prepareEditForm(Integer jobPostingId, User currentUser);

    /**
     * Form bị lỗi cần hiển thị lại: điền lại thông tin yêu cầu nguồn và trạng thái form từ DB,
     * giữ nguyên nội dung người dùng đã nhập.
     */
    JobPostingCreateRequest restoreSourceInfo(JobPostingCreateRequest edited, User currentUser);

    /** Lưu bản nháp từ yêu cầu đã duyệt. */
    Integer createJobPosting(JobPostingCreateRequest request, User currentUser);

    /** Tạo và đăng (hoặc hẹn đăng) trong cùng một giao dịch khi HR bấm "Đăng tin" trên form tạo. */
    Integer createAndPublishJobPosting(JobPostingCreateRequest request, User currentUser);

    /** "Lưu nháp" / "Lưu thay đổi": trạng thái giữ nguyên (tin hẹn đăng thì áp dụng lại lịch). */
    void updateJobPosting(Integer id, JobPostingCreateRequest request, User currentUser);

    /** "Đăng tin" / "Lên lịch đăng" / "Lưu và mở lại" trên form sửa. */
    void updateAndPublishJobPosting(Integer id, JobPostingCreateRequest request, User currentUser);

    /** Đăng tin nháp hoặc mở lại tin tạm dừng (nút trên trang chi tiết). */
    void publishJobPosting(Integer id, User currentUser);

    /** "Đăng ngay" tin đang hẹn đăng (ngày đăng = bây giờ). */
    void publishNow(Integer id, User currentUser);

    /** "Hủy lịch đăng": tin hẹn đăng trở về bản nháp. */
    void unschedule(Integer id, User currentUser);

    void pauseJobPosting(Integer id, User currentUser);

    /** Đóng tin (chỉ HR): giao cho TalentPoolService#closeJob xử lý hồ sơ / Offer còn lại. */
    void closeJobPosting(Integer id, User currentUser);

    /** Xóa bản nháp chưa có hồ sơ (kèm audit log). */
    void deleteJobPosting(Integer id, User currentUser);

    InternalJobPostingDetailResponse getInternalJobPostingDetail(Integer id, User currentUser);
}
