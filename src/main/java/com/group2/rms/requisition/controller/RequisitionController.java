package com.group2.rms.requisition.controller;

import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.util.FilterLinks;
import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.RequisitionSearchRequest;
import com.group2.rms.requisition.exception.RequisitionStateException;
import com.group2.rms.requisition.exception.RequisitionValidationException;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.requisition.service.RequisitionFormOptions;
import com.group2.rms.requisition.service.RequisitionListService;
import com.group2.rms.requisition.service.RequisitionService;
import com.group2.rms.requisition.validator.RequisitionValidator;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.beans.PropertyAccessException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DefaultBindingErrorProcessor;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import java.util.Locale;

@Controller
@RequestMapping("/requisitions")
@RequiredArgsConstructor
public class RequisitionController {
    private static final String LAST_PAGE_PATH = "requisition.lastPage.path";
    private static final String LAST_PAGE_USER = "requisition.lastPage.userId";
    private static final String DECISION_APPROVE = "approve";
    private static final String DECISION_REJECT = "reject";
    private static final String INVALID_ID_MESSAGE = "Mã yêu cầu tuyển dụng không hợp lệ.";
    private static final String STORAGE_ERROR =
            "Không thể lưu dữ liệu. Vui lòng kiểm tra lại giá trị các trường.";

    private final RequisitionService service;
    private final RequisitionListService listService;
    private final RequisitionAccess access;
    private final RequisitionValidator validator;
    private final RequisitionFormOptions formOptions;

    @InitBinder("form")
    // Chuẩn hóa dữ liệu form.
    void binder(WebDataBinder binder) {
        binder.setAllowedFields("action", "title", "departmentId", "originalTitle", "expectedStaffingDate", "numberOfPositions", "employmentType", "minSalary",
                "maxSalary",
                "workLocation", "workModel", "reasonForHiring", "originalRequisitionId",
                "jobDescription", "requirementDetails");

        PropertyEditorSupport salaryEditor = new PropertyEditorSupport() {
            @Override
            // Chuyển mức lương nhập thành số.
            public void setAsText(String text) {
                if (text == null || text.isBlank()) {
                    setValue(null);
                    return;
                }
                String normalized = text.trim();
                if (normalized.matches("\\d{1,3}(\\.\\d{3})+")) {
                    normalized = normalized.replace(".", "");
                }
                try {
                    setValue(new BigDecimal(normalized));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Mức lương không hợp lệ.");
                }
            }

            @Override
            // Định dạng mức lương để hiển thị.
            public String getAsText() {
                BigDecimal value = (BigDecimal) getValue();
                if (value == null) {
                    return "";
                }
                if (value.stripTrailingZeros().scale() <= 0) {
                    return String.format(Locale.US, "%,d", value.toBigInteger()).replace(',', '.');
                }
                return value.toPlainString();
            }
        };
        binder.registerCustomEditor(BigDecimal.class, "minSalary", salaryEditor);
        binder.registerCustomEditor(BigDecimal.class, "maxSalary", salaryEditor);

        // Nhập "1.5" / "abc" / "@#" vào ô số nguyên: báo câu lỗi riêng theo từng trường hợp
        binder.setBindingErrorProcessor(new DefaultBindingErrorProcessor() {
            @Override
            // Báo lỗi nhập số lượng dễ hiểu.
            public void processPropertyAccessException(PropertyAccessException ex, BindingResult bindingResult) {
                String field = ex.getPropertyName();
                String label = "numberOfPositions".equals(field) ? RequisitionValidator.POSITIONS_LABEL : null;
                if (label == null) {
                    super.processPropertyAccessException(ex, bindingResult);
                    return;
                }
                bindingResult.addError(new FieldError(bindingResult.getObjectName(), field, ex.getValue(), true,
                        null, null, RequisitionValidator.integerFormatMessage(label, ex.getValue())));
            }
        });
    }

    @ModelAttribute
    // Đưa thông tin người xem vào model.
    void viewer(Model model) {
        var actor = access.actor();
        model.addAttribute("viewerName", actor.getFullName());
        model.addAttribute("viewerRole", actor.getRole().getRoleName());
        model.addAttribute("canCreate", access.canCreate(actor));
    }

