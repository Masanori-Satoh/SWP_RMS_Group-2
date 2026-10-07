package com.group2.rms.interview.controller;

import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.repository.ApplicationRepository;
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
     * Màn hình danh sách lịch phỏng vấn dùng chung (Rule GBR-05 và Unified View).
     * Phân quyền:
     * - Candidate: Chỉ xem các lịch phỏng vấn của chính hồ sơ ứng tuyển của mình (Candidate Isolation).
     * - Hiring Manager: Xem toàn bộ lịch phỏng vấn thuộc phòng ban mình phụ trách.
     * - HR / Director: Xem toàn bộ lịch phỏng vấn của doanh nghiệp; có quyền tạo, sửa, hủy lịch.
     * - Interviewer: Chỉ xem lịch mình được gán vào hội đồng.
     */
    @GetMapping
    public String listInterviews(Authentication authentication, Model model) {
        User currentUser = resolveEffectiveUser(authentication);
        if (currentUser == null) {
            return "redirect:/login";
        }

        String roleName = (currentUser.getRole() != null) ? currentUser.getRole().getRoleName() : "";
        boolean isHr = isHrUser(currentUser, authentication);
        boolean isDirector = isDirectorUser(currentUser, authentication);
        boolean isHiringManager = "Hiring Manager".equalsIgnoreCase(roleName);
        boolean isCandidate = "Candidate".equalsIgnoreCase(roleName);

        List<InterviewScheduleResponse> schedules;
        if (isCandidate) {
            log.info("Ứng viên (ID: {}) đang xem các lịch phỏng vấn của chính mình.", currentUser.getUserId());
            schedules = interviewSchedulingService.getSchedulesForCandidate(currentUser.getUserId());
        } else if (isHiringManager) {
            Integer deptId = currentUser.getDepartment() != null ? currentUser.getDepartment().getDepartmentId() : null;
            log.info("Hiring Manager (ID: {}) đang xem lịch phỏng vấn của phòng ban ID {}.", currentUser.getUserId(), deptId);
            schedules = deptId != null
                    ? interviewSchedulingService.getSchedulesForDepartment(deptId)
                    : interviewSchedulingService.getMySchedulesForInterviewer(currentUser.getUserId());
        } else if (isHr || isDirector) {
            log.info("HR / Director (ID: {}) đang xem toàn bộ danh sách lịch phỏng vấn.", currentUser.getUserId());
            schedules = interviewSchedulingService.getAllForHR();
        } else {
            Integer interviewerId = currentUser.getUserId();
            log.info("Interviewer (ID: {}) đang xem các lịch phỏng vấn được phân công theo Rule GBR-05.", interviewerId);
            schedules = interviewSchedulingService.getMySchedulesForInterviewer(interviewerId);
        }

        boolean canCreate = isHr || isDirector;
        boolean canEdit = isHr || isDirector;
        boolean isCandidateView = isCandidate;

        model.addAttribute("schedules", schedules);
        model.addAttribute("isHr", isHr);
        model.addAttribute("isDirector", isDirector);
        model.addAttribute("canCreate", canCreate);
        model.addAttribute("canEdit", canEdit);
        model.addAttribute("isCandidateView", isCandidateView);
        model.addAttribute("currentUser", currentUser);
        return "interview/list";
    }

    /**
     * Hiển thị form tạo mới lịch phỏng vấn (Dành cho HR và Director).
     */
    @GetMapping("/new")
    public String showCreateForm(Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        User currentUser = resolveEffectiveUser(authentication);
        if (!canManageSchedules(currentUser, authentication)) {
            log.warn("Từ chối truy cập tạo lịch phỏng vấn: Người dùng không có vai trò HR hoặc Director.");
            redirectAttributes.addFlashAttribute("errorMessage", "Chỉ có nhân sự (HR) và Giám đốc (Director) mới có quyền tạo lịch phỏng vấn.");
            return "redirect:/interviews";
        }

        // Gợi ý mặc định: 09:00 sáng ngày mai
        LocalDateTime defaultStartTime = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        InterviewScheduleRequest request = InterviewScheduleRequest.builder()
                .interviewStatus(InterviewStatus.Scheduled)
                .interviewFormat(InterviewFormat.Online_GoogleMeet)
                .startTime(defaultStartTime)
                .endTime(defaultStartTime.plusHours(1))
                .build();

        prepareFormModel(model, false, null, currentUser);
        model.addAttribute("scheduleRequest", request);
        return "interview/form";
    }

    /**
     * Xử lý tạo mới lịch phỏng vấn (Dành cho HR và Director).
     */
    @PostMapping("/new")
    public String createSchedule(
            @Valid @ModelAttribute("scheduleRequest") InterviewScheduleRequest request,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        User currentUser = resolveEffectiveUser(authentication);
        if (!canManageSchedules(currentUser, authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chỉ có nhân sự (HR) và Giám đốc (Director) mới có quyền tạo lịch phỏng vấn.");
            return "redirect:/interviews";
        }

        // 1. Kiểm tra validation cơ bản
        if (bindingResult.hasErrors()) {
            log.warn("Tạo lịch phỏng vấn thất bại do dữ liệu không hợp lệ: {} lỗi", bindingResult.getErrorCount());
            prepareFormModel(model, false, null, currentUser);
            return "interview/form";
        }

        // 2. Gọi Service và bắt các ngoại lệ nghiệp vụ (Rule MSG26, HR Isolation, validation thời gian, v.v.)
        try {
            Integer creatorUserId = currentUser != null ? currentUser.getUserId() : 1;
            interviewSchedulingService.createSchedule(request, creatorUserId);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo lịch phỏng vấn thành công!");
            return "redirect:/interviews";
        } catch (InterviewStatusException | IllegalArgumentException ex) {
            log.warn("Ngoại lệ nghiệp vụ khi tạo lịch phỏng vấn: {}", ex.getMessage());
            bindingResult.reject("businessError", ex.getMessage());
            prepareFormModel(model, false, null, currentUser);
            return "interview/form";
        }
    }

    /**
     * Hiển thị form chỉnh sửa lịch phỏng vấn (Dành cho HR và Director).
     */
    @GetMapping("/{id}/edit")
    public String showEditForm(
            @PathVariable("id") Long id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        User currentUser = resolveEffectiveUser(authentication);
        if (!canManageSchedules(currentUser, authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chỉ có nhân sự (HR) và Giám đốc (Director) mới có quyền chỉnh sửa lịch phỏng vấn.");
            return "redirect:/interviews";
        }

        InterviewScheduleResponse detail = interviewSchedulingService.getScheduleDetail(id);
        if (detail == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy lịch phỏng vấn với mã: " + id);
            return "redirect:/interviews";
        }

        Set<Integer> interviewerIds = detail.interviewers() != null
                ? detail.interviewers().stream()
                        .map(PanelMemberResponse::interviewerId)
                        .collect(Collectors.toSet())
                : java.util.Collections.emptySet();

        InterviewScheduleRequest request = InterviewScheduleRequest.builder()
                .applicationId(detail.applicationId())
                .interviewFormat(detail.interviewFormat())
                .startTime(detail.startTime())
                .endTime(detail.endTime())
                .locationOrLink(detail.locationOrLink())
                .interviewStatus(detail.interviewStatus())
                .interviewerIds(interviewerIds)
                .build();

        prepareFormModel(model, true, id, currentUser);
        model.addAttribute("scheduleRequest", request);
        model.addAttribute("scheduleDetail", detail);
        return "interview/form";
    }

    /**
     * Xử lý cập nhật lịch phỏng vấn (Dành cho HR và Director).
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
        if (!canManageSchedules(currentUser, authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chỉ có nhân sự (HR) và Giám đốc (Director) mới có quyền cập nhật lịch phỏng vấn.");
            return "redirect:/interviews";
        }

        if (bindingResult.hasErrors()) {
            log.warn("Cập nhật lịch phỏng vấn #{} thất bại do validation lỗi.", id);
            prepareFormModel(model, true, id, currentUser);
            return "interview/form";
        }

        try {
            Integer updaterUserId = currentUser != null ? currentUser.getUserId() : 1;
            interviewSchedulingService.updateSchedule(id, request, updaterUserId);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật lịch phỏng vấn thành công!");
            return "redirect:/interviews";
        } catch (InterviewStatusException | IllegalArgumentException ex) {
            log.warn("Ngoại lệ nghiệp vụ khi cập nhật lịch #{}: {}", id, ex.getMessage());
            bindingResult.reject("businessError", ex.getMessage());
            prepareFormModel(model, true, id, currentUser);
            return "interview/form";
        }
    }

    /**
     * Hủy lịch phỏng vấn (Dành cho HR và Director).
     */
    @PostMapping("/{id}/cancel")
    public String cancelSchedule(
            @PathVariable("id") Long id,
            @RequestParam(value = "cancelReason", defaultValue = "Hủy lịch phỏng vấn") String cancelReason,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User currentUser = resolveEffectiveUser(authentication);
        if (!canManageSchedules(currentUser, authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chỉ có nhân sự (HR) và Giám đốc (Director) mới có quyền hủy lịch phỏng vấn.");
            return "redirect:/interviews";
        }

        try {
            Integer updaterUserId = currentUser != null ? currentUser.getUserId() : 1;
            interviewSchedulingService.cancelSchedule(id, cancelReason, updaterUserId);
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
     * Đảm bảo luôn nạp currentUser để header không bị hiển thị fallback 'Người dùng'.
     */
    private void prepareFormModel(Model model, boolean isEdit, Long interviewId, User currentUser) {
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("interviewId", interviewId);
        model.addAttribute("currentUser", currentUser);

        // Danh sách hồ sơ ứng tuyển
        List<Application> applications = applicationRepository.findAllWithCandidateAndJobPosting();
        model.addAttribute("applications", applications);

        // Danh sách Interviewer hợp lệ (Loại bỏ HR và Candidate; Director được giữ lại)
        List<User> availableInterviewers = userRepository.findAllWithRoleAndDepartment().stream()
                .filter(u -> u.getRole() != null
                        && RoleAuthorities.INTERNAL_ROLE_NAMES.contains(u.getRole().getRoleName())
                        && !"HR".equalsIgnoreCase(u.getRole().getRoleName()))
                .filter(u -> "Active".equalsIgnoreCase(u.getAccountStatus()))
                .toList();
        model.addAttribute("availableInterviewers", availableInterviewers);

        model.addAttribute("interviewFormats", InterviewFormat.values());

        // Nếu tạo mới: chỉ cho phép Scheduled ("Đã lên lịch") hoặc Completed ("Đã hoàn thành")
        // Nếu chỉnh sửa: hiển thị tất cả các trạng thái
        if (!isEdit) {
            model.addAttribute("interviewStatuses", List.of(InterviewStatus.Scheduled, InterviewStatus.Completed));
        } else {
            model.addAttribute("interviewStatuses", InterviewStatus.values());
        }
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
     * Xác định tài khoản hiệu dụng.
     */
    private User resolveEffectiveUser(Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user != null) {
            return user;
        }
        // Fallback môi trường demo/dev khi chưa có session
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
        return false;
    }

    /**
     * Kiểm tra xem người dùng có vai trò Director hay không.
     */
    private boolean isDirectorUser(User user, Authentication authentication) {
        if (user != null && user.getRole() != null) {
            return "Director".equalsIgnoreCase(user.getRole().getRoleName());
        }
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_DIRECTOR".equals(a.getAuthority()));
        }
        return false;
    }

    /**
     * Quyền quản lý lịch phỏng vấn (Tạo mới, Cập nhật, Hủy): Dành cho HR và Director.
     */
    private boolean canManageSchedules(User user, Authentication authentication) {
        return isHrUser(user, authentication) || isDirectorUser(user, authentication);
    }
}
