package com.group2.rms.requisition.controller;

import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.ScreeningCriteriaRequest;
import com.group2.rms.requisition.exception.RequisitionValidationException;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.requisition.service.RequisitionService;
import com.group2.rms.requisition.validator.RequisitionValidator;
import com.group2.rms.user.repository.DepartmentRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;

@Controller @RequestMapping("/requisitions") @RequiredArgsConstructor
public class RequisitionController {
    private final RequisitionService service;
    private final RequisitionAccess access;
    private final DepartmentRepository departments;

    @InitBinder("requisitionDto")
    void binder(WebDataBinder binder) {
        binder.setAutoGrowCollectionLimit(50);
        binder.setAllowedFields("action","version","title","departmentId","numberOfPositions","employmentType","minSalary","maxSalary",
            "gender","workLocation","workingHours","expectedStartDate","reasonForHiring","jobDescription","requirementDetails",
            "screeningCriteria[*].criteriaId","screeningCriteria[*].criteriaName","screeningCriteria[*].criteriaType",
            "screeningCriteria[*].requiredValue","screeningCriteria[*].weight","screeningCriteria[*].isMandatory");
    }
    @ModelAttribute
    void viewer(Model model) {
        var actor=access.actor();model.addAttribute("viewerName",actor.getFullName());model.addAttribute("viewerRole",actor.getRole().getRoleName());model.addAttribute("canCreate",access.canCreate(actor));
    }
    @GetMapping
    public String list(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size,
                       @RequestParam(defaultValue="") String q,@RequestParam(required=false) Integer departmentId,
                       @RequestParam(defaultValue="") String type,@RequestParam(defaultValue="") String status,
                       @RequestParam(defaultValue="newest") String sort,Model model) {
        if(!java.util.Set.of("newest","oldest","position_asc","position_desc").contains(sort)) sort="newest";
        var result=service.search(page,size,q,departmentId,type,status,sort);
        model.addAttribute("requisitions",result.getContent());model.addAttribute("currentPage",result.getNumber()+1);
        model.addAttribute("totalPages",Math.max(1,result.getTotalPages()));model.addAttribute("pageSize",result.getSize());
        model.addAttribute("startPage",Math.max(1,result.getNumber()-1));model.addAttribute("endPage",Math.min(Math.max(1,result.getTotalPages()),result.getNumber()+3));
        model.addAttribute("totalElements",result.getTotalElements());model.addAttribute("totalVisible",service.countVisible());
        model.addAttribute("search",q);model.addAttribute("selectedDepartment",departmentId);model.addAttribute("selectedType",type);model.addAttribute("selectedStatus",status);
        model.addAttribute("selectedSort",sort);
        options(model);return "requisitions/list";
    }
    @GetMapping("/create")
    public String createForm(Model model) {
        var actor=access.actor();access.requireCreate(actor);var d=new RequisitionRequest();d.setNumberOfPositions(1);d.setGender("Any");
        if(actor.getDepartment()!=null)d.setDepartmentId(actor.getDepartment().getDepartmentId());
        d.getScreeningCriteria().add(ScreeningCriteriaRequest.builder().weight(new BigDecimal("100")).isMandatory(false).build());
        model.addAttribute("requisitionDto",d);return form(model,false,null);
    }
    @GetMapping("/copy/{id}")
    public String copy(@PathVariable Integer id,Model model) {
        model.addAttribute("requisitionDto",service.copy(id));model.addAttribute("copyNotice","Copied into a new form. Nothing is saved until you choose Save Draft or Submit to Director.");return form(model,false,null);
    }
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id,Model model) { model.addAttribute("requisitionDto",service.getRequestDtoById(id));return form(model,true,id); }
    @PostMapping("/create")
    public String create(@ModelAttribute("requisitionDto") RequisitionRequest d,BindingResult errors,Model model,RedirectAttributes flash) {
        access.requireCreate(access.actor());
        if(!errors.hasErrors())try { Integer id=service.createRequisition(d);flash.addFlashAttribute("successMessage","submit".equals(d.getAction())?"Request submitted to Director.":"Draft saved.");return "redirect:/requisitions/"+id; }
        catch(RequisitionValidationException e){addErrors(errors,e);}catch(DataIntegrityViolationException e){errors.reject("storage","The data could not be saved. Check duplicate criteria and field values.");}
        return form(model,false,null);
    }
    @PostMapping("/edit/{id}")
    public String update(@PathVariable Integer id,@ModelAttribute("requisitionDto") RequisitionRequest d,BindingResult errors,Model model,RedirectAttributes flash) {
        if(!errors.hasErrors())try { service.updateRequisition(id,d);flash.addFlashAttribute("successMessage","submit".equals(d.getAction())?"Request submitted to Director.":"Draft saved.");return "redirect:/requisitions/"+id; }
        catch(RequisitionValidationException e){addErrors(errors,e);}catch(OptimisticLockingFailureException e){errors.reject("stale","This request changed. Reload the form.");}
        catch(DataIntegrityViolationException e){errors.reject("storage","The data could not be saved. Check duplicate criteria and field values.");}
        return form(model,true,id);
    }
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Integer id,@RequestParam Long version,RedirectAttributes flash) {
        try { service.deleteRequisition(id,version);flash.addFlashAttribute("successMessage","Requisition deleted.");return "redirect:/requisitions"; }
        catch(RequisitionValidationException e){flash.addFlashAttribute("failureMessage",e.getMessage());}
        catch(DataIntegrityViolationException e){flash.addFlashAttribute("failureMessage","This request is linked to other records and cannot be deleted.");}
        return "redirect:/requisitions/"+id;
    }
    @PostMapping("/{id}/decision")
    public String decide(@PathVariable Integer id,@RequestParam Long version,@RequestParam String decision,@RequestParam(defaultValue="") String comment,RedirectAttributes flash) {
        if(!"approve".equals(decision)&&!"reject".equals(decision))flash.addFlashAttribute("failureMessage","Choose an approval decision.");
        else try {service.decide(id,version,"approve".equals(decision),comment);flash.addFlashAttribute("successMessage","Decision saved.");}
        catch(RequisitionValidationException e){flash.addFlashAttribute("failureMessage",e.getMessage());flash.addFlashAttribute("decisionComment",comment);}
        return "redirect:/requisitions/"+id;
    }
    @PostMapping("/{id}/withdraw")
    public String withdraw(@PathVariable Integer id,@RequestParam Long version,RedirectAttributes flash) {
        try { service.withdraw(id,version);flash.addFlashAttribute("successMessage","Request withdrawn. You can edit the draft."); }
        catch(RequisitionValidationException e){flash.addFlashAttribute("failureMessage",e.getMessage());}
        return "redirect:/requisitions/"+id;
    }
    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id,Model model) {model.addAttribute("req",service.getById(id));return "requisitions/detail";}
    private String form(Model model,boolean edit,Integer id) {model.addAttribute("isEdit",edit);model.addAttribute("requisitionId",id);options(model);return "requisitions/form";}
    private void options(Model model) {model.addAttribute("departments",departments.findAll());model.addAttribute("employmentTypes",RequisitionValidator.EMPLOYMENT_TYPES);model.addAttribute("criteriaTypes",RequisitionValidator.CRITERIA_TYPES);model.addAttribute("genders",RequisitionValidator.GENDERS);}
    private void addErrors(BindingResult errors,RequisitionValidationException e) {e.getErrors().forEach((k,v)->errors.rejectValue(k,"invalid",v));}
}