    @GetMapping
    // Hiển thị danh sách yêu cầu.
    public String list(@ModelAttribute("searchRequest") RequisitionSearchRequest searchRequest,
            BindingResult invalidParams, HttpServletRequest request, Model model) {
        model.addAttribute("list", listService.getListPage(searchRequest,
                invalidParams.getFieldErrors().stream().map(FieldError::getField).distinct().toList()));
        rememberPage(request, request.getRequestURI()
                + (request.getQueryString() == null ? "" : "?" + request.getQueryString()));
        model.addAttribute("filterLinks", new FilterLinks(request.getRequestURI(), request.getQueryString()));
        model.addAttribute("activeMenu", "requisitions");
        model.addAttribute("bodyClass", "req-app req-list-page");
        return "requisitions/list";
    }

    @GetMapping("/create")
    // Hiển thị form tạo yêu cầu.
    public String createForm(Model model) {
        var currentUser = access.actor();
        access.requireCreate(currentUser);

        RequisitionRequest form = new RequisitionRequest();

        form.setNumberOfPositions(1);
        form.setEmploymentType("Full-time");
        form.setWorkModel("On-site");
        if (currentUser.getDepartment() != null && !access.isDirector(currentUser)) {
            form.setDepartmentId(currentUser.getDepartment().getDepartmentId());
        }

        model.addAttribute("form", form);
        return renderForm(model, false, null);
    }

    @GetMapping("/copy/{id}")
    // Hiển thị form sao chép yêu cầu.
    public String copy(@PathVariable("id") String idParam, Model model, RedirectAttributes flash,
            HttpServletRequest request) {
        Integer id = parseId(idParam);
        if (id == null) {
            return backWhenNotFound(INVALID_ID_MESSAGE, flash, request);
        }
        try {
            model.addAttribute("form", service.copy(id));
        } catch (RequisitionStateException e) {
            return backToPreviousPage(id, e, flash, request);
        } catch (ResourceNotFoundException e) {
            return backWhenNotFound(e.getMessage(), flash, request);
        }
        model.addAttribute("copyNotice", "Đã sao chép vào biểu mẫu mới. Biểu mẫu chưa được lưu cho đến khi bạn chọn Lưu bản nháp hoặc "
                + (access.isDirector(access.actor()) ? "Phê duyệt trực tiếp." : "Gửi Giám đốc duyệt."));
        return renderForm(model, false, null);
    }

    @GetMapping("/edit/{id}")
    // Hiển thị form sửa yêu cầu.
    public String edit(@PathVariable("id") String idParam, Model model, RedirectAttributes flash,
            HttpServletRequest request) {
        Integer id = parseId(idParam);
        if (id == null) {
            return backWhenNotFound(INVALID_ID_MESSAGE, flash, request);
        }
        try {
            model.addAttribute("form", service.prepareEditForm(id));
        } catch (RequisitionStateException e) {
            return backToPreviousPage(id, e, flash, request);
        } catch (ResourceNotFoundException e) {
            return backWhenNotFound(e.getMessage(), flash, request);
        }
        return renderForm(model, true, id);
    }

    @PostMapping("/create")
    // Tạo yêu cầu tuyển dụng.
    public String create(@ModelAttribute("form") RequisitionRequest form, BindingResult errors, Model model,
            RedirectAttributes flash) {
        access.requireCreate(access.actor());
        if (!errors.hasErrors()) {
            try {
                Integer requisitionId = service.createRequisition(form);
                flash.addFlashAttribute("successMessage", savedMessage(form.getAction()));
                return "redirect:/requisitions/" + requisitionId;
            } catch (RequisitionValidationException e) {
                addErrors(errors, e);
            } catch (DataIntegrityViolationException e) {
                errors.reject("storage", STORAGE_ERROR);
            }
        }
        return renderForm(model, false, null);
    }

