package com.group2.rms.service;

import com.group2.rms.entity.JobPosting;
import com.group2.rms.repository.CandidateRepository;
import com.group2.rms.repository.DepartmentRepository;
import com.group2.rms.repository.JobPostingRepository;
import com.group2.rms.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Read-only career views. Never expose User passwords or internal requisition fields. */
@Service
@Transactional(readOnly = true)
public class CareerService {
    private final JobPostingRepository postings;
    private final DepartmentRepository departments;
    private final UserRepository users;
    private final CandidateRepository candidates;

    public CareerService(JobPostingRepository postings, DepartmentRepository departments,
                         UserRepository users, CandidateRepository candidates) {
        this.postings = postings;
        this.departments = departments;
        this.users = users;
        this.candidates = candidates;
    }

    public List<PublicJob> openJobs() {
        return postings.findOpenPostings(LocalDateTime.now()).stream().map(CareerService::publicJob).toList();
    }

    public Optional<PublicJob> openJob(int id) {
        return postings.findOpenPosting(id, LocalDateTime.now()).map(CareerService::publicJob);
    }

    public List<DepartmentOption> departments() {
        return departments.findAll().stream()
                .map(d -> new DepartmentOption(d.getDepartmentId(), d.getDepartmentName()))
                .sorted(java.util.Comparator.comparing(DepartmentOption::departmentName)).toList();
    }

    public Optional<Viewer> viewer(String username) {
        return users.findByUsernameIgnoreCase(username)
                .map(u -> new Viewer(u.getFullName(), u.getRole().getRoleName()));
    }

    public Optional<CandidateDetails> candidateDetails(String username) {
        return users.findByUsernameIgnoreCase(username)
                .filter(u -> "Candidate".equals(u.getRole().getRoleName()))
                .flatMap(u -> candidates.findByAccountUserId(u.getUserId())
                        .map(c -> new CandidateDetails(u.getFullName(), u.getEmail(), u.getPhoneNumber(),
                                c.getLinkedInUrl(), c.getPortfolioUrl(), c.getAddress())));
    }

    private static PublicJob publicJob(JobPosting j) {
        var requisition = j.getRequisition();
        var department = requisition.getDepartment();
        return new PublicJob(j.getJobPostingId(), j.getPostingTitle(), department.getDepartmentId(),
                department.getDepartmentName(), requisition.getEmploymentType(), j.getWorkLocation(),
                j.getSalaryDisplay(), j.getPostingDate(), j.getApplicationDeadline(),
                lines(j.getJobDescription()), lines(j.getJobRequirements()), lines(j.getBenefits()));
    }

    // SQL seed contains both literal \\n and actual newlines. Output stays escaped by th:text.
    private static List<String> lines(String value) {
        if (value == null || value.isBlank()) return List.of();
        return value.replace("\\n", "\n").lines().map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public record PublicJob(Integer jobPostingId, String postingTitle, Integer departmentId,
                            String departmentName, String employmentType, String workLocation,
                            String salaryDisplay, LocalDateTime postingDate, LocalDateTime applicationDeadline,
                            List<String> description, List<String> requirements, List<String> benefits) { }
    public record DepartmentOption(Integer departmentId, String departmentName) { }
    public record Viewer(String fullName, String roleName) { }
    public record CandidateDetails(String fullName, String email, String phoneNumber,
                                   String linkedInUrl, String portfolioUrl, String address) { }
}
