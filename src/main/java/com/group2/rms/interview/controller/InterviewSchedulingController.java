package com.group2.rms.interview.controller;

import com.group2.rms.candidate.entity.Application;
import com.group2.rms.talentpool.service.RecruitmentEmailService;
import com.group2.rms.candidate.exception.ApplicationStatusException;
import com.group2.rms.interview.dto.InterviewScheduleRequest;
import com.group2.rms.interview.dto.InterviewScheduleResponse;
import com.group2.rms.interview.dto.PanelMemberResponse;
import com.group2.rms.interview.entity.*;
import com.group2.rms.interview.exception.InterviewStatusException;
import com.group2.rms.interview.service.InterviewFormOptions;
import com.group2.rms.interview.service.InterviewSchedulingService;
import com.group2.rms.user.entity.User;
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
 * Quản lý lịch phỏng vấn và phạm vi hiển thị theo vai trò.
 * HR/Director được tạo, sửa, hủy; HM xem theo phòng ban; Interviewer xem lịch được phân công.
 * Hồ sơ đủ điều kiện do module candidate kiểm soát; email mời/đổi/hủy do RecruitmentEmailService xử lý.
 */
@Controller
@RequestMapping("/interviews")
@RequiredArgsConstructor
@Slf4j
public class InterviewSchedulingController {
    private final RecruitmentEmailService recruitmentEmail;

    private final InterviewSchedulingService interviewSchedulingService;
    private final com.group2.rms.interview.service.InterviewResultService interviewResultService;
    private final InterviewFormOptions formOptions;

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

        /*
         * Giới hạn lịch theo người xem: ứng viên theo hồ sơ sở hữu, HM theo phòng ban,
         * HR/Director xem toàn bộ. HM chưa có phòng ban chỉ thấy lịch mình được phân công.
         */
        List<InterviewScheduleResponse> schedules;
        if (isCandidate) {
            log.info("Ứng viên (ID: {}) đang xem các lịch phỏng vấn của chính mình.", currentUser.getUserId());
            schedules = interviewSchedulingService.getSchedulesForCandidate(currentUser.getUserId());
        } else if (isHiringManager) {
            Integer deptId = currentUser.getDepartment() != null ? currentUser.getDepartment().getDepartmentId() : null;
            log.info("Hiring Manager (ID: {}) đang xem lịch phỏng vấn của phòng ban ID {}.", currentUser.getUserId(), deptId);
            schedules = deptId != null
                    ? interviewSchedulingService.getSchedulesForDepartment(deptId, currentUser.getUserId())
                    : interviewSchedulingService.getMySchedulesForInterviewer(currentUser.getUserId());
        } else if (isHr || isDirector) {
            log.info("HR / Director (ID: {}) đang xem toàn bộ danh sách lịch phỏng vấn.", currentUser.getUserId());
            schedules = interviewSchedulingService.getAllForHR();
        } else {
            Integer interviewerId = currentUser.getUserId();
            log.info("Interviewer (ID: {}) đang xem các lịch phỏng vấn được phân công theo Rule GBR-05.", interviewerId);
            schedules = interviewSchedulingService.getMySchedulesForInterviewer(interviewerId);
        }

        /* Quyền xem danh sách rộng hơn quyền quản lý lịch: chỉ HR/Director có nút tạo và sửa. */
        boolean canCreate = isHr || isDirector;
        boolean canEdit = isHr || isDirector;
        boolean isCandidateView = isCandidate;

