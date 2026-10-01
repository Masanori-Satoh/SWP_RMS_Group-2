package com.group2.rms.service;

import com.group2.rms.dto.request.*;
import com.group2.rms.entity.*;
import com.group2.rms.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Rollback;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.jpa.hibernate.ddl-auto=none", "spring.jpa.show-sql=false"})
@Transactional
@Rollback
class RequisitionReviewProbeTests {
    @Autowired RequisitionService service;
    @Autowired JobRequisitionRepository requisitions;
    @Autowired UserRepository users;
    @Autowired DepartmentRepository departments;
    @Autowired ScreeningCriteriaRepository criteria;
    @Autowired JobPostingRepository postings;
    @Autowired EntityManager em;

    private RequisitionRequest dto() {
        return RequisitionRequest.builder().action("submit")
            .title("review-probe-" + UUID.randomUUID()).departmentId(departments.findAll().getFirst().getDepartmentId())
            .hiringManagerId(users.findAll().getFirst().getUserId()).numberOfPositions(1).employmentType("Full-time")
            .reasonForHiring("Review test").jobDescription("Description").requirementDetails("Requirements")
            .screeningCriteria(new ArrayList<>()).build();
    }
    private ScreeningCriteriaRequest criterion(String name, String type, String weight) {
        return ScreeningCriteriaRequest.builder().criteriaName(name).criteriaType(type).requiredValue("Review")
            .weight(new BigDecimal(weight)).isMandatory(false).build();
    }
    private JobRequisition create(RequisitionRequest dto) {
        service.createRequisition(dto, dto.getHiringManagerId());
        em.flush(); em.clear();
        return requisitions.findAll().stream().filter(r -> dto.getTitle().equals(r.getTitle())).findFirst().orElseThrow();
    }
    private void rejected(String label, Runnable operation) {
        RuntimeException error = assertThrows(RuntimeException.class, operation::run);
        Throwable root = error;
        while (root.getCause() != null) root = root.getCause();
        System.out.println("PROBE " + label + ": " + root.getClass().getSimpleName() + " " + root.getMessage());
    }
    @Test void incompleteDraft() {
        var dto = dto(); dto.setAction("draft"); dto.setDepartmentId(null);
        rejected("incomplete draft", () -> create(dto));
    }
    @Test void certificationOption() {
        var dto = dto(); dto.setScreeningCriteria(List.of(criterion("Certification", "Certification", "100")));
        rejected("Certification option", () -> create(dto));
    }
    @Test void zeroWeightAcceptedByUi() {
        var dto = dto(); dto.setScreeningCriteria(List.of(criterion("One", "Skill", "100"), criterion("Two", "Skill", "0")));
        rejected("100 plus zero weights", () -> create(dto));
    }
    @Test void duplicateNames() {
        var dto = dto(); dto.setScreeningCriteria(List.of(criterion("SQL", "Skill", "50"), criterion("SQL", "Skill", "50")));
        rejected("duplicate criteria names", () -> create(dto));
    }
    @Test void unchangedCriteriaUpdate() {
        var dto = dto(); dto.setScreeningCriteria(List.of(criterion("SQL", "Skill", "100")));
        var req = create(dto);
        var edit = service.getRequestDtoById(req.getRequisitionId()); edit.setAction("save");
        try {service.updateRequisition(req.getRequisitionId(), edit); em.flush(); System.out.println("PROBE unchanged criteria update: SUCCESS");}
        catch (RuntimeException e) {Throwable root=e; while(root.getCause()!=null)root=root.getCause();System.out.println("PROBE unchanged criteria update: " + root.getMessage());}
    }
    @Test void deletePostedRequisition() {
        var dto = dto(); var req = create(dto);
        postings.saveAndFlush(JobPosting.builder().requisition(req).postingTitle("Review posting")
            .jobDescription("Review").jobRequirements("Review").postingStatus("Draft")
            .createdBy(users.findById(dto.getHiringManagerId()).orElseThrow()).build());
        em.flush(); em.clear();
        rejected("delete with linked posting", () -> {service.deleteRequisition(req.getRequisitionId()); em.flush();});
    }
    @Test void approvedCanBeChangedWithoutApproval() {
        var dto = dto(); var req = create(dto); req.setApprovalStatus("Approved"); requisitions.saveAndFlush(req);
        dto.setAction("save"); dto.setNumberOfPositions(99);
        service.updateRequisition(req.getRequisitionId(), dto); em.flush(); em.clear();
        var stored = requisitions.findById(req.getRequisitionId()).orElseThrow();
        assertEquals("Approved", stored.getApprovalStatus()); assertEquals(99, stored.getNumberOfPositions());
        System.out.println("PROBE approved update: changed to 99 positions while Approved");
    }
    @Test void submitWithoutCriteriaAndWithCandidateManager() {
        var dto = dto();
        var candidate = users.findAll().stream().filter(u -> "Candidate".equals(u.getRole().getRoleName())).findFirst().orElseThrow();
        dto.setHiringManagerId(candidate.getUserId());
        var req = create(dto);
        assertEquals("Pending_Director", req.getApprovalStatus()); assertTrue(req.getScreeningCriteria().isEmpty());
        assertEquals("Candidate", req.getHiringManager().getRole().getRoleName());
        System.out.println("PROBE submit validation: Candidate manager and zero criteria persisted as Pending_Director");
    }
}
