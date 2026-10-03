package com.group2.rms.requisition.service;

import com.group2.rms.admin.AuditLogRepository;
import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.ScreeningCriteriaRequest;
import com.group2.rms.requisition.entity.JobPosting;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.exception.RequisitionValidationException;
import com.group2.rms.requisition.repository.JobPostingRepository;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import com.group2.rms.user.repository.RoleRepository;
import com.group2.rms.user.repository.UserRepository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Rollback;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"spring.jpa.hibernate.ddl-auto=none","spring.jpa.show-sql=false"}) @Transactional @Rollback
class RequisitionReviewProbeTests {
 @Autowired RequisitionService service; @Autowired JobRequisitionRepository requisitions; @Autowired UserRepository users;
 @Autowired RoleRepository roles; @Autowired DepartmentRepository departments; @Autowired AuditLogRepository audit;
 @Autowired JobPostingRepository postings; @Autowired EntityManager em;
 User manager,director;
 @Test void searchFindsDepartmentsAndSortsAcrossPages(){
  var first=complete("draft");first.setTitle("Zulu Engineer");int older=service.createRequisition(first);
  stored(older);
  var second=complete("draft");second.setTitle("Alpha Engineer");int newer=service.createRequisition(second);stored(newer);
  String department=manager.getDepartment().getDepartmentName();
  assertEquals(2,service.search(1,10,department,null,"Full-time","Draft","newest").getTotalElements());
  assertEquals(newer,service.search(1,1,"",null,"","","newest").getContent().getFirst().getRequisitionId());
  assertEquals(older,service.search(2,1,"",null,"","","newest").getContent().getFirst().getRequisitionId());
  assertEquals(older,service.search(1,1,"",null,"","","oldest").getContent().getFirst().getRequisitionId());
  assertEquals(newer,service.search(1,1,"",null,"","","position_asc").getContent().getFirst().getRequisitionId());
  assertEquals(older,service.search(1,1,"",null,"","","position_desc").getContent().getFirst().getRequisitionId());
  var noDepartment=complete("draft");noDepartment.setTitle("No department role");noDepartment.setDepartmentId(null);
  int unassigned=service.createRequisition(noDepartment);stored(unassigned);
  assertEquals(unassigned,service.search(1,10,"No department",null,"","","newest").getContent().getFirst().getRequisitionId());
  signIn(account("Hiring Manager"));assertTrue(service.search(1,10,department,null,"","","oldest").isEmpty());
 }
 @BeforeEach void actors(){manager=account("Hiring Manager");director=account("Director");signIn(manager);}
 @AfterEach void cleanup(){SecurityContextHolder.clearContext();}
 private User account(String role){String id="reqtest"+UUID.randomUUID().toString().replace("-","").substring(0,12);return users.saveAndFlush(User.builder().username(id).email(id+"@example.test").fullName(role+" test").passwordHash("test-only").accountStatus("Active").department(departments.findAll().getFirst()).role(roles.findAll().stream().filter(r->role.equals(r.getRoleName())).findFirst().orElseThrow()).build());}
 private void signIn(User u){SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(u.getUsername(),"",List.of()));}
 private ScreeningCriteriaRequest criterion(String name,String weight){return ScreeningCriteriaRequest.builder().criteriaName(name).criteriaType("Skill").requiredValue("Two years").weight(new BigDecimal(weight)).isMandatory(true).build();}
 private RequisitionRequest complete(String action){return RequisitionRequest.builder().action(action).title("Java Engineer").departmentId(manager.getDepartment().getDepartmentId()).numberOfPositions(2).employmentType("Full-time").gender("Any").workLocation("Da Nang").workingHours("Mon-Fri, 08:00-17:00").expectedStartDate(LocalDate.now().plusDays(30)).reasonForHiring("Mở rộng đội ngũ phát triển phần mềm").jobDescription("Develop software").requirementDetails("Java experience").screeningCriteria(new ArrayList<>(List.of(criterion("Java","100")))).build();}
 private JobRequisition stored(Integer id){em.flush();em.clear();return requisitions.findById(id).orElseThrow();}
 @Test void incompleteDraftKeepsPartialInputsWithoutSubmissionRules(){var d=new RequisitionRequest();d.setAction("draft");d.setExpectedStartDate(LocalDate.now().minusDays(3));d.setScreeningCriteria(List.of(ScreeningCriteriaRequest.builder().criteriaType("Skill").weight(new BigDecimal("25")).build()));int id=service.createRequisition(d);var r=stored(id);assertNull(r.getDepartment());assertNull(r.getEmploymentType());assertEquals("Draft",r.getApprovalStatus());assertEquals(1,r.getScreeningCriteria().size());assertEquals(manager.getUserId(),r.getHiringManager().getUserId());assertEquals(d.getExpectedStartDate(),r.getExpectedStartDate());}
 @Test void incompleteSubmissionIsRejectedBeforeInsert(){var d=new RequisitionRequest();d.setAction("submit");var e=assertThrows(RequisitionValidationException.class,()->service.createRequisition(d));assertTrue(e.getErrors().containsKey("title"));assertTrue(e.getErrors().containsKey("screeningCriteria"));}
 @Test void databaseFieldsAndUnicodeReasonRoundTrip(){var d=complete("draft");d.setReasonForHiring("Tuyển dụng nhân sự tiếng Việt. ".repeat(50));int id=service.createRequisition(d);stored(id);var view=service.getById(id);assertEquals(d.getReasonForHiring().trim(),view.getReasonForHiring());assertEquals(d.getWorkingHours(),view.getWorkingHours());assertEquals(d.getExpectedStartDate(),view.getExpectedStartDate());assertEquals("Any",view.getGender());}
 @Test void editUpdatesDetailsOnlyChangesLoggedAndNoOpKeepsVersion(){int id=service.createRequisition(complete("draft"));stored(id);var d=service.getRequestDtoById(id);d.setAction("draft");var version=d.getVersion();service.updateRequisition(id,d);assertEquals(version,stored(id).getVersion());assertEquals(1,service.getById(id).getActivityLog().size());d=service.getRequestDtoById(id);d.setAction("draft");d.setWorkLocation("Ha Noi");service.updateRequisition(id,d);stored(id);var view=service.getById(id);assertEquals("Ha Noi",view.getWorkLocation());assertEquals(2,view.getActivityLog().size());assertTrue(view.getActivityLog().getFirst().getDescription().contains("Location: Da Nang → Ha Noi"));assertFalse(view.getActivityLog().getFirst().getDescription().contains("Job description"));}
 @Test void criteriaRenameSwapAndRemoveAddKeepWorking(){var d=complete("draft");d.setScreeningCriteria(new ArrayList<>(List.of(criterion("Java","50"),criterion("SQL","50"))));int id=service.createRequisition(d);stored(id);d=service.getRequestDtoById(id);d.setAction("draft");d.getScreeningCriteria().get(0).setCriteriaName("SQL");d.getScreeningCriteria().get(1).setCriteriaName("Java");service.updateRequisition(id,d);stored(id);d=service.getRequestDtoById(id);d.setAction("draft");d.getScreeningCriteria().clear();d.getScreeningCriteria().add(criterion("Java","100"));service.updateRequisition(id,d);assertEquals(1,stored(id).getScreeningCriteria().size());}
 @Test void copyDoesNotSaveAndCannotReuseCriterionIdsOrVersion(){int id=service.createRequisition(complete("submit"));stored(id);long count=service.countVisible();var copy=service.copy(id);assertNull(copy.getVersion());assertNull(copy.getScreeningCriteria().getFirst().getCriteriaId());assertEquals(count,service.countVisible());copy.setAction("draft");int newId=service.createRequisition(copy);assertNotEquals(id,newId);assertEquals("Pending_Director",stored(id).getApprovalStatus());assertEquals("Draft",stored(newId).getApprovalStatus());}
 @Test void rejectEditResubmitApproveLifecycle(){int id=service.createRequisition(complete("submit"));var r=stored(id);signIn(director);service.decide(id,r.getVersion(),false,"Clarify budget");stored(id);signIn(manager);assertEquals("Clarify budget",service.getById(id).getRejectionReason());var d=service.getRequestDtoById(id);d.setAction("submit");d.setReasonForHiring("Revised budget");service.updateRequisition(id,d);r=stored(id);signIn(director);service.decide(id,r.getVersion(),true,"Approved");stored(id);signIn(manager);assertFalse(service.getById(id).isEditable());assertEquals(4,service.getById(id).getTimeline().size());}
 @Test void withdrawInvalidatesDirectorForm(){int id=service.createRequisition(complete("submit"));var r=stored(id);long old=r.getVersion();service.withdraw(id,old);stored(id);var d=service.getRequestDtoById(id);d.setAction("submit");service.updateRequisition(id,d);stored(id);signIn(director);assertThrows(RequisitionValidationException.class,()->service.decide(id,old,true,""));}
 @Test void deleteRejectedCleansDependenciesAndRetainsAudit(){int id=service.createRequisition(complete("submit"));var r=stored(id);signIn(director);service.decide(id,r.getVersion(),false,"Budget unavailable");r=stored(id);signIn(manager);service.deleteRequisition(id,r.getVersion());em.flush();assertFalse(requisitions.existsById(id));assertEquals("DELETE",audit.findByEntityNameAndEntityIdOrderByTimestampDesc("JobRequisition",""+id).getFirst().getAction());}
 @Test void scopeAndAllFiltersAreEnforced(){int id=service.createRequisition(complete("draft"));stored(id);assertEquals(1,service.search(1,10,"java",manager.getDepartment().getDepartmentId(),"Full-time","Draft").getTotalElements());assertTrue(service.search(1,10,"java",null,"Part-time","").isEmpty());assertTrue(service.search(1,10,"%",null,"","").isEmpty());signIn(account("Hiring Manager"));assertTrue(service.search(1,10,"",null,"","").isEmpty());assertThrows(AccessDeniedException.class,()->service.getById(id));}
 @Test void linkedPostingPreventsDeletion(){int id=service.createRequisition(complete("draft"));var r=stored(id);postings.saveAndFlush(JobPosting.builder().requisition(r).postingTitle("Test").jobDescription("Test").jobRequirements("Test").postingStatus("Draft").createdBy(users.findById(manager.getUserId()).orElseThrow()).build());assertFalse(service.getById(id).isDeletable());assertThrows(RequisitionValidationException.class,()->service.deleteRequisition(id,r.getVersion()));}
}
