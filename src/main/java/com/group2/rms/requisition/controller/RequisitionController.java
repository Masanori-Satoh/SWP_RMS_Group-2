package com.group2.rms.requisition.controller;

import com.group2.rms.core.exception.ResourceNotFoundException;
import com.group2.rms.core.util.FilterLinks;
import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.RequisitionSearchRequest;
import com.group2.rms.requisition.exception.RequisitionStateException;
import com.group2.rms.requisition.exception.RequisitionValidationException;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.requisition.service.RequisitionListService;
import com.group2.rms.requisition.service.RequisitionService;
import com.group2.rms.requisition.validator.RequisitionValidator;
import com.group2.rms.user.repository.DepartmentRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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

/*
 * CONTROLLER: màn /requisitions (danh sách, form tạo / sửa / sao chép, chi tiết, gửi duyệt, duyệt, rút, xóa); nghiệp vụ ở RequisitionService.
 */
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
    private final DepartmentRepository departments;

    @InitBinder("form")
    void binder(WebDataBinder binder) {
        // departmentId chỉ có tác dụng với Giám đốc; originalTitle chỉ để form hiện số đợt
        binder.setAllowedFields("action", "title", "departmentId", "originalTitle", "numberOfPositions", "employmentType", "minSalary",
                "maxSalary",
                "gender", "workLocation", "workModel", "probationPeriod", "reasonForHiring", "recruitmentRound", "originalRequisitionId",
                "jobDescription", "requirementDetails");

        // Ô lương hiển thị kiểu Việt Nam "20.000.000"
        PropertyEditorSupport salaryEditor = new PropertyEditorSupport() {
            @Override
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
    }

    @ModelAttribute
    void viewer(Model model) {
        var actor = access.actor();
        model.addAttribute("viewerName", actor.getFullName());
        model.addAttribute("viewerRole", actor.getRole().getRoleName());
        model.addAttribute("canCreate", access.canCreate(actor));
    }

    //màn danh sách
    @GetMapping
    public String list(@ModelAttribute("searchRequest") RequisitionSearchRequest searchRequest,
            BindingResult invalidParams, HttpServletRequest request, Model model) {
        model.addAttribute("list", listService.getListPage(searchRequest));
        rememberPage(request, request.getRequestURI()
                + (request.getQueryString() == null ? "" : "?" + request.getQueryString()));
        // Tạo link "bỏ một bộ lọc" cho các chip, giữ nguyên các tham số khác đang có trên URL
        model.addAttribute("filterLinks", new FilterLinks(request.getRequestURI(), request.getQueryString()));
        // activeMenu làm sáng menu sidebar, bodyClass giữ CSS cũ của trang
        model.addAttribute("activeMenu", "requisitions");
        model.addAttribute("bodyClass", "req-app req-list-page");
        return "requisitions/list";
    }

    // tạo requisition
    @GetMapping("/create")
    public String createForm(Model model) {
        var currentUser = access.actor();
        // Rule: Chỉ Hiring Manager / Director mới được tạo yêu cầu
        access.requireCreate(currentUser);

        RequisitionRequest form = new RequisitionRequest();

        // các trường bắt buộc trong db
        form.setNumberOfPositions(1);
        form.setRecruitmentRound(1);
        form.setEmploymentType("Full-time");
        form.setWorkModel("On-site");
        form.setGender(RequisitionValidator.GENDER_ANY);
        // Giám đốc tự chọn phòng ban trên form nên không điền sẵn
        if (currentUser.getDepartment() != null && !access.isDirector(currentUser)) {
            form.setDepartmentId(currentUser.getDepartment().getDepartmentId());
        }

        model.addAttribute("form", form);
        return renderForm(model, false, null);
    }

    // id nhận dạng chuỗi để URL gõ tay như "{2796}" vẫn hiểu là 2796 (không ra lỗi 500)
    @GetMapping("/copy/{id}")
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
    public String create(@Valid @ModelAttribute("form") RequisitionRequest form, BindingResult errors, Model model,
            RedirectAttributes flash) {
        // Rule: Xác thực quyền tạo trước khi xử lý nghiệp vụ
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
    public String update(@PathVariable Integer id, @Valid @ModelAttribute("form") RequisitionRequest form,
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
    public String decide(@PathVariable Integer id, @RequestParam String decision,
            @RequestParam(defaultValue = "") String comment, RedirectAttributes flash, HttpServletRequest request) {
        // Rule: Hai nút trên trang chi tiết gửi decision=approve hoặc decision=reject, giá trị khác thì bỏ qua
        boolean isApprove = DECISION_APPROVE.equals(decision);
        boolean isReject = DECISION_REJECT.equals(decision);
        if (!isApprove && !isReject) {
            flash.addFlashAttribute("failureMessage", "Vui lòng chọn quyết định phê duyệt.");
            return "redirect:/requisitions/" + id;
        }
        try {
            service.decide(id, isApprove, comment);
            flash.addFlashAttribute("successMessage", "Đã lưu quyết định thành công.");
        } catch (RequisitionStateException e) {
            return backToPreviousPage(id, e, flash, request);
        } catch (RequisitionValidationException e) {
            flash.addFlashAttribute("failureMessage", e.getMessage());
            // Giữ lại ý kiến đã gõ để người dùng không phải gõ lại
            flash.addFlashAttribute("decisionComment", comment);
        }
        return "redirect:/requisitions/" + id;
    }

    @PostMapping("/{id}/withdraw")
    public String withdraw(@PathVariable Integer id, RedirectAttributes flash, HttpServletRequest request) {
        try {
            service.withdraw(id);
            flash.addFlashAttribute("successMessage", "Đã rút lại yêu cầu thành công. Bạn có thể chỉnh sửa lại bản nháp.");
            // Rút lại là để sửa → mở thẳng form sửa
            return "redirect:/requisitions/edit/" + id;
        } catch (RequisitionStateException e) {
            return backToPreviousPage(id, e, flash, request);
        } catch (RequisitionValidationException e) {
            flash.addFlashAttribute("failureMessage", e.getMessage());
        }
        return "redirect:/requisitions/" + id;
    }

    // Chỉ nhớ trang danh sách/chi tiết đã render thành công cho đúng tài khoản trong phiên.
    private void rememberPage(HttpServletRequest request, String path) {
        var session = request.getSession();
        session.setAttribute(LAST_PAGE_PATH, path);
        session.setAttribute(LAST_PAGE_USER, access.actor().getUserId());
    }

    // Trang danh sách / chi tiết đã xem gần nhất của đúng tài khoản này; chưa có thì trả null.
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

    private boolean isListPage(String path) {
        return path.equals("/requisitions") || path.startsWith("/requisitions?");
    }

    // Gõ URL sai trạng thái hoặc gửi form cũ: trở lại trang đã xem, kèm toast lỗi.
    private String backToPreviousPage(Integer id, RequisitionStateException e, RedirectAttributes flash,
            HttpServletRequest request) {
        flash.addFlashAttribute("failureMessage", e.getMessage());
        String previous = rememberedPage(request);
        if (previous != null && (isListPage(previous) || previous.equals("/requisitions/" + id))) {
            return "redirect:" + previous;
        }
        return "redirect:/requisitions/" + id;
    }

    // Gõ URL không đủ quyền (HM khác sửa nháp, HR mở yêu cầu chưa duyệt...): toast lỗi thay vì trang 403.
    @ExceptionHandler(AccessDeniedException.class)
    public String whenAccessDenied(AccessDeniedException e, RedirectAttributes flash, HttpServletRequest request) {
        if (isListPage(request.getRequestURI())) {
            // Chính danh sách bị chặn thì về dashboard, tránh chuyển trang lặp vô hạn
            flash.addFlashAttribute("failureMessage", e.getMessage());
            return "redirect:/dashboard";
        }
        return backWhenNotFound(e.getMessage(), flash, request);
    }

    // Mã sai, không tồn tại hoặc không đủ quyền: trở lại trang đã xem (tránh lặp nếu chính trang đó bị chặn), không thì về danh sách.
    private String backWhenNotFound(String message, RedirectAttributes flash, HttpServletRequest request) {
        flash.addFlashAttribute("failureMessage", message);
        String previous = rememberedPage(request);
        if (previous != null && !previous.equals(request.getRequestURI())) {
            return "redirect:" + previous;
        }
        return "redirect:/requisitions";
    }

    // Lấy phần số của mã gõ trên URL (vd "{2796}" -> 2796); không có số thì trả null.
    private static Integer parseId(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("\\D", "");
        if (digits.isEmpty() || digits.length() > 9) {
            return null;
        }
        return Integer.valueOf(digits);
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") String idParam, Model model, RedirectAttributes flash,
            HttpServletRequest request) {
        Integer id = parseId(idParam);
        if (id == null) {
            return backWhenNotFound(INVALID_ID_MESSAGE, flash, request);
        }
        try {
            // getById kiểm tra quyền xem (RequisitionAccess.requireView) và tính sẵn mọi nhãn / cờ quyền
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

    // Rule: Phân biệt thông báo khi lưu bản nháp, gửi duyệt, hoặc Giám đốc phê duyệt trực tiếp
    private String savedMessage(String action) {
        if (!RequisitionValidator.isSubmit(action)) {
            return "Đã lưu bản nháp thành công.";
        }
        return access.isDirector(access.actor())
                ? "Yêu cầu đã được phê duyệt. HR có thể tạo tin tuyển dụng ngay."
                : "Yêu cầu đã được gửi lên Giám đốc phê duyệt.";
    }

    /** Mọi đường hiển thị form (tạo, sửa, sao chép, render lại khi có lỗi) đều đi qua hàm này. */
    private String renderForm(Model model, boolean edit, Integer id) {
        model.addAttribute("isEdit", edit);
        model.addAttribute("requisitionId", id);
        // Giám đốc: tự chọn phòng ban và nút gửi là "Phê duyệt trực tiếp"
        model.addAttribute("directApprove", access.isDirector(access.actor()));
        addFormOptions(model);
        // Trang dùng layout/internal: làm sáng menu + gắn class để CSS của trang áp dụng
        model.addAttribute("activeMenu", "requisitions");
        model.addAttribute("bodyClass", "req-app req-form-page");
        return "requisitions/form";
    }

    private void addFormOptions(Model model) {
        model.addAttribute("departments", departments.findAll());
        model.addAttribute("employmentTypes", RequisitionValidator.EMPLOYMENT_TYPES);
        model.addAttribute("genders", RequisitionValidator.GENDERS);
        model.addAttribute("probationLimit", RequisitionValidator.PROBATION_CHAR_LIMIT);
        // Giới hạn số lượng cần tuyển, dùng cho min/max của ô số lượng và nút −/+
        model.addAttribute("positionsMin", RequisitionValidator.POSITIONS_MIN);
        model.addAttribute("positionsMax", RequisitionValidator.POSITIONS_MAX);
        model.addAttribute("roundMin", RequisitionValidator.ROUND_MIN);
        model.addAttribute("roundMax", RequisitionValidator.ROUND_MAX);
    }

    private void addErrors(BindingResult errors, RequisitionValidationException e) {
        e.getErrors().forEach((field, message) -> errors.rejectValue(field, "invalid", message));
    }
}
