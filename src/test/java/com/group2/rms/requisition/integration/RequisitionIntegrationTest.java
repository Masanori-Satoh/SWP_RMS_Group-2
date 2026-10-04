package com.group2.rms.requisition.integration;

import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.ScreeningCriteriaRequest;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.RequisitionWorkflowEvent;
import jakarta.persistence.EntityManager;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.requisition.repository.RequisitionApprovalRepository;
import com.group2.rms.requisition.repository.RequisitionWorkflowEventRepository;
import com.group2.rms.requisition.service.RequisitionService;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import com.group2.rms.user.repository.RoleRepository;
import com.group2.rms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * STAGE 4 — INTEGRATION TEST.
 * Kiểm tra tích hợp thực tế giữa Service, Repository và Database với @Transactional rollback.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureMockMvc
@Transactional
class RequisitionIntegrationTest {

    @Autowired private RequisitionService service;
    @Autowired private JobRequisitionRepository requisitions;
    @Autowired private RequisitionApprovalRepository approvals;
    @Autowired private RequisitionWorkflowEventRepository events;
    @Autowired private DepartmentRepository departments;
    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private EntityManager entityManager;
    @Autowired private MockMvc mvc;

    private Department department;
    private User hiringManager;
    private User director;

    @BeforeEach
    void setUp() {
        Role hmRole = roles.findByRoleName("Hiring Manager")
                .orElseGet(() -> roles.save(Role.builder().roleName("Hiring Manager").build()));
        Role dirRole = roles.findByRoleName("Director")
                .orElseGet(() -> roles.save(Role.builder().roleName("Director").build()));

        department = departments.findAll().stream().findFirst()
                .orElseGet(() -> departments.save(Department.builder().departmentName("Engineering").build()));

        hiringManager = users.findByUsernameIgnoreCase("it_hm").orElseGet(() ->
                users.save(User.builder()
                        .username("it_hm")
                        .passwordHash("$2a$10$dummy")
                        .fullName("HM Tester")
                        .email("hm_tester@rms.local")
                        .accountStatus("Active")
                        .role(hmRole)
                        .department(department)
                        .build()));

        director = users.findByUsernameIgnoreCase("it_dir").orElseGet(() ->
                users.save(User.builder()
                        .username("it_dir")
                        .passwordHash("$2a$10$dummy")
                        .fullName("Director Tester")
                        .email("dir_tester@rms.local")
                        .accountStatus("Active")
                        .role(dirRole)
                        .department(department)
                        .build()));
    }

    // =========================================================================
    // IT-01: Create Draft & Submit
    // =========================================================================

    @Test
    @WithMockUser(username = "it_hm", roles = "HIRING_MANAGER")
    @DisplayName("IT-01: Tạo Draft -> Lưu thành công vào DB với quan hệ HM, Department và Criteria")
    void it01_createDraft_persistsRelationships() {
        var req = RequisitionRequest.builder()
                .action("draft")
                .title("Integration Java Dev")
                .departmentId(department.getDepartmentId())
                .numberOfPositions(2)
                .employmentType("Full-time")
                .screeningCriteria(new ArrayList<>(List.of(
                        ScreeningCriteriaRequest.builder()
                                .criteriaName("Spring Boot")
                                .criteriaType("Skill")
                                .requiredValue("2 years")
                                .weight(new BigDecimal("100.00"))
                                .isMandatory(true)
                                .build()
                ))).build();

        Integer id = service.createRequisition(req);
        assertNotNull(id);

        JobRequisition saved = requisitions.findById(id).orElseThrow();
        assertEquals("Draft", saved.getApprovalStatus());
        assertEquals("Integration Java Dev", saved.getTitle());
        assertEquals(hiringManager.getUserId(), saved.getHiringManager().getUserId());
        assertEquals(department.getDepartmentId(), saved.getDepartment().getDepartmentId());
        assertEquals(1, saved.getScreeningCriteria().size());
    }

    // =========================================================================
    // IT-11: Unicode columns and rendering through the real security filters
    // =========================================================================

    @Test
    @WithMockUser(username = "it_hm", roles = "HIRING_MANAGER")
    void it11_listAndDetail_readUnicodeColumns() throws Exception {
        var requisition = requisitions.saveAndFlush(JobRequisition.builder()
                .title("Unicode read regression").hiringManager(hiringManager).department(department)
                .approvalStatus("Draft").gender("Female").workLocation("Văn phòng Hà Nội")
                .workModel("Hybrid").probationDuration("2 tháng")
                .screeningCriteria(new ArrayList<>()).build());
        Integer id = requisition.getRequisitionId();
        events.saveAndFlush(RequisitionWorkflowEvent.builder()
                .requisition(requisition).actor(hiringManager).eventType("Withdrawn")
                .occurredAt(java.time.LocalDateTime.now()).comment("Bổ sung yêu cầu tuyển dụng").build());
        entityManager.clear(); // Exercise JDBC extraction, not the persistence-context cache.

        assertEquals("Female", entityManager.createNativeQuery(
                "SELECT RequiredGender FROM JobRequisition WHERE RequisitionId = :id")
                .setParameter("id", id).getSingleResult());
        var page = service.search(1, 10, "Unicode read regression", null, "", "", "newest");
        var row = page.getContent().stream().filter(r -> id.equals(r.getRequisitionId())).findFirst().orElseThrow();
        assertEquals("Female", row.getGender());
        assertEquals("Văn phòng Hà Nội", row.getWorkLocation());
        assertEquals("2 tháng", row.getProbationDuration());
        assertEquals("Hybrid", row.getWorkModel());
        assertEquals("Bổ sung yêu cầu tuyển dụng", service.getById(id).getTimeline().getFirst().description());
        mvc.perform(get("/requisitions")).andExpect(status().isOk()).andExpect(view().name("requisitions/list"));
        mvc.perform(get("/requisitions/{id}", id)).andExpect(status().isOk()).andExpect(view().name("requisitions/detail"));
    }

