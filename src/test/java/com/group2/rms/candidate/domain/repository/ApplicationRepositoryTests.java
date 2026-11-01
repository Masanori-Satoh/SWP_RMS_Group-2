package com.group2.rms.candidate.domain.repository;

import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.candidate.domain.entity.Candidate;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.service.JobPostingAccess;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import com.group2.rms.user.repository.RoleRepository;
import com.group2.rms.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * INTEGRATION TEST: ApplicationRepository#existsActiveByCandidateInSameRequisition trên SQL Server thật (BR-CAN-01).
 * Dựng 1 ứng viên, đợt 1 có tin Lần 1 + Lần 2 ("Đăng lại"), đợt 2 là Requisition mới; rollback sau mỗi test.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional
class ApplicationRepositoryTests {

    @Autowired
    private ApplicationRepository applications;
    @Autowired
    private JobRequisitionRepository requisitions;
    @Autowired
    private DepartmentRepository departments;
    @Autowired
    private UserRepository users;
    @Autowired
    private RoleRepository roles;
    @Autowired
    private EntityManager entityManager;

    private Candidate candidate;
    private JobPosting roundOnePostingOne;
    private JobPosting roundOnePostingTwo;
    private JobPosting roundTwoPosting;

    @BeforeEach
    void setUp() {
        Department department = departments.save(Department.builder().departmentName("IT-AR Phòng Kỹ thuật")
                .departmentStatus("Active").build());
        User hiringManager = savedUser("it_ar_hm", RequisitionAccess.ROLE_HIRING_MANAGER, department);
        User hr = savedUser("it_ar_hr", RequisitionAccess.ROLE_HR, department);
        candidate = Candidate.builder().account(savedUser("it_ar_cand", "Candidate", null)).build();
        entityManager.persist(candidate);

        JobRequisition roundOne = savedRequisition(department, hiringManager, 1, null);
        JobRequisition roundTwo = savedRequisition(department, hiringManager, 2, roundOne);
        roundOnePostingOne = savedPosting(roundOne, 1, JobPostingAccess.STATUS_CLOSED, hr);
        roundOnePostingTwo = savedPosting(roundOne, 2, JobPostingAccess.STATUS_PUBLISHED, hr);
        roundTwoPosting = savedPosting(roundTwo, 1, JobPostingAccess.STATUS_PUBLISHED, hr);
    }

    // ----- Hàm hỗ trợ -----

    private User savedUser(String username, String roleName, Department inDepartment) {
        Role role = roles.findByRoleName(roleName).orElseGet(() -> roles.save(Role.builder().roleName(roleName).build()));
        return users.findByUsernameIgnoreCase(username).orElseGet(() -> users.save(User.builder()
                .username(username).passwordHash("$2a$10$dummy").fullName("Tester " + username)
                .email(username + "@rms.local").accountStatus("Active").role(role).department(inDepartment).build()));
    }

    private JobRequisition savedRequisition(Department department, User hiringManager, int round, JobRequisition original) {
        return requisitions.saveAndFlush(JobRequisition.builder().title("IT-AR Backend").recruitmentRound(round)
                .originalRequisition(original).numberOfPositions(1).employmentType("Full-time").gender("Any")
                .workModel("On-site").jobDescription("Integration-test responsibilities")
                .requirementDetails("Integration-test requirements").approvalStatus(RequisitionAccess.STATUS_APPROVED)
                .hiringManager(hiringManager).department(department).build());
    }

    private JobPosting savedPosting(JobRequisition requisition, int postingNo, String status, User hr) {
        JobPosting posting = JobPosting.builder().requisition(requisition).postingNo(postingNo)
                .postingTitle("IT-AR Backend lần " + postingNo).jobDescription("Mô tả").jobRequirements("Yêu cầu")
                .postingStatus(status).postingDate(LocalDateTime.now().minusDays(1)).createdBy(hr).build();
        entityManager.persist(posting);
        return posting;
    }

    private void savedApplication(JobPosting posting, String status) {
        entityManager.persist(Application.builder().candidate(candidate).jobPosting(posting)
                .appliedCvUrl("/cv/it-ar.pdf").submissionDate(LocalDateTime.now()).applicationStatus(status).build());
        entityManager.flush();
    }

    private boolean appliedInSameRequisition(JobPosting target) {
        return applications.existsActiveByCandidateInSameRequisition(candidate.getCandidateId(), target.getJobPostingId());
    }

    // ----- Test -----

    @Test
    @DisplayName("Đã nộp tin Lần 1 -> Tin Đăng lại (Lần 2) cùng đợt bị coi là đã ứng tuyển")
    void existsActiveByCandidateInSameRequisition_shouldBeTrue_whenAppliedToEarlierPostingOfSameRound() {
        // Arrange
        savedApplication(roundOnePostingOne, "Rejected");

        // Act + Assert
        assertTrue(appliedInSameRequisition(roundOnePostingTwo));
    }

    @Test
    @DisplayName("Đã nộp ở đợt 1 -> Tin của đợt 2 (Requisition mới) vẫn được nộp")
    void existsActiveByCandidateInSameRequisition_shouldBeFalse_whenTargetIsInNewRound() {
        // Arrange
        savedApplication(roundOnePostingOne, "Rejected");

        // Act + Assert
        assertFalse(appliedInSameRequisition(roundTwoPosting));
    }

    @Test
    @DisplayName("Chỉ có đề xuất HM_Suggested ở tin cũ -> Không tính là đã ứng tuyển")
    void existsActiveByCandidateInSameRequisition_shouldBeFalse_whenOnlyHmSuggestion() {
        // Arrange
        savedApplication(roundOnePostingOne, "HM_Suggested");

        // Act + Assert
        assertFalse(appliedInSameRequisition(roundOnePostingTwo));
    }
}
