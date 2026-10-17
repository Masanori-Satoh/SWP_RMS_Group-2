package com.group2.rms.candidate;

import com.group2.rms.candidate.repository.*;
import com.group2.rms.candidate.service.ApplicationAccess;
import com.group2.rms.interview.entity.InterviewFinalResult;
import com.group2.rms.interview.repository.*;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Existing local interview fixtures; only bookmark/notes are written, and the transaction is rolled back. */
@SpringBootTest(properties="spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureMockMvc
@Transactional
class InterviewPotentialIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired InterviewFinalResultRepository finals;
    @Autowired CandidateRepository candidates;
    @Autowired ApplicationRepository applications;
    @Autowired UserRepository users;
    @Autowired ApplicationAccess applicationAccess;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;

    private InterviewFinalResult fixture(String decision) {
        var fixture = finals.findAll().stream().filter(f -> decision.equals(f.getFinalDecision()))
                .filter(f -> "Active".equals(f.getHiringManager().getAccountStatus())
                        && "Hiring Manager".equals(f.getHiringManager().getRole().getRoleName()))
                .filter(f -> applicationAccess.canView(f.getHiringManager(),f.getInterviewSchedule().getApplication()))
                .findFirst().orElseThrow(() -> new AssertionError("Local fixture needs a "+decision+" interview with an authorized active HM."));
        // Seed data may already mark every completed candidate. Reset only the flag
        // inside this rolled-back transaction to exercise the first-save path.
        fixture.getInterviewSchedule().getApplication().getCandidate().setPotential(false);
        em.flush();
        return fixture;
    }
    private Map<String,Object> snapshot(InterviewFinalResult f) {
        var s=f.getInterviewSchedule(); var a=s.getApplication(); var job=a.getJobPosting();
        var result=new LinkedHashMap<String,Object>();
        result.put("application",jdbc.queryForList("SELECT * FROM Application WHERE ApplicationId=?",a.getApplicationId()));
        result.put("schedule",jdbc.queryForList("SELECT * FROM InterviewSchedule WHERE InterviewId=?",s.getInterviewId()));
        result.put("final",jdbc.queryForList("SELECT * FROM InterviewFinalResult WHERE InterviewId=?",s.getInterviewId()));
        result.put("evaluations",jdbc.queryForList("SELECT * FROM InterviewEvaluation WHERE InterviewId=? ORDER BY EvaluationId",s.getInterviewId()));
        result.put("offers",jdbc.queryForList("SELECT * FROM OfferProposal WHERE ApplicationId=? ORDER BY OfferId",a.getApplicationId()));
        result.put("posting",jdbc.queryForList("SELECT * FROM JobPosting WHERE JobPostingId=?",job.getJobPostingId()));
        result.put("requisition",jdbc.queryForList("SELECT * FROM JobRequisition WHERE RequisitionId=?",job.getRequisition().getRequisitionId()));
        return result;
    }
    private User hr() { return users.findAllWithRoleAndDepartment().stream()
            .filter(u -> "HR".equals(u.getRole().getRoleName()) && "Active".equals(u.getAccountStatus())).findFirst().orElseThrow(); }
    private long marks(int candidateId) { return jdbc.queryForObject("SELECT COUNT(*) FROM AuditLog WHERE EntityName='Candidate' AND EntityId=? AND NewValue='TalentPoolMarked'",Long.class,Integer.toString(candidateId)); }

    @ParameterizedTest @ValueSource(strings={"Passed","Failed"})
    void hmCanBookmarkAfterEitherFinalDecisionWithoutChangingInterviewOrOffer(String decision) throws Exception {
        var f=fixture(decision); var a=f.getInterviewSchedule().getApplication(); var hm=f.getHiringManager();
        int appId=a.getApplicationId(),candidateId=a.getCandidate().getCandidateId(); long interviewId=f.getInterviewSchedule().getInterviewId();
        var before=snapshot(f); long candidateCount=candidates.count(),appCount=applications.count();
        mvc.perform(get("/interviews/"+interviewId+"/result").with(user(hm.getUsername()).roles("HIRING_MANAGER")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("/talent-pool/applications/"+appId+"/bookmark")));
        assertFalse(candidates.findById(candidateId).orElseThrow().isPotential());
        assertEquals(before,snapshot(f));
        mvc.perform(post("/talent-pool/applications/"+appId+"/bookmark").with(user(hm.getUsername()).roles("HIRING_MANAGER"))
                        .with(csrf()).param("content","Potential after "+decision))
                .andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(true)).andExpect(jsonPath("$.candidateId").value(candidateId));
        em.flush();
        assertTrue(candidates.findById(candidateId).orElseThrow().isPotential());
        assertEquals(before,snapshot(f)); assertEquals(candidateCount,candidates.count()); assertEquals(appCount,applications.count());
        var staff=hr();
        mvc.perform(get("/talent-pool/applications/"+appId+"/bookmark").with(user(staff.getUsername()).roles("HR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.notes[0].author").value(hm.getFullName()))
                .andExpect(jsonPath("$.notes[0].content").value("Potential after "+decision));
        mvc.perform(get("/talent-pool").param("keyword",a.getCandidate().getAccount().getEmail()).with(user(staff.getUsername()).roles("HR")))
                .andExpect(status().isOk()).andExpect(content().string(containsString(a.getCandidate().getAccount().getFullName())));
    }

    @Test void hrCanAppendSharedNotesWithoutRepeatedMarkingOrAnyWorkflowMutation() throws Exception {
        var f=fixture("Passed"); var a=f.getInterviewSchedule().getApplication(); var staff=hr();
        int id=a.getApplicationId(),cid=a.getCandidate().getCandidateId(); var before=snapshot(f);
        long originalMarks=marks(cid);
        var originalNotes=jdbc.queryForList("SELECT * FROM TalentPoolNote WHERE CandidateId=? ORDER BY NoteId",cid);
        mvc.perform(get("/interviews/"+f.getInterviewSchedule().getInterviewId()+"/result").with(user(staff.getUsername()).roles("HR")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("data-tp-potential")));
        mvc.perform(post("/talent-pool/applications/"+id+"/bookmark").with(user(staff.getUsername()).roles("HR")).with(csrf()).param("content",""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(true));
        em.flush(); assertEquals(originalMarks+1,marks(cid));
        mvc.perform(post("/talent-pool/applications/"+id+"/bookmark").with(user(staff.getUsername()).roles("HR")).with(csrf()).param("content","Ghi chú từ HR"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.notes[0].author").value(staff.getFullName()));
        em.flush(); assertEquals(originalMarks+1,marks(cid)); assertEquals(before,snapshot(f));
        var currentNotes=jdbc.queryForList("SELECT * FROM TalentPoolNote WHERE CandidateId=? ORDER BY NoteId",cid);
        assertEquals(originalNotes.size()+1,currentNotes.size()); assertTrue(currentNotes.containsAll(originalNotes));
        mvc.perform(post("/talent-pool/applications/"+id+"/bookmark").with(user(staff.getUsername()).roles("HR")).with(csrf()).param("content","x".repeat(1001)))
                .andExpect(status().isBadRequest());
        assertEquals(currentNotes,jdbc.queryForList("SELECT * FROM TalentPoolNote WHERE CandidateId=? ORDER BY NoteId",cid));
        assertEquals(before,snapshot(f));
    }
}
