package com.group2.rms.requisition.validator;

import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.ScreeningCriteriaRequest;
import com.group2.rms.requisition.exception.RequisitionValidationException;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class RequisitionValidatorTests {
 private final RequisitionValidator validator=new RequisitionValidator();
 private RequisitionRequest valid(){return RequisitionRequest.builder().action("submit").title("Java Engineer").departmentId(1).numberOfPositions(1).employmentType("Full-time").expectedStartDate(LocalDate.now().plusDays(1)).workLocation("Office").reasonForHiring("Expansion").jobDescription("Develop software").requirementDetails("Experience").screeningCriteria(new ArrayList<>(List.of(ScreeningCriteriaRequest.builder().criteriaName("Java").criteriaType("Skill").requiredValue("Two years").weight(new BigDecimal("100")).build()))).build();}
 @Test void draftsSkipRequiredFieldsAndWeightTotal(){var d=new RequisitionRequest();d.setAction("draft");d.setScreeningCriteria(List.of(ScreeningCriteriaRequest.builder().criteriaName("Partial").weight(new BigDecimal("20")).build()));assertDoesNotThrow(()->validator.validate(d));d.setAction("submit");assertThrows(RequisitionValidationException.class,()->validator.validate(d));}
 @Test void savedDraftPlaceholderIsNotASubmittableJobTitle(){var d=valid();d.setTitle("Untitled requisition");var e=assertThrows(RequisitionValidationException.class,()->validator.validate(d));assertTrue(e.getErrors().containsKey("title"));d.setAction("draft");assertDoesNotThrow(()->validator.validate(d));}
 @Test void submitChecksAllBasicFieldsAndDates(){var d=valid();d.setTitle("  ");d.setEmploymentType("Invalid");d.setExpectedStartDate(LocalDate.now().minusDays(1));d.setNumberOfPositions(null);var e=assertThrows(RequisitionValidationException.class,()->validator.validate(d));assertTrue(e.getErrors().keySet().containsAll(List.of("title","employmentType","expectedStartDate","numberOfPositions")));}
 @Test void limitsAndSalaryRangeAreChecked(){var d=valid();d.setReasonForHiring("x".repeat(2000));assertDoesNotThrow(()->validator.validate(d));d.setReasonForHiring("x".repeat(2001));d.setMinSalary(BigDecimal.TEN);d.setMaxSalary(BigDecimal.ONE);var e=assertThrows(RequisitionValidationException.class,()->validator.validate(d));assertTrue(e.getErrors().containsKey("reasonForHiring"));assertTrue(e.getErrors().containsKey("maxSalary"));}
 @Test void duplicateCriteriaAndInvalidWeightsCannotBeSubmitted(){var d=valid();d.getScreeningCriteria().add(ScreeningCriteriaRequest.builder().criteriaName(" java ").criteriaType("Invalid").weight(BigDecimal.ZERO).build());var e=assertThrows(RequisitionValidationException.class,()->validator.validate(d));assertTrue(e.getErrors().keySet().containsAll(List.of("screeningCriteria[1].criteriaName","screeningCriteria[1].criteriaType","screeningCriteria[1].weight","screeningCriteria[1].requiredValue")));}
}
