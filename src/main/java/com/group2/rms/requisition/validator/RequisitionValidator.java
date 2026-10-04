package com.group2.rms.requisition.validator;

import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.ScreeningCriteriaRequest;
import com.group2.rms.requisition.exception.RequisitionValidationException;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** Drafts skip completeness rules; storage limits still prevent failed or truncated writes. */
@Component
public class RequisitionValidator {
    public static final int REASON_LIMIT=2000;
    public static final List<String> EMPLOYMENT_TYPES=List.of("Full-time","Part-time","Internship","Contract");
    public static final List<String> CRITERIA_TYPES=List.of("Education","Experience","Skill","Knockout");
    public static final List<String> GENDERS=List.of("Any","Male","Female");
    public static final List<String> WORK_MODELS=List.of("On-site","Remote","Hybrid");
    public void validate(RequisitionRequest d) {
        var errors=new LinkedHashMap<String,String>(); boolean submit="submit".equals(d.getAction());
        if(!Set.of("draft","submit").contains(Objects.toString(d.getAction(),""))) errors.put("action","Choose Save Draft or Submit to Director.");
        d.setTitle(clean(d.getTitle())); d.setEmploymentType(clean(d.getEmploymentType()));
        d.setReasonForHiring(clean(d.getReasonForHiring())); d.setJobDescription(clean(d.getJobDescription())); d.setRequirementDetails(clean(d.getRequirementDetails()));
        d.setWorkLocation(clean(d.getWorkLocation())); d.setWorkModel(clean(d.getWorkModel())); d.setProbationDuration(clean(d.getProbationDuration())); d.setGender(clean(d.getGender()));
        text(errors,"title",d.getTitle(),200,submit); text(errors,"reasonForHiring",d.getReasonForHiring(),REASON_LIMIT,submit);
        if (submit && "Untitled requisition".equalsIgnoreCase(d.getTitle())) {
            errors.put("title", "Replace the draft placeholder with a job title before submitting.");
        }
        text(errors,"jobDescription",d.getJobDescription(),2000,submit); text(errors,"requirementDetails",d.getRequirementDetails(),2000,submit);
        text(errors,"workLocation",d.getWorkLocation(),255,submit); text(errors,"probationDuration",d.getProbationDuration(),255,false);
        choice(errors,"employmentType",d.getEmploymentType(),EMPLOYMENT_TYPES,submit); choice(errors,"workModel",d.getWorkModel(),WORK_MODELS,submit); choice(errors,"gender",d.getGender(),GENDERS,false);
        if(submit&&d.getDepartmentId()==null) errors.put("departmentId","Select a department.");
        if(submit&&d.getNumberOfPositions()==null) errors.put("numberOfPositions","Enter the number of openings.");
        if(d.getNumberOfPositions()!=null&&d.getNumberOfPositions()<1) errors.put("numberOfPositions","Use a whole number greater than zero.");
        if(submit&&(d.getExpectedStartDate()==null||d.getExpectedStartDate().isBefore(LocalDate.now()))) errors.put("expectedStartDate","Choose today or a future date.");
        money(errors,"minSalary",d.getMinSalary()); money(errors,"maxSalary",d.getMaxSalary());
        if(d.getMinSalary()!=null&&d.getMaxSalary()!=null&&d.getMinSalary().compareTo(d.getMaxSalary())>0) errors.put("maxSalary","Maximum salary must be at least the minimum salary.");
        if(d.getScreeningCriteria()==null) d.setScreeningCriteria(new ArrayList<>());
        if(d.getScreeningCriteria().size()>50) errors.put("screeningCriteria","Use at most 50 criteria.");
        Set<String> names=new HashSet<>(); Set<Integer> ids=new HashSet<>(); BigDecimal total=BigDecimal.ZERO; int count=0;
        for(int i=0;i<d.getScreeningCriteria().size();i++) {
            var row=d.getScreeningCriteria().get(i); String key="screeningCriteria["+i+"].";
            if(row==null) { errors.put("screeningCriteria","Invalid criterion."); continue; }
            row.setCriteriaName(clean(row.getCriteriaName())); row.setCriteriaType(clean(row.getCriteriaType())); row.setRequiredValue(clean(row.getRequiredValue()));
            if(blank(row)&&!submit) continue;
            count++;
            text(errors,key+"criteriaName",row.getCriteriaName(),150,submit); text(errors,key+"requiredValue",row.getRequiredValue(),255,submit);
            choice(errors,key+"criteriaType",row.getCriteriaType(),CRITERIA_TYPES,submit);
            if(row.getCriteriaName()!=null&&!names.add(row.getCriteriaName().toLowerCase(Locale.ROOT))) errors.put(key+"criteriaName","Criterion names must be unique.");
            if(row.getCriteriaId()!=null&&!ids.add(row.getCriteriaId())) errors.put(key+"criteriaName","A criterion was submitted twice.");
            var weight=row.getWeight();
            if(submit&&weight==null) errors.put(key+"weight","Enter a weight.");
            if(weight!=null) {
                if(weight.signum()<=0||weight.compareTo(new BigDecimal(submit?"100":"999.99"))>0||weight.stripTrailingZeros().scale()>2) errors.put(key+"weight",submit?"Use 0.01–100 with at most two decimal places.":"Use a positive weight with at most two decimal places.");
                total=total.add(weight);
            }
        }
        if(submit&&(count==0||total.compareTo(new BigDecimal("100"))!=0)) errors.put("screeningCriteria","Add complete criteria with total weight exactly 100%.");
        if(!errors.isEmpty()) throw new RequisitionValidationException(errors);
    }
    public static String clean(String v) { return v==null||v.isBlank()?null:v.trim(); }
    public static boolean blank(ScreeningCriteriaRequest r) { return clean(r.getCriteriaName())==null&&clean(r.getCriteriaType())==null&&clean(r.getRequiredValue())==null&&!Boolean.TRUE.equals(r.getIsMandatory()); }
    private void text(Map<String,String> e,String key,String v,int max,boolean required) { if(required&&v==null)e.put(key,"Required before submitting to the Director."); else if(v!=null&&v.length()>max)e.put(key,"Use at most "+max+" characters."); }
    private void choice(Map<String,String> e,String key,String v,List<String> choices,boolean required) { if((required||v!=null)&&(v==null||!choices.contains(v)))e.put(key,"Select a valid option."); }
    private void money(Map<String,String> e,String key,BigDecimal v) { if(v!=null&&(v.signum()<0||v.compareTo(new BigDecimal("9999999999999999.99"))>0||v.stripTrailingZeros().scale()>2))e.put(key,"Enter a non-negative amount with at most two decimal places."); }
}
