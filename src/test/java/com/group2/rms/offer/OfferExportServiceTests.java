package com.group2.rms.offer;

import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.candidate.domain.entity.Candidate;
import com.group2.rms.core.exception.BaseBusinessException;
import com.group2.rms.offer.dto.OfferExportFilterRequest;
import com.group2.rms.offer.dto.OfferExportRequest;
import com.group2.rms.offer.dto.OfferExportScope;
import com.group2.rms.offer.entity.OfferProposal;
import com.group2.rms.offer.repository.OfferApprovalRepository;
import com.group2.rms.offer.repository.OfferProposalRepository;
import com.group2.rms.offer.service.OfferExportServiceImpl;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.User;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfferExportServiceTests {

    @Mock
    private OfferProposalRepository offerProposalRepository;

    @Mock
    private OfferApprovalRepository offerApprovalRepository;

    @InjectMocks
    private OfferExportServiceImpl exportService;

    private OfferProposal mockOffer1;
    private OfferProposal mockOffer2;

    @BeforeEach
    void setUp() {
        Department dept = Department.builder().departmentId(1).departmentName("Phòng Kỹ thuật").build();
        User candUser = User.builder().userId(10).fullName("Nguyễn Văn A").email("a.nguyen@test.com").phoneNumber("0987654321").build();
        Candidate cand = Candidate.builder().candidateId(20).account(candUser).build();
        JobRequisition req = JobRequisition.builder().requisitionId(30).department(dept).build();
        JobPosting posting = JobPosting.builder().jobPostingId(40).requisition(req).postingTitle("Senior Java Developer").build();
        Application app = Application.builder().applicationId(50).candidate(cand).jobPosting(posting).build();

        User hrUser = User.builder().userId(1).fullName("HR Lan").build();

        mockOffer1 = OfferProposal.builder()
                .offerId(1)
                .application(app)
                .offeredPositionTitle("Senior Java Developer")
                .proposedSalary(new BigDecimal("36000000"))
                .probationSalary(new BigDecimal("32000000"))
                .expectedStartDate(LocalDate.of(2026, 11, 1))
                .workLocation("Hà Nội")
                .benefitsPackage("Bảo hiểm PVI, 14 ngày phép")
                .proposedBy(hrUser)
                .offerStatus("Sent_Candidate")
                .isDeleted(false)
                .build();
        mockOffer1.setCreatedAt(LocalDateTime.of(2026, 10, 1, 9, 0));
        mockOffer1.setUpdatedAt(LocalDateTime.of(2026, 10, 2, 10, 0));

        mockOffer2 = OfferProposal.builder()
                .offerId(2)
                .application(app)
                .offeredPositionTitle("Junior Java Developer")
                .proposedSalary(new BigDecimal("15000000"))
                .probationSalary(new BigDecimal("12750000"))
                .expectedStartDate(LocalDate.of(2026, 11, 15))
                .workLocation("Hồ Chí Minh")
                .proposedBy(hrUser)
                .offerStatus("Pending_Director")
                .isDeleted(false)
                .build();
        mockOffer2.setCreatedAt(LocalDateTime.of(2026, 10, 3, 14, 0));
        mockOffer2.setUpdatedAt(LocalDateTime.of(2026, 10, 3, 14, 0));
    }

    @Test
    void exportUsesTheSavedProbationDuration() throws Exception {
        mockOffer1.setProbationDays(45);
        when(offerProposalRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(mockOffer1));
        OfferExportRequest request = new OfferExportRequest(
                OfferExportScope.SELECTED, List.of(1), null, List.of("probationDays"));
        byte[] result = exportService.exportOffersToExcel(request, "HR");
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            assertThat(workbook.getSheet("Offers").getRow(1).getCell(0).getNumericCellValue()).isEqualTo(45);
        }
    }

    @Test
    @DisplayName("Export SELECTED scope: Thành công tạo workbook đúng số bản ghi và đúng kiểu dữ liệu cell")
    void testExportSelectedScope_Success() throws Exception {
        when(offerProposalRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(mockOffer1, mockOffer2));

        OfferExportRequest request = new OfferExportRequest(
                OfferExportScope.SELECTED,
                List.of(1, 2),
                null,
                List.of("offerId", "candidateName", "proposedSalary", "probationSalary", "probationPercentage", "offerStatus", "expectedStartDate", "createdAt")
        );

        byte[] result = exportService.exportOffersToExcel(request, "hr_tester");
        assertThat(result).isNotEmpty();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(2);
            assertThat(wb.getSheetName(0)).isEqualTo("Offers");
            assertThat(wb.getSheetName(1)).isEqualTo("Summary");

            Sheet offersSheet = wb.getSheetAt(0);
            assertThat(offersSheet.getLastRowNum()).isEqualTo(2); // Header (0) + 2 data rows (1, 2)

            Row headerRow = offersSheet.getRow(0);
            assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("Mã đề xuất");
            assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("Ứng viên");
            assertThat(headerRow.getCell(2).getStringCellValue()).isEqualTo("Lương chính thức (VNĐ)");
            assertThat(headerRow.getCell(4).getStringCellValue()).isEqualTo("Tỷ lệ thử việc");

            // Row 1 data assertions
            Row dataRow1 = offersSheet.getRow(1);
            assertThat(dataRow1.getCell(0).getNumericCellValue()).isEqualTo(1.0);
            assertThat(dataRow1.getCell(1).getStringCellValue()).isEqualTo("Nguyễn Văn A");

            // Numeric Salary Check
            Cell salaryCell = dataRow1.getCell(2);
            assertThat(salaryCell.getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(salaryCell.getNumericCellValue()).isEqualTo(36000000.0);

            // Numeric Percentage Check
            Cell pctCell = dataRow1.getCell(4);
            assertThat(pctCell.getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(pctCell.getNumericCellValue()).isCloseTo(32000000.0 / 36000000.0, org.assertj.core.data.Offset.offset(0.0001));

            // Status Label in Vietnamese
            Cell statusCell = dataRow1.getCell(5);
            assertThat(statusCell.getStringCellValue()).isEqualTo("Đã gửi thư mời");

            // Summary Sheet Check
            Sheet summarySheet = wb.getSheetAt(1);
            assertThat(summarySheet.getRow(0).getCell(0).getStringCellValue()).contains("BÁO CÁO TỔNG HỢP DANH SÁCH THƯ MỜI NHẬN VIỆC");
            assertThat(summarySheet.getRow(2).getCell(1).getStringCellValue()).isEqualTo("hr_tester");
            assertThat(summarySheet.getRow(6).getCell(1).getNumericCellValue()).isEqualTo(2.0);
        }
    }

    @Test
    @DisplayName("Export SELECTED scope: Lỗi khi không truyền offerIds")
    void testExportSelectedScope_EmptyIds_ThrowsException() {
        OfferExportRequest request = new OfferExportRequest(
                OfferExportScope.SELECTED,
                Collections.emptyList(),
                null,
                List.of("offerId", "candidateName")
        );

        assertThatThrownBy(() -> exportService.exportOffersToExcel(request, "hr"))
                .isInstanceOf(BaseBusinessException.class)
                .hasMessageContaining("Vui lòng chọn ít nhất một đề xuất");
    }

    @Test
    @DisplayName("Export FILTERED scope: Lấy toàn bộ bản ghi lọc không bị giới hạn trang")
    void testExportFilteredScope_Success() throws Exception {
        when(offerProposalRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(mockOffer1));

        OfferExportFilterRequest filters = new OfferExportFilterRequest("Java", "Sent_Candidate", "LATEST");
        OfferExportRequest request = new OfferExportRequest(
                OfferExportScope.FILTERED,
                null,
                filters,
                List.of("offerId", "candidateName", "offeredPositionTitle", "offerStatus")
        );

        byte[] result = exportService.exportOffersToExcel(request, "hr_manager");
        assertThat(result).isNotEmpty();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = wb.getSheet("Offers");
            assertThat(sheet.getLastRowNum()).isEqualTo(1);
            assertThat(sheet.getRow(1).getCell(3).getStringCellValue()).isEqualTo("Đã gửi thư mời");

            Sheet summary = wb.getSheet("Summary");
            assertThat(summary.getRow(5).getCell(1).getStringCellValue()).contains("Java");
        }
    }

    @Test
    @DisplayName("Export ALL scope: Thành công và bỏ qua soft-deleted records")
    void testExportAllScope_Success() throws Exception {
        when(offerProposalRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(mockOffer1, mockOffer2));

        OfferExportRequest request = new OfferExportRequest(
                OfferExportScope.ALL,
                null,
                null,
                List.of("offerId", "offerStatus")
        );

        byte[] result = exportService.exportOffersToExcel(request, "hr");
        assertThat(result).isNotEmpty();

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = wb.getSheet("Offers");
            assertThat(sheet.getLastRowNum()).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("Validation: Từ chối nếu danh sách columns rỗng")
    void testExport_EmptyColumns_ThrowsException() {
        OfferExportRequest request = new OfferExportRequest(
                OfferExportScope.ALL,
                null,
                null,
                Collections.emptyList()
        );

        assertThatThrownBy(() -> exportService.exportOffersToExcel(request, "hr"))
                .isInstanceOf(BaseBusinessException.class)
                .hasMessageContaining("Vui lòng chọn ít nhất một cột dữ liệu");
    }

    @Test
    @DisplayName("Validation: Từ chối nếu chứa column không nằm trong whitelist")
    void testExport_InvalidColumn_ThrowsException() {
        OfferExportRequest request = new OfferExportRequest(
                OfferExportScope.ALL,
                null,
                null,
                List.of("offerId", "passwordHash")
        );

        assertThatThrownBy(() -> exportService.exportOffersToExcel(request, "hr"))
                .isInstanceOf(BaseBusinessException.class)
                .hasMessageContaining("không hợp lệ hoặc không được phép xuất: passwordHash");
    }

    @Test
    @DisplayName("Generate filename: Sinh tên file chuẩn theo scope và ngày hiện tại")
    void testGenerateExportFilename() {
        LocalDate today = LocalDate.now();

        OfferExportRequest reqAll = new OfferExportRequest(OfferExportScope.ALL, null, null, List.of("offerId"));
        assertThat(exportService.generateExportFilename(reqAll)).isEqualTo("offers_" + today + ".xlsx");

        OfferExportRequest reqFiltered = new OfferExportRequest(OfferExportScope.FILTERED, null, null, List.of("offerId"));
        assertThat(exportService.generateExportFilename(reqFiltered)).isEqualTo("offers_filtered_" + today + ".xlsx");

        OfferExportRequest reqSelected = new OfferExportRequest(OfferExportScope.SELECTED, List.of(1, 2, 3), null, List.of("offerId"));
        assertThat(exportService.generateExportFilename(reqSelected)).isEqualTo("offers_selected_3_" + today + ".xlsx");
    }

    // =========================================================================
    // EXTENDED UNIT TESTS (English)
    // =========================================================================

    @Test
    @DisplayName("Export SELECTED scope: Throws BaseBusinessException when requested IDs return no active records")
    void testExportSelectedScope_NonExistingIds_ThrowsException() {
        when(offerProposalRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(Collections.emptyList());

        OfferExportRequest request = new OfferExportRequest(
                OfferExportScope.SELECTED,
                List.of(999, 1000),
                null,
                List.of("offerId", "candidateName")
        );

        assertThatThrownBy(() -> exportService.exportOffersToExcel(request, "hr"))
                .isInstanceOf(BaseBusinessException.class)
                .hasMessageContaining("Không có dữ liệu thư mời nhận việc phù hợp");
    }

    @Test
    @DisplayName("Export validation: Throws BaseBusinessException when export scope is null")
    void testExport_NullScope_ThrowsException() {
        OfferExportRequest request = new OfferExportRequest(
                null,
                null,
                null,
                List.of("offerId")
        );

        assertThatThrownBy(() -> exportService.exportOffersToExcel(request, "hr"))
                .isInstanceOf(BaseBusinessException.class)
                .hasMessageContaining("Phạm vi xuất dữ liệu không được để trống");
    }

    @Test
    @DisplayName("Export validation: Handles duplicate requested columns gracefully")
    void testExport_DuplicateColumns_HandlesGracefully() {
        when(offerProposalRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(mockOffer1));

        OfferExportRequest request = new OfferExportRequest(
                OfferExportScope.ALL,
                null,
                null,
                List.of("offerId", "offerId", "candidateName")
        );

        byte[] result = exportService.exportOffersToExcel(request, "hr");
        assertThat(result).isNotEmpty();
    }
}
