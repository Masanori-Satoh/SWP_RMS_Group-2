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
        if(!Set.of("draft","submit").contains(Objects.toString(d.getAction(),""))) errors.put("action","Vui lòng chọn Lưu bản nháp hoặc Gửi Giám đốc duyệt.");
        d.setTitle(clean(d.getTitle())); d.setEmploymentType(clean(d.getEmploymentType()));
        d.setReasonForHiring(clean(d.getReasonForHiring())); d.setJobDescription(clean(d.getJobDescription())); d.setRequirementDetails(clean(d.getRequirementDetails()));
        d.setWorkLocation(clean(d.getWorkLocation())); d.setWorkModel(clean(d.getWorkModel())); d.setProbationDuration(clean(d.getProbationDuration())); d.setGender(clean(d.getGender()));
        text(errors,"title",d.getTitle(),200,submit); text(errors,"reasonForHiring",d.getReasonForHiring(),REASON_LIMIT,submit);
        if (submit && "Untitled requisition".equalsIgnoreCase(d.getTitle())) {
            errors.put("title", "Vui lòng nhập chức danh công việc cụ thể trước khi gửi duyệt.");
        }
        text(errors,"jobDescription",d.getJobDescription(),2000,submit); text(errors,"requirementDetails",d.getRequirementDetails(),2000,submit);
        text(errors,"workLocation",d.getWorkLocation(),255,submit); text(errors,"probationDuration",d.getProbationDuration(),255,false);
        choice(errors,"employmentType",d.getEmploymentType(),EMPLOYMENT_TYPES,submit); choice(errors,"workModel",d.getWorkModel(),WORK_MODELS,submit); choice(errors,"gender",d.getGender(),GENDERS,false);
        if(submit&&d.getDepartmentId()==null) errors.put("departmentId","Vui lòng chọn phòng ban.");
        if(submit&&d.getNumberOfPositions()==null) errors.put("numberOfPositions","Vui lòng nhập số lượng cần tuyển.");
        if(d.getNumberOfPositions()!=null&&d.getNumberOfPositions()<1) errors.put("numberOfPositions","Số lượng cần tuyển phải là số nguyên lớn hơn 0.");
        if(submit&&(d.getExpectedStartDate()==null||d.getExpectedStartDate().isBefore(LocalDate.now()))) errors.put("expectedStartDate","Vui lòng chọn ngày hôm nay hoặc ngày trong tương lai.");
        money(errors,"minSalary",d.getMinSalary()); money(errors,"maxSalary",d.getMaxSalary());
        if(d.getMinSalary()!=null&&d.getMaxSalary()!=null&&d.getMinSalary().compareTo(d.getMaxSalary())>0) errors.put("maxSalary","Mức lương tối đa phải lớn hơn hoặc bằng mức lương tối thiểu.");
        if(d.getScreeningCriteria()==null) d.setScreeningCriteria(new ArrayList<>());
        if(d.getScreeningCriteria().size()>50) errors.put("screeningCriteria","Chỉ được thêm tối đa 50 tiêu chí.");
        Set<String> names=new HashSet<>(); Set<Integer> ids=new HashSet<>(); BigDecimal total=BigDecimal.ZERO; int count=0;
        for(int i=0;i<d.getScreeningCriteria().size();i++) {
            var row=d.getScreeningCriteria().get(i); String key="screeningCriteria["+i+"].";
            if(row==null) { errors.put("screeningCriteria","Tiêu chí không hợp lệ."); continue; }
            row.setCriteriaName(clean(row.getCriteriaName())); row.setCriteriaType(clean(row.getCriteriaType())); row.setRequiredValue(clean(row.getRequiredValue()));
            if(blank(row)&&!submit) continue;
            count++;
            text(errors,key+"criteriaName",row.getCriteriaName(),150,submit); text(errors,key+"requiredValue",row.getRequiredValue(),255,submit);
            choice(errors,key+"criteriaType",row.getCriteriaType(),CRITERIA_TYPES,submit);
            if(row.getCriteriaName()!=null&&!names.add(row.getCriteriaName().toLowerCase(Locale.ROOT))) errors.put(key+"criteriaName","Tên các tiêu chí không được trùng nhau.");
            if(row.getCriteriaId()!=null&&!ids.add(row.getCriteriaId())) errors.put(key+"criteriaName","Tiêu chí đã được gửi hai lần.");
            var weight=row.getWeight();
            if(submit&&weight==null) errors.put(key+"weight","Vui lòng nhập trọng số.");
            if(weight!=null) {
                if(weight.signum()<=0||weight.compareTo(new BigDecimal(submit?"100":"999.99"))>0||weight.stripTrailingZeros().scale()>2) errors.put(key+"weight",submit?"Trọng số phải từ 0.01 đến 100 với tối đa hai chữ số thập phân.":"Trọng số phải là số dương với tối đa hai chữ số thập phân.");
                total=total.add(weight);
            }
        }
        if(submit&&(count==0||total.compareTo(new BigDecimal("100"))!=0)) errors.put("screeningCriteria","Vui lòng thêm đầy đủ tiêu chí với tổng trọng số đúng bằng 100%.");
        if(!errors.isEmpty()) throw new RequisitionValidationException(errors);
    }
    public static String clean(String v) { return v==null||v.isBlank()?null:v.trim(); }
    public static boolean blank(ScreeningCriteriaRequest r) { return clean(r.getCriteriaName())==null&&clean(r.getCriteriaType())==null&&clean(r.getRequiredValue())==null&&!Boolean.TRUE.equals(r.getIsMandatory()); }
    private void text(Map<String,String> e,String key,String v,int max,boolean required) { if(required&&v==null)e.put(key,"Trường này bắt buộc nhập trước khi gửi phê duyệt."); else if(v!=null&&v.length()>max)e.put(key,"Tối đa "+max+" ký tự."); }
    private void choice(Map<String,String> e,String key,String v,List<String> choices,boolean required) { if((required||v!=null)&&(v==null||!choices.contains(v)))e.put(key,"Vui lòng chọn một tùy chọn hợp lệ."); }
    private void money(Map<String,String> e,String key,BigDecimal v) { if(v!=null&&(v.signum()<0||v.compareTo(new BigDecimal("9999999999999999.99"))>0||v.stripTrailingZeros().scale()>2))e.put(key,"Vui lòng nhập số tiền không âm với tối đa 2 chữ số thập phân."); }
}
