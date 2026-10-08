package com.group2.rms.offer.service;

import com.group2.rms.offer.dto.OfferExportRequest;

/**
 * Service xử lý xuất dữ liệu Offer ra định dạng file Excel (.xlsx) cho HR.
 * Tuân thủ quy chuẩn ARCHITECTURE_GUIDE.md:
 * - Decoupled khỏi controller.
 * - Truy vấn trực tiếp từ database qua JPA Specification (không lấy từ DOM).
 * - Bọc @Transactional(readOnly = true) để ngăn ngừa LazyInitializationException.
 */
public interface OfferExportService {

    /**
     * Xuất dữ liệu Offer ra mảng byte đại diện cho workbook Excel (.xlsx).
     *
     * @param request Yêu cầu xuất (scope, offerIds, filters, columns)
     * @param exportedBy Tên người dùng thực hiện xuất báo cáo
     * @return Mảng byte chứa nội dung file .xlsx
     */
    byte[] exportOffersToExcel(OfferExportRequest request, String exportedBy);

    /**
     * Sinh tên file Excel chuẩn hóa theo phạm vi và ngày xuất.
     * Ví dụ: offers_filtered_2026-10-07.xlsx, offers_selected_3_2026-10-07.xlsx, offers_2026-10-07.xlsx.
     *
     * @param request Yêu cầu xuất
     * @return Tên file đã được sanitize an toàn
     */
    String generateExportFilename(OfferExportRequest request);
}