        model.addAttribute("schedules", schedules);
        /*
         * Ứng viên không nhận nhãn kết quả hay tiến độ chấm điểm nội bộ.
         * Hai map trạng thái chỉ bổ sung cho danh sách của người dùng nội bộ.
         */
        model.addAttribute("decisionStates", isCandidate ? java.util.Map.of() : interviewResultService.decisionStates(schedules));
        model.addAttribute("evaluationStates", isCandidate ? java.util.Map.of()
                : interviewResultService.evaluationStates(schedules, currentUser.getUserId()));
        model.addAttribute("isHr", isHr);
        model.addAttribute("isDirector", isDirector);
        model.addAttribute("canCreate", canCreate);
        model.addAttribute("canEdit", canEdit);
        model.addAttribute("isCandidateView", isCandidateView);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activeMenu", "interviews");
        return "interview/list";
    }

    /**
     * Hiển thị form tạo mới lịch phỏng vấn (Dành cho HR và Director).
     */
    @GetMapping("/new")
    public String showCreateForm(
            @RequestParam(value = "applicationId", required = false) Integer applicationId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        User currentUser = resolveEffectiveUser(authentication);
        // ducnm sửa: vai trò xem được danh sách lịch (HM, Interviewer) mà không có quyền tạo thì vẫn vào được
        // trang (còn sidebar/topbar/tiêu đề) nhưng ẩn form, thay bằng banner "Bạn không có quyền truy cập".
        if (!canManageSchedules(currentUser, authentication)) {
            log.warn("Từ chối truy cập tạo lịch phỏng vấn: Người dùng không có vai trò HR hoặc Director.");
            model.addAttribute("accessDenied", true);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("activeMenu", "interviews");
            model.addAttribute("isEdit", false);
            return "interview/form";
        }

        /*
         * Gợi ý lịch lúc 09:00 ngày mai. Khi mở từ trang hồ sơ, chỉ khóa ứng viên
         * nếu hồ sơ đó còn nằm trong danh sách đủ điều kiện lên lịch.
         */
        LocalDateTime defaultStartTime = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        // Mở từ trang hồ sơ (/applications/{id}): chọn sẵn hồ sơ trong dropdown
        InterviewScheduleRequest request = InterviewScheduleRequest.builder()
                .applicationId(applicationId)
                .interviewFormat(InterviewFormat.Online_GoogleMeet)
                .startTime(defaultStartTime)
                .endTime(defaultStartTime.plusHours(1))
                .build();

        prepareFormModel(model, false, null, currentUser);
        // Mở từ hồ sơ: cố định ứng viên; hồ sơ không đủ điều kiện thì quay lại trang hồ sơ kèm lý do
        if (applicationId != null && !lockApplication(model, applicationId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hồ sơ #" + applicationId
                    + " chưa đủ điều kiện xếp lịch phỏng vấn (cần đã qua trưởng bộ phận và tin tuyển dụng đang mở).");
            return "redirect:/applications/" + applicationId;
        }
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
            @RequestParam(value = "lockApplication", defaultValue = "false") boolean lockApplication,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        User currentUser = resolveEffectiveUser(authentication);
        if (!canManageSchedules(currentUser, authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chỉ có nhân sự (HR) và Giám đốc (Director) mới có quyền tạo lịch phỏng vấn.");
            return "redirect:/interviews";
        }

        /*
         * Khi form lỗi, giữ dữ liệu đã nhập và trạng thái khóa hồ sơ như lúc mở từ trang hồ sơ.
         */
        if (bindingResult.hasErrors()) {
            log.warn("Tạo lịch phỏng vấn thất bại do dữ liệu không hợp lệ: {} lỗi", bindingResult.getErrorCount());
            prepareFormModel(model, false, null, currentUser);
            keepLock(model, lockApplication, request.getApplicationId());
            return "interview/form";
        }

        /*
         * Kiểm tra lại điều kiện hồ sơ, trùng giờ và hội đồng lúc lưu,
         * vì hồ sơ có thể đã chuyển vòng sau khi người dùng mở form.
         */
        try {
            Integer creatorUserId = currentUser != null ? currentUser.getUserId() : 1;
            InterviewScheduleResponse saved = interviewSchedulingService.createSchedule(request, creatorUserId);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo lịch phỏng vấn thành công!");
            /*
             * Chỉ yêu cầu gửi thư sau khi đã lưu lịch. Lỗi gửi thư được ghi audit để HR gửi lại,
             * không làm mất lịch đã tạo.
             */
            recruitmentEmail.sendInterview(saved.interviewId());
            return "redirect:/interviews";
        } catch (InterviewStatusException | IllegalArgumentException | ApplicationStatusException ex) {
            // dunglt bổ sung: requireScheduleable ném ApplicationStatusException (trước là TalentPoolException)
            log.warn("Ngoại lệ nghiệp vụ khi tạo lịch phỏng vấn: {}", ex.getMessage());
            bindingResult.reject("businessError", ex.getMessage());
            prepareFormModel(model, false, null, currentUser);
            keepLock(model, lockApplication, request.getApplicationId());
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
        // ducnm sửa: vai trò xem được danh sách (HM, Interviewer) mà không có quyền sửa thì vẫn vào được trang
        // (còn sidebar/topbar/tiêu đề) nhưng ẩn form, thay bằng banner "Bạn không có quyền truy cập".
        if (!canManageSchedules(currentUser, authentication)) {
            model.addAttribute("accessDenied", true);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("activeMenu", "interviews");
            model.addAttribute("isEdit", true);
            model.addAttribute("interviewId", id);
            return "interview/form";
        }

        InterviewScheduleResponse detail = interviewSchedulingService.getScheduleDetail(id);
        if (detail == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy lịch phỏng vấn với mã: " + id);
            return "redirect:/interviews";
        }

        /*
         * Giữ lại hội đồng hiện tại khi mở form sửa; vai trò bỏ trống sẽ được service suy từ tài khoản.
         */
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
                .interviewerIds(interviewerIds)
                .build();

        prepareFormModel(model, true, id, currentUser);
        model.addAttribute("scheduleRequest", request);
        model.addAttribute("scheduleDetail", detail);
        return "interview/form";
    }

    /**
     * Cập nhật lịch cho HR/Director; giữ nguyên hồ sơ gắn với lịch,
     * không chuyển lịch sang ứng viên khác qua applicationId của form.
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
            InterviewScheduleResponse updated = interviewSchedulingService.updateSchedule(id, request, currentUser != null ? currentUser.getUserId() : 1);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật lịch phỏng vấn thành công!");
            /*
             * Mỗi lần đổi lịch được gửi thư thông báo mới, kể cả thư mời ban đầu đã gửi thành công.
             */
            recruitmentEmail.sendInterviewRescheduled(updated.interviewId());
            return "redirect:/interviews";
        } catch (InterviewStatusException | IllegalArgumentException ex) {
            log.warn("Ngoại lệ nghiệp vụ khi cập nhật lịch #{}: {}", id, ex.getMessage());
            bindingResult.reject("businessError", ex.getMessage());
            prepareFormModel(model, true, id, currentUser);
            return "interview/form";
        }
    }

    /**
     * Hủy lịch cho HR/Director, giữ lịch sử bằng trạng thái Cancelled.
     * Hủy buổi phỏng vấn không đồng nghĩa loại hồ sơ ứng tuyển.
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
            // ducnm bổ sung: gửi ngầm cho ứng viên, không ảnh hưởng tới việc đã hủy lịch
            recruitmentEmail.sendInterviewCancelled(id, cancelReason);
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
        /*
         * Giữ hồ sơ hiện tại trong form sửa dù hồ sơ không còn đủ điều kiện tạo lịch mới.
         * HR vốn đã được xếp vào hội đồng cũng phải giữ được lựa chọn chính mình.
         */
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("interviewId", interviewId);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activeMenu", "interviews");

        // Danh sách hồ sơ ứng tuyển
        Integer currentApplicationId = isEdit && interviewId != null
                ? interviewSchedulingService.getScheduleDetail(interviewId).applicationId() : null;
        model.addAttribute("applications", formOptions.eligibleApplications(currentApplicationId));

        // Danh sách người phỏng vấn hợp lệ: mọi tài khoản nội bộ đang Active (kể cả HR khác, Director).
        // HR đang đặt lịch không thấy chính mình, trừ khi sửa lịch mà HR khác đã xếp mình vào hội đồng.
        Integer hiddenSelfId = isHrSelfHidden(currentUser, isEdit, interviewId) ? currentUser.getUserId() : null;
        model.addAttribute("availableInterviewers", formOptions.availableInterviewers(hiddenSelfId));

        model.addAttribute("interviewFormats", InterviewFormat.values());
        // ducnm bổ sung: không còn ô chọn trạng thái trên form — hệ thống tự suy (Scheduled/Rescheduled khi lưu).
    }

    /**
     * Cố định hồ sơ trên form khi mở từ danh sách / chi tiết hồ sơ.
     * Chỉ khóa khi hồ sơ nằm trong danh sách đủ điều kiện xếp lịch đã nạp ở prepareFormModel.
     *
     * @return false nếu hồ sơ không đủ điều kiện
     */
    @SuppressWarnings("unchecked")
    private boolean lockApplication(Model model, Integer applicationId) {
        /*
         * Chỉ cố định hồ sơ còn đủ điều kiện, tránh lên lịch từ một liên kết hồ sơ đã chuyển vòng.
         */
        List<Application> applications = (List<Application>) model.getAttribute("applications");
        Application locked = applications == null ? null : applications.stream()
                .filter(a -> applicationId.equals(a.getApplicationId()))
                .findFirst().orElse(null);
        if (locked == null) {
            return false;
        }
        model.addAttribute("lockedApplication", locked);
        return true;
    }

    /** Form bị trả lại do lỗi: giữ nguyên trạng thái khóa hồ sơ như lúc mở. */
    private void keepLock(Model model, boolean lockApplication, Integer applicationId) {
        if (lockApplication && applicationId != null) {
            lockApplication(model, applicationId);
        }
    }

    /**
     * HR không tự đưa mình vào hội đồng nên ẩn chính mình khỏi danh sách chọn.
     * Ngoại lệ: đang sửa lịch mà mình đã có trong hội đồng (do HR khác xếp), để không bị gỡ ngầm khi lưu.
     */
    private boolean isHrSelfHidden(User currentUser, boolean isEdit, Long interviewId) {
        if (currentUser == null || currentUser.getRole() == null
                || !"HR".equalsIgnoreCase(currentUser.getRole().getRoleName())) {
            return false;
        }
        if (!isEdit || interviewId == null) {
            return true;
        }
        InterviewScheduleResponse detail = interviewSchedulingService.getScheduleDetail(interviewId);
        return detail == null || detail.interviewers() == null || detail.interviewers().stream()
                .noneMatch(member -> member != null && currentUser.getUserId().equals(member.interviewerId()));
    }

    /**
     * Lấy thông tin tài khoản người dùng hiện tại từ Spring Security Authentication.
     */
    private User getCurrentUser(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return formOptions.findByUsername(authentication.getName()).orElse(null);
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
        return formOptions.fallbackUser();
    }

    /**
     * Kiểm tra xem người dùng có vai trò HR hay không.
     */
    private boolean isHrUser(User user, Authentication authentication) {
        /* Ưu tiên vai trò trên tài khoản; authority chỉ là phương án dự phòng khi thiếu dữ liệu tài khoản. */
        if (user != null && user.getRole() != null) {
            String role = user.getRole().getRoleName();
            return "HR".equalsIgnoreCase(role);
        }
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_HR".equals(a.getAuthority()));
        }
        return false;
    }

    /**
     * Kiểm tra xem người dùng có vai trò Director hay không.
     */
    private boolean isDirectorUser(User user, Authentication authentication) {
        /* Cùng thứ tự xác định vai trò với nhánh HR để quyền hiển thị và quyền ghi lịch thống nhất. */
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
