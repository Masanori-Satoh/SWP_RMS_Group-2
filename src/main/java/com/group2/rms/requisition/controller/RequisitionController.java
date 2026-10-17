package com.group2.rms.requisition.controller;

import com.group2.rms.requisition.dto.RequisitionRequest;
import com.group2.rms.requisition.dto.RequisitionSearchRequest;
import com.group2.rms.requisition.dto.ScreeningCriteriaRequest;
import com.group2.rms.requisition.exception.RequisitionValidationException;
import com.group2.rms.requisition.service.RequisitionAccess;
import com.group2.rms.requisition.service.RequisitionService;
import com.group2.rms.requisition.validator.RequisitionValidator;
import com.group2.rms.user.repository.DepartmentRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/requisitions")
@RequiredArgsConstructor
public class RequisitionController {
    private final RequisitionService service;
    private final RequisitionAccess access;
    private final DepartmentRepository departments;

    @InitBinder("requisitionDto")
    void binder(WebDataBinder binder) {
        binder.setAutoGrowCollectionLimit(50);
        // Không có departmentId: phòng ban luôn do service gán (phòng ban của người tạo), không nhận từ form
        binder.setAllowedFields("action", "title", "numberOfPositions", "employmentType", "minSalary",
                "maxSalary",
                "gender", "workLocation", "workModel", "probationDays", "expectedStartDate", "reasonForHiring", "hiringType", "originalRequisitionId",
                "jobDescription", "requirementDetails",
                "screeningCriteria[*].criteriaId", "screeningCriteria[*].criteriaName",
                "screeningCriteria[*].criteriaType",
                "screeningCriteria[*].requiredValue", "screeningCriteria[*].weight",
                "screeningCriteria[*].isMandatory");

        // Ô lương hiển thị kiểu Việt Nam "20.000.000" (dấu chấm ngăn cách hàng nghìn).
        // Đọc: "20.000.000" → 20000000; số viết liền ("20000000", ví dụ gửi từ Postman) vẫn đọc bình thường.
        // Ghi (khi render lại form): 20000000 → "20.000.000".
        java.beans.PropertyEditorSupport salaryEditor = new java.beans.PropertyEditorSupport() {
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
                if (value == null) return "";
                if (value.stripTrailingZeros().scale() <= 0) {
                    return String.format(java.util.Locale.US, "%,d", value.toBigInteger()).replace(',', '.');
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

    /**
     * Màn danh sách. Mọi tham số trên URL (?q=...&status=...&page=...) được Spring gom vào RequisitionSearchRequest.
     * Controller không xử lý gì thêm: service lo phân quyền, làm sạch bộ lọc và truy vấn.
     *
     * BindingResult được khai báo để giá trị sai định dạng (ví dụ ?page=abc) chỉ bị bỏ qua (thành null)
     * thay vì làm lỗi cả trang.
     */
    @GetMapping
    public String list(@ModelAttribute("searchRequest") RequisitionSearchRequest searchRequest,
            BindingResult invalidParams, HttpServletRequest request, Model model) {
        model.addAttribute("list", service.getListPage(searchRequest));
        // Tạo link "bỏ một bộ lọc" cho các chip, giữ nguyên các tham số khác đang có trên URL
        model.addAttribute("filterLinks", new RequisitionListLinks(request.getRequestURI(), request.getQueryString()));
        // Trang đã dùng layout/internal: activeMenu làm sáng mục menu ở sidebar,
        // bodyClass gắn lên <body> để CSS cũ của trang (.req-app, .req-list-page) vẫn áp dụng
        model.addAttribute("activeMenu", "requisitions");
        model.addAttribute("bodyClass", "req-app req-list-page");
        return "requisitions/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        var currentUser = access.actor();
        // Rule: Chỉ người dùng có quyền (Hiring Manager / Admin) mới được tạo yêu cầu
        access.requireCreate(currentUser);

        RequisitionRequest requisitionRequest = new RequisitionRequest();
        // Giá trị chọn sẵn cho form tạo mới (đúng wireframe). Các cột này NOT NULL trong DB nên luôn cần có giá trị.
        requisitionRequest.setNumberOfPositions(1);
        requisitionRequest.setRecruitmentRound(1);
        requisitionRequest.setEmploymentType("Full-time");
        requisitionRequest.setWorkModel("On-site");
        requisitionRequest.setGender(RequisitionValidator.GENDER_ANY);
        if (currentUser.getDepartment() != null) {
            requisitionRequest.setDepartmentId(currentUser.getDepartment().getDepartmentId());
        }
        requisitionRequest.getScreeningCriteria()
                .add(ScreeningCriteriaRequest.builder().weight(new BigDecimal("100")).isMandatory(false).build());

        model.addAttribute("requisitionDto", requisitionRequest);
        return form(model, false, null);
    }

    @GetMapping("/copy/{id}")
    public String copy(@PathVariable Integer id, Model model) {
        model.addAttribute("requisitionDto", service.copy(id));
        model.addAttribute("copyNotice",
                "Đã sao chép vào biểu mẫu mới. Biểu mẫu chưa được lưu cho đến khi bạn chọn Lưu bản nháp hoặc Gửi Giám đốc duyệt.");
        return form(model, false, null);
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {
        model.addAttribute("requisitionDto", service.getRequestDtoById(id));
        return form(model, true, id);
    }

    @PostMapping("/create")
    public String create(@ModelAttribute("requisitionDto") RequisitionRequest requisitionRequest, BindingResult errors, Model model,
            RedirectAttributes flash) {
        // Rule: Xác thực quyền tạo trước khi xử lý nghiệp vụ
        access.requireCreate(access.actor());
        if (!errors.hasErrors()) {
            try {
                Integer requisitionId = service.createRequisition(requisitionRequest);
                // Rule: Phân biệt thông báo khi lưu bản nháp hoặc nộp phê duyệt
                boolean isSubmitted = "submit".equals(requisitionRequest.getAction());
                flash.addFlashAttribute("successMessage",
                        isSubmitted ? "Yêu cầu đã được gửi lên Giám đốc phê duyệt." : "Đã lưu bản nháp thành công.");
                return "redirect:/requisitions/" + requisitionId;
            } catch (RequisitionValidationException e) {
                addErrors(errors, e);
            } catch (DataIntegrityViolationException e) {
                errors.reject("storage", "Không thể lưu dữ liệu. Vui lòng kiểm tra lại tính trùng lặp tiêu chí và giá trị các trường.");
            }
        }
        return form(model, false, null);
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable Integer id, @ModelAttribute("requisitionDto") RequisitionRequest requisitionRequest,
            BindingResult errors, Model model, RedirectAttributes flash) {
        if (!errors.hasErrors()) {
            try {
                service.updateRequisition(id, requisitionRequest);
                boolean isSubmitted = "submit".equals(requisitionRequest.getAction());
                flash.addFlashAttribute("successMessage",
                        isSubmitted ? "Yêu cầu đã được gửi lên Giám đốc phê duyệt." : "Đã lưu bản nháp thành công.");
                return "redirect:/requisitions/" + id;
            } catch (RequisitionValidationException e) {
                addErrors(errors, e);
            } catch (DataIntegrityViolationException e) {
                errors.reject("storage", "Không thể lưu dữ liệu. Vui lòng kiểm tra lại tính trùng lặp tiêu chí và giá trị các trường.");
            }
        }
        return form(model, true, id);
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            service.deleteRequisition(id);
            flash.addFlashAttribute("successMessage", "Đã xóa yêu cầu tuyển dụng thành công.");
            return "redirect:/requisitions";
        } catch (RequisitionValidationException e) {
            flash.addFlashAttribute("failureMessage", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            flash.addFlashAttribute("failureMessage", "Yêu cầu này đã được liên kết với dữ liệu khác và không thể xóa.");
        }
        return "redirect:/requisitions/" + id;
    }

    @PostMapping("/{id}/decision")
    public String decide(@PathVariable Integer id, @RequestParam String decision,
            @RequestParam(defaultValue = "") String comment, RedirectAttributes flash) {
        if (!"approve".equals(decision) && !"reject".equals(decision))
            flash.addFlashAttribute("failureMessage", "Vui lòng chọn quyết định phê duyệt.");
        else
            try {
                service.decide(id, "approve".equals(decision), comment);
                flash.addFlashAttribute("successMessage", "Đã lưu quyết định thành công.");
            } catch (RequisitionValidationException e) {
                flash.addFlashAttribute("failureMessage", e.getMessage());
                flash.addFlashAttribute("decisionComment", comment);
            }
        return "redirect:/requisitions/" + id;
    }

    @PostMapping("/{id}/withdraw")
    public String withdraw(@PathVariable Integer id, RedirectAttributes flash) {
        try {
            service.withdraw(id);
            flash.addFlashAttribute("successMessage", "Đã rút lại yêu cầu thành công. Bạn có thể chỉnh sửa lại bản nháp.");
        } catch (RequisitionValidationException e) {
            flash.addFlashAttribute("failureMessage", e.getMessage());
        }
        return "redirect:/requisitions/" + id;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        // getById kiểm tra quyền xem (RequisitionAccess.requireView) và tính sẵn mọi nhãn / cờ quyền
        model.addAttribute("req", service.getById(id));
        model.addAttribute("activeMenu", "requisitions");
        model.addAttribute("bodyClass", "req-app req-detail-page");
        return "requisitions/detail";
    }

    /** Mọi đường hiển thị form (tạo, sửa, sao chép, render lại khi có lỗi) đều đi qua hàm này. */
    private String form(Model model, boolean edit, Integer id) {
        model.addAttribute("isEdit", edit);
        model.addAttribute("requisitionId", id);
        options(model);
        // Trang dùng layout/internal: làm sáng menu + gắn class để CSS của trang áp dụng
        model.addAttribute("activeMenu", "requisitions");
        model.addAttribute("bodyClass", "req-app req-form-page");
        return "requisitions/form";
    }

    private void options(Model model) {
        model.addAttribute("departments", departments.findAll());
        model.addAttribute("employmentTypes", RequisitionValidator.EMPLOYMENT_TYPES);
        model.addAttribute("criteriaTypes", RequisitionValidator.CRITERIA_TYPES);
        model.addAttribute("genders", RequisitionValidator.GENDERS);
        // Giới hạn thử việc lấy từ hằng số của validator, để form và JS không phải viết lại số 1 / 180
        model.addAttribute("probationMinDays", RequisitionValidator.PROBATION_MIN_DAYS);
        model.addAttribute("probationMaxDays", RequisitionValidator.PROBATION_MAX_DAYS);
        // Giới hạn số lượng cần tuyển, dùng cho min/max của ô số lượng và nút −/+
        model.addAttribute("positionsMin", RequisitionValidator.POSITIONS_MIN);
        model.addAttribute("positionsMax", RequisitionValidator.POSITIONS_MAX);
        // Khoảng ngày bắt đầu được chọn: từ hôm nay tới hôm nay + START_DATE_MAX_YEARS năm
        model.addAttribute("startDateMin", LocalDate.now());
        model.addAttribute("startDateMax", LocalDate.now().plusYears(RequisitionValidator.START_DATE_MAX_YEARS));
    }

    private void addErrors(BindingResult errors, RequisitionValidationException e) {
        e.getErrors().forEach((k, v) -> errors.rejectValue(k, "invalid", v));
    }
}
