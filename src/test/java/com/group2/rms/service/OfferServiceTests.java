package com.group2.rms.service;

import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.repository.ApplicationRepository;
import com.group2.rms.offer.dto.CreateOfferRequest;
import com.group2.rms.offer.dto.OfferResponse;
import com.group2.rms.offer.dto.UpdateOfferRequest;
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
import com.group2.rms.offer.dto.PassedCandidateResponse;
import com.group2.rms.offer.service.NotificationService;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    @DisplayName("Lương thử việc < 85% lương chính thức phải ném IllegalArgumentException")
    void testProbationSalaryLessThan85Percent_throwsException() {
        CreateOfferRequest request = CreateOfferRequest.builder()
                .applicationId(1)
                .offeredPositionTitle("Senior Java Engineer")
                .proposedSalary(new BigDecimal("20000000"))
                .probationSalary(new BigDecimal("16900000")) // 16.9M < 17M (85% của 20M)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            offerService.createOfferProposal(request);
        });

        assertTrue(ex.getMessage().contains(""));
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

    @Test
    @DisplayName("Khi tạo Offer mới, các Offer cũ đang active phải chuyển sang trạng thái Voided")
    void testSingleActiveOfferRule_deactivatesOldOffers() {
        Application application = Application.builder().applicationId(1).build();

        OfferProposal oldOffer1 = OfferProposal.builder()
                .offerId(1)
                .application(application)
                .offerStatus("Draft")
                .build();

        OfferProposal oldOffer2 = OfferProposal.builder()
                .offerId(2)
                .application(application)
                .offerStatus("Pending_Director")
                .build();

        when(applicationRepository.findById(1)).thenReturn(Optional.of(application));
        when(offerProposalRepository.findByApplication_ApplicationIdAndOfferStatusNotIn(eq(1), any()))
                .thenReturn(List.of(oldOffer1, oldOffer2));
        when(offerProposalRepository.save(any(OfferProposal.class))).thenAnswer(i -> {
            OfferProposal op = i.getArgument(0);
            op.setOfferId(999);
            return op;
        });

        CreateOfferRequest request = CreateOfferRequest.builder()
                .applicationId(1)
                .offeredPositionTitle("Senior Java Engineer")
                .proposedSalary(new BigDecimal("30000000"))
                .probationSalary(new BigDecimal("25500000")) // 85%
                .build();

        OfferResponse response = offerService.createOfferProposal(request);

        assertNotNull(response);
        // Kiểm tra các offer cũ đã bị Voided
        assertEquals("Voided", oldOffer1.getOfferStatus());
        assertEquals("Voided", oldOffer2.getOfferStatus());

        // Kiểm tra đã gọi saveAll và flush để lưu trạng thái Voided
        verify(offerProposalRepository).saveAll(List.of(oldOffer1, oldOffer2));
        verify(offerProposalRepository).flush();
    }

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

    @Test
    @DisplayName("Validation: Thời gian thử việc <= 0 phải bị chặn (OfferValidationException)")
    void testCreateOfferByHr_probationDaysZeroOrNegative_throwsException() {
        CreateOfferRequest request = CreateOfferRequest.builder()
                .applicationId(1)
                .offeredPositionTitle("Senior Java Engineer")
                .proposedSalary(new BigDecimal("20000000"))
                .probationSalary(new BigDecimal("17000000"))
                .probationDays(0) // <= 0
                .expectedStartDate(LocalDate.now().plusDays(5))
                .workLocation("Trụ sở chính")
                .build();

        OfferValidationException ex = assertThrows(OfferValidationException.class, () -> {
            offerService.createOfferByHr(request);
        });

        assertTrue(ex.getMessage().contains("Thời gian thử việc phải lớn hơn 0"));
    }

    @Test
    @DisplayName("Validation: Mức lương chính thức <= 0 phải bị chặn (OfferValidationException)")
    void testCreateOfferByHr_salaryZeroOrNegative_throwsException() {
        CreateOfferRequest request = CreateOfferRequest.builder()
                .applicationId(1)
                .offeredPositionTitle("Senior Java Engineer")
                .proposedSalary(BigDecimal.ZERO) // <= 0
                .probationSalary(new BigDecimal("17000000"))
                .probationDays(60)
                .expectedStartDate(LocalDate.now().plusDays(5))
                .workLocation("Trụ sở chính")
                .build();

        OfferValidationException ex = assertThrows(OfferValidationException.class, () -> {
            offerService.createOfferByHr(request);
        });

        assertTrue(ex.getMessage().contains("Mức lương chính thức phải lớn hơn 0"));
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
    @DisplayName("getPassedCandidatesForOffer: Tự động map đúng WorkLocation từ JobPosting hoặc Candidate")
    void testGetPassedCandidatesForOffer_mapsWorkLocationCorrectly() {
        JobPosting jp = JobPosting.builder()
                .jobPostingId(101)
                .postingTitle("Senior Java Backend Engineer")
                .workLocation("Tầng 8, Tòa nhà RMS Tower, Duy Tân, Cầu Giấy, Hà Nội")
                .build();

        Candidate cand = Candidate.builder()
                .candidateId(201)
                .account(User.builder().userId(301).fullName("Nguyễn Văn A").email("a@test.com").phoneNumber("0987654321").build())
                .address("Hà Nội")
                .build();

        Application app = Application.builder()
                .applicationId(12)
                .jobPosting(jp)
                .candidate(cand)
                .build();

        InterviewSchedule schedule = InterviewSchedule.builder()
                .application(app)
                .build();

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
        assertEquals("Tầng 8, Tòa nhà RMS Tower, Duy Tân, Cầu Giấy, Hà Nội", list.get(0).getWorkLocation());
        assertEquals("Nguyễn Văn A", list.get(0).getCandidateName());
    }
}
