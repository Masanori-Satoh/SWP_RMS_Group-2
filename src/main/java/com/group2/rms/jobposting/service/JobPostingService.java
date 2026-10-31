package com.group2.rms.jobposting.service;

import com.group2.rms.core.dto.FilterOption;
import com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse;
import com.group2.rms.jobposting.dto.JobPostingRequest;
import com.group2.rms.user.entity.User;

import java.util.List;

/**
 * Nghiệp vụ một tin tuyển dụng: mở form, tạo, sửa, chuyển trạng thái, xóa, xem chi tiết, bảng điều khiển HR.
 * Màn danh sách nằm ở JobPostingListService. Người thao tác luôn truyền từ phiên (JobPostingAccess#actor).
 */
public interface JobPostingService {

    /** Yêu cầu đã duyệt chưa có tin đang mở, cho ô "Đổi yêu cầu"; kèm "(đã đăng N lần)". */
    List<FilterOption> requisitionChoices(User currentUser);

    /** Form tạo tin điền sẵn từ yêu cầu đã duyệt. */
    JobPostingRequest prepareCreateForm(Integer requisitionId, User currentUser);

    /** "Đăng lại" tin đã đóng: form tạo tin lần mới, nội dung lấy từ tin đã đóng. */
    JobPostingRequest prepareRepostForm(Integer closedPostingId, User currentUser);

    /** Form chỉnh sửa tin đang nháp / đang đăng / tạm dừng. */
    JobPostingRequest prepareEditForm(Integer jobPostingId, User currentUser);

    /** Hiện lại form lỗi: điền lại thông tin nguồn từ DB, giữ nội dung người dùng đã nhập. */
    JobPostingRequest restoreSourceInfo(JobPostingRequest edited, User currentUser);

    /** Lưu bản nháp từ yêu cầu đã duyệt. */
    Integer createJobPosting(JobPostingRequest request, User currentUser);

    /** Tạo và đăng trong cùng một giao dịch khi HR bấm "Đăng tin" trên form tạo. */
    Integer createAndPublishJobPosting(JobPostingRequest request, User currentUser);

    /** "Lưu nháp" / "Lưu thay đổi": trạng thái giữ nguyên. */
    void updateJobPosting(Integer id, JobPostingRequest request, User currentUser);

    /** "Đăng tin" / "Lưu và mở lại" trên form sửa. */
    void updateAndPublishJobPosting(Integer id, JobPostingRequest request, User currentUser);

    /** Đăng tin nháp hoặc mở lại tin tạm dừng (nút trên trang chi tiết). */
    void publishJobPosting(Integer id, User currentUser);

    void pauseJobPosting(Integer id, User currentUser);

    /** Đóng tin (chỉ HR): giao cho TalentPoolService#closeJob xử lý hồ sơ / Offer còn lại. */
    void closeJobPosting(Integer id, User currentUser);

    /** Xóa bản nháp chưa có hồ sơ (kèm audit log). */
    void deleteJobPosting(Integer id, User currentUser);

    InternalJobPostingDetailResponse getInternalJobPostingDetail(Integer id, User currentUser);
}
