package com.rms.feature.audit;

/**
 * Service ghi nhận vết kiểm toán hệ thống theo ràng buộc BR-SEC-02 / GBR-12.
 */
public interface AuditLogService {

    /**
     * Ghi vết thao tác vào bảng AuditLog.
     *
     * @param userId     ID của người dùng thực hiện thao tác
     * @param action     Loại hành động (CREATE, UPDATE, DELETE, v.v.)
     * @param entityName Tên thực thể tác động (ví dụ: OfferProposal)
     * @param entityId   Khóa chính của thực thể
     * @param oldValue   Giá trị cũ trước khi thay đổi
     * @param newValue   Giá trị mới sau khi thay đổi
     */
    void log(Integer userId, String action, String entityName, String entityId, String oldValue, String newValue);
}
