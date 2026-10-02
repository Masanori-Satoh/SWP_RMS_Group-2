package com.group2.rms.interview.controller;

import com.group2.rms.candidate.Application;
import com.group2.rms.candidate.ApplicationRepository;
import com.group2.rms.core.security.RoleAuthorities;
import com.group2.rms.interview.dto.InterviewScheduleRequest;
import com.group2.rms.interview.dto.InterviewScheduleResponse;
import com.group2.rms.interview.dto.PanelMemberResponse;
import com.group2.rms.interview.entity.*;
import com.group2.rms.interview.exception.InterviewStatusException;
import com.group2.rms.interview.service.InterviewSchedulingService;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring Web MVC Controller điều hướng và xử lý nghiệp vụ Lên lịch phỏng vấn (Interview Scheduling - Iteration 1).
 * Tuân thủ nghiêm ngặt các nguyên tắc:
 * - Package-by-Feature: com.group2.rms.interview
 * - Phân quyền hiển thị (GBR-05): HR thấy toàn bộ lịch và các nút Create/Edit/Cancel; Interviewer chỉ xem lịch được phân công.
 * - Validation & Exception handling (MSG26 & GBR-01): Bắt buộc dùng BindingResult, không để crash HTTP 500.
 * - HR Isolation: HR không được phân công vào hội đồng phỏng vấn.
 * - Không chứa trường interviewRound (đã loại bỏ).
 */
@Controller
@RequestMapping("/interviews")
@RequiredArgsConstructor
@Slf4j
public class InterviewSchedulingController {

    private final InterviewSchedulingService interviewSchedulingService;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    /**
     * Màn hình danh sách lịch phỏng vấn dùng chung (Rule GBR-05).
     * Phân quyền:
     * - HR: Xem tất cả lịch, hiển thị các nút thao tác Tạo mới, Chỉnh sửa, Hủy.
     * - Interviewer / Hiring Manager: Chỉ xem lịch mình được gán vào hội đồng, ẩn toàn bộ nút thao tác.
     */
    @GetMapping
    public String listInterviews(Authentication authentication, Model model) {
        User currentUser = resolveEffectiveUser(authentication);
        if (currentUser != null && currentUser.getRole() != null && "Candidate".equalsIgnoreCase(currentUser.getRole().getRoleName())) {
            log.warn("Ứng viên (ID: {}) không có quyền truy cập trang quản lý lịch phỏng vấn nội bộ.", currentUser.getUserId());
            return "redirect:/dashboard";
        }
        boolean isHr = isHrUser(currentUser, authentication);

        List<InterviewScheduleResponse> schedules;
        if (isHr) {
            log.info("Người dùng HR (ID: {}) đang xem toàn bộ danh sách lịch phỏng vấn.",
                    currentUser != null ? currentUser.getUserId() : "N/A");
            schedules = interviewSchedulingService.getAllForHR();
        } else {
            Integer interviewerId = currentUser != null ? currentUser.getUserId() : 0;
            log.info("Interviewer (ID: {}) đang xem các lịch phỏng vấn được phân công theo Rule GBR-05.", interviewerId);
            schedules = interviewSchedulingService.getMySchedulesForInterviewer(interviewerId);
        }

        model.addAttribute("schedules", schedules);
        model.addAttribute("isHr", isHr);
        model.addAttribute("currentUser", currentUser);
        return "interview/list";
    }

    /**
     * Hiển thị form tạo mới lịch phỏng vấn (Chỉ dành cho HR).
     */
    @GetMapping("/new")
    public String showCreateForm(Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        User currentUser = resolveEffectiveUser(authentication);
        if (!isHrUser(currentUser, authentication)) {
            log.warn("Từ chối truy cập tạo lịch phỏng vấn: Người dùng không có vai trò HR.");
            redirectAttributes.addFlashAttribute("errorMessage", "Chỉ có nhân sự (HR) mới có quyền tạo lịch phỏng vấn.");
            return "redirect:/interviews";
        }

        InterviewScheduleRequest request = new InterviewScheduleRequest();
        request.setInterviewStatus(InterviewStatus.Scheduled);
        request.setInterviewFormat(InterviewFormat.Online_GoogleMeet);

        // Gợi ý mặc định: 09:00 sáng ngày mai
        LocalDateTime defaultStartTime = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        request.setStartTime(defaultStartTime);
        request.setEndTime(defaultStartTime.plusHours(1));

        prepareFormModel(model, false, null);
        model.addAttribute("scheduleRequest", request);
        return "interview/form";
    }

