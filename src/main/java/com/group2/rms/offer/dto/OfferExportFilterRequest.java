package com.group2.rms.offer.dto;

/**
 * Tiêu chí lọc dữ liệu Offer truyền từ giao diện người dùng (Thymeleaf/AJAX).
 * Chuẩn Java record theo ARCHITECTURE_GUIDE.md.
 */
public record OfferExportFilterRequest(
        String keyword,
        String status,
        String sort
) {}
