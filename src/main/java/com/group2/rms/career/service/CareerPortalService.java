package com.group2.rms.career.service;

import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.career.dto.CareerJobDetailResponse;
import com.group2.rms.career.dto.CareerJobListResponse;
import com.group2.rms.requisition.entity.JobPosting;
import com.group2.rms.requisition.repository.JobPostingRepository;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CareerPortalService {

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    public Page<CareerJobListResponse> getPublishedJobs(String keyword, Integer departmentId, String employmentType, Pageable pageable) {
        Page<JobPosting> jobs = jobPostingRepository.findPublishedJobs(keyword, departmentId, employmentType, pageable);
        return jobs.map(job -> new CareerJobListResponse(
                job.getJobPostingId(),
                job.getPostingTitle(),
                job.getRequisition() != null && job.getRequisition().getDepartment() != null ? job.getRequisition().getDepartment().getDepartmentName() : "",
                job.getWorkLocation(),
                job.getRequisition() != null ? job.getRequisition().getEmploymentType() : "",
                job.getSalaryDisplay(),
                job.getApplicationDeadline(),
                job.getPostingDate()
        ));
    }

    private String formatRichText(String text) {
        if (text == null) return null;
        // Chuyển chuỗi literal "\n" hoặc ký tự newline thật thành thẻ <br>
        return text.replace("\\n", "<br/>").replace("\n", "<br/>");
    }

    public CareerJobDetailResponse getPublishedJobDetail(Integer id, Authentication authentication) {
        JobPosting job = jobPostingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng"));

        boolean isAcceptingApplications = "Published".equals(job.getPostingStatus()) && 
                                          (job.getApplicationDeadline() == null || !job.getApplicationDeadline().isBefore(LocalDateTime.now()));
        
        boolean hasApplied = false;
        // Logic check hasApplied here (require Candidate/Application repo)

        return new CareerJobDetailResponse(
                job.getJobPostingId(),
                job.getPostingTitle(),
                job.getRequisition() != null && job.getRequisition().getDepartment() != null ? job.getRequisition().getDepartment().getDepartmentName() : "",
                job.getWorkLocation(),
                job.getRequisition() != null ? job.getRequisition().getEmploymentType() : "",
                job.getSalaryDisplay(),
                job.getApplicationDeadline(),
                job.getPostingDate(),
                formatRichText(job.getJobDescription()),
                formatRichText(job.getJobRequirements()),
                formatRichText(job.getBenefits()),
                hasApplied,
                isAcceptingApplications
        );
    }

    public List<Department> getAllActiveDepartments() {
        return departmentRepository.findAll();
    }
}
