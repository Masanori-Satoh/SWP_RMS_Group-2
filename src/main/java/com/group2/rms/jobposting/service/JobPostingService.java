package com.group2.rms.jobposting.service;

import com.group2.rms.jobposting.dto.InternalJobPostingDetailResponse;
import com.group2.rms.jobposting.dto.JobPostingDashboardResponse;
import com.group2.rms.jobposting.dto.InternalJobPostingResponse;
import com.group2.rms.jobposting.dto.JobPostingCreateRequest;
import com.group2.rms.user.entity.User;
import org.springframework.data.domain.Page;

public interface JobPostingService {

    /**
     * Tìm kiếm và phân trang danh sách tin tuyển dụng nội bộ dành cho HR/Admin.
     */
    Page<InternalJobPostingResponse> searchInternalJobPostings(
            int page,
            int size,
            String q,
            Integer departmentId,
            String status,
            String sortOrder,
            User currentUser
    );

    /**
     * Chuẩn bị dữ liệu prefill cho form tạo tin tuyển dụng từ Requisition đã Approved.
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
