package com.group2.rms.offer.service;

import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.entity.Candidate;
import com.group2.rms.core.exception.BaseBusinessException;
import com.group2.rms.offer.dto.OfferExportFilterRequest;
import com.group2.rms.offer.dto.OfferExportRequest;
import com.group2.rms.offer.dto.OfferExportRow;
import com.group2.rms.offer.dto.OfferExportScope;
import com.group2.rms.offer.entity.OfferApproval;
import com.group2.rms.offer.entity.OfferProposal;
import com.group2.rms.offer.repository.OfferApprovalRepository;
import com.group2.rms.offer.repository.OfferProposalRepository;
import com.group2.rms.user.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfferExportServiceImpl implements OfferExportService {

    private final OfferProposalRepository offerProposalRepository;
    private final OfferApprovalRepository offerApprovalRepository;

    /**
     * Danh sách các cột được phép xuất (Whitelist) kèm tiêu đề tiếng Việt tương ứng.
     */
    private static final Map<String, String> COLUMN_WHITELIST = new LinkedHashMap<>();

    static {
        // Nhóm Mặc định (Default Group)
        COLUMN_WHITELIST.put("offerId", "Mã Offer");
        COLUMN_WHITELIST.put("candidateName", "Ứng viên");
        COLUMN_WHITELIST.put("candidateEmail", "Email");
        COLUMN_WHITELIST.put("offeredPositionTitle", "Vị trí đề xuất");
        COLUMN_WHITELIST.put("proposedSalary", "Lương chính thức (VND)");
        COLUMN_WHITELIST.put("probationSalary", "Lương thử việc (VND)");
        COLUMN_WHITELIST.put("probationPercentage", "Tỷ lệ thử việc");
        COLUMN_WHITELIST.put("offerStatus", "Trạng thái");
        COLUMN_WHITELIST.put("expectedStartDate", "Ngày bắt đầu dự kiến");
        COLUMN_WHITELIST.put("workLocation", "Địa điểm làm việc");
        COLUMN_WHITELIST.put("createdAt", "Ngày lập");
        COLUMN_WHITELIST.put("updatedAt", "Ngày cập nhật");

        // Nhóm Mở rộng (Extended Group)
        COLUMN_WHITELIST.put("applicationId", "Mã đơn ứng tuyển");
        COLUMN_WHITELIST.put("candidatePhone", "Số điện thoại");
        COLUMN_WHITELIST.put("departmentName", "Phòng ban");
        COLUMN_WHITELIST.put("requisitionId", "Mã Requisition");
        COLUMN_WHITELIST.put("jobPostingId", "Mã Job Posting");
        COLUMN_WHITELIST.put("probationDays", "Thời gian thử việc (ngày)");
        COLUMN_WHITELIST.put("benefitsPackage", "Gói phúc lợi");
        COLUMN_WHITELIST.put("proposedByName", "Người tạo đề xuất");
        COLUMN_WHITELIST.put("directorName", "Director xử lý");
        COLUMN_WHITELIST.put("directorDecision", "Quyết định Director");
        COLUMN_WHITELIST.put("directorComments", "Phản hồi Director");
        COLUMN_WHITELIST.put("approvedAt", "Thời điểm duyệt");
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportOffersToExcel(OfferExportRequest request, String exportedBy) {
        validateExportRequest(request);

        // 1. Lấy danh sách entities theo Scope & Filters
        List<OfferProposal> entities = fetchOffersByScope(request);
        if (entities.isEmpty()) {
            throw new BaseBusinessException("Không có dữ liệu đề xuất Offer phù hợp để xuất Excel.", "EMPTY_EXPORT_DATA");
        }

        // 2. Chuyển đổi entities sang DTO phẳng (OfferExportRow)
        boolean needDirectorDetails = request.columns().stream().anyMatch(col ->
                col.equals("directorName") || col.equals("directorDecision")
                        || col.equals("directorComments") || col.equals("approvedAt"));

        List<OfferExportRow> rows = entities.stream()
                .map(entity -> mapToExportRow(entity, needDirectorDetails))
                .toList();

        // 3. Xây dựng Apache POI Workbook với 2 sheet: Offers & Summary
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            buildOffersSheet(workbook, rows, request.columns());
            buildSummarySheet(workbook, rows, request, exportedBy);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Lỗi khi ghi file Excel xuất Offer: ", e);
            throw new BaseBusinessException("Đã xảy ra lỗi khi tạo tệp Excel. Vui lòng thử lại sau.", "EXCEL_GENERATION_FAILED");
        }
    }

    @Override
    public String generateExportFilename(OfferExportRequest request) {
        LocalDate today = LocalDate.now();
        OfferExportScope scope = request.scope() != null ? request.scope() : OfferExportScope.ALL;

        return switch (scope) {
            case FILTERED -> String.format("offers_filtered_%s.xlsx", today);
            case SELECTED -> {
                int count = request.offerIds() != null ? request.offerIds().size() : 0;
                yield String.format("offers_selected_%d_%s.xlsx", count, today);
            }
            case ALL -> String.format("offers_%s.xlsx", today);
        };
    }

    // =========================================================================
    // VALIDATION
    // =========================================================================

    private void validateExportRequest(OfferExportRequest request) {
        if (request == null || request.scope() == null) {
            throw new BaseBusinessException("Phạm vi xuất dữ liệu không được để trống.", "INVALID_EXPORT_REQUEST");
        }

        if (request.scope() == OfferExportScope.SELECTED) {
            if (request.offerIds() == null || request.offerIds().isEmpty()) {
                throw new BaseBusinessException("Vui lòng chọn ít nhất một đề xuất để xuất dữ liệu.", "EMPTY_SELECTED_OFFERS");
            }
        }

        if (request.columns() == null || request.columns().isEmpty()) {
            throw new BaseBusinessException("Vui lòng chọn ít nhất một cột dữ liệu để xuất.", "EMPTY_COLUMNS");
        }

        // Kiểm tra whitelist các cột được chọn
        for (String col : request.columns()) {
            if (!COLUMN_WHITELIST.containsKey(col)) {
                throw new BaseBusinessException("Cột dữ liệu yêu cầu không hợp lệ hoặc không được phép xuất: " + col, "INVALID_EXPORT_COLUMN");
            }
        }
    }

    // =========================================================================
    // DATABASE QUERY / RESOLVE SCOPE
    // =========================================================================

    private List<OfferProposal> fetchOffersByScope(OfferExportRequest request) {
        OfferExportScope scope = request.scope();
        OfferExportFilterRequest filters = request.filters();

        String timeSort = filters != null ? filters.sort() : "DEFAULT";
        Sort sort = resolveSort(timeSort);

        Specification<OfferProposal> baseActiveSpec = (root, query, builder) ->
                builder.or(builder.isNull(root.get("isDeleted")), builder.isFalse(root.get("isDeleted")));

        if (scope == OfferExportScope.SELECTED) {
            List<Integer> ids = request.offerIds();
            Specification<OfferProposal> selectedSpec = (root, query, builder) ->
                    builder.and(baseActiveSpec.toPredicate(root, query, builder), root.get("offerId").in(ids));
            return offerProposalRepository.findAll(selectedSpec, sort);
        }

        if (scope == OfferExportScope.ALL) {
            return offerProposalRepository.findAll(baseActiveSpec, sort);
        }

        // Scope: FILTERED
        String keyword = filters != null ? filters.keyword() : null;
        String status = filters != null ? filters.status() : null;

        boolean hasSearch = keyword != null && !keyword.trim().isEmpty();
        boolean hasStatus = status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim());

        Specification<OfferProposal> filteredSpec = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(baseActiveSpec.toPredicate(root, query, builder));

            if (hasSearch) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                Join<OfferProposal, Application> appJoin = root.join("application", JoinType.LEFT);
                Join<Application, Candidate> candJoin = appJoin.join("candidate", JoinType.LEFT);
                Join<Candidate, User> userJoin = candJoin.join("account", JoinType.LEFT);

                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("offeredPositionTitle")), pattern),
                        builder.like(builder.lower(userJoin.get("fullName")), pattern)
                ));
            }

            if (hasStatus) {
                String s = status.trim();
                if ("Director_Approved".equalsIgnoreCase(s) || "Approved".equalsIgnoreCase(s)) {
                    predicates.add(builder.or(
                            builder.equal(builder.lower(root.get("offerStatus")), "director_approved"),
                            builder.equal(builder.lower(root.get("offerStatus")), "approved")
                    ));
                } else if ("Director_Rejected".equalsIgnoreCase(s) || "Rejected".equalsIgnoreCase(s)) {
                    predicates.add(builder.or(
                            builder.equal(builder.lower(root.get("offerStatus")), "director_rejected"),
                            builder.equal(builder.lower(root.get("offerStatus")), "rejected")
                    ));
                } else {
                    predicates.add(builder.equal(builder.lower(root.get("offerStatus")), s.toLowerCase(Locale.ROOT)));
                }
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };

        return offerProposalRepository.findAll(filteredSpec, sort);
    }

    private Sort resolveSort(String timeSort) {
        if ("EARLIEST".equalsIgnoreCase(timeSort)) {
            return Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("offerId"));
        } else if ("LATEST".equalsIgnoreCase(timeSort)) {
            return Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("offerId"));
        } else {
            return Sort.by(Sort.Order.asc("offerId"));
        }
    }

    // =========================================================================
    // DTO MAPPING
    // =========================================================================

    private OfferExportRow mapToExportRow(OfferProposal offer, boolean needDirectorDetails) {
        var app = offer.getApplication();
        var cand = app != null ? app.getCandidate() : null;
        var user = cand != null ? cand.getAccount() : null;
        var jobPosting = app != null ? app.getJobPosting() : null;
        var requisition = jobPosting != null ? jobPosting.getRequisition() : null;
        var department = requisition != null ? requisition.getDepartment() : null;

        // Tính tỷ lệ thử việc
        Double probationPercentage = null;
        if (offer.getProposedSalary() != null
                && offer.getProposedSalary().compareTo(BigDecimal.ZERO) > 0
                && offer.getProbationSalary() != null) {
            probationPercentage = offer.getProbationSalary().doubleValue() / offer.getProposedSalary().doubleValue();
        }

        // Lấy thông tin Director nếu cần
        String directorName = null;
        String directorDecision = null;
        String directorComments = null;
        LocalDateTime approvedAt = null;

        if (needDirectorDetails) {
            List<OfferApproval> approvals = offerApprovalRepository
                    .findByOfferProposal_OfferIdOrderByApprovedAtDesc(offer.getOfferId());
            if (!approvals.isEmpty()) {
                OfferApproval latest = approvals.get(0);
                if (latest.getDirector() != null) {
                    directorName = latest.getDirector().getFullName();
                }
                directorDecision = mapDirectorStatusLabel(latest.getStatus());
                directorComments = latest.getDirectorComments();
                approvedAt = latest.getApprovedAt();
            }
        }

        String proposedByName = offer.getProposedBy() != null ? offer.getProposedBy().getFullName() : null;

        return OfferExportRow.builder()
                .offerId(offer.getOfferId())
                .candidateName(user != null ? user.getFullName() : null)
                .candidateEmail(user != null ? user.getEmail() : null)
                .offeredPositionTitle(offer.getOfferedPositionTitle())
                .proposedSalary(offer.getProposedSalary())
                .probationSalary(offer.getProbationSalary())
                .probationPercentage(probationPercentage)
                .offerStatus(mapOfferStatusLabel(offer.getOfferStatus()))
                .rawOfferStatus(offer.getOfferStatus())
                .expectedStartDate(offer.getExpectedStartDate())
                .workLocation(offer.getWorkLocation())
                .createdAt(offer.getCreatedAt())
                .updatedAt(offer.getUpdatedAt())
                .applicationId(app != null ? app.getApplicationId() : null)
                .candidatePhone(user != null ? user.getPhoneNumber() : null)
                .departmentName(department != null ? department.getDepartmentName() : null)
                .requisitionId(requisition != null ? requisition.getRequisitionId() : null)
                .jobPostingId(jobPosting != null ? jobPosting.getJobPostingId() : null)
                .probationDays(60) // Quy chuẩn mặc định BR-OFF-01
                .benefitsPackage(offer.getBenefitsPackage())
                .proposedByName(proposedByName)
                .directorName(directorName)
                .directorDecision(directorDecision)
                .directorComments(directorComments)
                .approvedAt(approvedAt)
                .build();
    }

    public static String mapOfferStatusLabel(String status) {
        if (status == null) return "-";
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "draft" -> "Bản thảo";
            case "pending_director" -> "Chờ Director duyệt";
            case "director_approved", "approved" -> "Director đã duyệt";
            case "director_rejected", "rejected" -> "Director từ chối";
            case "sent_candidate" -> "Đã gửi ứng viên";
            case "accepted" -> "Ứng viên đồng ý";
            case "declined" -> "Ứng viên từ chối";
            case "canceled", "cancelled" -> "Đã hủy";
            case "voided" -> "Không còn hiệu lực";
            default -> status;
        };
    }

    private String mapDirectorStatusLabel(String status) {
        if (status == null) return "-";
        if ("approved".equalsIgnoreCase(status)) return "Đã phê duyệt";
        if ("rejected".equalsIgnoreCase(status)) return "Đã từ chối";
        return status;
    }

    // =========================================================================
    // EXCEL SHEET 1: OFFERS
    // =========================================================================

    private void buildOffersSheet(Workbook workbook, List<OfferExportRow> rows, List<String> columns) {
        Sheet sheet = workbook.createSheet("Offers");

        DataFormat df = workbook.createDataFormat();

        // 1. Cell Styles
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontName("Calibri");
        headerFont.setFontHeightInPoints((short) 11);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle defaultStyle = workbook.createCellStyle();
        defaultStyle.setBorderTop(BorderStyle.THIN);
        defaultStyle.setBorderBottom(BorderStyle.THIN);
        defaultStyle.setBorderLeft(BorderStyle.THIN);
        defaultStyle.setBorderRight(BorderStyle.THIN);
        defaultStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle centerStyle = workbook.createCellStyle();
        centerStyle.cloneStyleFrom(defaultStyle);
        centerStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle salaryStyle = workbook.createCellStyle();
        salaryStyle.cloneStyleFrom(defaultStyle);
        salaryStyle.setDataFormat(df.getFormat("#,##0"));
        salaryStyle.setAlignment(HorizontalAlignment.RIGHT);

        CellStyle percentStyle = workbook.createCellStyle();
        percentStyle.cloneStyleFrom(defaultStyle);
        percentStyle.setDataFormat(df.getFormat("0.00%"));
        percentStyle.setAlignment(HorizontalAlignment.RIGHT);

        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.cloneStyleFrom(defaultStyle);
        dateStyle.setDataFormat(df.getFormat("dd/MM/yyyy"));
        dateStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle datetimeStyle = workbook.createCellStyle();
        datetimeStyle.cloneStyleFrom(defaultStyle);
        datetimeStyle.setDataFormat(df.getFormat("dd/MM/yyyy HH:mm"));
        datetimeStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle wrapStyle = workbook.createCellStyle();
        wrapStyle.cloneStyleFrom(defaultStyle);
        wrapStyle.setWrapText(true);

        // 2. Tạo Header Row
        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(24);

        for (int c = 0; c < columns.size(); c++) {
            String colKey = columns.get(c);
            String headerTitle = COLUMN_WHITELIST.getOrDefault(colKey, colKey);
            Cell cell = headerRow.createCell(c);
            cell.setCellValue(headerTitle);
            cell.setCellStyle(headerStyle);
        }

        // 3. Tạo Data Rows
        int rowIdx = 1;
        for (OfferExportRow rowData : rows) {
            Row row = sheet.createRow(rowIdx++);
            row.setHeightInPoints(20);

            for (int c = 0; c < columns.size(); c++) {
                String colKey = columns.get(c);
                Cell cell = row.createCell(c);
                writeCellValue(cell, colKey, rowData, defaultStyle, centerStyle, salaryStyle, percentStyle, dateStyle, datetimeStyle, wrapStyle);
            }
        }

        // 4. Freeze header row & Auto-filter
        sheet.createFreezePane(0, 1);
        if (!rows.isEmpty() && !columns.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(0, rows.size(), 0, columns.size() - 1));
        }

        // 5. Auto size column với padding hợp lý
        for (int c = 0; c < columns.size(); c++) {
            sheet.autoSizeColumn(c);
            int currentWidth = sheet.getColumnWidth(c);
            int minWidth = 3200; // ~11-12 chars
            int maxWidth = 12000;
            if (currentWidth < minWidth) {
                sheet.setColumnWidth(c, minWidth);
            } else if (currentWidth > maxWidth) {
                sheet.setColumnWidth(c, maxWidth);
            } else {
                sheet.setColumnWidth(c, currentWidth + 600); // Thêm chút khoảng thở
            }
        }
    }

    private void writeCellValue(Cell cell, String colKey, OfferExportRow r,
                                CellStyle defaultStyle, CellStyle centerStyle,
                                CellStyle salaryStyle, CellStyle percentStyle,
                                CellStyle dateStyle, CellStyle datetimeStyle,
                                CellStyle wrapStyle) {
        switch (colKey) {
            case "offerId" -> {
                if (r.offerId() != null) {
                    cell.setCellValue(r.offerId());
                    cell.setCellStyle(centerStyle);
                } else {
                    cell.setCellStyle(centerStyle);
                }
            }
            case "candidateName" -> {
                cell.setCellValue(r.candidateName() != null ? r.candidateName() : "");
                cell.setCellStyle(defaultStyle);
            }
            case "candidateEmail" -> {
                cell.setCellValue(r.candidateEmail() != null ? r.candidateEmail() : "");
                cell.setCellStyle(defaultStyle);
            }
            case "offeredPositionTitle" -> {
                cell.setCellValue(r.offeredPositionTitle() != null ? r.offeredPositionTitle() : "");
                cell.setCellStyle(defaultStyle);
            }
            case "proposedSalary" -> {
                if (r.proposedSalary() != null) {
                    cell.setCellValue(r.proposedSalary().doubleValue());
                    cell.setCellStyle(salaryStyle);
                } else {
                    cell.setCellStyle(salaryStyle);
                }
            }
            case "probationSalary" -> {
                if (r.probationSalary() != null) {
                    cell.setCellValue(r.probationSalary().doubleValue());
                    cell.setCellStyle(salaryStyle);
                } else {
                    cell.setCellStyle(salaryStyle);
                }
            }
            case "probationPercentage" -> {
                if (r.probationPercentage() != null) {
                    cell.setCellValue(r.probationPercentage());
                    cell.setCellStyle(percentStyle);
                } else {
                    cell.setCellStyle(percentStyle);
                }
            }
            case "offerStatus" -> {
                cell.setCellValue(r.offerStatus() != null ? r.offerStatus() : "");
                cell.setCellStyle(centerStyle);
            }
            case "expectedStartDate" -> {
                if (r.expectedStartDate() != null) {
                    cell.setCellValue(r.expectedStartDate());
                    cell.setCellStyle(dateStyle);
                } else {
                    cell.setCellStyle(centerStyle);
                }
            }
            case "workLocation" -> {
                cell.setCellValue(r.workLocation() != null ? r.workLocation() : "");
                cell.setCellStyle(defaultStyle);
            }
            case "createdAt" -> {
                if (r.createdAt() != null) {
                    cell.setCellValue(r.createdAt());
                    cell.setCellStyle(datetimeStyle);
                } else {
                    cell.setCellStyle(centerStyle);
                }
            }
            case "updatedAt" -> {
                if (r.updatedAt() != null) {
                    cell.setCellValue(r.updatedAt());
                    cell.setCellStyle(datetimeStyle);
                } else {
                    cell.setCellStyle(centerStyle);
                }
            }
            case "applicationId" -> {
                if (r.applicationId() != null) {
                    cell.setCellValue(r.applicationId());
                    cell.setCellStyle(centerStyle);
                } else {
                    cell.setCellStyle(centerStyle);
                }
            }
            case "candidatePhone" -> {
                cell.setCellValue(r.candidatePhone() != null ? r.candidatePhone() : "");
                cell.setCellStyle(centerStyle);
            }
            case "departmentName" -> {
                cell.setCellValue(r.departmentName() != null ? r.departmentName() : "");
                cell.setCellStyle(defaultStyle);
            }
            case "requisitionId" -> {
                if (r.requisitionId() != null) {
                    cell.setCellValue(r.requisitionId());
                    cell.setCellStyle(centerStyle);
                } else {
                    cell.setCellStyle(centerStyle);
                }
            }
            case "jobPostingId" -> {
                if (r.jobPostingId() != null) {
                    cell.setCellValue(r.jobPostingId());
                    cell.setCellStyle(centerStyle);
                } else {
                    cell.setCellStyle(centerStyle);
                }
            }
            case "probationDays" -> {
                if (r.probationDays() != null) {
                    cell.setCellValue(r.probationDays());
                    cell.setCellStyle(centerStyle);
                } else {
                    cell.setCellStyle(centerStyle);
                }
            }
            case "benefitsPackage" -> {
                cell.setCellValue(r.benefitsPackage() != null ? r.benefitsPackage() : "");
                cell.setCellStyle(wrapStyle);
            }
            case "proposedByName" -> {
                cell.setCellValue(r.proposedByName() != null ? r.proposedByName() : "");
                cell.setCellStyle(defaultStyle);
            }
            case "directorName" -> {
                cell.setCellValue(r.directorName() != null ? r.directorName() : "");
                cell.setCellStyle(defaultStyle);
            }
            case "directorDecision" -> {
                cell.setCellValue(r.directorDecision() != null ? r.directorDecision() : "");
                cell.setCellStyle(centerStyle);
            }
            case "directorComments" -> {
                cell.setCellValue(r.directorComments() != null ? r.directorComments() : "");
                cell.setCellStyle(wrapStyle);
            }
            case "approvedAt" -> {
                if (r.approvedAt() != null) {
                    cell.setCellValue(r.approvedAt());
                    cell.setCellStyle(datetimeStyle);
                } else {
                    cell.setCellStyle(centerStyle);
                }
            }
            default -> {
                cell.setCellValue("");
                cell.setCellStyle(defaultStyle);
            }
        }
    }

    // =========================================================================
    // EXCEL SHEET 2: SUMMARY
    // =========================================================================

    private void buildSummarySheet(Workbook workbook, List<OfferExportRow> rows, OfferExportRequest request, String exportedBy) {
        Sheet sheet = workbook.createSheet("Summary");

        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);

        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);

        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldFont.setFontHeightInPoints((short) 10);

        CellStyle labelStyle = workbook.createCellStyle();
        labelStyle.setFont(boldFont);

        CellStyle borderHeaderStyle = workbook.createCellStyle();
        borderHeaderStyle.setFont(boldFont);
        borderHeaderStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        borderHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        borderHeaderStyle.setBorderTop(BorderStyle.THIN);
        borderHeaderStyle.setBorderBottom(BorderStyle.THIN);
        borderHeaderStyle.setBorderLeft(BorderStyle.THIN);
        borderHeaderStyle.setBorderRight(BorderStyle.THIN);
        borderHeaderStyle.setAlignment(HorizontalAlignment.LEFT);

        CellStyle borderDataStyle = workbook.createCellStyle();
        borderDataStyle.setBorderTop(BorderStyle.THIN);
        borderDataStyle.setBorderBottom(BorderStyle.THIN);
        borderDataStyle.setBorderLeft(BorderStyle.THIN);
        borderDataStyle.setBorderRight(BorderStyle.THIN);

        CellStyle borderCenterStyle = workbook.createCellStyle();
        borderCenterStyle.cloneStyleFrom(borderDataStyle);
        borderCenterStyle.setAlignment(HorizontalAlignment.CENTER);

        // Row 0: Title
        Row r0 = sheet.createRow(0);
        Cell c0 = r0.createCell(0);
        c0.setCellValue("BÁO CÁO TỔNG HỢP DANH SÁCH ĐỀ XUẤT OFFER");
        c0.setCellStyle(titleStyle);

        // Row 2: Người xuất
        Row r2 = sheet.createRow(2);
        Cell c2_0 = r2.createCell(0);
        c2_0.setCellValue("Người xuất:");
        c2_0.setCellStyle(labelStyle);
        r2.createCell(1).setCellValue(exportedBy != null ? exportedBy : "HR");

        // Row 3: Thời gian xuất
        Row r3 = sheet.createRow(3);
        Cell c3_0 = r3.createCell(0);
        c3_0.setCellValue("Thời gian xuất:");
        c3_0.setCellStyle(labelStyle);
        r3.createCell(1).setCellValue(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));

        // Row 4: Phạm vi xuất
        Row r4 = sheet.createRow(4);
        Cell c4_0 = r4.createCell(0);
        c4_0.setCellValue("Phạm vi xuất:");
        c4_0.setCellStyle(labelStyle);
        String scopeDesc = switch (request.scope()) {
            case FILTERED -> "Danh sách đang lọc";
            case SELECTED -> "Các đề xuất được chọn (" + rows.size() + ")";
            case ALL -> "Toàn bộ Offer";
        };
        r4.createCell(1).setCellValue(scopeDesc);

        // Row 5: Điều kiện lọc (nếu có)
        OfferExportFilterRequest filters = request.filters();
        Row r5 = sheet.createRow(5);
        Cell c5_0 = r5.createCell(0);
        c5_0.setCellValue("Bộ lọc áp dụng:");
        c5_0.setCellStyle(labelStyle);

        String filterDesc;
        if (request.scope() == OfferExportScope.FILTERED && filters != null) {
            String kw = (filters.keyword() != null && !filters.keyword().isBlank()) ? filters.keyword().trim() : "Tất cả";
            String st = (filters.status() != null && !filters.status().isBlank() && !"ALL".equalsIgnoreCase(filters.status()))
                    ? mapOfferStatusLabel(filters.status()) : "Tất cả";
            String sortText = "EARLIEST".equalsIgnoreCase(filters.sort()) ? "Sớm nhất" :
                    "LATEST".equalsIgnoreCase(filters.sort()) ? "Muộn nhất" : "Mặc định";
            filterDesc = String.format("Từ khóa: '%s' | Trạng thái: '%s' | Sắp xếp: '%s'", kw, st, sortText);
        } else {
            filterDesc = "Không áp dụng bộ lọc";
        }
        r5.createCell(1).setCellValue(filterDesc);

        // Row 6: Tổng số Offer trong file
        Row r6 = sheet.createRow(6);
        Cell c6_0 = r6.createCell(0);
        c6_0.setCellValue("Tổng số đề xuất xuất:");
        c6_0.setCellStyle(labelStyle);
        Cell c6_1 = r6.createCell(1);
        c6_1.setCellValue(rows.size());

        // Row 8: Bảng Thống Kê Trạng Thái
        Row r8 = sheet.createRow(8);
        Cell c8_0 = r8.createCell(0);
        c8_0.setCellValue("Trạng thái đề xuất");
        c8_0.setCellStyle(borderHeaderStyle);
        Cell c8_1 = r8.createCell(1);
        c8_1.setCellValue("Số lượng");
        c8_1.setCellStyle(borderHeaderStyle);

        // Đếm theo từng trạng thái trên TẬP DỮ LIỆU ĐƯỢC XUẤT
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        statusCounts.put("Bản thảo (Draft)", 0L);
        statusCounts.put("Chờ Director duyệt (Pending_Director)", 0L);
        statusCounts.put("Director đã duyệt (Approved)", 0L);
        statusCounts.put("Director từ chối (Rejected)", 0L);
        statusCounts.put("Đã gửi ứng viên (Sent_Candidate)", 0L);
        statusCounts.put("Ứng viên đồng ý (Accepted)", 0L);
        statusCounts.put("Ứng viên từ chối (Declined)", 0L);

        for (OfferExportRow row : rows) {
            String s = row.rawOfferStatus();
            if (s == null) continue;
            switch (s.trim().toLowerCase(Locale.ROOT)) {
                case "draft" -> statusCounts.put("Bản thảo (Draft)", statusCounts.get("Bản thảo (Draft)") + 1);
                case "pending_director" -> statusCounts.put("Chờ Director duyệt (Pending_Director)", statusCounts.get("Chờ Director duyệt (Pending_Director)") + 1);
                case "director_approved", "approved" -> statusCounts.put("Director đã duyệt (Approved)", statusCounts.get("Director đã duyệt (Approved)") + 1);
                case "director_rejected", "rejected" -> statusCounts.put("Director từ chối (Rejected)", statusCounts.get("Director từ chối (Rejected)") + 1);
                case "sent_candidate" -> statusCounts.put("Đã gửi ứng viên (Sent_Candidate)", statusCounts.get("Đã gửi ứng viên (Sent_Candidate)") + 1);
                case "accepted" -> statusCounts.put("Ứng viên đồng ý (Accepted)", statusCounts.get("Ứng viên đồng ý (Accepted)") + 1);
                case "declined" -> statusCounts.put("Ứng viên từ chối (Declined)", statusCounts.get("Ứng viên từ chối (Declined)") + 1);
            }
        }

        int currRow = 9;
        for (Map.Entry<String, Long> entry : statusCounts.entrySet()) {
            Row r = sheet.createRow(currRow++);
            Cell cellLabel = r.createCell(0);
            cellLabel.setCellValue(entry.getKey());
            cellLabel.setCellStyle(borderDataStyle);

            Cell cellVal = r.createCell(1);
            cellVal.setCellValue(entry.getValue());
            cellVal.setCellStyle(borderCenterStyle);
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.setColumnWidth(0, Math.max(sheet.getColumnWidth(0), 10000));
        sheet.setColumnWidth(1, Math.max(sheet.getColumnWidth(1), 4000));
    }
}
