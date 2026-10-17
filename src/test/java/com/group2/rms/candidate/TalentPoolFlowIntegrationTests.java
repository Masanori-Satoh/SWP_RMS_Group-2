package com.group2.rms.candidate;

import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.repository.*;
import com.group2.rms.candidate.service.*;
import com.group2.rms.dashboard.DashboardMetricsRepository;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

/** Real SQL Server, schema validation only; every test rolls back its draft and audit rows. */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureMockMvc
@Transactional
class TalentPoolFlowIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ApplicationRepository applications;
    @Autowired CandidateRepository candidates;
    @Autowired UserRepository users;
    @Autowired JobPostingRepository jobs;
    @Autowired RequisitionAccess access;
    @Autowired ApplicationStatusService statusService;
    @Autowired DashboardMetricsRepository metrics;
    @Autowired EntityManager em;
    @Autowired TalentPoolService pool;

    record Fixture(User hm, User hr, JobPosting target, Application source) { }
    private Fixture fixture() {
        var accounts = users.findAllWithRoleAndDepartment();
        var hr = accounts.stream().filter(u -> "HR".equals(u.getRole().getRoleName()) && "Active".equals(u.getAccountStatus())).findFirst().orElseThrow();
        for (var hm : accounts) {
            if (!"Hiring Manager".equals(hm.getRole().getRoleName()) || !"Active".equals(hm.getAccountStatus())) continue;
            for (var target : jobs.findAll()) {
                if (!"Published".equals(target.getPostingStatus()) || !access.inManagedDepartment(hm, target.getRequisition())
                        || target.getApplicationDeadline() != null && target.getApplicationDeadline().isBefore(LocalDateTime.now())) continue;
                for (var source : applications.findAllWithCandidateAndJobPosting()) {
                    int cid = source.getCandidate().getCandidateId();
                    String cv = source.getAppliedCvUrl();
                    if (!source.getCandidate().isPotential() || source.getJobPosting().getJobPostingId().equals(target.getJobPostingId())
                            || cv == null || !cv.startsWith("/samples/cv/") || !new ClassPathResource("static" + cv).exists()) continue;
                    if (applications.existsActiveByCandidateAndJobPosting(cid, target.getJobPostingId())
                            || applications.findFirstByCandidate_CandidateIdAndJobPosting_JobPostingIdAndApplicationStatus(cid, target.getJobPostingId(), "HM_Suggested").isPresent()) continue;
                    if (applications.findPreviousCvByCandidateId(cid).stream().anyMatch(a -> a.getApplicationId().equals(source.getApplicationId())))
                        return new Fixture(hm, hr, target, source);
                }
            }
        }
        throw new AssertionError("Local fixture needs an active HM, open managed posting and potential candidate with a bundled source CV.");
    }
    private String payload(Fixture f) { return "{\"sourceApplicationIds\":[" + f.source().getApplicationId() + "]}"; }
    private int create(Fixture f) throws Exception {
        mvc.perform(post("/talent-pool/jobs/" + f.target().getJobPostingId() + "/suggest")
                        .with(user(f.hm().getUsername()).roles("HIRING_MANAGER")).with(csrf())
                        .contentType("application/json").content(payload(f)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.addedCount").value(1));
        em.flush();
        return applications.findFirstByCandidate_CandidateIdAndJobPosting_JobPostingIdAndApplicationStatus(
                f.source().getCandidate().getCandidateId(), f.target().getJobPostingId(), "HM_Suggested").orElseThrow().getApplicationId();
    }
    private List<Integer> formalIds() { return applications.findPipelineIds(true, null, null, false, null, null, null, "newest"); }

    private int recommend(Fixture f) throws Exception {
        mvc.perform(post("/talent-pool/jobs/" + f.target().getJobPostingId() + "/recommend")
                        .with(user(f.hr().getUsername()).roles("HR")).with(csrf()).contentType("application/json").content(payload(f)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        em.flush();
        return applications.findFirstByCandidate_CandidateIdAndJobPosting_JobPostingIdAndApplicationStatus(
                f.source().getCandidate().getCandidateId(),f.target().getJobPostingId(),"HM_Suggested").orElseThrow().getApplicationId();
    }

    @Test void realHrRecommendationHmReviewAllCvsAndNotesThenConsentUseOneDraft() throws Exception {
        var f = fixture(); int jobId=f.target().getJobPostingId(), cid=f.source().getCandidate().getCandidateId();
        long appCount=applications.count(), candidateCount=candidates.count(), formalCount=metrics.applications();
        String sourceCv=f.source().getAppliedCvUrl(), sourceStatus=f.source().getApplicationStatus();
        var oldCvs=applications.findPreviousCvByCandidateId(cid);
        int id=recommend(f);
        assertEquals(1,applications.countPendingHrRecommendation(id));
        assertEquals(1,applications.countPendingHrRecommendationsForJob(jobId));
        assertEquals(appCount+1,applications.count()); assertEquals(candidateCount,candidates.count());
        assertFalse(formalIds().contains(id)); assertEquals(formalCount,metrics.applications());
        assertFalse(applications.findSuggestionsByJobPostingId(jobId).stream().anyMatch(a -> a.getApplicationId().equals(id)));
        mvc.perform(get("/internal/job-postings/"+jobId).with(user(f.hm().getUsername()).roles("HIRING_MANAGER")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Gợi ý từ HR")));
        mvc.perform(get("/talent-pool/jobs/"+jobId+"/recommendations").with(user(f.hm().getUsername()).roles("HIRING_MANAGER")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("recommendationId="+id)))
                .andExpect(content().string(containsString(f.hr().getFullName())));
        var detail=mvc.perform(get("/talent-pool/"+cid+"?recommendationId="+id).with(user(f.hm().getUsername()).roles("HIRING_MANAGER")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("/talent-pool/recommendations/"+id+"/decision")))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        for(var cv:oldCvs) assertTrue(detail.contains("/talent-pool/"+cid+"/cv/"+cv.getApplicationId()));
        mvc.perform(post("/talent-pool/"+cid+"/notes?recommendationId="+id).with(user(f.hm().getUsername()).roles("HIRING_MANAGER"))
                        .with(csrf()).param("content","Flow C integration note"))
                .andExpect(status().is3xxRedirection());
        em.flush();
        mvc.perform(get("/talent-pool/"+cid+"?recommendationId="+id).with(user(f.hm().getUsername()).roles("HIRING_MANAGER")))
                .andExpect(content().string(containsString("Flow C integration note")));
        mvc.perform(post("/talent-pool/applications/"+id+"/confirm").with(user(f.hr().getUsername()).roles("HR")).with(csrf()).param("consent","true"))
                .andExpect(flash().attributeExists("errorMessage"));
        assertEquals("HM_Suggested",applications.findById(id).orElseThrow().getApplicationStatus());
        mvc.perform(post("/talent-pool/jobs/"+jobId+"/recommend").with(user(f.hr().getUsername()).roles("HR")).with(csrf())
                        .contentType("application/json").content(payload(f))).andExpect(status().isBadRequest());
        mvc.perform(post("/talent-pool/recommendations/"+id+"/decision").with(user(f.hm().getUsername()).roles("HIRING_MANAGER"))
                        .with(csrf()).param("decision","Pass")).andExpect(flash().attributeExists("successMessage"));
        em.flush(); em.clear();
        assertEquals(0,applications.countPendingHrRecommendation(id));
        assertEquals(appCount+1,applications.count()); assertEquals(formalCount,metrics.applications());
        assertEquals("HM_Suggested",applications.findById(id).orElseThrow().getApplicationStatus());
        assertTrue(applications.findSuggestionsByJobPostingId(jobId).stream().anyMatch(a -> a.getApplicationId().equals(id)));
        assertThrows(com.group2.rms.candidate.exception.TalentPoolException.class,()->statusService.requireScheduleable(id));
        mvc.perform(post("/talent-pool/recommendations/"+id+"/decision").with(user(f.hm().getUsername()).roles("HIRING_MANAGER"))
                        .with(csrf()).param("decision","Fail")).andExpect(flash().attributeExists("errorMessage"));
        mvc.perform(post("/talent-pool/applications/"+id+"/confirm").with(user(f.hr().getUsername()).roles("HR")).with(csrf()).param("consent","true"))
                .andExpect(redirectedUrl("/interviews/new?applicationId="+id));
        em.flush(); em.clear();
        assertEquals("HM_Passed",applications.findById(id).orElseThrow().getApplicationStatus());
        assertTrue(formalIds().contains(id)); assertEquals(formalCount+1,metrics.applications());
        assertDoesNotThrow(()->statusService.requireScheduleable(id));
        assertEquals(sourceCv,applications.findById(f.source().getApplicationId()).orElseThrow().getAppliedCvUrl());
        assertEquals(sourceStatus,applications.findById(f.source().getApplicationId()).orElseThrow().getApplicationStatus());
        assertEquals(candidateCount,candidates.count());
    }

    @Test void realHmRejectionStaysOutOfProposalsAndFormalApplicationsAfterRefresh() throws Exception {
        var f=fixture(); long before=metrics.applications(); int id=recommend(f), cid=f.source().getCandidate().getCandidateId();
        mvc.perform(post("/talent-pool/recommendations/"+id+"/decision").with(user(f.hm().getUsername()).roles("HIRING_MANAGER"))
                        .with(csrf()).param("decision","Fail")).andExpect(flash().attributeExists("successMessage"));
        em.flush(); em.clear();
        assertEquals("Rejected",applications.findById(id).orElseThrow().getApplicationStatus());
        assertEquals(0,applications.countPendingHrRecommendation(id)); assertEquals(1,applications.countTalentPoolCancellation(id));
        assertEquals(before,metrics.applications()); assertFalse(formalIds().contains(id));
        assertTrue(candidates.findById(cid).orElseThrow().isPotential());
        assertFalse(applications.findSuggestionsByJobPostingId(f.target().getJobPostingId()).stream().anyMatch(a->a.getApplicationId().equals(id)));
        mvc.perform(get("/talent-pool/"+cid+"?recommendationId="+id).with(user(f.hm().getUsername()).roles("HIRING_MANAGER")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("HM đã từ chối")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("data-pool-decision"))));
        mvc.perform(post("/talent-pool/recommendations/"+id+"/decision").with(user(f.hm().getUsername()).roles("HIRING_MANAGER"))
                        .with(csrf()).param("decision","Pass")).andExpect(flash().attributeExists("errorMessage"));
    }

    @Test void realHmSubmissionHrConsentAndInterviewEligibilityPreserveOriginalCandidateAndCv() throws Exception {
        var f = fixture(); int cid = f.source().getCandidate().getCandidateId();
        long candidateCount = candidates.count(), appCount = applications.count(), formalCount = metrics.applications();
        String originalStatus = f.source().getApplicationStatus(), originalCv = f.source().getAppliedCvUrl();
        int id = create(f);
        assertEquals(candidateCount, candidates.count()); assertEquals(appCount + 1, applications.count());
        var draft = applications.findById(id).orElseThrow();
        assertEquals(cid, draft.getCandidate().getCandidateId()); assertEquals(originalCv, draft.getAppliedCvUrl());
        assertFalse(formalIds().contains(id)); assertEquals(formalCount, metrics.applications());
        assertFalse(applications.findOwnedByUserId(f.source().getCandidate().getAccount().getUserId()).stream().anyMatch(a -> a.getApplicationId().equals(id)));
        mvc.perform(get("/talent-pool/jobs/" + f.target().getJobPostingId() + "/suggestions")
                        .with(user(f.hr().getUsername()).roles("HR")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("/applications/" + id)));
        mvc.perform(post("/talent-pool/jobs/" + f.target().getJobPostingId() + "/suggest")
                        .with(user(f.hm().getUsername()).roles("HIRING_MANAGER")).with(csrf())
                        .contentType("application/json").content(payload(f)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        assertEquals(appCount + 1, applications.count());
        mvc.perform(post("/talent-pool/applications/" + id + "/confirm")
                        .with(user(f.hr().getUsername()).roles("HR")).with(csrf()).param("consent", "true"))
                .andExpect(redirectedUrl("/interviews/new?applicationId=" + id));
        em.flush(); em.clear();
        assertEquals("HM_Passed", applications.findById(id).orElseThrow().getApplicationStatus());
        assertTrue(formalIds().contains(id)); assertEquals(formalCount + 1, metrics.applications());
        assertTrue(applications.findEligibleForInterview().stream().anyMatch(a -> a.getApplicationId().equals(id)));
        assertDoesNotThrow(() -> statusService.requireScheduleable(id));
        mvc.perform(get("/applications/" + id).with(user(f.hm().getUsername()).roles("HIRING_MANAGER")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("HR xác nhận ứng viên đồng ý dùng CV cũ")));
        assertEquals(originalStatus, applications.findById(f.source().getApplicationId()).orElseThrow().getApplicationStatus());
        assertEquals(originalCv, applications.findById(f.source().getApplicationId()).orElseThrow().getAppliedCvUrl());
        assertEquals(candidateCount, candidates.count());
    }

    @Test void realHrDeclineRetainsPrivateDraftButExcludesItFromFormalQueriesAndCounts() throws Exception {
        var f = fixture(); long before = metrics.applications(); int id = create(f);
        mvc.perform(post("/talent-pool/applications/" + id + "/decline")
                        .with(user(f.hr().getUsername()).roles("HR")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        em.flush(); em.clear();
        assertEquals("Rejected", applications.findById(id).orElseThrow().getApplicationStatus());
        assertEquals(1, applications.countTalentPoolCancellation(id));
        assertFalse(formalIds().contains(id)); assertEquals(before, metrics.applications());
        assertFalse(applications.findSuggestionsByJobPostingId(f.target().getJobPostingId()).stream().anyMatch(a -> a.getApplicationId().equals(id)));
        mvc.perform(get("/applications/" + id).with(user(f.hr().getUsername()).roles("HR")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Đề xuất đã kết thúc")));
        assertThrows(com.group2.rms.candidate.exception.TalentPoolException.class, () -> statusService.requireScheduleable(id));
    }
}
