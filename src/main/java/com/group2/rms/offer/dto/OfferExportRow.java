package com.group2.rms.offer.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO dữ liệu phẳng chứa thông tin chi tiết một Offer sau khi truy vấn và map từ Database.
 * Dùng để ghi dữ liệu an toàn vào các ô (cells) trong Apache POI Workbook.
 */
@Builder
public record OfferExportRow(
        Integer offerId,
        String candidateName,
        String candidateEmail,
        String offeredPositionTitle,
        BigDecimal proposedSalary,
        BigDecimal probationSalary,
        Double probationPercentage,
        String offerStatus,
        String rawOfferStatus,
        LocalDate expectedStartDate,
        String workLocation,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer applicationId,
        String candidatePhone,
        String departmentName,
        Integer requisitionId,
        Integer jobPostingId,
        Integer probationDays,
        String benefitsPackage,
        String proposedByName,
        String directorName,
        String directorDecision,
        String directorComments,
        LocalDateTime approvedAt
) {}
