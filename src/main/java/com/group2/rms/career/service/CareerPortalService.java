package com.group2.rms.career.service;

import com.group2.rms.candidate.service.ApplicationSubmissionService;
import com.group2.rms.career.dto.DepartmentFilterResponse;
import com.group2.rms.career.dto.PublicJobDetailResponse;
import com.group2.rms.career.dto.PublicJobListResponse;
import com.group2.rms.career.dto.ViewerProfileResponse;
import com.group2.rms.career.repository.PublicJobSearch;
import com.group2.rms.core.exception.BaseBusinessException;
import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.requisition.service.RequisitionLabels;
import com.group2.rms.user.repository.DepartmentRepository;
import com.group2.rms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service xử lý nghiệp vụ tìm kiếm, hiển thị tin tuyển dụng và thông tin ứng tuyển trên cổng Career.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CareerPortalService {

    private final JobPostingRepository jobPostingRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final ApplicationSubmissionService applicationSubmissionService;

    /**
     * Truy vấn phân trang danh sách các tin tuyển dụng đang công khai theo các tiêu chí lọc cơ bản.
     */
    public Page<PublicJobListResponse> getPublishedJobs(
            String keyword,
            Integer departmentId,
            String employmentType,
            Pageable pageable) {
        return getPublishedJobs(keyword, departmentId, employmentType, null, null, pageable);
    }

    /**
     * Truy vấn phân trang danh sách các tin tuyển dụng đang công khai phù hợp với các tiêu chí lọc.
     */
    public Page<PublicJobListResponse> getPublishedJobs(
            String keyword,
            Integer departmentId,
            String employmentType,
            String workModel,
            String salaryRange,
            Pageable pageable) {
        List<String> terms = PublicJobSearch.terms(keyword);
        Pageable page = terms.isEmpty() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<JobPosting> jobs = jobPostingRepository.findAll(
                PublicJobSearch.openJobs(terms, departmentId, employmentType, workModel, salaryRange),
                page);
        return jobs.map(job -> new PublicJobListResponse(
                job.getJobPostingId(),
                job.getPostingTitle(),
                job.getRequisition() != null && job.getRequisition().getDepartment() != null
                        ? job.getRequisition().getDepartment().getDepartmentName()
                        : "",
                "Remote".equals(job.effectiveWorkModel()) ? "Từ xa" : job.getWorkLocation(),
                employmentTypeLabel(job),
                job.getSalaryDisplay(),
                job.getApplicationDeadline(),
                job.getPostingDate()));
    }

    /**
     * Lấy danh sách tin tuyển dụng mới đăng gần nhất phục vụ trang chủ hoặc widget đề xuất.
     */
    public List<PublicJobListResponse> getLatestJobs(int limit) {
        return getPublishedJobs(
                null,
                null,
                null,
                PageRequest.of(0, limit, Sort.by(
                        Sort.Order.desc("priority"),
                        Sort.Order.desc("postingDate")))).getContent();
    }

    private static String employmentTypeLabel(JobPosting job) {
        String type = job.effectiveEmploymentType();
        return type == null ? "" : RequisitionLabels.employmentType(type);
    }

    private String formatRichText(String text) {
        if (text == null) {
            return null;
        }
        return text.replace("\\n", "<br/>").replace("\n", "<br/>");
    }

    private List<String> formatListText(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return value.replace("\\n", "\n")
                .lines()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Lấy thông tin chi tiết tin tuyển dụng, kiểm tra trạng thái mở và định dạng văn bản mô tả.
     */
    public PublicJobDetailResponse getPublishedJobDetail(Integer id, String username) {
        JobPosting job = jobPostingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng"));

        if (!job.isVisibleToCandidates(LocalDateTime.now())) {
            throw new ResourceNotFoundException("Tin tuyển dụng này không còn khả dụng.");
        }

        boolean isAcceptingApplications = (job.getApplicationDeadline() == null
                || !job.getApplicationDeadline().isBefore(LocalDateTime.now()));

        boolean hasApplied = applicationSubmissionService.hasApplied(username, id);

        return new PublicJobDetailResponse(
                job.getJobPostingId(),
                job.getPostingTitle(),
                job.getRequisition() != null && job.getRequisition().getDepartment() != null
                        ? job.getRequisition().getDepartment().getDepartmentName()
                        : "",
                job.effectiveWorkLocation(),
                job.getWorkingHours(),
                employmentTypeLabel(job),
                RequisitionLabels.workModel(job.effectiveWorkModel()),
                job.getNumberOfPositionsDisplay(),
                job.getSalaryDisplay(),
                job.getApplicationDeadline(),
                job.getPostingDate(),
                formatRichText(job.getJobDescription()),
                formatListText(job.getJobDescription()),
                formatRichText(job.getJobRequirements()),
                formatListText(job.getJobRequirements()),
                formatRichText(job.getBenefits()),
                formatListText(job.getBenefits()),
                hasApplied,
                isAcceptingApplications);
    }

    /**
     * Kiểm tra tính khả dụng của tin tuyển dụng trước khi cho phép nộp hồ sơ.
     */
    public void validateJobForApplication(Integer id) {
        jobPostingRepository.findById(id)
                .filter(j -> j.isVisibleToCandidates(LocalDateTime.now()))
                .filter(j -> j.getApplicationDeadline() == null
                        || !j.getApplicationDeadline().isBefore(LocalDateTime.now()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tin tuyển dụng này không còn khả dụng hoặc đã ngừng nhận hồ sơ."));
    }

    /**
     * Lấy danh sách phòng ban thông minh, ưu tiên phòng ban có tin tuyển dụng lên đầu.
     */
    public List<DepartmentFilterResponse> getSmartSortedDepartments() {
        Set<Integer> activeDeptIds = jobPostingRepository.findActiveDepartmentIds(LocalDateTime.now());

        return departmentRepository.findAll().stream()
                .map(d -> new DepartmentFilterResponse(d.getDepartmentId(), d.getDepartmentName()))
                .sorted(Comparator
                        .comparing((DepartmentFilterResponse dept) -> !activeDeptIds.contains(dept.departmentId()))
                        .thenComparing(DepartmentFilterResponse::departmentName))
                .toList();
    }

    /**
     * Kiểm tra tài khoản có vai trò CANDIDATE hợp lệ trước khi cho phép nộp hồ sơ.
     */
    public void validateCandidateApplicationProfile(String username) {
        if (username == null) {
            throw new BaseBusinessException("Vui lòng đăng nhập để tiếp tục.", "UNAUTHORIZED");
        }

        userRepository.findByUsernameIgnoreCase(username)
                .filter(u -> "Candidate".equals(u.getRole().getRoleName()))
                .orElseThrow(() -> new BaseBusinessException(
                        "Bạn không có quyền truy cập trang này. Vui lòng đăng nhập với tài khoản Ứng viên.",
                        "FORBIDDEN_ROLE"));
    }

    /**
     * Truy vấn thông tin người dùng đang xem hồ sơ theo username.
     */
    public Optional<ViewerProfileResponse> getViewerProfile(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return userRepository.findByUsernameIgnoreCase(username)
                .map(u -> new ViewerProfileResponse(
                        u.getFullName(),
                        u.getEmail(),
                        u.getRole() != null ? u.getRole().getRoleName() : ""));
    }
}
