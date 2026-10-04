package com.group2.rms.requisition.service;

import com.group2.rms.admin.dto.ActivityLogResponse;
import com.group2.rms.admin.entity.AuditLog;
import com.group2.rms.admin.repository.AuditLogRepository;
import com.group2.rms.notification.NotificationService;
import com.group2.rms.requisition.dto.ApprovalResponse;
import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.RequisitionResponse;
import com.group2.rms.requisition.dto.RequisitionTimelineResponse;
import com.group2.rms.requisition.dto.ScreeningCriteriaRequest;
import com.group2.rms.requisition.dto.ScreeningCriteriaResponse;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.requisition.entity.RequisitionApproval;
import com.group2.rms.requisition.entity.RequisitionWorkflowEvent;
import com.group2.rms.requisition.entity.ScreeningCriteria;
import com.group2.rms.requisition.exception.RequisitionValidationException;
import com.group2.rms.requisition.repository.JobPostingRepository;
import com.group2.rms.requisition.repository.JobRequisitionRepository;
import com.group2.rms.requisition.repository.RequisitionApprovalRepository;
import com.group2.rms.requisition.repository.RequisitionWorkflowEventRepository;
import com.group2.rms.requisition.validator.RequisitionValidator;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service @Transactional @RequiredArgsConstructor
public class RequisitionServiceImpl implements RequisitionService {
    private final JobRequisitionRepository requisitions;
    private final DepartmentRepository departments;
    private final RequisitionApprovalRepository approvals;
    private final JobPostingRepository postings;
    private final AuditLogRepository audit;
    private final RequisitionWorkflowEventRepository events;
    private final RequisitionAccess access;
    private final RequisitionValidator validator;
    private final NotificationService notificationService;