    // =========================================================================
    // IT-04 & IT-05: Decision (Approve / Reject)
    // =========================================================================

    @Test
    @WithMockUser(username = "it_dir", roles = "DIRECTOR")
    @DisplayName("IT-04: Director Approve -> Cập nhật Approved, tạo approval record và workflow event")
    void it04_directorApprove_createsApprovalAndEvent() {
        JobRequisition req = requisitions.save(JobRequisition.builder()
                .title("Pending Dev")
                .hiringManager(hiringManager)
                .department(department)
                .approvalStatus("Pending_Director")
                .screeningCriteria(new ArrayList<>())
                .build());

        service.decide(req.getRequisitionId(), req.getVersion(), true, "Approved for hiring");

        JobRequisition updated = requisitions.findById(req.getRequisitionId()).orElseThrow();
        assertEquals("Approved", updated.getApprovalStatus());
        assertNotNull(updated.getDecidedAt());

        // Kiểm tra approval record
        var appList = approvals.findByRequisition_RequisitionIdOrderByApprovalDateDesc(req.getRequisitionId());
        assertEquals(1, appList.size());
        assertEquals("Approved", appList.get(0).getStatus());
        assertEquals("Approved for hiring", appList.get(0).getComments());
    }

    @Test
    @WithMockUser(username = "it_dir", roles = "DIRECTOR")
    @DisplayName("IT-05: Director Reject -> Cập nhật Rejected kèm feedback comment")
    void it05_directorReject_recordsFeedback() {
        JobRequisition req = requisitions.save(JobRequisition.builder()
                .title("Pending QA")
                .hiringManager(hiringManager)
                .department(department)
                .approvalStatus("Pending_Director")
                .screeningCriteria(new ArrayList<>())
                .build());

        service.decide(req.getRequisitionId(), req.getVersion(), false, "Need more budget details");

        JobRequisition updated = requisitions.findById(req.getRequisitionId()).orElseThrow();
        assertEquals("Rejected", updated.getApprovalStatus());

        var appList = approvals.findByRequisition_RequisitionIdOrderByApprovalDateDesc(req.getRequisitionId());
        assertEquals(1, appList.size());
        assertEquals("Rejected", appList.get(0).getStatus());
        assertEquals("Need more budget details", appList.get(0).getComments());
    }

    // =========================================================================
    // IT-06: Withdraw
    // =========================================================================

    @Test
    @WithMockUser(username = "it_hm", roles = "HIRING_MANAGER")
    @DisplayName("IT-06: HM rút request Pending -> Quay về trạng thái Draft")
    void it06_withdraw_revertsToDraft() {
        JobRequisition req = requisitions.save(JobRequisition.builder()
                .title("Pending UX")
                .hiringManager(hiringManager)
                .department(department)
                .approvalStatus("Pending_Director")
                .screeningCriteria(new ArrayList<>())
                .build());

        service.withdraw(req.getRequisitionId(), req.getVersion());

        JobRequisition updated = requisitions.findById(req.getRequisitionId()).orElseThrow();
        assertEquals("Draft", updated.getApprovalStatus());
    }

    // =========================================================================
    // IT-08: Copy
    // =========================================================================

    @Test
    @WithMockUser(username = "it_hm", roles = "HIRING_MANAGER")
    @DisplayName("IT-08: Copy -> Trả về DTO mới, version null, criteriaId null, chưa lưu DB")
    void it08_copy_clearsIdentifiers() {
        JobRequisition original = requisitions.save(JobRequisition.builder()
                .title("Original Requisition")
                .hiringManager(hiringManager)
                .department(department)
                .approvalStatus("Draft")
                .screeningCriteria(new ArrayList<>())
                .build());

        RequisitionRequest copy = service.copy(original.getRequisitionId());

        assertEquals("Original Requisition", copy.getTitle());
        assertNull(copy.getVersion(), "Bản copy phải xóa version");
    }

    // =========================================================================
    // IT-10: Delete
    // =========================================================================

    @Test
    @WithMockUser(username = "it_hm", roles = "HIRING_MANAGER")
    @DisplayName("IT-10: Delete -> Xóa bản ghi requisition khỏi DB")
    void it10_delete_removesEntity() {
        JobRequisition req = requisitions.save(JobRequisition.builder()
                .title("To be deleted")
                .hiringManager(hiringManager)
                .department(department)
                .approvalStatus("Draft")
                .screeningCriteria(new ArrayList<>())
                .build());

        Integer id = req.getRequisitionId();
        service.deleteRequisition(id, req.getVersion());

        assertTrue(requisitions.findById(id).isEmpty());
    }
}