    @PostMapping("/edit/{id}")
    // Lưu thay đổi của yêu cầu.
    public String update(@PathVariable Integer id, @ModelAttribute("form") RequisitionRequest form,
            BindingResult errors, Model model, RedirectAttributes flash, HttpServletRequest request) {
        if (!errors.hasErrors()) {
            try {
                service.updateRequisition(id, form);
                flash.addFlashAttribute("successMessage", savedMessage(form.getAction()));
                return "redirect:/requisitions/" + id;
            } catch (RequisitionStateException e) {
                return backToPreviousPage(id, e, flash, request);
            } catch (RequisitionValidationException e) {
                addErrors(errors, e);
            } catch (DataIntegrityViolationException e) {
                errors.reject("storage", STORAGE_ERROR);
            }
        }
        return renderForm(model, true, id);
    }

    @PostMapping("/delete/{id}")
    // Xóa yêu cầu đủ điều kiện.
    public String delete(@PathVariable Integer id, RedirectAttributes flash, HttpServletRequest request) {
        try {
            service.deleteRequisition(id);
            flash.addFlashAttribute("successMessage", "Đã xóa yêu cầu tuyển dụng thành công.");
            return "redirect:/requisitions";
        } catch (RequisitionStateException e) {
            return backToPreviousPage(id, e, flash, request);
        } catch (RequisitionValidationException e) {
            flash.addFlashAttribute("failureMessage", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            flash.addFlashAttribute("failureMessage", "Yêu cầu này đã được liên kết với dữ liệu khác và không thể xóa.");
        }
        return "redirect:/requisitions/" + id;
    }

    @PostMapping("/{id}/decision")
    // Xử lý quyết định phê duyệt.
    public String decide(@PathVariable Integer id, @RequestParam(defaultValue = "") String decision,
            @RequestParam(defaultValue = "") String comment, RedirectAttributes flash, HttpServletRequest request) {
        boolean isApprove = DECISION_APPROVE.equals(decision);
        boolean isReject = DECISION_REJECT.equals(decision);
        if (!isApprove && !isReject) {
            flash.addFlashAttribute("failureMessage", "Vui lòng chọn quyết định phê duyệt.");
            return "redirect:/requisitions/" + id;
        }
        try {
            service.decide(id, isApprove, comment);
            flash.addFlashAttribute("successMessage", isReject
                    ? "Đã từ chối yêu cầu tuyển dụng."
                    : "Đã lưu quyết định thành công.");
            if (isReject) {
                flash.addFlashAttribute("decisionRejectedToast", true);
            }
        } catch (RequisitionStateException e) {
            return backToPreviousPage(id, e, flash, request);
        } catch (RequisitionValidationException e) {
            flash.addFlashAttribute("failureMessage", e.getMessage());
            flash.addFlashAttribute("decisionComment", comment);
        }
        return "redirect:/requisitions/" + id;
    }

    @PostMapping("/{id}/close")
    // Đóng yêu cầu tuyển dụng.
    public String close(@PathVariable Integer id, RedirectAttributes flash, HttpServletRequest request) {
        try {
            service.closeRequisition(id);
            flash.addFlashAttribute("successMessage", "Đã đóng yêu cầu tuyển dụng.");
        } catch (RequisitionStateException e) {
            return backToPreviousPage(id, e, flash, request);
        } catch (RequisitionValidationException e) {
            flash.addFlashAttribute("failureMessage", e.getMessage());
        }
        return "redirect:/requisitions/" + id;
    }

    // Ghi nhớ trang danh sách hiện tại.
    private void rememberPage(HttpServletRequest request, String path) {
        var session = request.getSession();
        session.setAttribute(LAST_PAGE_PATH, path);
        session.setAttribute(LAST_PAGE_USER, access.actor().getUserId());
    }

    // Lấy trang danh sách đã ghi nhớ.
    private String rememberedPage(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session == null || !access.actor().getUserId().equals(session.getAttribute(LAST_PAGE_USER))) {
            return null;
        }
        Object path = session.getAttribute(LAST_PAGE_PATH);
        if (path instanceof String previous) {
            return previous;
        }
        return null;
    }

    // Kiểm tra đường dẫn trang danh sách.
    private boolean isListPage(String path) {
        return path.equals("/requisitions") || path.startsWith("/requisitions?");
    }

