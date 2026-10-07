package com.group2.rms.offer.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * DTO nhận yêu cầu xuất Excel danh sách Offer từ HR.
 * Áp dụng cấu trúc Java record theo ARCHITECTURE_GUIDE.md.
 */
public record OfferExportRequest(
        @NotNull(message = "Phạm vi xuất dữ liệu không được để trống")
        OfferExportScope scope,

        List<Integer> offerIds,

        OfferExportFilterRequest filters,

        @NotEmpty(message = "Vui lòng chọn ít nhất một cột dữ liệu để xuất")
        List<String> columns
) {}