    private Specification<JobRequisition> scope(User actor) {
        return (r,q,cb) -> switch(access.role(actor)) {
            case "Hiring Manager" -> cb.equal(r.get("hiringManager").get("userId"),actor.getUserId());
            case "Director" -> cb.notEqual(r.get("approvalStatus"),"Draft");
            case "HR" -> cb.equal(r.get("approvalStatus"),"Approved");
            default -> cb.conjunction();
        };
    }
    @Override @Transactional(readOnly=true)
    public Page<RequisitionResponse> search(int page,int size,String query,Integer department,String type,String status) {
        return search(page,size,query,department,type,status,"newest");
    }
    @Override @Transactional(readOnly=true)
    public Page<RequisitionResponse> search(int page,int size,String query,Integer department,String type,String status,String order) {
        User actor=access.actor(); var filter=scope(actor);
        String term=Objects.toString(query,"").trim().toLowerCase(Locale.ROOT);
        if(term.length()>120)term=term.substring(0,120);
        String pattern="%"+term.replace("\\","\\\\").replace("%","\\%").replace("_","\\_").replace("[","\\[")+"%";
        if(!term.isEmpty()) filter=filter.and((r,q,cb)->cb.or(
            cb.like(cb.lower(r.get("title")),pattern,'\\'),
            cb.like(cb.lower(r.join("department",jakarta.persistence.criteria.JoinType.LEFT).get("departmentName")),pattern,'\\')));
        if(department!=null)filter=filter.and((r,q,cb)->cb.equal(r.get("department").get("departmentId"),department));
        if(type!=null&&!type.isBlank())filter=filter.and((r,q,cb)->cb.equal(r.get("employmentType"),type));
        if(status!=null&&!status.isBlank())filter=filter.and((r,q,cb)->cb.equal(r.get("approvalStatus"),status));
        int count=Math.max(1,Math.min(size,50));
        var sort=switch(Objects.toString(order,"newest")) {
            case "oldest" -> Sort.by(Sort.Order.asc("createdAt"),Sort.Order.asc("requisitionId"));
            case "position_asc" -> Sort.by(Sort.Order.asc("title"),Sort.Order.desc("requisitionId"));
            case "position_desc" -> Sort.by(Sort.Order.desc("title"),Sort.Order.desc("requisitionId"));
            default -> Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("requisitionId"));
        };
        var result=requisitions.findAll(filter,PageRequest.of(Math.max(0,page-1),count,sort));
        if(result.getTotalPages()>0&&result.getNumber()>=result.getTotalPages())result=requisitions.findAll(filter,PageRequest.of(result.getTotalPages()-1,count,sort));
        return result.map(r->response(r,actor,false));
    }
    @Override @Transactional(readOnly=true)
    public long countVisible() { return requisitions.count(scope(access.actor())); }
    @Override @Transactional(readOnly=true)
    public RequisitionResponse getById(Integer id) { var actor=access.actor();var r=find(id,false);access.requireView(actor,r);return response(r,actor,true); }
    @Override @Transactional(readOnly=true)
    public RequisitionRequest getRequestDtoById(Integer id) { var r=find(id,false);access.requireEdit(access.actor(),r);return request(r); }
    @Override @Transactional(readOnly=true)
    public RequisitionRequest copy(Integer id) {
        var actor=access.actor();access.requireCreate(actor);var r=find(id,false);access.requireView(actor,r);
        var copy=request(r);copy.setVersion(null);copy.getScreeningCriteria().forEach(c->c.setCriteriaId(null));return copy;
    }
    private RequisitionRequest request(JobRequisition r) {
        var rows=r.getScreeningCriteria().stream().map(c->ScreeningCriteriaRequest.builder().criteriaId(c.getCriteriaId()).criteriaName(c.getCriteriaName())
            .criteriaType(c.getCriteriaType()).requiredValue(c.getRequiredValue()).weight(c.getWeight()).isMandatory(c.getIsMandatory()).build()).collect(Collectors.toCollection(ArrayList::new));
        return RequisitionRequest.builder().version(r.getVersion()).title(r.getTitle()).departmentId(r.getDepartment()==null?null:r.getDepartment().getDepartmentId())
            .numberOfPositions(r.getNumberOfPositions()).employmentType(r.getEmploymentType()).minSalary(r.getMinSalary()).maxSalary(r.getMaxSalary())
            .gender(r.getGender()).workLocation(r.getWorkLocation()).workModel(r.getWorkModel()).probationDuration(r.getProbationDuration()).expectedStartDate(r.getExpectedStartDate())
            .reasonForHiring(r.getReasonForHiring()).jobDescription(r.getJobDescription()).requirementDetails(r.getRequirementDetails()).screeningCriteria(rows).build();
    }
    @Override
    public Integer createRequisition(RequisitionRequest d) {
        var actor=access.actor();access.requireCreate(actor);validator.validate(d);
        if(d.getScreeningCriteria().stream().anyMatch(c->c.getCriteriaId()!=null))throw invalid("screeningCriteria","New criteria cannot reference existing records.");
        var r=new JobRequisition();r.setHiringManager(actor);r.setScreeningCriteria(new ArrayList<>());apply(r,d);
        d.getScreeningCriteria().stream().filter(c->!RequisitionValidator.blank(c)).forEach(c->r.getScreeningCriteria().add(criterion(r,c)));
        r.setApprovalStatus("submit".equals(d.getAction())?"Pending_Director":"Draft");
        if("submit".equals(d.getAction()))r.setSubmittedAt(LocalDateTime.now());
        requisitions.saveAndFlush(r);
        if(r.getSubmittedAt()!=null)event(r,actor,"Submitted","Sent to the Director approval queue.");
        log(r,actor,"CREATE",null,"Created "+r.getTitle()+" · "+r.getApprovalStatus());return r.getRequisitionId();
    }
    @Override
    public void updateRequisition(Integer id,RequisitionRequest d) {
        var actor=access.actor();var r=find(id,true);access.requireEdit(actor,r);version(r,d.getVersion());validator.validate(d);
        var before=snapshot(r);synchronizeCriteria(r,d.getScreeningCriteria());apply(r,d);
        r.setApprovalStatus("submit".equals(d.getAction())?"Pending_Director":"Draft");
        if("submit".equals(d.getAction())) { r.setSubmittedAt(LocalDateTime.now());r.setDecidedAt(null);event(r,actor,"Submitted","Sent to the Director approval queue."); }
        var after=snapshot(r);Set<String> keys=new LinkedHashSet<>(before.keySet());keys.addAll(after.keySet());
        var changed=keys.stream().filter(k->!Objects.equals(before.get(k),after.get(k))).toList();
        if(!changed.isEmpty()) {
            r.setUpdatedAt(LocalDateTime.now());requisitions.saveAndFlush(r);
            log(r,actor,"UPDATE",changed.stream().map(k->k+": "+display(before.get(k))).collect(Collectors.joining("\n")),
                changed.stream().map(k->k+": "+display(before.get(k))+" → "+display(after.get(k))).collect(Collectors.joining("\n")));
        }
    }
    private void synchronizeCriteria(JobRequisition r,List<ScreeningCriteriaRequest> input) {
        var rows=input.stream().filter(c->!RequisitionValidator.blank(c)).toList();
        var existing=r.getScreeningCriteria().stream().collect(Collectors.toMap(ScreeningCriteria::getCriteriaId,c->c));
        if(rows.stream().anyMatch(c->c.getCriteriaId()!=null&&!existing.containsKey(c.getCriteriaId())))throw invalid("screeningCriteria","A criterion does not belong to this request.");
        var kept=rows.stream().map(ScreeningCriteriaRequest::getCriteriaId).filter(Objects::nonNull).collect(Collectors.toSet());
        boolean changed=r.getScreeningCriteria().removeIf(c->!kept.contains(c.getCriteriaId()));
        // Release old unique names before swaps or replacement inserts.
        for(var row:rows) { var old=existing.get(row.getCriteriaId());if(old!=null&&!Objects.equals(old.getCriteriaName(),row.getCriteriaName())) { old.setCriteriaName("__rename_"+UUID.randomUUID());changed=true; } }
        if(changed)requisitions.flush();
        for(var row:rows) { var old=existing.get(row.getCriteriaId());if(old==null)r.getScreeningCriteria().add(criterion(r,row));else copyCriterion(old,row); }
    }
    private ScreeningCriteria criterion(JobRequisition r,ScreeningCriteriaRequest d) { var c=new ScreeningCriteria();c.setRequisition(r);copyCriterion(c,d);return c; }
    private void copyCriterion(ScreeningCriteria c,ScreeningCriteriaRequest d) { c.setCriteriaName(d.getCriteriaName());c.setCriteriaType(d.getCriteriaType());c.setRequiredValue(d.getRequiredValue());c.setWeight(d.getWeight());c.setIsMandatory(Boolean.TRUE.equals(d.getIsMandatory())); }
    private void apply(JobRequisition r,RequisitionRequest d) {
        r.setTitle(d.getTitle()==null?"Untitled requisition":d.getTitle());
        r.setDepartment(d.getDepartmentId()==null?null:departments.findById(d.getDepartmentId()).orElseThrow(()->invalid("departmentId","Select an existing department.")));
        r.setNumberOfPositions(d.getNumberOfPositions());r.setEmploymentType(d.getEmploymentType());r.setMinSalary(d.getMinSalary());r.setMaxSalary(d.getMaxSalary());
        r.setGender(d.getGender());r.setWorkLocation(d.getWorkLocation());r.setWorkModel(d.getWorkModel());r.setProbationDuration(d.getProbationDuration());r.setExpectedStartDate(d.getExpectedStartDate());
        r.setReasonForHiring(d.getReasonForHiring());r.setJobDescription(d.getJobDescription());r.setRequirementDetails(d.getRequirementDetails());
    }
    @Override
    public void deleteRequisition(Integer id,Long expectedVersion) {
        var actor=access.actor();var r=find(id,true);access.requireEdit(actor,r);version(r,expectedVersion);
        if(postings.existsByRequisition_RequisitionId(id))throw invalid("action","This request has linked job postings and cannot be deleted.");
        log(r,actor,"DELETE",null,"Deleted requisition: "+r.getTitle());
        approvals.deleteAll(approvals.findByRequisition_RequisitionIdOrderByApprovalDateDesc(id));events.deleteByRequisition_RequisitionId(id);
        requisitions.delete(r);requisitions.flush();
    }
    @Override
    public void decide(Integer id,Long expectedVersion,boolean approved,String comment) {
        var actor=access.actor();var r=find(id,true);access.requireView(actor,r);version(r,expectedVersion);
        if(!access.canDecide(actor,r))throw new AccessDeniedException("Only a Director can decide a pending request.");
        comment=RequisitionValidator.clean(comment);
        if(!approved&&comment==null)throw invalid("comment","Explain what the Hiring Manager must change.");
        if(comment!=null&&comment.length()>1000)throw invalid("comment","Use at most 1000 characters.");
        String status=approved?"Approved":"Rejected";r.setApprovalStatus(status);r.setDecidedAt(LocalDateTime.now());
        approvals.save(RequisitionApproval.builder().requisition(r).director(actor).status(status).comments(comment).build());
        requisitions.saveAndFlush(r);event(r,actor,status,comment);
        log(r,actor,"UPDATE","Status: Pending_Director","Status: Pending_Director → "+status+(comment==null?"":"\nDirector feedback: "+comment));
        if(!approved) {
            notificationService.notifyRequisitionRejected(r,actor,comment);
        }
    }
    @Override
    public void withdraw(Integer id,Long expectedVersion) {
        var actor=access.actor();var r=find(id,true);access.requireView(actor,r);version(r,expectedVersion);
        if(!access.owns(actor,r)||!"Pending_Director".equals(r.getApprovalStatus()))throw new AccessDeniedException("Only the requester can withdraw a pending request.");
        r.setApprovalStatus("Draft");requisitions.saveAndFlush(r);event(r,actor,"Withdrawn","Withdrawn for editing.");
        log(r,actor,"UPDATE","Status: Pending_Director","Status: Pending_Director → Draft (withdrawn)");
    }
    private JobRequisition find(Integer id,boolean lock) { return (lock?requisitions.findForUpdate(id):requisitions.findById(id)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Requisition not found.")); }
    private void version(JobRequisition r,Long expected) { if(expected==null||!Objects.equals(r.getVersion(),expected))throw invalid("action","This request has changed. Reload the page before trying again."); }
    private RequisitionValidationException invalid(String field,String message) { return new RequisitionValidationException(Map.of(field,message)); }
    private void log(JobRequisition r,User actor,String action,String old,String text) { audit.save(AuditLog.builder().user(actor).action(action).entityName("JobRequisition").entityId(r.getRequisitionId().toString()).oldValue(old).newValue(text).build()); }
    private void event(JobRequisition r,User actor,String type,String comment) { events.save(RequisitionWorkflowEvent.builder().requisition(r).actor(actor).eventType(type).occurredAt(LocalDateTime.now()).comment(comment).build()); }
    private String display(Object v) { return v==null?"Not specified":v.toString(); }
    private String number(java.math.BigDecimal v) { return v==null?null:v.stripTrailingZeros().toPlainString(); }
    private Map<String,String> snapshot(JobRequisition r) {
        Map<String,String> m=new LinkedHashMap<>();m.put("Job title",r.getTitle());m.put("Department",r.getDepartment()==null?null:r.getDepartment().getDepartmentName());
        m.put("Openings",display(r.getNumberOfPositions()));m.put("Employment type",r.getEmploymentType());m.put("Minimum salary",number(r.getMinSalary()));m.put("Maximum salary",number(r.getMaxSalary()));
        m.put("Gender",r.getGender());m.put("Location",r.getWorkLocation());m.put("Work model",r.getWorkModel());m.put("Probation duration",r.getProbationDuration());m.put("Expected start date",display(r.getExpectedStartDate()));
        m.put("Reason for hiring",r.getReasonForHiring());m.put("Job description",r.getJobDescription());m.put("Candidate requirements",r.getRequirementDetails());m.put("Status",r.getApprovalStatus());
        int i=0;for(var c:r.getScreeningCriteria())m.put("Criterion: "+(c.getCriteriaName()==null?"Unnamed "+(++i):c.getCriteriaName()),display(c.getCriteriaType())+"; "+display(c.getRequiredValue())+"; weight "+display(number(c.getWeight()))+"%; mandatory "+Boolean.TRUE.equals(c.getIsMandatory()));return m;
    }
    private RequisitionResponse response(JobRequisition r,User actor,boolean detail) {
        boolean editable=access.canEdit(actor,r);
        var d=RequisitionResponse.builder().requisitionId(r.getRequisitionId()).version(r.getVersion()).title(r.getTitle())
            .departmentName(r.getDepartment()==null?"Not specified":r.getDepartment().getDepartmentName()).hiringManagerName(r.getHiringManager().getFullName())
            .numberOfPositions(r.getNumberOfPositions()).employmentType(r.getEmploymentType()).approvalStatus(r.getApprovalStatus()).createdAt(r.getCreatedAt())
            .minSalary(r.getMinSalary()).maxSalary(r.getMaxSalary()).gender(r.getGender()).workLocation(r.getWorkLocation()).workModel(r.getWorkModel()).probationDuration(r.getProbationDuration()).expectedStartDate(r.getExpectedStartDate())
            .reasonForHiring(r.getReasonForHiring()).jobDescription(r.getJobDescription()).requirementDetails(r.getRequirementDetails())
            .editable(editable).deletable(editable&&!postings.existsByRequisition_RequisitionId(r.getRequisitionId())).decidable(access.canDecide(actor,r))
            .withdrawable(access.owns(actor,r)&&"Pending_Director".equals(r.getApprovalStatus())).build();
        if(detail) {
            if ("Draft".equals(r.getApprovalStatus())) {
                var submission = request(r);
                submission.setAction("submit");
                try { validator.validate(submission); }
                catch (RequisitionValidationException incomplete) { d.setIncomplete(true); }
            }
            d.setScreeningCriteria(r.getScreeningCriteria().stream().map(c->{var x=new ScreeningCriteriaResponse();x.setCriteriaId(c.getCriteriaId());x.setCriteriaName(c.getCriteriaName());x.setCriteriaType(c.getCriteriaType());x.setRequiredValue(c.getRequiredValue());x.setWeight(c.getWeight());x.setIsMandatory(c.getIsMandatory());return x;}).toList());
            d.setApprovals(approvals.findByRequisition_RequisitionIdOrderByApprovalDateDesc(r.getRequisitionId()).stream().map(a->ApprovalResponse.builder().approverName(a.getDirector().getFullName()).status(a.getStatus()).approvalDate(a.getApprovalDate()).comments(a.getComments()).build()).toList());
            d.setActivityLog(audit.findByEntityNameAndEntityIdOrderByTimestampDesc("JobRequisition",r.getRequisitionId().toString()).stream().map(a->ActivityLogResponse.builder().auditLogId(a.getAuditLogId()).action(a.getAction()).performedBy(a.getUser().getFullName()).description(a.getNewValue()).timestamp(a.getTimestamp()).build()).toList());
            d.setTimeline(events.findByRequisition_RequisitionIdOrderByOccurredAtDescEventIdDesc(r.getRequisitionId()).stream().map(e->new RequisitionTimelineResponse(e.getEventType(),e.getActor().getFullName(),e.getOccurredAt(),e.getComment())).toList());
            if("Rejected".equals(r.getApprovalStatus())&&!d.getApprovals().isEmpty())d.setRejectionReason(d.getApprovals().getFirst().getComments());
        }
        return d;
    }
}
