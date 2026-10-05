package com.group2.rms.service;

import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.repository.ApplicationRepository;
import com.group2.rms.offer.dto.CreateOfferRequest;
import com.group2.rms.offer.dto.OfferResponse;
import com.group2.rms.offer.dto.UpdateOfferRequest;
import com.group2.rms.core.exception.BaseBusinessException;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.offer.dto.OfferDetailResponse;
import com.group2.rms.offer.entity.OfferApproval;
import com.group2.rms.offer.entity.OfferNegotiation;
import com.group2.rms.offer.entity.OfferProposal;
import com.group2.rms.offer.exception.OfferValidationException;
import com.group2.rms.offer.repository.OfferApprovalRepository;
import com.group2.rms.offer.repository.OfferNegotiationRepository;
import com.group2.rms.offer.repository.OfferProposalRepository;
import com.group2.rms.offer.service.OfferServiceImpl;
import com.group2.rms.interview.repository.InterviewFinalResultRepository;
import com.group2.rms.interview.entity.InterviewFinalResult;
import com.group2.rms.interview.entity.InterviewSchedule;
import com.group2.rms.candidate.entity.Candidate;
import com.group2.rms.requisition.entity.JobPosting;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.user.entity.Department;
import com.group2.rms.offer.dto.PassedCandidateResponse;
import com.group2.rms.offer.service.NotificationService;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfferServiceTests {

    @Mock
    private OfferProposalRepository offerProposalRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InterviewFinalResultRepository interviewFinalResultRepository;

    @Mock
    private OfferApprovalRepository offerApprovalRepository;

    @Mock
    private OfferNegotiationRepository offerNegotiationRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OfferServiceImpl offerService;

    @Test
    @DisplayName("Nghiệp vụ: Lương thử việc < 85% lương chính thức phải ném OfferValidationException")
    void testProbationSalaryLessThan85Percent_throwsException() {
        CreateOfferRequest request = CreateOfferRequest.builder()
                .applicationId(1)
                .offeredPositionTitle("Senior Java Engineer")
                .proposedSalary(new BigDecimal("20000000"))
                .probationSalary(new BigDecimal("16900000")) // 16.9M < 17M (85% của 20M)
                .probationDays(60)
                .expectedStartDate(LocalDate.now().plusDays(5))
                .workLocation("Trụ sở chính")
                .build();

        OfferValidationException ex = assertThrows(OfferValidationException.class, () -> {
            offerService.createOfferByHr(request);
        });

        assertTrue(ex.getMessage().contains("85%"));
        verify(offerProposalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lương thử việc đúng bằng 85% lương chính thức phải hợp lệ")
    void testProbationSalaryExactly85Percent_passes() {
        Application application = Application.builder().applicationId(1).build();
        User proposedBy = User.builder().userId(10).fullName("Hiring Manager").build();

        when(applicationRepository.findById(1)).thenReturn(Optional.of(application));
        when(userRepository.findById(10)).thenReturn(Optional.of(proposedBy));
        when(offerProposalRepository.findByApplication_ApplicationIdAndOfferStatusNotIn(eq(1), any()))
                .thenReturn(List.of());
        when(offerProposalRepository.save(any(OfferProposal.class))).thenAnswer(i -> {
            OfferProposal op = i.getArgument(0);
            op.setOfferId(101);
            return op;
        });

        CreateOfferRequest request = CreateOfferRequest.builder()
                .applicationId(1)
                .offeredPositionTitle("Senior Java Engineer")
                .proposedSalary(new BigDecimal("20000000"))
                .probationSalary(new BigDecimal("17000000")) // Đúng 85% của 20M
                .expectedStartDate(LocalDate.now().plusWeeks(2))
                .proposedById(10)
                .build();

        OfferResponse response = offerService.createOfferProposal(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("20000000"), response.getProposedSalary());
        assertEquals(new BigDecimal("17000000"), response.getProbationSalary());
        assertEquals("Pending_Director", response.getOfferStatus());
    }

    // Lưu ý: Đã loại bỏ testSingleActiveOfferRule_deactivatesOldOffers() cũ (triết lý Voided offer đang Pending_Director)
    // để đồng nhất 100% với quy tắc GBR-07 (Nhóm B bị khóa, ném OFFER_LOCKED_STATE; Nhóm A cập nhật in-place).

    @Test
    @DisplayName("Validation: Ngày bắt đầu dự kiến bằng ngày hiện tại phải bị chặn (OfferValidationException)")
    void testCreateOfferByHr_startDateToday_throwsException() {
        CreateOfferRequest request = CreateOfferRequest.builder()
                .applicationId(1)
                .offeredPositionTitle("Senior Java Engineer")
                .proposedSalary(new BigDecimal("20000000"))
                .probationSalary(new BigDecimal("17000000"))
                .probationDays(60)
                .expectedStartDate(LocalDate.now()) // Ngày hôm nay (không hợp lệ, phải > hôm nay)
                .workLocation("Trụ sở chính")
                .build();

        OfferValidationException ex = assertThrows(OfferValidationException.class, () -> {
            offerService.createOfferByHr(request);
        });

        assertTrue(ex.getMessage().contains("Ngày bắt đầu dự kiến phải lớn hơn ngày hiện tại"));
    }

    @Test
    @DisplayName("Validation: Ngày bắt đầu dự kiến trong quá khứ phải bị chặn (OfferValidationException)")
    void testCreateOfferByHr_startDatePast_throwsException() {
        CreateOfferRequest request = CreateOfferRequest.builder()
                .applicationId(1)
                .offeredPositionTitle("Senior Java Engineer")
                .proposedSalary(new BigDecimal("20000000"))
                .probationSalary(new BigDecimal("17000000"))
                .probationDays(60)
                .expectedStartDate(LocalDate.now().minusDays(1))
                .workLocation("Trụ sở chính")
                .build();

        OfferValidationException ex = assertThrows(OfferValidationException.class, () -> {
            offerService.createOfferByHr(request);
        });

        assertTrue(ex.getMessage().contains("Ngày bắt đầu dự kiến phải lớn hơn ngày hiện tại"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -60})
    @DisplayName("Validation: Thời gian thử việc <= 0 phải bị chặn (OfferValidationException)")
    void testCreateOfferByHr_probationDaysZeroOrNegative_throwsException(int days) {
        CreateOfferRequest request = CreateOfferRequest.builder()
                .applicationId(1)
                .offeredPositionTitle("Senior Java Engineer")
                .proposedSalary(new BigDecimal("20000000"))
                .probationSalary(new BigDecimal("17000000"))
                .probationDays(days) // 0, -1, -60
                .expectedStartDate(LocalDate.now().plusDays(5))
                .workLocation("Trụ sở chính")
                .build();

        OfferValidationException ex = assertThrows(OfferValidationException.class, () -> {
            offerService.createOfferByHr(request);
        });

        assertTrue(ex.getMessage().contains("Thời gian thử việc phải lớn hơn 0"));
        verify(offerProposalRepository, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({"0", "-1", "-10000000"})
    @DisplayName("Validation: Mức lương chính thức <= 0 phải bị chặn (OfferValidationException)")
    void testCreateOfferByHr_salaryZeroOrNegative_throwsException(String salaryStr) {
        CreateOfferRequest request = CreateOfferRequest.builder()
                .applicationId(1)
                .offeredPositionTitle("Senior Java Engineer")
                .proposedSalary(new BigDecimal(salaryStr)) // 0, -1, -10000000
                .probationSalary(new BigDecimal("17000000"))
                .probationDays(60)
                .expectedStartDate(LocalDate.now().plusDays(5))
                .workLocation("Trụ sở chính")
                .build();

        OfferValidationException ex = assertThrows(OfferValidationException.class, () -> {
            offerService.createOfferByHr(request);
        });

        assertTrue(ex.getMessage().contains("Mức lương chính thức phải lớn hơn 0"));
        verify(offerProposalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Validation: Địa điểm làm việc bị để trống phải bị chặn (OfferValidationException)")
    void testCreateOfferByHr_workLocationBlank_throwsException() {
        CreateOfferRequest request = CreateOfferRequest.builder()
                .applicationId(1)
                .offeredPositionTitle("Senior Java Engineer")
                .proposedSalary(new BigDecimal("20000000"))
                .probationSalary(new BigDecimal("17000000"))
                .probationDays(60)
                .expectedStartDate(LocalDate.now().plusDays(5))
                .workLocation("   ") // Rỗng
                .build();

        OfferValidationException ex = assertThrows(OfferValidationException.class, () -> {
            offerService.createOfferByHr(request);
        });

        assertTrue(ex.getMessage().contains("Địa điểm làm việc không được để trống"));
    }

    @Test
    @DisplayName("Validation: Ngày mai, lương > 0, thời gian thử việc > 0 và gói phúc lợi để trống vẫn tạo thành công")
    void testCreateOfferByHr_allValidExceptBenefits_passes() {
        Application application = Application.builder().applicationId(1).build();

        when(applicationRepository.findById(1)).thenReturn(Optional.of(application));
        when(offerProposalRepository.findByApplication_ApplicationId(1)).thenReturn(Optional.empty());
        when(offerProposalRepository.save(any(OfferProposal.class))).thenAnswer(i -> {
            OfferProposal op = i.getArgument(0);
            op.setOfferId(555);
            return op;
        });

        CreateOfferRequest request = CreateOfferRequest.builder()
                .applicationId(1)
                .offeredPositionTitle("Senior Java Engineer")
                .proposedSalary(new BigDecimal("25000000"))
                .probationSalary(new BigDecimal("21250000")) // 85%
                .probationDays(60)
                .expectedStartDate(LocalDate.now().plusDays(1)) // Ngày mai (> ngày hiện tại)
                .workLocation("Văn phòng Mộc RMS Hà Nội")
                .benefitsPackage(null) // Để trống phúc lợi -> vẫn hợp lệ
                .isDraft(false)
                .build();

        OfferResponse response = offerService.createOfferByHr(request);

        assertNotNull(response);
        assertEquals(555, response.getOfferId());
        assertEquals("Pending_Director", response.getOfferStatus());
        assertEquals(new BigDecimal("25000000"), response.getProposedSalary());
        assertNull(response.getBenefitsPackage());
    }

    @Test
    @DisplayName("Validation: Update Offer với ngày bắt đầu <= ngày hiện tại phải bị chặn")
    void testUpdateOfferByHr_invalidStartDate_throwsException() {
        OfferProposal existing = OfferProposal.builder()
                .offerId(10)
                .offerStatus("Draft")
                .build();

        when(offerProposalRepository.findById(10)).thenReturn(Optional.of(existing));

        UpdateOfferRequest request = UpdateOfferRequest.builder()
                .offeredPositionTitle("Senior Java Engineer")
                .proposedSalary(new BigDecimal("20000000"))
                .probationSalary(new BigDecimal("17000000"))
                .probationDays(60)
                .expectedStartDate(LocalDate.now()) // Không hợp lệ
                .workLocation("Trụ sở")
                .build();

        OfferValidationException ex = assertThrows(OfferValidationException.class, () -> {
            offerService.updateOfferByHr(10, request);
        });

        assertTrue(ex.getMessage().contains("Ngày bắt đầu dự kiến phải lớn hơn ngày hiện tại"));
    }

    @Test
    @DisplayName("Bean Validation: CreateOfferRequest vi phạm tất cả các ràng buộc bắt buộc ngoại trừ benefitsPackage")
    void testCreateOfferRequest_beanValidationConstraints() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        // Object rỗng/sai lệch toàn bộ
        CreateOfferRequest invalidRequest = CreateOfferRequest.builder()
                .applicationId(null)
                .offeredPositionTitle("")
                .proposedSalary(BigDecimal.ZERO)
                .probationSalary(new BigDecimal("-1000"))
                .probationDays(0)
                .expectedStartDate(LocalDate.now()) // Hôm nay -> vi phạm @Future và isExpectedStartDateValid
                .workLocation("   ")
                .benefitsPackage(null) // Không vi phạm vì không bắt buộc
                .build();

        var violations = validator.validate(invalidRequest);
        assertFalse(violations.isEmpty());

        // Kiểm tra từng trường đều có violation
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("applicationId")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("offeredPositionTitle")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("proposedSalary")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("probationSalary")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("probationDays")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("expectedStartDate") || v.getMessage().contains("Ngày bắt đầu")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("workLocation")));

        // Đảm bảo benefitsPackage KHÔNG có violation nào
        assertFalse(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("benefitsPackage")));
    }

    @Test
    @DisplayName("Bean Validation: CreateOfferRequest hợp lệ khi điền đầy đủ và để trống benefitsPackage")
    void testCreateOfferRequest_beanValidationSuccess() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        CreateOfferRequest validRequest = CreateOfferRequest.builder()
                .applicationId(10)
                .offeredPositionTitle("Senior Developer")
                .proposedSalary(new BigDecimal("30000000"))
                .probationSalary(new BigDecimal("25500000"))
                .probationDays(60)
                .expectedStartDate(LocalDate.now().plusDays(2))
                .workLocation("Mộc RMS Building")
                .benefitsPackage(null) // Để trống phúc lợi
                .build();

        var violations = validator.validate(validRequest);
        assertTrue(violations.isEmpty(), "Không được có vi phạm validation nào với request hợp lệ");
    }

    @Test
    @DisplayName("WorkLocation Precedence: Khi cả JobPosting và Candidate đều có địa chỉ, ưu tiên JobPosting")
    void testGetPassedCandidatesForOffer_whenBothLocationsPresent_prefersJobPostingLocation() {
        JobPosting jp = JobPosting.builder()
                .jobPostingId(101)
                .postingTitle("Senior Java Backend Engineer")
                .workLocation("Tầng 8, Tòa nhà RMS Tower, Duy Tân, Cầu Giấy, Hà Nội")
                .build();

        Candidate cand = Candidate.builder()
                .candidateId(201)
                .account(User.builder().userId(301).fullName("Nguyễn Văn A").email("a@test.com").phoneNumber("0987654321").build())
                .address("Cầu Giấy, Hà Nội")
                .build();

        Application app = Application.builder()
                .applicationId(12)
                .jobPosting(jp)
                .candidate(cand)
                .build();

        InterviewSchedule schedule = InterviewSchedule.builder().application(app).build();
        InterviewFinalResult result = InterviewFinalResult.builder()
                .finalResultId(1)
                .interviewSchedule(schedule)
                .finalDecision("Passed")
                .finalSummaryComments("Tốt")
                .recommendedSalary(new BigDecimal("35000000"))
                .build();

        when(interviewFinalResultRepository.findAllPassedWithDetails()).thenReturn(List.of(result));

        List<PassedCandidateResponse> list = offerService.getPassedCandidatesForOffer();

        assertNotNull(list);
        assertEquals(1, list.size());
        // Khẳng định ưu tiên JobPosting.workLocation
        assertEquals("Tầng 8, Tòa nhà RMS Tower, Duy Tân, Cầu Giấy, Hà Nội", list.get(0).getWorkLocation());
        assertEquals("Nguyễn Văn A", list.get(0).getCandidateName());
    }

    @Test
    @DisplayName("WorkLocation Precedence: Khi JobPosting không có địa chỉ, fallback về Candidate.address")
    void testGetPassedCandidatesForOffer_whenJobLocationMissing_fallbackToCandidateAddress() {
        JobPosting jp = JobPosting.builder()
                .jobPostingId(102)
                .postingTitle("Frontend React Engineer")
                .workLocation(null) // Không có workLocation
                .build();

        Candidate cand = Candidate.builder()
                .candidateId(202)
                .account(User.builder().userId(302).fullName("Trần Thị B").email("b@test.com").phoneNumber("0912345678").build())
                .address("Thanh Xuân, Hà Nội") // Có address
                .build();

        Application app = Application.builder()
                .applicationId(13)
                .jobPosting(jp)
                .candidate(cand)
                .build();

        InterviewSchedule schedule = InterviewSchedule.builder().application(app).build();
        InterviewFinalResult result = InterviewFinalResult.builder()
                .finalResultId(2)
                .interviewSchedule(schedule)
                .finalDecision("Passed")
                .finalSummaryComments("Tốt")
                .recommendedSalary(new BigDecimal("22000000"))
                .build();

        when(interviewFinalResultRepository.findAllPassedWithDetails()).thenReturn(List.of(result));

        List<PassedCandidateResponse> list = offerService.getPassedCandidatesForOffer();

        assertNotNull(list);
        assertEquals(1, list.size());
        // Khẳng định fallback về Candidate.address
        assertEquals("Thanh Xuân, Hà Nội", list.get(0).getWorkLocation());
        assertEquals("Trần Thị B", list.get(0).getCandidateName());
    }

    @Test
    @DisplayName("WorkLocation Precedence: Khi cả JobPosting và Candidate đều không có địa chỉ, fallback về default")
    void testGetPassedCandidatesForOffer_whenBothLocationsMissing_fallbackToDefaultLocation() {
        JobPosting jp = JobPosting.builder()
                .jobPostingId(103)
                .postingTitle("DevOps Engineer")
                .workLocation("   ") // Rỗng
                .build();

        Candidate cand = Candidate.builder()
                .candidateId(203)
                .account(User.builder().userId(303).fullName("Lê Văn C").email("c@test.com").phoneNumber("0900112233").build())
                .address(null) // Trống
                .build();

        Application app = Application.builder()
                .applicationId(14)
                .jobPosting(jp)
                .candidate(cand)
                .build();

        InterviewSchedule schedule = InterviewSchedule.builder().application(app).build();
        InterviewFinalResult result = InterviewFinalResult.builder()
                .finalResultId(3)
                .interviewSchedule(schedule)
                .finalDecision("Passed")
                .finalSummaryComments("Tốt")
                .recommendedSalary(new BigDecimal("28000000"))
                .build();

        when(interviewFinalResultRepository.findAllPassedWithDetails()).thenReturn(List.of(result));

        List<PassedCandidateResponse> list = offerService.getPassedCandidatesForOffer();

        assertNotNull(list);
        assertEquals(1, list.size());
        // Khẳng định fallback về giá trị mặc định của hệ thống
        assertEquals("Trụ sở chính Mộc RMS", list.get(0).getWorkLocation());
    }

    @Test
    @DisplayName("Nghiệp vụ: Lương thử việc lớn hơn lương chính thức phải ném OfferValidationException")
    void testCreateOfferByHr_probationSalaryGreaterThanProposed_throwsException() {
        CreateOfferRequest request = CreateOfferRequest.builder()
                .applicationId(10)
                .offeredPositionTitle("Backend Dev")
                .proposedSalary(new BigDecimal("20000000"))
                .probationSalary(new BigDecimal("25000000")) // > 20.000.000
                .probationDays(60)
                .expectedStartDate(LocalDate.now().plusDays(2))
                .workLocation("Trụ sở chính")
                .build();

        OfferValidationException ex = assertThrows(OfferValidationException.class, () -> {
            offerService.createOfferByHr(request);
        });

        assertTrue(ex.getMessage().contains("Lương thử việc không được vượt quá lương chính thức"));
    }

    @Test
    @DisplayName("Bean Validation: Lương thử việc lớn hơn lương chính thức vi phạm validation")
    void testCreateOfferRequest_probationSalaryGreaterThanProposed_hasViolation() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        CreateOfferRequest request = CreateOfferRequest.builder()
                .applicationId(10)
                .offeredPositionTitle("Backend Dev")
                .proposedSalary(new BigDecimal("20000000"))
                .probationSalary(new BigDecimal("21000000")) // > 20.000.000
                .probationDays(60)
                .expectedStartDate(LocalDate.now().plusDays(2))
                .workLocation("Trụ sở chính")
                .build();

        var violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Lương thử việc không được vượt quá lương chính thức")));
    }

    @Test
    @DisplayName("GBR-07 getPassedCandidatesForOffer: Loại bỏ ứng viên có Offer Nhóm B và giữ lại ứng viên Nhóm A (Draft) hoặc chưa có Offer")
    void testGetPassedCandidatesForOffer_filtersGroupB_keepsGroupA() {
        // App 1: có offer Pending_Director (Nhóm B -> BỊ LOẠI)
        Application app1 = Application.builder().applicationId(1).build();
        InterviewSchedule s1 = InterviewSchedule.builder().application(app1).build();
        InterviewFinalResult r1 = InterviewFinalResult.builder().finalResultId(1).interviewSchedule(s1).finalDecision("Passed").build();

        // App 2: có offer Accepted (Nhóm B -> BỊ LOẠI)
        Application app2 = Application.builder().applicationId(2).build();
        InterviewSchedule s2 = InterviewSchedule.builder().application(app2).build();
        InterviewFinalResult r2 = InterviewFinalResult.builder().finalResultId(2).interviewSchedule(s2).finalDecision("Passed").build();

        // App 3: có offer Draft (Nhóm A duy nhất -> ĐƯỢC PHÉP HIỂN THỊ ĐỂ TẠO ĐÈ)
        Application app3 = Application.builder().applicationId(3).build();
        InterviewSchedule s3 = InterviewSchedule.builder().application(app3).build();
        InterviewFinalResult r3 = InterviewFinalResult.builder().finalResultId(3).interviewSchedule(s3).finalDecision("Passed").build();

        // App 4: chưa có Offer nào -> ĐƯỢC PHÉP HIỂN THỊ
        Application app4 = Application.builder().applicationId(4).build();
        InterviewSchedule s4 = InterviewSchedule.builder().application(app4).build();
        InterviewFinalResult r4 = InterviewFinalResult.builder().finalResultId(4).interviewSchedule(s4).finalDecision("Passed").build();

        // App 5: có offer Rejected (Nhóm B -> BỊ LOẠI)
        Application app5 = Application.builder().applicationId(5).build();
        InterviewSchedule s5 = InterviewSchedule.builder().application(app5).build();
        InterviewFinalResult r5 = InterviewFinalResult.builder().finalResultId(5).interviewSchedule(s5).finalDecision("Passed").build();

        OfferProposal offer1 = OfferProposal.builder().offerId(101).offerStatus("Pending_Director").build();
        OfferProposal offer2 = OfferProposal.builder().offerId(102).offerStatus("Accepted").build();
        OfferProposal offer3 = OfferProposal.builder().offerId(103).offerStatus("Draft").build();
        OfferProposal offer5 = OfferProposal.builder().offerId(105).offerStatus("Rejected").build();

        when(interviewFinalResultRepository.findAllPassedWithDetails()).thenReturn(List.of(r1, r2, r3, r4, r5));
        when(offerProposalRepository.findByApplication_ApplicationId(1)).thenReturn(Optional.of(offer1));
        when(offerProposalRepository.findByApplication_ApplicationId(2)).thenReturn(Optional.of(offer2));
        when(offerProposalRepository.findByApplication_ApplicationId(3)).thenReturn(Optional.of(offer3));
        when(offerProposalRepository.findByApplication_ApplicationId(4)).thenReturn(Optional.empty());
        when(offerProposalRepository.findByApplication_ApplicationId(5)).thenReturn(Optional.of(offer5));

        List<PassedCandidateResponse> list = offerService.getPassedCandidatesForOffer();

        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals(3, list.get(0).getApplicationId());
        assertEquals("Draft", list.get(0).getExistingOfferStatus());
        assertEquals(4, list.get(1).getApplicationId());
        assertNull(list.get(1).getExistingOfferStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Pending_Director", "Approved", "Director_Approved", "Sent_Candidate", "Accepted",
            "Rejected", "Director_Rejected", "Negotiating", "Declined", "Canceled", "Voided"
    })
    @DisplayName("GBR-07 createOfferByHr: Đơn ứng tuyển đang có Offer thuộc Nhóm B bị chặn ném BaseBusinessException (OFFER_LOCKED_STATE)")
    void testCreateOfferByHr_existingOfferInGroupB_throwsException(String lockedStatus) {
        Application application = Application.builder().applicationId(1).build();
        OfferProposal existingOffer = OfferProposal.builder()
                .offerId(100)
                .application(application)
                .offerStatus(lockedStatus) // Nhóm B
                .isDeleted(false)
                .build();

        when(applicationRepository.findById(1)).thenReturn(Optional.of(application));
        when(offerProposalRepository.findByApplication_ApplicationId(1)).thenReturn(Optional.of(existingOffer));

        CreateOfferRequest request = CreateOfferRequest.builder()
                .applicationId(1)
                .offeredPositionTitle("Backend Dev")
                .proposedSalary(new BigDecimal("20000000"))
                .probationSalary(new BigDecimal("17000000"))
                .probationDays(60)
                .expectedStartDate(LocalDate.now().plusDays(5))
                .workLocation("Trụ sở chính")
                .isDraft(false)
                .build();

        BaseBusinessException ex = assertThrows(BaseBusinessException.class, () -> {
            offerService.createOfferByHr(request);
        });

        assertEquals("OFFER_LOCKED_STATE", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Nhóm B"));
        verify(offerProposalRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Draft"})
    @DisplayName("GBR-07 createOfferByHr: Đơn ứng tuyển đang có Offer thuộc Nhóm A (Draft) được phép tạo đè in-place thành công")
    void testCreateOfferByHr_existingOfferInGroupA_overridesSuccessfully(String overridableStatus) {
        Application application = Application.builder().applicationId(1).build();
        OfferProposal existingOffer = OfferProposal.builder()
                .offerId(100)
                .application(application)
                .offeredPositionTitle("Old Title")
                .proposedSalary(new BigDecimal("15000000"))
                .probationSalary(new BigDecimal("13000000"))
                .offerStatus(overridableStatus) // Nhóm A (Draft)
                .isDeleted(false)
                .build();

        when(applicationRepository.findById(1)).thenReturn(Optional.of(application));
        when(offerProposalRepository.findByApplication_ApplicationId(1)).thenReturn(Optional.of(existingOffer));
        when(offerProposalRepository.save(any(OfferProposal.class))).thenAnswer(i -> i.getArgument(0));

        CreateOfferRequest request = CreateOfferRequest.builder()
                .applicationId(1)
                .offeredPositionTitle("New Senior Title")
                .proposedSalary(new BigDecimal("25000000"))
                .probationSalary(new BigDecimal("21500000"))
                .probationDays(60)
                .expectedStartDate(LocalDate.now().plusDays(5))
                .workLocation("Văn phòng mới")
                .isDraft(false)
                .build();

        OfferResponse response = offerService.createOfferByHr(request);

        assertNotNull(response);
        assertEquals(100, response.getOfferId(), "Offer ID phải được giữ nguyên khi cập nhật in-place");
        assertEquals("New Senior Title", response.getOfferedPositionTitle());
        assertEquals(new BigDecimal("25000000"), response.getProposedSalary());
        assertEquals("Pending_Director", response.getOfferStatus());
        verify(offerProposalRepository).save(any(OfferProposal.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Draft"})
    @DisplayName("GBR-07 updateOfferByHr: Cho phép cập nhật Offer khi ở trạng thái thuộc Nhóm A (Draft)")
    void testUpdateOfferByHr_groupA_allowed(String editableStatus) {
        OfferProposal offer = OfferProposal.builder()
                .offerId(50)
                .offerStatus(editableStatus) // Nhóm A (Draft)
                .build();

        when(offerProposalRepository.findById(50)).thenReturn(Optional.of(offer));
        when(offerProposalRepository.save(any(OfferProposal.class))).thenAnswer(i -> i.getArgument(0));

        UpdateOfferRequest request = UpdateOfferRequest.builder()
                .offeredPositionTitle("Lead Developer")
                .proposedSalary(new BigDecimal("40000000"))
                .probationSalary(new BigDecimal("35000000"))
                .probationDays(60)
                .expectedStartDate(LocalDate.now().plusDays(10))
                .workLocation("Trụ sở chính")
                .isDraft(true)
                .build();

        OfferResponse response = offerService.updateOfferByHr(50, request);

        assertNotNull(response);
        assertEquals("Draft", response.getOfferStatus());
        assertEquals(new BigDecimal("40000000"), response.getProposedSalary());
        verify(offerProposalRepository).save(any(OfferProposal.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Pending_Director", "Approved", "Director_Approved", "Sent_Candidate", "Accepted",
            "Rejected", "Director_Rejected", "Negotiating", "Declined", "Canceled", "Voided"
    })
    @DisplayName("GBR-07 updateOfferByHr: Cố tình cập nhật Offer ở trạng thái Nhóm B bị chặn ném BaseBusinessException (OFFER_STATUS_INVALID)")
    void testUpdateOfferByHr_groupB_throwsException(String lockedStatus) {
        OfferProposal offer = OfferProposal.builder()
                .offerId(50)
                .offerStatus(lockedStatus) // Nhóm B
                .build();

        when(offerProposalRepository.findById(50)).thenReturn(Optional.of(offer));

        UpdateOfferRequest request = UpdateOfferRequest.builder()
                .proposedSalary(new BigDecimal("40000000"))
                .probationSalary(new BigDecimal("35000000"))
                .probationDays(60)
                .expectedStartDate(LocalDate.now().plusDays(10))
                .workLocation("Trụ sở chính")
                .build();

        BaseBusinessException ex = assertThrows(BaseBusinessException.class, () -> {
            offerService.updateOfferByHr(50, request);
        });

        assertEquals("OFFER_STATUS_INVALID", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Nhóm A"));
        verify(offerProposalRepository, never()).save(any());
    }

    @Test
    @DisplayName("getOfferById: Tìm thấy Offer thành công trả về OfferResponse")
    void testGetOfferById_found_returnsOfferResponse() {
        OfferProposal offer = OfferProposal.builder()
                .offerId(1)
                .offeredPositionTitle("Senior Developer")
                .proposedSalary(new BigDecimal("30000000"))
                .probationSalary(new BigDecimal("25500000"))
                .offerStatus("Draft")
                .build();

        when(offerProposalRepository.findById(1)).thenReturn(Optional.of(offer));

        OfferResponse response = offerService.getOfferById(1);

        assertNotNull(response);
        assertEquals(1, response.getOfferId());
        assertEquals("Senior Developer", response.getOfferedPositionTitle());
    }

    @Test
    @DisplayName("getOfferById: Không tìm thấy Offer ném ResourceNotFoundException")
    void testGetOfferById_notFound_throwsException() {
        when(offerProposalRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            offerService.getOfferById(999);
        });
    }

    @Test
    @DisplayName("getOfferByApplicationId: Tìm thấy Offer theo ApplicationId thành công")
    void testGetOfferByApplicationId_found_returnsOfferResponse() {
        OfferProposal offer = OfferProposal.builder()
                .offerId(5)
                .offerStatus("Pending_Director")
                .build();

        when(offerProposalRepository.findByApplication_ApplicationId(10)).thenReturn(Optional.of(offer));

        OfferResponse response = offerService.getOfferByApplicationId(10);

        assertNotNull(response);
        assertEquals(5, response.getOfferId());
        assertEquals("Pending_Director", response.getOfferStatus());
    }

    @Test
    @DisplayName("getOfferByApplicationId: Không tìm thấy Offer theo ApplicationId ném ResourceNotFoundException")
    void testGetOfferByApplicationId_notFound_throwsException() {
        when(offerProposalRepository.findByApplication_ApplicationId(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            offerService.getOfferByApplicationId(999);
        });
    }

    @Test
    @DisplayName("getAllOffersForHr: Lấy toàn bộ Offer (status = null hoặc ALL)")
    void testGetAllOffersForHr_allStatus_queriesActive() {
        PageRequest pageable = PageRequest.of(0, 10);
        OfferProposal offer = OfferProposal.builder().offerId(1).offerStatus("Draft").build();
        Page<OfferProposal> page = new PageImpl<>(List.of(offer), pageable, 1);

        when(offerProposalRepository.findAllActiveByOrderByOfferIdAsc(pageable)).thenReturn(page);

        Page<OfferResponse> resultAll = offerService.getAllOffersForHr("ALL", pageable);
        assertNotNull(resultAll);
        assertEquals(1, resultAll.getTotalElements());

        Page<OfferResponse> resultNull = offerService.getAllOffersForHr(null, pageable);
        assertNotNull(resultNull);
        assertEquals(1, resultNull.getTotalElements());
    }

    @Test
    @DisplayName("getAllOffersForHr: Lấy Offer theo Status cụ thể")
    void testGetAllOffersForHr_filteredStatus_queriesByStatus() {
        PageRequest pageable = PageRequest.of(0, 10);
        OfferProposal offer = OfferProposal.builder().offerId(2).offerStatus("Pending_Director").build();
        Page<OfferProposal> page = new PageImpl<>(List.of(offer), pageable, 1);

        when(offerProposalRepository.findActiveByStatusOrderByOfferIdAsc("Pending_Director", pageable)).thenReturn(page);

        Page<OfferResponse> result = offerService.getAllOffersForHr("Pending_Director", pageable);
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Pending_Director", result.getContent().get(0).getOfferStatus());
    }

    @Test
    @DisplayName("getOfferDetailForHr: Lấy chi tiết Offer đầy đủ Candidate, Lịch sử duyệt của Director và Đàm phán")
    void testGetOfferDetailForHr_returnsComprehensiveDetails() {
        Department dept = Department.builder().departmentId(1).departmentName("Phòng Công Nghệ").build();
        JobRequisition req = JobRequisition.builder().requisitionId(10).department(dept).build();
        JobPosting jp = JobPosting.builder().jobPostingId(20).postingTitle("Backend Engineer").requisition(req).build();
        User candUser = User.builder().userId(100).fullName("Trần Thị B").email("b@test.com").phoneNumber("0912345678").build();
        Candidate cand = Candidate.builder().candidateId(200).account(candUser).address("Đà Nẵng").build();
        Application app = Application.builder().applicationId(300).jobPosting(jp).candidate(cand).appliedCvUrl("cv.pdf").build();

        OfferProposal offer = OfferProposal.builder()
                .offerId(1)
                .application(app)
                .offeredPositionTitle("Backend Engineer")
                .proposedSalary(new BigDecimal("35000000"))
                .probationSalary(new BigDecimal("30000000"))
                .offerStatus("Approved")
                .build();

        User director = User.builder().userId(2).fullName("Nguyễn Giám Đốc").build();
        OfferApproval approval = OfferApproval.builder()
                .offerApprovalId(1)
                .director(director)
                .status("Approved")
                .directorComments("Đồng ý tuyển dụng")
                .build();

        OfferNegotiation negotiation = OfferNegotiation.builder()
                .negotiationId(1)
                .candidateCounterSalary(new BigDecimal("38000000"))
                .candidateNotes("Mong muốn mức lương cao hơn")
                .hrResponseNotes("Đồng ý điều chỉnh lên 35M")
                .build();

        InterviewFinalResult finalResult = InterviewFinalResult.builder()
                .finalResultId(1)
                .finalDecision("Passed")
                .finalSummaryComments("Ứng viên xuất sắc")
                .recommendedSalary(new BigDecimal("35000000"))
                .build();

        when(offerProposalRepository.findById(1)).thenReturn(Optional.of(offer));
        when(interviewFinalResultRepository.findByApplicationIdOrderByApprovedAtDesc(300)).thenReturn(List.of(finalResult));
        when(offerApprovalRepository.findByOfferProposal_OfferIdOrderByApprovedAtDesc(1)).thenReturn(List.of(approval));
        when(offerNegotiationRepository.findByOfferProposal_OfferIdOrderByNegotiationDateDesc(1)).thenReturn(List.of(negotiation));

        OfferDetailResponse detail = offerService.getOfferDetailForHr(1);

        assertNotNull(detail);
        assertEquals(1, detail.getOfferId());
        assertEquals("Backend Engineer", detail.getOfferedPositionTitle());
        assertEquals("Trần Thị B", detail.getCandidateName());
        assertEquals("b@test.com", detail.getCandidateEmail());
        assertEquals("Phòng Công Nghệ", detail.getDepartmentName());
        assertEquals("Passed", detail.getFinalDecision());
        assertEquals(1, detail.getApprovalHistory().size());
        assertEquals("Nguyễn Giám Đốc", detail.getApprovalHistory().get(0).getDirectorName());
        assertEquals(1, detail.getNegotiationHistory().size());
        assertEquals(new BigDecimal("38000000"), detail.getNegotiationHistory().get(0).getCandidateCounterSalary());
    }

    @Test
    @DisplayName("deleteDraftOfferByHr: Xóa bản thảo Offer khi ở trạng thái Draft thành công (Soft delete)")
    void testDeleteDraftOfferByHr_draftStatus_softDeletesSuccessfully() {
        OfferProposal offer = OfferProposal.builder()
                .offerId(1)
                .offerStatus("Draft")
                .isDeleted(false)
                .build();

        when(offerProposalRepository.findById(1)).thenReturn(Optional.of(offer));

        offerService.deleteDraftOfferByHr(1);

        assertTrue(offer.getIsDeleted());
        verify(offerProposalRepository).save(offer);
    }

    @Test
    @DisplayName("deleteDraftOfferByHr: Cố tình xóa Offer không phải Draft ném BaseBusinessException")
    void testDeleteDraftOfferByHr_notDraft_throwsException() {
        OfferProposal offer = OfferProposal.builder()
                .offerId(1)
                .offerStatus("Pending_Director")
                .isDeleted(false)
                .build();

        when(offerProposalRepository.findById(1)).thenReturn(Optional.of(offer));

        BaseBusinessException ex = assertThrows(BaseBusinessException.class, () -> {
            offerService.deleteDraftOfferByHr(1);
        });

        assertEquals("OFFER_NOT_DRAFT", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Chỉ được phép xóa bản thảo"));
    }

    @Test
    @DisplayName("sendOfferToCandidate: HR gửi Offer đã duyệt cho ứng viên thành công, đổi trạng thái Application sang Offered")
    void testSendOfferToCandidate_approvedStatus_success() {
        Application app = Application.builder().applicationId(10).applicationStatus("Interview_Passed").build();
        OfferProposal offer = OfferProposal.builder()
                .offerId(1)
                .application(app)
                .offerStatus("Approved")
                .build();

        when(offerProposalRepository.findById(1)).thenReturn(Optional.of(offer));
        when(offerProposalRepository.save(any(OfferProposal.class))).thenAnswer(i -> i.getArgument(0));

        OfferResponse response = offerService.sendOfferToCandidate(1);

        assertNotNull(response);
        assertEquals("Sent_Candidate", response.getOfferStatus());
        assertEquals("Offered", app.getApplicationStatus());
        verify(applicationRepository).save(app);
        verify(notificationService).sendOfferLetterToCandidate(offer);
    }

    @Test
    @DisplayName("sendOfferToCandidate: Offer chưa được duyệt ném BaseBusinessException")
    void testSendOfferToCandidate_notApprovedStatus_throwsException() {
        OfferProposal offer = OfferProposal.builder()
                .offerId(1)
                .offerStatus("Draft")
                .build();

        when(offerProposalRepository.findById(1)).thenReturn(Optional.of(offer));

        BaseBusinessException ex = assertThrows(BaseBusinessException.class, () -> {
            offerService.sendOfferToCandidate(1);
        });

        assertEquals("OFFER_NOT_APPROVED", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Chỉ được gửi thư mời nhận việc khi Offer đã được Director phê duyệt"));
    }
}