    /**
     * Xử lý tạo mới lịch phỏng vấn (Dành cho HR).
     * Bắt buộc dùng BindingResult: nếu có lỗi form hoặc lỗi nghiệp vụ, return "interview/form",
     * không văng lỗi 500, giữ nguyên dữ liệu user đã nhập.
     */
    @PostMapping("/new")
    public String createSchedule(
            @Valid @ModelAttribute("scheduleRequest") InterviewScheduleRequest request,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        User currentUser = resolveEffectiveUser(authentication);
        if (!isHrUser(currentUser, authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chỉ có nhân sự (HR) mới có quyền tạo lịch phỏng vấn.");
            return "redirect:/interviews";
        }

        // 1. Kiểm tra validation từ annotation (@NotNull, @Future, MSG26 @AssertTrue, @NotEmpty)
        if (bindingResult.hasErrors()) {
            log.warn("Tạo lịch phỏng vấn thất bại do dữ liệu không hợp lệ: {} lỗi", bindingResult.getErrorCount());
            prepareFormModel(model, false, null);
            return "interview/form";
        }

        // 2. Gọi Service và bắt các ngoại lệ nghiệp vụ (Rule MSG26, HR Isolation, v.v.)
        try {
            Integer hrUserId = currentUser != null ? currentUser.getUserId() : 1;
            interviewSchedulingService.createSchedule(request, hrUserId);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo lịch phỏng vấn thành công!");
            return "redirect:/interviews";
        } catch (InterviewStatusException | IllegalArgumentException ex) {
            log.warn("Ngoại lệ nghiệp vụ khi tạo lịch phỏng vấn: {}", ex.getMessage());
            bindingResult.reject("businessError", ex.getMessage());
            prepareFormModel(model, false, null);
            return "interview/form";
        }
    }

    /**
     * Hiển thị form chỉnh sửa lịch phỏng vấn (Chỉ dành cho HR).
     */
    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable("id") Long id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        User currentUser = resolveEffectiveUser(authentication);
        if (!isHrUser(currentUser, authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chỉ có nhân sự (HR) mới có quyền chỉnh sửa lịch phỏng vấn.");
            return "redirect:/interviews";
        }

        InterviewScheduleResponse detail = interviewSchedulingService.getScheduleDetail(id);
        if (detail == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy lịch phỏng vấn với mã: " + id);
            return "redirect:/interviews";
        }

        // Điền dữ liệu hiện tại vào DTO form
        InterviewScheduleRequest request = new InterviewScheduleRequest();
        request.setApplicationId(detail.applicationId());
        request.setInterviewFormat(detail.interviewFormat());
        request.setStartTime(detail.startTime());
        request.setEndTime(detail.endTime());
        request.setLocationOrLink(detail.locationOrLink());
        request.setInterviewStatus(detail.interviewStatus());

        if (detail.interviewers() != null) {
            Set<Integer> interviewerIds = detail.interviewers().stream()
                    .map(PanelMemberResponse::interviewerId)
                    .collect(Collectors.toSet());
            request.setInterviewerIds(interviewerIds);
        }

        prepareFormModel(model, true, id);
        model.addAttribute("scheduleRequest", request);
        model.addAttribute("scheduleDetail", detail);
        return "interview/form";
    }

    /**
     * Xử lý cập nhật lịch phỏng vấn (Dành cho HR).
     * Tuân thủ nghiêm ngặt Rule GBR-01 (không lùi trạng thái) và Rule MSG26 (endTime > startTime).
     * Controller bắt exception và add error vào BindingResult để trang không crash 500.
     */
    @PostMapping("/{id}/edit")
    public String updateSchedule(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("scheduleRequest") InterviewScheduleRequest request,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        User currentUser = resolveEffectiveUser(authentication);
        if (!isHrUser(currentUser, authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chỉ có nhân sự (HR) mới có quyền cập nhật lịch phỏng vấn.");
            return "redirect:/interviews";
        }

        // 1. Kiểm tra validation cơ bản từ DTO
        if (bindingResult.hasErrors()) {
            log.warn("Cập nhật lịch phỏng vấn #{} thất bại do validation lỗi.", id);
            prepareFormModel(model, true, id);
            return "interview/form";
        }

        // 2. Gọi Service cập nhật và xử lý ngoại lệ Rule GBR-01 / MSG26
        try {
            Integer hrUserId = currentUser != null ? currentUser.getUserId() : 1;
            interviewSchedulingService.updateSchedule(id, request, hrUserId);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật lịch phỏng vấn thành công!");
            return "redirect:/interviews";
        } catch (InterviewStatusException | IllegalArgumentException ex) {
            log.warn("Ngoại lệ nghiệp vụ khi cập nhật lịch #{}: {}", id, ex.getMessage());
            bindingResult.reject("businessError", ex.getMessage());
            prepareFormModel(model, true, id);
            return "interview/form";
        }
    }

    /**
     * Hủy lịch phỏng vấn (Chỉ dành cho HR).
     */
    @PostMapping("/{id}/cancel")
    public String cancelSchedule(
            @PathVariable("id") Long id,
            @RequestParam(value = "cancelReason", defaultValue = "Hủy lịch bởi HR") String cancelReason,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User currentUser = resolveEffectiveUser(authentication);
        if (!isHrUser(currentUser, authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chỉ có nhân sự (HR) mới có quyền hủy lịch phỏng vấn.");
            return "redirect:/interviews";
        }

        try {
            Integer hrUserId = currentUser != null ? currentUser.getUserId() : 1;
            interviewSchedulingService.cancelSchedule(id, cancelReason, hrUserId);
            redirectAttributes.addFlashAttribute("successMessage", "Hủy lịch phỏng vấn thành công!");
        } catch (InterviewStatusException | IllegalArgumentException ex) {
            log.warn("Lỗi khi hủy lịch #{}: {}", id, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/interviews";
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    /**
     * Nạp dữ liệu các dropdown và metadata vào Model phục vụ form.html.
     */
    private void prepareFormModel(Model model, boolean isEdit, Long interviewId) {
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("interviewId", interviewId);

        // Danh sách hồ sơ ứng tuyển (Eager load candidate, account, jobPosting tránh LazyInitializationException)
        List<Application> applications = applicationRepository.findAllWithCandidateAndJobPosting();
        model.addAttribute("applications", applications);

        // Danh sách Interviewer hợp lệ (Chỉ gồm nhân viên nội bộ không thuộc HR, loại bỏ ứng viên)
        List<User> availableInterviewers = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null
                        && RoleAuthorities.INTERNAL_ROLE_NAMES.contains(u.getRole().getRoleName())
                        && !"HR".equalsIgnoreCase(u.getRole().getRoleName()))
                .filter(u -> "Active".equalsIgnoreCase(u.getAccountStatus()))
                .toList();
        model.addAttribute("availableInterviewers", availableInterviewers);

        model.addAttribute("interviewFormats", InterviewFormat.values());
        model.addAttribute("interviewStatuses", InterviewStatus.values());
    }

    /**
     * Lấy thông tin tài khoản người dùng hiện tại từ Spring Security Authentication.
     */
    private User getCurrentUser(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return userRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
        }
        return null;
    }

    /**
     * Xác định tài khoản hiệu dụng (có fallback tìm tài khoản HR nếu chưa có session bảo mật trong môi trường test/dev).
     */
    private User resolveEffectiveUser(Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user != null) {
            return user;
        }
        // Fallback môi trường demo/dev: tìm tài khoản HR đầu tiên
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && "HR".equalsIgnoreCase(u.getRole().getRoleName()))
                .findFirst()
                .orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null));
    }

    /**
     * Kiểm tra xem người dùng có vai trò HR hay không.
     */
    private boolean isHrUser(User user, Authentication authentication) {
        if (user != null && user.getRole() != null) {
            String role = user.getRole().getRoleName();
            return "HR".equalsIgnoreCase(role) || "System Admin".equalsIgnoreCase(role);
        }
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_HR".equals(a.getAuthority()) || RoleAuthorities.SYSTEM_ADMIN.equals(a.getAuthority()));
        }
        return true; // Mặc định dev mode
    }
}
