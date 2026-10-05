package com.group2.rms.requisition.controller;

import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.RequisitionResponse;
import com.group2.rms.requisition.dto.ScreeningCriteriaRequest;
import com.group2.rms.requisition.exception.RequisitionValidationException;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.requisition.service.RequisitionService;
import com.group2.rms.requisition.validator.RequisitionValidator;
import com.group2.rms.user.entity.Department;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Controller điều hướng và xử lý các trang Job Requisition.
 * Tuân thủ quy định tại docs/members/hoangnh/rule_code_nhh.md
 */
@Controller
@RequestMapping("/requisitions")
@RequiredArgsConstructor
public class RequisitionController {

    public static final String DEFAULT_SORT = "newest";
    public static final Set<String> ALLOWED_SORTS = Set.of("newest", "oldest", "position_asc", "position_desc");

    public static final String MSG_DRAFT_SAVED = "Đã lưu bản nháp thành công.";
    public static final String MSG_SUBMITTED = "Đã gửi yêu cầu tuyển dụng tới Giám đốc phê duyệt.";
    public static final String MSG_DELETED = "Đã xóa yêu cầu tuyển dụng thành công.";
    public static final String MSG_DECISION_SAVED = "Đã lưu quyết định thành công.";
    public static final String MSG_WITHDRAWN = "Đã rút lại yêu cầu. Bạn có thể tiếp tục chỉnh sửa bản nháp.";
    public static final String MSG_COPY_NOTICE = "Đã sao chép vào biểu mẫu mới. Vui lòng bấm Lưu nháp hoặc Gửi duyệt để lưu lại.";

    private final RequisitionService requisitionService;
    private final RequisitionAccess requisitionAccess;
    private final DepartmentRepository departmentRepository;

    @InitBinder("requisitionDto")
    void initDataBinder(WebDataBinder binder) {
        binder.setAutoGrowCollectionLimit(50);
        binder.setAllowedFields(
                "action", "requisitionCode", "title", "departmentId", "recruitmentRound",
                "numberOfPositions", "employmentType", "minSalary", "maxSalary",
                "gender", "workLocation", "workModel", "probationDuration",
                "expectedStartDate", "reasonForHiring", "jobDescription", "requirementDetails",
                "screeningCriteria[*].criteriaId", "screeningCriteria[*].criteriaName",
                "screeningCriteria[*].criteriaType", "screeningCriteria[*].requiredValue",
                "screeningCriteria[*].weight", "screeningCriteria[*].isMandatory"
        );
    }

    @ModelAttribute
    void populateViewerAttributes(Model model) {
        User currentUser = requisitionAccess.actor();
        model.addAttribute("viewerName", currentUser.getFullName());
        model.addAttribute("viewerRole", currentUser.getRole().getRoleName());
        model.addAttribute("canCreate", requisitionAccess.canCreate(currentUser));
    }

