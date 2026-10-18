package com.group2.rms.requisition.service;

import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.RequisitionResponse;

/**
 * Nghiệp vụ một Yêu cầu tuyển dụng: xem chi tiết, form, tạo, sửa, xóa, duyệt, rút lại.
 * Màn danh sách (lọc, tìm kiếm, phân trang) nằm ở RequisitionListService.
 */
public interface RequisitionService {
    RequisitionResponse getById(Integer id);

    RequisitionRequest getRequestDtoById(Integer id);

    RequisitionRequest copy(Integer id);

    Integer createRequisition(RequisitionRequest dto);

    void updateRequisition(Integer id, RequisitionRequest dto);

    void deleteRequisition(Integer id);

    void decide(Integer id, boolean approved, String comment);

    void withdraw(Integer id);
}