    // Quay lại trang trước khi thao tác bị chặn.
    private String backToPreviousPage(Integer id, RequisitionStateException e, RedirectAttributes flash,
            HttpServletRequest request) {
        flash.addFlashAttribute("failureMessage", e.getMessage());
        String previous = rememberedPage(request);
        if (previous != null && (isListPage(previous) || previous.equals("/requisitions/" + id))) {
            return "redirect:" + previous;
        }
        return "redirect:/requisitions/" + id;
    }

    @ExceptionHandler(AccessDeniedException.class)
    // Xử lý lỗi thiếu quyền truy cập.
    public String whenAccessDenied(AccessDeniedException e, RedirectAttributes flash, HttpServletRequest request) {
        if (isListPage(request.getRequestURI())) {
            flash.addFlashAttribute("failureMessage", e.getMessage());
            return "redirect:/dashboard";
        }
        return backWhenNotFound(e.getMessage(), flash, request);
    }

    // Quay lại danh sách khi không tìm thấy yêu cầu.
    private String backWhenNotFound(String message, RedirectAttributes flash, HttpServletRequest request) {
        flash.addFlashAttribute("failureMessage", message);
        String previous = rememberedPage(request);
        if (previous != null && !previous.equals(request.getRequestURI())) {
            return "redirect:" + previous;
        }
        return "redirect:/requisitions";
    }

    // Chuyển mã yêu cầu thành số.
    private static Integer parseId(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("\\D", "");
        if (digits.isEmpty() || digits.length() > 9) {
            return null;
        }
        return Integer.valueOf(digits);
    }

    @GetMapping("/{id}")
    // Hiển thị chi tiết yêu cầu.
    public String detail(@PathVariable("id") String idParam, Model model, RedirectAttributes flash,
            HttpServletRequest request) {
        Integer id = parseId(idParam);
        if (id == null) {
            return backWhenNotFound(INVALID_ID_MESSAGE, flash, request);
        }
        try {
            model.addAttribute("req", service.getById(id));
        } catch (ResourceNotFoundException e) {
            return backWhenNotFound(e.getMessage(), flash, request);
        }
        rememberPage(request, "/requisitions/" + id);
        model.addAttribute("activeMenu", "requisitions");
        model.addAttribute("bodyClass", "req-app req-detail-page");
        model.addAttribute("commentLimit", RequisitionValidator.COMMENT_CHAR_LIMIT);
        return "requisitions/detail";
    }

    // Tạo thông báo sau khi lưu.
    private String savedMessage(String action) {
        if (!RequisitionValidator.isSubmit(action)) {
            return "Đã lưu bản nháp thành công.";
        }
        return access.isDirector(access.actor())
                ? "Yêu cầu đã được phê duyệt. HR có thể tạo tin tuyển dụng ngay."
                : "Yêu cầu đã được gửi lên Giám đốc phê duyệt.";
    }

    // Chuẩn bị và hiển thị form yêu cầu.
    private String renderForm(Model model, boolean edit, Integer id) {
        model.addAttribute("isEdit", edit);
        model.addAttribute("requisitionId", id);
        model.addAttribute("today", validator.today());
        model.addAttribute("directApprove", access.isDirector(access.actor()));
        addFormOptions(model);
        model.addAttribute("activeMenu", "requisitions");
        model.addAttribute("bodyClass", "req-app req-form-page");
        return "requisitions/form";
    }

    // Thêm các lựa chọn cho form.
    private void addFormOptions(Model model) {
        var allDepartments = formOptions.allDepartments();
        var actor = access.actor();
        model.addAttribute("departments", allDepartments);

        model.addAttribute("departmentChoices", allDepartments.stream()
                .filter(department -> "Active".equals(department.getDepartmentStatus())
                        && access.canManageDepartment(actor, department.getDepartmentId(), department.getManager()))
                .toList());
        model.addAttribute("employmentTypes", RequisitionValidator.EMPLOYMENT_TYPES);
        model.addAttribute("positionsMin", RequisitionValidator.POSITIONS_MIN);
    }

    // Đưa lỗi kiểm tra vào form.
    private void addErrors(BindingResult errors, RequisitionValidationException e) {
        e.getErrors().forEach((field, message) -> errors.rejectValue(field, "invalid", message));
    }
}
