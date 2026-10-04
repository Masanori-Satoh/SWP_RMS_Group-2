package com.group2.rms.career.service;

import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.career.dto.PublicJobDetailResponse;
import com.group2.rms.career.dto.PublicJobListResponse;
import com.group2.rms.career.dto.DepartmentFilterResponse;
import com.group2.rms.career.dto.ViewerProfileResponse;
import com.group2.rms.requisition.entity.JobPosting;
import com.group2.rms.requisition.repository.JobPostingRepository;
import com.group2.rms.user.repository.DepartmentRepository;
import com.group2.rms.user.repository.UserRepository;
import com.group2.rms.candidate.repository.CandidateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Comparator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CareerPortalService {

    private final JobPostingRepository jobPostingRepository;

    private final DepartmentRepository departmentRepository;

    private final UserRepository userRepository;

    private final CandidateRepository candidateRepository;

    public Page<PublicJobListResponse> getPublishedJobs(String keyword, Integer departmentId, String employmentType,
            Pageable pageable) {
        Page<JobPosting> jobs = jobPostingRepository.findPublishedJobs(keyword, departmentId, employmentType, pageable);
        return jobs.map(job -> new PublicJobListResponse(
                job.getJobPostingId(),
                job.getPostingTitle(),
                job.getRequisition() != null && job.getRequisition().getDepartment() != null
                        ? job.getRequisition().getDepartment().getDepartmentName()
                        : "",
                job.getWorkLocation(),
                job.getRequisition() != null ? job.getRequisition().getEmploymentType() : "",
                job.getSalaryDisplay(),
                job.getApplicationDeadline(),
                job.getPostingDate()));
    }

    private String formatRichText(String text) {
        if (text == null)
            return null;
        return text.replace("\\n", "<br/>").replace("\n", "<br/>");
    }

    private List<String> formatListText(String value) {
        if (value == null || value.isBlank())
            return List.of();
        return value.replace("\\n", "\n").lines().map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public PublicJobDetailResponse getPublishedJobDetail(Integer id, String username) {
        JobPosting job = jobPostingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng"));

        if (!"Published".equals(job.getPostingStatus())) {
            throw new ResourceNotFoundException("Tin tuyển dụng này không còn khả dụng.");
        }

        boolean isAcceptingApplications = (job.getApplicationDeadline() == null
                || !job.getApplicationDeadline().isBefore(LocalDateTime.now()));

        boolean hasApplied = false;
        // Có thể bổ sung check hasApplied ở đây (gọi CandidateRepository)

        return new PublicJobDetailResponse(
                job.getJobPostingId(),
                job.getPostingTitle(),
                job.getRequisition() != null && job.getRequisition().getDepartment() != null
                        ? job.getRequisition().getDepartment().getDepartmentName()
                        : "",
                job.getWorkLocation(),
                job.getRequisition() != null ? job.getRequisition().getEmploymentType() : "",
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

    public void validateJobForApplication(Integer id) {
        jobPostingRepository.findById(id)
                .filter(j -> "Published".equals(j.getPostingStatus()))
                .filter(j -> j.getApplicationDeadline() == null
                        || !j.getApplicationDeadline().isBefore(LocalDateTime.now()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tin tuyển dụng này không còn khả dụng hoặc đã ngừng nhận hồ sơ."));
    }

    public List<DepartmentFilterResponse> getSmartSortedDepartments() {
        List<JobPosting> openJobs = jobPostingRepository.findOpenPostings(LocalDateTime.now());
        Set<Integer> activeDeptIds = openJobs.stream()
                .filter(j -> j.getRequisition() != null && j.getRequisition().getDepartment() != null)
                .map(j -> j.getRequisition().getDepartment().getDepartmentId())
                .collect(Collectors.toSet());

        return departmentRepository.findAll().stream()
                .map(d -> new DepartmentFilterResponse(d.getDepartmentId(), d.getDepartmentName()))
                .sorted(Comparator
                        .comparing((DepartmentFilterResponse dept) -> !activeDeptIds.contains(dept.departmentId()))
                        .thenComparing(DepartmentFilterResponse::departmentName))
                .toList();
    }

    public void validateCandidateApplicationProfile(String username) {
        if (username == null)
            throw new com.group2.rms.core.exception.BaseBusinessException("Vui lòng đăng nhập để tiếp tục.",
                    "UNAUTHORIZED");

        userRepository.findByUsernameIgnoreCase(username)
                .filter(u -> "Candidate".equals(u.getRole().getRoleName()))
                .orElseThrow(() -> new com.group2.rms.core.exception.BaseBusinessException(
                        "Bạn không có quyền truy cập trang này. Vui lòng đăng nhập với tài khoản Ứng viên.",
                        "FORBIDDEN_ROLE"));
    }

    public Optional<ViewerProfileResponse> getViewerProfile(String username) {
        if (username == null)
            return Optional.empty();
        return userRepository.findByUsernameIgnoreCase(username)
                .map(u -> new ViewerProfileResponse(u.getFullName(), u.getEmail(),
                        u.getRole() != null ? u.getRole().getRoleName() : ""));
    }
}
