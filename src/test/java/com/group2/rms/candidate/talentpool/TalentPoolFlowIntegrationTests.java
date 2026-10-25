package com.group2.rms.candidate.talentpool;

import com.group2.rms.candidate.domain.rule.ApplicationStatusService;
import com.group2.rms.candidate.infra.cv.CvStorage;
import com.group2.rms.candidate.domain.repository.ApplicationRepository;
import com.group2.rms.candidate.domain.repository.CandidateRepository;
import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.candidate.application.submission.ApplicationSubmittedEvent;
import com.group2.rms.candidate.talentpool.service.TalentPoolService;
import com.group2.rms.candidate.talentpool.exception.TalentPoolException;
import com.group2.rms.dashboard.DashboardMetricsRepository;
import com.group2.rms.jobposting.entity.JobPosting;
import com.group2.rms.jobposting.repository.JobPostingRepository;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
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
@RecordApplicationEvents
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
    @Autowired CvStorage storage;
    @Autowired ApplicationEvents events;

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
    private List<Integer> formalIds() { return applications.findPipelineIds(true, null, null, null, null, null, "newest"); }

    private int recommend(Fixture f) throws Exception {
        mvc.perform(post("/talent-pool/jobs/" + f.target().getJobPostingId() + "/recommend")
                        .with(user(f.hr().getUsername()).roles("HR")).with(csrf()).contentType("application/json").content(payload(f)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        em.flush();
        return applications.findFirstByCandidate_CandidateIdAndJobPosting_JobPostingIdAndApplicationStatus(
                f.source().getCandidate().getCandidateId(),f.target().getJobPostingId(),"HM_Suggested").orElseThrow().getApplicationId();
    }

    @Test void realHrWithdrawalHidesDraftPreservesFormalCountsAndAllowsSendingAgain() throws Exception {
        var f=fixture(); int jobId=f.target().getJobPostingId(), cid=f.source().getCandidate().getCandidateId();
        long formal=metrics.applications(), candidateCount=candidates.count();
        String sourceCv=f.source().getAppliedCvUrl(), sourceStatus=f.source().getApplicationStatus();
        var oldNotes=poolNotesCount(cid);
        int id=recommend(f);
        mvc.perform(get("/internal/job-postings/"+jobId).with(user(f.hr().getUsername()).roles("HR")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Gợi ý đã gửi cho HM")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Gợi ý ứng viên cho HM"))));
        mvc.perform(post("/talent-pool/jobs/"+jobId+"/recommendations/"+id+"/withdraw")
                        .with(user(f.hr().getUsername()).roles("HR")).with(csrf()))
                .andExpect(flash().attributeExists("successMessage"));
        em.flush(); em.clear();
        assertEquals("Rejected",applications.findById(id).orElseThrow().getApplicationStatus());
        assertEquals(0,applications.countPendingHrRecommendation(id));
        assertEquals(1,applications.countTalentPoolCancellation(id));
        assertFalse(applications.findHrRecommendations(jobId).stream().anyMatch(a->a.getApplicationId().equals(id)));
        assertFalse(formalIds().contains(id)); assertEquals(formal,metrics.applications());
        assertEquals(candidateCount,candidates.count()); assertTrue(candidates.findById(cid).orElseThrow().isPotential());
        assertEquals(sourceCv,applications.findById(f.source().getApplicationId()).orElseThrow().getAppliedCvUrl());
        assertEquals(sourceStatus,applications.findById(f.source().getApplicationId()).orElseThrow().getApplicationStatus());
        assertEquals(oldNotes,poolNotesCount(cid));
        mvc.perform(post("/talent-pool/recommendations/"+id+"/decision").with(user(f.hm().getUsername()).roles("HIRING_MANAGER"))
                        .with(csrf()).param("decision","Pass")).andExpect(status().isNotFound());
        mvc.perform(get("/talent-pool/"+cid+"?recommendationId="+id).with(user(f.hm().getUsername()).roles("HIRING_MANAGER")))
                .andExpect(status().isNotFound());
        int replacement=recommend(f); assertNotEquals(id,replacement);
        assertEquals(1,applications.countPendingHrRecommendation(replacement));
    }
    private long poolNotesCount(int cid) {
        return em.createQuery("select count(n) from TalentPoolNote n where n.candidate.candidateId = :id",Long.class)
                .setParameter("id",cid).getSingleResult();
    }
    @Test void realAcceptedRecommendationCannotBeWithdrawnByHr() throws Exception {
        var f=fixture(); int id=recommend(f), jobId=f.target().getJobPostingId();
        mvc.perform(post("/talent-pool/recommendations/"+id+"/decision").with(user(f.hm().getUsername()).roles("HIRING_MANAGER"))
                        .with(csrf()).param("decision","Pass")).andExpect(flash().attributeExists("successMessage"));
        mvc.perform(post("/talent-pool/jobs/"+jobId+"/recommendations/"+id+"/withdraw")
                        .with(user(f.hr().getUsername()).roles("HR")).with(csrf()))
                .andExpect(flash().attributeExists("errorMessage"));
        em.flush(); em.clear();
        assertEquals("HM_Suggested",applications.findById(id).orElseThrow().getApplicationStatus());
        assertEquals(0,applications.countTalentPoolCancellation(id));
        assertTrue(applications.findHrRecommendations(jobId).stream().anyMatch(a->a.getApplicationId().equals(id)));
        assertTrue(applications.findSuggestionsByJobPostingId(jobId).stream().anyMatch(a->a.getApplicationId().equals(id)));
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
        assertThrows(TalentPoolException.class,()->statusService.requireScheduleable(id));
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
        assertThrows(TalentPoolException.class, () -> statusService.requireScheduleable(id));
    }

    @ParameterizedTest
    @ValueSource(strings={"HM_ADDED", "HR_ACCEPTED", "HR_PENDING", "NO_PROPOSAL"})
    void candidateNewCvUsesMatchingApprovedProposalToEnterInterview(String origin) throws Exception {
        var f=fixture(); int jobId=f.target().getJobPostingId(), cid=f.source().getCandidate().getCandidateId();
        int sourceId=f.source().getApplicationId();
        String sourceCv=f.source().getAppliedCvUrl(), sourceStatus=f.source().getApplicationStatus();
        long appCount=applications.count(), candidateCount=candidates.count();
        Integer proposalId=null;
        if ("HM_ADDED".equals(origin)) proposalId=create(f);
        else if (!"NO_PROPOSAL".equals(origin)) {
            proposalId=recommend(f);
            if ("HR_ACCEPTED".equals(origin)) {
                mvc.perform(post("/talent-pool/recommendations/"+proposalId+"/decision")
                                .with(user(f.hm().getUsername()).roles("HIRING_MANAGER")).with(csrf()).param("decision","Pass"))
                        .andExpect(flash().attributeExists("successMessage"));
            } else {
                // The database update itself must also prevent bypassing the pending HM decision.
                assertEquals(0,applications.submitSuggestionWithNewCv(proposalId,sourceCv,"HM_Passed"));
            }
        }
        byte[] pdf=new ClassPathResource("static"+sourceCv).getContentAsByteArray();
        var file=new MockMultipartFile("cvFile","updated-cv.pdf","application/pdf",pdf);
        mvc.perform(multipart("/jobs/"+jobId+"/apply").file(file)
                        .with(user(f.source().getCandidate().getAccount().getUsername()).roles("CANDIDATE")).with(csrf()))
                .andExpect(redirectedUrl("/jobs/"+jobId)).andExpect(flash().attribute("applySuccess",true));
        em.flush(); em.clear();
        var saved=applications.findAllWithCandidateAndJobPosting().stream()
                .filter(a -> a.getCandidate().getCandidateId().equals(cid) && a.getJobPosting().getJobPostingId().equals(jobId))
                .findFirst().orElseThrow();
        int id=saved.getApplicationId();
        boolean approved=Set.of("HM_ADDED","HR_ACCEPTED").contains(origin);
        assertEquals(approved?"HM_Passed":"Applied",saved.getApplicationStatus());
        if (proposalId!=null) assertEquals(proposalId,id,"reuse the same proposal, not a second application");
        assertTrue(saved.getAppliedCvUrl().startsWith("local:cv/"+cid+"/"));
        assertNotEquals(sourceCv,saved.getAppliedCvUrl());
        assertArrayEquals(pdf,storage.load(saved.getAppliedCvUrl().substring("local:".length())).getContentAsByteArray());
        assertEquals(appCount+1,applications.count()); assertEquals(candidateCount,candidates.count());
        assertEquals(sourceCv,applications.findById(sourceId).orElseThrow().getAppliedCvUrl());
        assertEquals(sourceStatus,applications.findById(sourceId).orElseThrow().getApplicationStatus());
        assertTrue(formalIds().contains(id));
        assertFalse(applications.findSuggestionsByJobPostingId(jobId).stream().anyMatch(a -> a.getApplicationId().equals(id)));
        if (approved) {
            assertEquals(0,events.stream(ApplicationSubmittedEvent.class).count());
            assertDoesNotThrow(()->statusService.requireScheduleable(id));
            assertTrue(applications.findEligibleForInterview().stream().anyMatch(a -> a.getApplicationId().equals(id)));
            mvc.perform(get("/interviews/new").param("applicationId",Integer.toString(id))
                            .with(user(f.hr().getUsername()).roles("HR")))
                    .andExpect(status().isOk()).andExpect(content().string(containsString("Lên lịch phỏng vấn")));
        } else {
            assertEquals(1,events.stream(ApplicationSubmittedEvent.class).filter(e->e.applicationId().equals(id)).count());
            assertThrows(TalentPoolException.class,()->statusService.requireScheduleable(id));
        }
        // A second upload cannot create another application or replace the confirmed CV.
        mvc.perform(multipart("/jobs/"+jobId+"/apply").file(file)
                        .with(user(f.source().getCandidate().getAccount().getUsername()).roles("CANDIDATE")).with(csrf()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Bạn đã ứng tuyển vị trí này.")));
        assertEquals(appCount+1,applications.count());
        assertEquals(saved.getAppliedCvUrl(),applications.findById(id).orElseThrow().getAppliedCvUrl());
    }
}
