package com.group2.rms.requisition.service;

import com.group2.rms.requisition.dto.InternalJobPostingResponse;
import com.group2.rms.requisition.dto.JobPostingCreateRequest;
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

    /**
     * Tạo và lưu Job Posting (Draft hoặc Published).
     * @return ID của Job Posting vừa tạo
     */
    Integer createJobPosting(JobPostingCreateRequest request, User currentUser);
}
