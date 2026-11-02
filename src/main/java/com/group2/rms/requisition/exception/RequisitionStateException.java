package com.group2.rms.requisition.exception;

import com.group2.rms.core.exception.BaseBusinessException;

/*
 * EXCEPTION: đúng người nhưng yêu cầu đang ở trạng thái không cho thao tác (vd. sửa phiếu Chờ duyệt / Đã duyệt qua URL).
 * Controller bắt để quay về trang chi tiết kèm thông báo, không hiện trang lỗi.
 */
public class RequisitionStateException extends BaseBusinessException {

    public static final String NO_ACCESS_MESSAGE = "Bạn không có quyền truy cập vào chức năng hoặc tài nguyên này.";

    public RequisitionStateException() {
        super(NO_ACCESS_MESSAGE, "REQUISITION_STATE_BLOCKED");
    }
}
