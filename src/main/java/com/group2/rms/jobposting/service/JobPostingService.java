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

    // Lấy yêu cầu có thể chọn để tạo tin.
    List<FilterOption> requisitionChoices(User currentUser);

    // Chuẩn bị form tạo tin.
    JobPostingRequest prepareCreateForm(Integer requisitionId, User currentUser);

    // Chuẩn bị form đăng lại tin.
    JobPostingRequest prepareRepostForm(Integer closedPostingId, User currentUser);

    // Chuẩn bị form sửa tin.
    JobPostingRequest prepareEditForm(Integer jobPostingId, User currentUser);

    // Khôi phục thông tin nguồn khi form lỗi.
    JobPostingRequest restoreSourceInfo(JobPostingRequest edited, User currentUser);

    // Tạo tin nháp.
    Integer createJobPosting(JobPostingRequest request, User currentUser);

    // Tạo và đăng tin trong một giao dịch.
    Integer createAndPublishJobPosting(JobPostingRequest request, User currentUser);

    // Lưu thay đổi của tin.
    void updateJobPosting(Integer id, JobPostingRequest request, User currentUser);

    // Lưu và đăng tin.
    void updateAndPublishJobPosting(Integer id, JobPostingRequest request, User currentUser);

    // Đăng hoặc mở lại tin.
    void publishJobPosting(Integer id, User currentUser);

    // Tạm dừng tin.
    void pauseJobPosting(Integer id, User currentUser);

    // Đóng tin tuyển dụng.
    void closeJobPosting(Integer id, User currentUser);

    // Xóa tin nháp đủ điều kiện.
    void deleteJobPosting(Integer id, User currentUser);

    // Lấy chi tiết tin nội bộ.
    InternalJobPostingDetailResponse getInternalJobPostingDetail(Integer id, User currentUser);
}
