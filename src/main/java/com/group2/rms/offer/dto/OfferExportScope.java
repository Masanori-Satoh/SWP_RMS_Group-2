package com.group2.rms.offer.dto;

/**
 * Phạm vi xuất dữ liệu Offer cho HR:
 * - FILTERED: Toàn bộ danh sách theo điều kiện tìm kiếm/lọc hiện tại (bỏ phân trang).
 * - SELECTED: Chỉ các Offer được chọn qua checkbox (dựa trên offerIds).
 * - ALL: Toàn bộ Offer hợp lệ (chưa bị xóa mềm) trong hệ thống.
 */
public enum OfferExportScope {
    FILTERED,
    SELECTED,
    ALL
}