    @GetMapping
    public String listRequisitions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(defaultValue = "") String position,
            @RequestParam(required = false) Integer round,
            @RequestParam(defaultValue = "") String type,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "newest") String sort,
            Model model) {

        String sanitizedSort = ALLOWED_SORTS.contains(sort) ? sort : DEFAULT_SORT;
        User currentUser = requisitionAccess.actor();

        Page<RequisitionResponse> requisitionPage;
        if ((position != null && !position.isBlank()) || round != null) {
            requisitionPage = requisitionService.search(page, size, q, departmentId, position, round, type, status, sanitizedSort);
        } else {
            requisitionPage = requisitionService.search(page, size, q, departmentId, type, status, sanitizedSort);
        }

        if (requisitionPage == null) {
            requisitionPage = new PageImpl<>(Collections.emptyList());
        }

        model.addAttribute("requisitions", requisitionPage.getContent());
        model.addAttribute("currentPage", requisitionPage.getNumber() + 1);
        model.addAttribute("totalPages", Math.max(1, requisitionPage.getTotalPages()));
        model.addAttribute("pageSize", requisitionPage.getSize());
        model.addAttribute("startPage", Math.max(1, requisitionPage.getNumber() - 1));
        model.addAttribute("endPage", Math.min(Math.max(1, requisitionPage.getTotalPages()), requisitionPage.getNumber() + 3));
        model.addAttribute("totalElements", requisitionPage.getTotalElements());
        model.addAttribute("totalVisible", requisitionService.countVisible());

        model.addAttribute("search", q);
        model.addAttribute("selectedDepartment", departmentId);
        model.addAttribute("selectedPosition", position);
        model.addAttribute("selectedRound", round);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedSort", sanitizedSort);

        List<String> availablePositions = requisitionService.getAvailablePositions(currentUser);
        model.addAttribute("positions", availablePositions != null ? availablePositions : Collections.emptyList());

        List<Integer> availableRounds = requisitionService.getAvailableRounds();
        model.addAttribute("rounds", availableRounds != null ? availableRounds : Collections.emptyList());

        populateFormOptions(model);
        return "requisitions/list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        User currentUser = requisitionAccess.actor();
        requisitionAccess.requireCreate(currentUser);

        RequisitionRequest requisitionDto = new RequisitionRequest();
        requisitionDto.setNumberOfPositions(1);
        requisitionDto.setGender("Any");
        requisitionDto.setRecruitmentRound(1);

        List<Department> userDepartments = requisitionService.getAvailableDepartments(currentUser);
        if (userDepartments.size() == 1) {
            requisitionDto.setDepartmentId(userDepartments.getFirst().getDepartmentId());
        } else if (currentUser.getDepartment() != null) {
            requisitionDto.setDepartmentId(currentUser.getDepartment().getDepartmentId());
        }

        requisitionDto.getScreeningCriteria().add(
                ScreeningCriteriaRequest.builder()
                        .weight(new BigDecimal("100"))
                        .isMandatory(false)
                        .build()
        );

        model.addAttribute("requisitionDto", requisitionDto);
        return renderForm(model, false, null);
    }

    @GetMapping("/copy/{id}")
    public String copyRequisition(@PathVariable Integer id, Model model) {
        model.addAttribute("requisitionDto", requisitionService.copy(id));
        model.addAttribute("copyNotice", MSG_COPY_NOTICE);
        return renderForm(model, false, null);
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        model.addAttribute("requisitionDto", requisitionService.getRequestDtoById(id));
        return renderForm(model, true, id);
    }

    @PostMapping("/create")
    public String handleCreateRequisition(
            @ModelAttribute("requisitionDto") RequisitionRequest requisitionDto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        requisitionAccess.requireCreate(requisitionAccess.actor());

        if (!bindingResult.hasErrors()) {
            try {
                Integer createdRequisitionId = requisitionService.createRequisition(requisitionDto);
                String successNotice = "submit".equals(requisitionDto.getAction())
                        ? MSG_SUBMITTED
                        : MSG_DRAFT_SAVED;
                redirectAttributes.addFlashAttribute("successMessage", successNotice);
                return "redirect:/requisitions/" + createdRequisitionId;
            } catch (RequisitionValidationException validationException) {
                bindValidationErrors(bindingResult, validationException);
            } catch (DataIntegrityViolationException integrityException) {
                bindingResult.reject("storage", "Không thể lưu dữ liệu. Vui lòng kiểm tra lại thông tin trùng lặp.");
            }
        }

        return renderForm(model, false, null);
    }

    @PostMapping("/edit/{id}")
    public String handleUpdateRequisition(
            @PathVariable Integer id,
            @ModelAttribute("requisitionDto") RequisitionRequest requisitionDto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (!bindingResult.hasErrors()) {
            try {
                requisitionService.updateRequisition(id, requisitionDto);
                String successNotice = "submit".equals(requisitionDto.getAction())
                        ? MSG_SUBMITTED
                        : MSG_DRAFT_SAVED;
                redirectAttributes.addFlashAttribute("successMessage", successNotice);
                return "redirect:/requisitions/" + id;
            } catch (RequisitionValidationException validationException) {
                bindValidationErrors(bindingResult, validationException);
            } catch (DataIntegrityViolationException integrityException) {
                bindingResult.reject("storage", "Không thể cập nhật dữ liệu. Vui lòng kiểm tra lại thông tin trùng lặp.");
            }
        }

        return renderForm(model, true, id);
    }

    @PostMapping("/delete/{id}")
    public String handleDeleteRequisition(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            requisitionService.deleteRequisition(id);
            redirectAttributes.addFlashAttribute("successMessage", MSG_DELETED);
            return "redirect:/requisitions";
        } catch (RequisitionValidationException validationException) {
            redirectAttributes.addFlashAttribute("failureMessage", validationException.getMessage());
        } catch (DataIntegrityViolationException integrityException) {
            redirectAttributes.addFlashAttribute("failureMessage", "Yêu cầu tuyển dụng này đã liên kết dữ liệu khác và không thể xóa.");
        }
        return "redirect:/requisitions/" + id;
    }

    @PostMapping("/{id}/decision")
    public String handleDecision(
            @PathVariable Integer id,
            @RequestParam String decision,
            @RequestParam(defaultValue = "") String comment,
            RedirectAttributes redirectAttributes) {

        if (!"approve".equals(decision) && !"reject".equals(decision)) {
            redirectAttributes.addFlashAttribute("failureMessage", "Vui lòng chọn quyết định phê duyệt hợp lệ.");
            return "redirect:/requisitions/" + id;
        }

        try {
            requisitionService.decide(id, "approve".equals(decision), comment);
            redirectAttributes.addFlashAttribute("successMessage", MSG_DECISION_SAVED);
        } catch (RequisitionValidationException validationException) {
            redirectAttributes.addFlashAttribute("failureMessage", validationException.getMessage());
            redirectAttributes.addFlashAttribute("decisionComment", comment);
        }

        return "redirect:/requisitions/" + id;
    }

    @PostMapping("/{id}/withdraw")
    public String handleWithdraw(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            requisitionService.withdraw(id);
            redirectAttributes.addFlashAttribute("successMessage", MSG_WITHDRAWN);
        } catch (RequisitionValidationException validationException) {
            redirectAttributes.addFlashAttribute("failureMessage", validationException.getMessage());
        }
        return "redirect:/requisitions/" + id;
    }

    @GetMapping("/{id}")
    public String showRequisitionDetail(@PathVariable Integer id, Model model) {
        model.addAttribute("req", requisitionService.getById(id));
        return "requisitions/detail";
    }

    private String renderForm(Model model, boolean isEdit, Integer requisitionId) {
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("requisitionId", requisitionId);
        populateFormOptions(model);
        return "requisitions/form";
    }

    private void populateFormOptions(Model model) {
        User currentUser = requisitionAccess.actor();
        List<Department> availableDepartments = requisitionService.getAvailableDepartments(currentUser);
        if (availableDepartments == null || availableDepartments.isEmpty()) {
            availableDepartments = departmentRepository.findAll();
        }
        model.addAttribute("departments", availableDepartments != null ? availableDepartments : Collections.emptyList());
        model.addAttribute("managedDepartmentsCount", availableDepartments != null ? availableDepartments.size() : 0);
        model.addAttribute("employmentTypes", RequisitionValidator.EMPLOYMENT_TYPES);
        model.addAttribute("criteriaTypes", RequisitionValidator.CRITERIA_TYPES);
        model.addAttribute("genders", RequisitionValidator.GENDERS);
    }

    private void bindValidationErrors(BindingResult bindingResult, RequisitionValidationException validationException) {
        validationException.getErrors().forEach((field, message) -> bindingResult.rejectValue(field, "invalid", message));
    }
}
