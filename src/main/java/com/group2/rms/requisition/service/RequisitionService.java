package com.group2.rms.requisition.service;

import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.RequisitionResponse;

public interface RequisitionService {
    // Lấy chi tiết yêu cầu theo mã.
    RequisitionResponse getById(Integer id);

    // Chuẩn bị dữ liệu sửa yêu cầu.
    RequisitionRequest prepareEditForm(Integer id);

    // Chuẩn bị bản sao từ yêu cầu gốc.
    RequisitionRequest copy(Integer id);

    // Tạo yêu cầu tuyển dụng.
    Integer createRequisition(RequisitionRequest request);

    // Cập nhật yêu cầu tuyển dụng.
    void updateRequisition(Integer id, RequisitionRequest request);

    // Xóa yêu cầu đủ điều kiện.
    void deleteRequisition(Integer id);

    // Xử lý quyết định phê duyệt.
    void decide(Integer id, boolean approved, String comment);

    // Đóng yêu cầu đã hoàn tất.
    void closeRequisition(Integer id);

}
