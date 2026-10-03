package com.group2.rms.requisition.controller;

import com.group2.rms.admin.ActivityLogResponse;
import com.group2.rms.core.config.SecurityConfig;
import com.group2.rms.core.security.DatabaseUserDetailsService;
import com.group2.rms.core.security.RoleAuthorities;
import com.group2.rms.requisition.dto.ApprovalResponse;
import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.RequisitionResponse;
import com.group2.rms.requisition.dto.ScreeningCriteriaResponse;
import com.group2.rms.requisition.exception.RequisitionValidationException;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.requisition.service.RequisitionService;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.Role;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import com.group2.rms.user.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
@WebMvcTest(RequisitionController.class) @Import({SecurityConfig.class,DatabaseUserDetailsService.class})
class RequisitionReviewWebProbeTests {
 @Autowired MockMvc mvc; @MockitoBean RequisitionService service; @MockitoBean RequisitionAccess access;
 @Test void listKeepsFiltersAndSortInPagination()throws Exception{
  var s=session("Hiring Manager");
  when(service.search(1,10,"Engineering",1,"Full-time","Draft","oldest"))
    .thenReturn(new PageImpl<>(List.of(detail()),PageRequest.of(0,10),12));
  mvc.perform(get("/requisitions").session(s).param("q","Engineering").param("departmentId","1")
    .param("type","Full-time").param("status","Draft").param("sort","oldest"))
    .andExpect(status().isOk()).andExpect(model().attribute("selectedSort","oldest"))
    .andExpect(content().string(containsString("sort=oldest")))
    .andExpect(content().string(containsString("q=Engineering")))
    .andExpect(content().string(containsString("12 matching requisitions in total")));
  when(service.search(1,10,"",null,"","","newest")).thenReturn(Page.empty());
  mvc.perform(get("/requisitions").session(s).param("sort","unknown-field"))
    .andExpect(status().isOk()).andExpect(model().attribute("selectedSort","newest"))
    .andDo(r->preview("empty",r));
 }
 @MockitoBean UserRepository users; @MockitoBean DepartmentRepository departments;
 private MockHttpSession session(String role){var dept=Department.builder().departmentId(1).departmentName("Engineering").build();var u=User.builder().userId(123).username("req-user").fullName("Test Manager").accountStatus("Active").role(Role.builder().roleName(role).build()).department(dept).build();when(users.findByUsernameIgnoreCase("req-user")).thenReturn(Optional.of(u));when(access.actor()).thenReturn(u);when(access.canCreate(u)).thenReturn(!"Director".equals(role));when(departments.findAll()).thenReturn(List.of(dept));var auth=UsernamePasswordAuthenticationToken.authenticated("req-user","",List.of(new SimpleGrantedAuthority(RoleAuthorities.fromRoleName(role))));var s=new MockHttpSession();s.setAttribute("SPRING_SECURITY_CONTEXT",new SecurityContextImpl(auth));return s;}
 private RequisitionResponse detail(){var criterion=new ScreeningCriteriaResponse();criterion.setCriteriaId(1);criterion.setCriteriaName("Java programming");criterion.setCriteriaType("Skill");criterion.setRequiredValue("Two years");criterion.setWeight(new BigDecimal("100"));criterion.setIsMandatory(true);return RequisitionResponse.builder().requisitionId(10).version(0L).title("Business Development Executive (B2B)").departmentName("Sales & Marketing").hiringManagerName("Test Manager").approvalStatus("Approved").createdAt(LocalDateTime.now()).employmentType("Full-time").numberOfPositions(2).minSalary(new BigDecimal("15000000")).maxSalary(new BigDecimal("25000000")).gender("Any").workLocation("Da Nang").workingHours("Monday-Friday").expectedStartDate(LocalDate.now().plusDays(30)).reasonForHiring("Mở rộng đội ngũ kinh doanh phần mềm.").jobDescription("Tìm kiếm khách hàng tiềm năng B2B, tư vấn giải pháp chuyển đổi số.").requirementDetails("Kỹ năng giao tiếp và thuyết trình xuất sắc.").screeningCriteria(List.of(criterion)).approvals(List.of(ApprovalResponse.builder().approverName("Director").status("Approved").approvalDate(LocalDateTime.now()).comments("Phê duyệt mở 2 vị trí.").build())).timeline(List.of()).activityLog(List.of(ActivityLogResponse.builder().action("CREATE").performedBy("Test Manager").timestamp(LocalDateTime.now()).description("Drafted B2B Sales requisition").build())).build();}
 @Test void candidatesCannotAccessAndGetCannotDelete()throws Exception{mvc.perform(get("/requisitions").session(session("Candidate"))).andExpect(status().isForbidden());mvc.perform(get("/requisitions/delete/10").session(session("Hiring Manager"))).andExpect(status().isMethodNotAllowed());verifyNoInteractions(service);}
 @Test void formOnlyBindsExistingDatabaseFields()throws Exception{mvc.perform(get("/requisitions/create").session(session("Hiring Manager"))).andExpect(status().isOk()).andExpect(content().string(containsString("name=\"workingHours\""))).andExpect(content().string(not(containsString("name=\"workFormat\"")))).andExpect(content().string(containsString("name=\"expectedStartDate\""))).andExpect(content().string(not(containsString("name=\"hiringManagerId\"")))).andExpect(content().string(containsString("name=\"_csrf\""))).andDo(r->preview("form",r));}
 @Test void fieldErrorsKeepUserInputAndSaveDraftPassesItsAction()throws Exception{var s=session("Hiring Manager");var page=mvc.perform(get("/requisitions/create").session(s)).andReturn();var csrf=(CsrfToken)page.getRequest().getAttribute(CsrfToken.class.getName());when(service.createRequisition(any())).thenThrow(new RequisitionValidationException(Map.of("workLocation","Enter a location.")));mvc.perform(post("/requisitions/create").session(s).param(csrf.getParameterName(),csrf.getToken()).param("action","submit").param("title","Keep this title")).andExpect(status().isOk()).andExpect(content().string(containsString("Keep this title"))).andExpect(content().string(containsString("Enter a location."))).andExpect(content().string(containsString("aria-invalid=\"true\""))).andDo(r->preview("form-error",r));doReturn(10).when(service).createRequisition(any());mvc.perform(post("/requisitions/create").session(s).param(csrf.getParameterName(),csrf.getToken()).param("action","draft")).andExpect(redirectedUrl("/requisitions/10"));verify(service).createRequisition(argThat(d->"draft".equals(d.getAction())));}
 @Test void listFiltersPaginationAndSafeDeleteRender()throws Exception{var s=session("System Admin");var draft=detail();draft.setApprovalStatus("Draft");draft.setEditable(true);draft.setDeletable(true);var pending=detail();pending.setRequisitionId(11);pending.setTitle("Japanese-speaking Engineer");pending.setApprovalStatus("Pending_Director");when(service.search(1,10,"",null,"","","newest")).thenReturn(new PageImpl<>(List.of(draft,pending),PageRequest.of(0,10),12));when(service.countVisible()).thenReturn(12L);mvc.perform(get("/requisitions").session(s)).andExpect(status().isOk()).andExpect(content().string(containsString("name=\"status\""))).andExpect(content().string(containsString("data-delete-url=\"/requisitions/delete/10\""))).andExpect(content().string(containsString("name=\"_csrf\""))).andDo(r->preview("list",r));}
 @Test void detailAndCopyRenderExistingValuesWithoutCreatingAnything()throws Exception{var s=session("Hiring Manager");when(service.getById(10)).thenReturn(detail());mvc.perform(get("/requisitions/10").session(s)).andExpect(status().isOk()).andExpect(content().string(containsString("Da Nang"))).andExpect(content().string(containsString("activity-details"))).andExpect(content().string(not(containsString("href=\"/admin/accounts\"")))).andDo(r->preview("detail",r));var copy=new RequisitionRequest();copy.setTitle("Copied role");when(service.copy(10)).thenReturn(copy);mvc.perform(get("/requisitions/copy/10").session(s)).andExpect(status().isOk()).andExpect(content().string(containsString("Copied role"))).andExpect(content().string(containsString("action=\"/requisitions/create\"")));verify(service,never()).createRequisition(any());}
 @Test void deleteDecisionAndWithdrawRequireCsrfAndCarryVersion()throws Exception{var s=session("Director");var d=detail();d.setApprovalStatus("Pending_Director");d.setDecidable(true);when(service.getById(10)).thenReturn(d);var page=mvc.perform(get("/requisitions/10").session(s)).andExpect(status().isOk()).andDo(r->preview("decision",r)).andReturn();var csrf=(CsrfToken)page.getRequest().getAttribute(CsrfToken.class.getName());mvc.perform(post("/requisitions/delete/10").session(s).param("version","0")).andExpect(status().isForbidden());mvc.perform(post("/requisitions/10/decision").session(s).param(csrf.getParameterName(),csrf.getToken()).param("version","0").param("decision","reject").param("comment","Review budget")).andExpect(redirectedUrl("/requisitions/10"));verify(service).decide(10,0L,false,"Review budget");s=session("Hiring Manager");mvc.perform(post("/requisitions/delete/10").session(s).param(csrf.getParameterName(),csrf.getToken()).param("version","0")).andExpect(status().isForbidden());}
 @Test void inaccessibleRequisitionKeepsForbiddenStatus() throws Exception {
  var session = session("Hiring Manager");
  when(service.getById(10)).thenThrow(new org.springframework.security.access.AccessDeniedException("Outside your scope."));
  mvc.perform(get("/requisitions/10").session(session)).andExpect(status().isForbidden());
 }
 @Test void missingRequisitionKeepsNotFoundStatus() throws Exception {
  var session = session("Hiring Manager");
  when(service.getById(10)).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Requisition not found."));
  mvc.perform(get("/requisitions/10").session(session)).andExpect(status().isNotFound());
 }
 private void preview(String name,org.springframework.test.web.servlet.MvcResult r)throws Exception{if(!Boolean.getBoolean("requisition.preview"))return;var p=java.nio.file.Path.of("target","requisition-preview");java.nio.file.Files.createDirectories(p);java.nio.file.Files.writeString(p.resolve(name+".html"),r.getResponse().getContentAsString());}
}
