package com.group2.rms.interview.service;

import com.group2.rms.candidate.entity.Application;
import com.group2.rms.candidate.repository.ApplicationRepository;
import com.group2.rms.candidate.service.ApplicationStatusService;
import com.group2.rms.core.security.RoleAuthorities;
import com.group2.rms.interview.dto.InterviewScheduleRequest;
import com.group2.rms.interview.dto.InterviewScheduleResponse;
import com.group2.rms.interview.dto.PanelMemberRequest;
import com.group2.rms.interview.entity.*;
import com.group2.rms.interview.exception.InterviewStatusException;
import com.group2.rms.interview.repository.InterviewPanelRepository;
import com.group2.rms.interview.repository.InterviewScheduleRepository;
import com.group2.rms.notification.service.NotificationService;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Quản lý lịch, hội đồng và phạm vi xem lịch theo vai trò.
 * HR tạo lịch không được tự thêm mình vào hội đồng; Director được tự tham gia.
 * Khi tạo lịch, module candidate đồng bộ hồ sơ sang Interviewing và module notification báo người liên quan.
 *
 * Các luồng đọc cũng đồng bộ lịch hết giờ sang Completed trước khi trả dữ liệu.
 * Email mời/đổi/hủy được controller yêu cầu gửi sau khi service lưu lịch hoàn tất.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InterviewSchedulingServiceImpl implements InterviewSchedulingService {

    private final InterviewScheduleRepository interviewScheduleRepository;
    private final InterviewPanelRepository interviewPanelRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final InterviewTimeValidationService interviewTimeValidationService;
    // dunglt bổ sung: đổi trạng thái hồ sơ khi xếp lịch (module candidate, chiều interview → candidate)
    private final ApplicationStatusService applicationStatusService;
    // hoangnh bổ sung: thông báo in-app khi tạo / đổi / hủy lịch
    private final NotificationService notificationService;

    /**
     * Tạo lịch mới, thay thế lịch chưa bắt đầu nếu có và giữ lịch cũ ở trạng thái Cancelled.
     * Lỗi ở bước kiểm tra tiếp theo rollback cả việc hủy lịch cũ.
     */
    @Override
    @Transactional
    public InterviewScheduleResponse createSchedule(InterviewScheduleRequest request, Integer creatorUserId) {
        /*
         * Hoàn thành lịch hết giờ trước khi tìm lịch cần thay thế,
         * tránh coi buổi đã kết thúc là lịch còn hoạt động.
         */
        log.info("Người dùng ID {} đang tạo lịch phỏng vấn cho Application ID {}", creatorUserId, request.getApplicationId());
        completeEndedSchedules();

        /*
         * Tài khoản người tạo dùng cho CreatedBy và quy tắc HR tự gán hội đồng;
         * người tạo lịch không nhất thiết là một thành viên hội đồng.
         */
        User creatorUser = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản người dùng với ID: " + creatorUserId));
        String creatorRole = creatorUser.getRole() != null ? creatorUser.getRole().getRoleName() : "";
        boolean isHrCreator = "HR".equalsIgnoreCase(creatorRole);
        boolean isDirectorCreator = "Director".equalsIgnoreCase(creatorRole);

        /*
         * Một ứng viên có thể có nhiều hồ sơ. Kiểm tra trùng giờ ở bước sau phải theo ứng viên,
         * không chỉ hồ sơ đang được lên lịch.
         */
        Application application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hồ sơ ứng tuyển với ID: " + request.getApplicationId()));

        /*
         * Module candidate xác nhận hồ sơ còn ở vòng phỏng vấn và tin còn Published,
         * kể cả các dữ kiện offer/lịch đã có. Sau đó mới xét trùng giờ và lịch thay thế.
         */
        applicationStatusService.requireScheduleable(application.getApplicationId());
        // ducnm bổ sung: hủy lịch cũ trước khi kiểm tra trùng giờ để lịch thay thế được trùng giờ với chính lịch cũ.
        // Lỗi ở các bước sau sẽ rollback cả việc hủy này.
        boolean replacesActiveSchedule = cancelActiveSchedulesOf(application.getApplicationId());
        interviewTimeValidationService.validateForCreate(request, application);

        /*
         * Hội đồng chỉ gồm tài khoản nội bộ Active, không lặp người.
         * Nếu chưa chỉ định vai trò, suy vai trò hội đồng từ tài khoản được chọn.
         */
        List<PanelMemberRequest> panelRequests = request.getPanelMembers();
        if (panelRequests == null || panelRequests.isEmpty()) {
            throw new IllegalArgumentException("Hội đồng phỏng vấn phải có ít nhất một người phỏng vấn.");
        }

        /*
         * Kiểm tra đủ hội đồng trước khi lưu; giữ từng người đi cùng vai trò tương ứng.
         */
        List<User> validatedInterviewers = new ArrayList<>();
        List<RoleInPanel> validatedRoles = new ArrayList<>();
        Set<Integer> uniqueInterviewerIds = new HashSet<>();

        for (PanelMemberRequest memberReq : panelRequests) {
            Integer interviewerId = memberReq.interviewerId();
            if (interviewerId == null) {
                throw new IllegalArgumentException("Interviewer ID trong hội đồng không được để trống.");
            }

            if (!uniqueInterviewerIds.add(interviewerId)) {
                throw new IllegalArgumentException("Trùng lặp người phỏng vấn trong hội đồng: ID " + interviewerId);
            }

            // Quy tắc: HR tạo lịch không được phép tự gán mình vào hội đồng.
            // Tuy nhiên Director tạo lịch THÌ ĐƯỢC PHÉP tự gán mình vào hội đồng.
            if (isHrCreator && interviewerId.equals(creatorUserId)) {
                throw new InterviewStatusException("Quy tắc nghiệp vụ: HR tạo lịch không được phép tự gán mình vào Hội đồng phỏng vấn.");
            }

            User interviewer = userRepository.findById(interviewerId)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người phỏng vấn với ID: " + interviewerId));

            if (interviewer.getRole() != null) {
                String memberRole = interviewer.getRole().getRoleName();
                if ("Candidate".equalsIgnoreCase(memberRole) || !RoleAuthorities.INTERNAL_ROLE_NAMES.contains(memberRole)) {
                    throw new InterviewStatusException("Người dùng " + interviewer.getFullName() +
                            " không được phép tham gia Hội đồng phỏng vấn.");
                }
            }

            if (!"Active".equalsIgnoreCase(interviewer.getAccountStatus())) {
                throw new InterviewStatusException("Tài khoản người phỏng vấn " + interviewer.getFullName() + " không ở trạng thái Active.");
            }

            RoleInPanel roleInPanel = memberReq.roleInPanel() != null
                    ? memberReq.roleInPanel()
                    : defaultRoleInPanel(interviewer);

            validatedInterviewers.add(interviewer);
            validatedRoles.add(roleInPanel);
        }

        /*
         * Thay lịch đang hoạt động thì lịch mới là Rescheduled, trường hợp còn lại là Scheduled.
         * Trạng thái không lấy theo lựa chọn gửi từ form.
         */
        InterviewStatus initialStatus = replacesActiveSchedule ? InterviewStatus.Rescheduled : InterviewStatus.Scheduled;
        InterviewSchedule schedule = InterviewSchedule.builder()
                .application(application)
                .interviewFormat(request.getInterviewFormat())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .locationOrLink(request.getLocationOrLink())
                .interviewStatus(initialStatus)
                .createdBy(creatorUser)
                .build();

        InterviewSchedule savedSchedule = interviewScheduleRepository.save(schedule);

        /*
         * Lưu lịch trước để có khóa gắn hội đồng, rồi đồng bộ hồ sơ và phát thông báo theo lịch đầy đủ thành viên.
         */
        for (int i = 0; i < validatedInterviewers.size(); i++) {
            savedSchedule.addPanelMember(validatedInterviewers.get(i), validatedRoles.get(i));
        }

        InterviewSchedule fullySavedSchedule = interviewScheduleRepository.save(savedSchedule);
        // dunglt bổ sung: xếp lịch thì hồ sơ HM_Passed → Interviewing (luồng trạng thái hồ sơ)
        applicationStatusService.onInterviewScheduled(application.getApplicationId());
        // hoangnh bổ sung: báo hội đồng và ứng viên
        // ducnm sửa: hồ sơ đã có lịch trước đó -> lịch mới này là Rescheduled, báo đúng chữ "đã thay đổi"
        if (initialStatus == InterviewStatus.Rescheduled) {
            notificationService.notifyInterviewRescheduled(fullySavedSchedule, creatorUser);
        } else {
            notificationService.notifyInterviewScheduled(fullySavedSchedule, creatorUser);
        }
        log.info("Tạo lịch phỏng vấn thành công với Interview ID {}", fullySavedSchedule.getInterviewId());

        return InterviewScheduleResponse.fromEntity(fullySavedSchedule);
    }

    /**
     * Chỉ sửa lịch đang hoạt động, giữ nguyên hồ sơ và người tạo.
     * Hội đồng chỉ được thay khi có danh sách mới không rỗng; lần sửa thành công được tính là đổi lịch.
     */
    @Override
    @Transactional
    public InterviewScheduleResponse updateSchedule(Long interviewId, InterviewScheduleRequest request, Integer updaterUserId) {
        /*
         * Đồng bộ trạng thái trước khi sửa để không nhận thay đổi trên buổi đã hoàn thành,
         * kể cả form được mở từ trước giờ kết thúc.
         */
        log.info("Người dùng ID {} đang cập nhật lịch phỏng vấn ID {}", updaterUserId, interviewId);
        completeEndedSchedules();

        InterviewSchedule schedule = interviewScheduleRepository.findByIdWithDetails(interviewId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lịch phỏng vấn với ID: " + interviewId));

        // Lịch đã Hoàn thành hoặc Đã hủy là trạng thái cuối, không sửa được nữa (GBR-01)
        InterviewStatus statusBeforeUpdate = schedule.getInterviewStatus();
        if (statusBeforeUpdate == InterviewStatus.Completed || statusBeforeUpdate == InterviewStatus.Cancelled) {
            throw new InterviewStatusException("Lịch phỏng vấn đã " +
                    (statusBeforeUpdate == InterviewStatus.Completed ? "hoàn thành" : "bị hủy") + ", không thể sửa được nữa.");
        }

        User updaterUser = userRepository.findById(updaterUserId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản người dùng với ID: " + updaterUserId));
        String updaterRole = updaterUser.getRole() != null ? updaterUser.getRole().getRoleName() : "";
        boolean isHrUpdater = "HR".equalsIgnoreCase(updaterRole);

        /*
         * Kiểm tra khoảng giờ mới, loại chính lịch đang sửa khỏi xung đột
         * nhưng vẫn xét các hồ sơ khác của cùng ứng viên.
         */
        interviewTimeValidationService.validateForUpdate(interviewId, request, schedule);

        // 2. Cập nhật thời gian
        if (request.getStartTime() != null) {
            schedule.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            schedule.setEndTime(request.getEndTime());
        }

        // 3. Cập nhật các trường thông tin cơ bản
        if (request.getInterviewFormat() != null) {
            schedule.setInterviewFormat(request.getInterviewFormat());
        }
        if (request.getLocationOrLink() != null) {
            schedule.setLocationOrLink(request.getLocationOrLink());
        }

        // Mọi lần sửa thành công lịch đang hoạt động đều coi là đổi lịch (không cho người dùng tự chọn trạng thái)
        if (schedule.getInterviewStatus() == InterviewStatus.Scheduled) {
            schedule.transitionTo(InterviewStatus.Rescheduled);
        }

        /*
         * HR được giữ chỗ vốn do người khác phân công, nhưng không được tự thêm chỗ mới.
         * Ghi nhận người bị gỡ để thông báo riêng sau khi lưu.
         */
        List<User> removedInterviewers = new ArrayList<>();
        if (request.getPanelMembers() != null && !request.getPanelMembers().isEmpty()) {
            // HR đã được HR khác xếp vào hội đồng thì khi sửa lịch vẫn được giữ chỗ của mình
            Set<Integer> currentMemberIds = new HashSet<>();
            schedule.getInterviewPanels().forEach(panel -> {
                if (panel.getInterviewer() != null) {
                    currentMemberIds.add(panel.getInterviewer().getUserId());
                }
            });
            Set<Integer> uniqueIds = new HashSet<>();
            List<User> newInterviewers = new ArrayList<>();
            List<RoleInPanel> newRoles = new ArrayList<>();

            for (PanelMemberRequest memberReq : request.getPanelMembers()) {
                Integer interviewerId = memberReq.interviewerId();
                if (!uniqueIds.add(interviewerId)) {
                    throw new IllegalArgumentException("Trùng lặp người phỏng vấn trong hội đồng: ID " + interviewerId);
                }

                // HR cập nhật không được phép tự thêm mình vào hội đồng
                if (isHrUpdater && interviewerId.equals(updaterUserId) && !currentMemberIds.contains(updaterUserId)) {
                    throw new InterviewStatusException("Quy tắc nghiệp vụ: HR không được phép tự gán mình vào Hội đồng phỏng vấn.");
                }

                User interviewer = userRepository.findById(interviewerId)
                        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người phỏng vấn với ID: " + interviewerId));

                if (interviewer.getRole() != null) {
                    String memberRole = interviewer.getRole().getRoleName();
                    if ("Candidate".equalsIgnoreCase(memberRole) || !RoleAuthorities.INTERNAL_ROLE_NAMES.contains(memberRole)) {
                        throw new InterviewStatusException("Người dùng " + interviewer.getFullName() +
                                " không được phép tham gia Hội đồng phỏng vấn.");
                    }
                }
                if (!"Active".equalsIgnoreCase(interviewer.getAccountStatus())) {
                    throw new InterviewStatusException("Tài khoản người phỏng vấn " + interviewer.getFullName() + " không ở trạng thái Active.");
                }

                RoleInPanel role = memberReq.roleInPanel() != null
                        ? memberReq.roleInPanel()
                        : defaultRoleInPanel(interviewer);

                newInterviewers.add(interviewer);
                newRoles.add(role);
            }

            // Người đang trong hội đồng mà không còn trong danh sách mới -> báo riêng sau khi lưu
            schedule.getInterviewPanels().forEach(panel -> {
                if (panel.getInterviewer() != null && !uniqueIds.contains(panel.getInterviewer().getUserId())) {
                    removedInterviewers.add(panel.getInterviewer());
                }
            });

            /*
             * Flush việc xóa hội đồng cũ trước khi thêm lại để tránh trùng khóa ghép,
             * đặc biệt khi giữ người nhưng đổi vai trò. Flush chưa commit nên vẫn rollback được toàn bộ.
             */
            schedule.getInterviewPanels().clear();
            interviewScheduleRepository.saveAndFlush(schedule);

            for (int i = 0; i < newInterviewers.size(); i++) {
                schedule.addPanelMember(newInterviewers.get(i), newRoles.get(i));
            }
        }

        /*
         * Thông báo hội đồng mới, ứng viên và người bị gỡ sau khi lưu lịch thay đổi.
         */
        InterviewSchedule updated = interviewScheduleRepository.save(schedule);
        // hoangnh bổ sung: lịch vẫn còn diễn ra thì báo hội đồng và ứng viên
        InterviewStatus status = updated.getInterviewStatus();
        if (status == InterviewStatus.Scheduled || status == InterviewStatus.Rescheduled) {
            notificationService.notifyInterviewRescheduled(updated, updaterUser);
            notificationService.notifyInterviewersRemoved(updated, removedInterviewers, updaterUser);
        }
        log.info("Cập nhật lịch phỏng vấn ID {} thành công", interviewId);

        return InterviewScheduleResponse.fromEntity(updated);
    }

    private void completeEndedSchedules() {
        /*
         * Đồng bộ lịch Scheduled/Rescheduled có EndTime <= now trước khi đọc.
         * Không clear session ở đây vì tầng gọi còn sử dụng tài khoản đăng nhập.
         */
        interviewScheduleRepository.completeEndedSchedules(LocalDateTime.now());
    }

    /**
     * ducnm bổ sung: mỗi hồ sơ chỉ có một lịch đang hoạt động. Các lịch chưa bắt đầu bị thay thế (Cancelled);
     * lịch đã bắt đầu và chưa kết thúc thì không được thay vì hội đồng có thể đang phỏng vấn, đang chấm phiếu.
     *
     * @return true nếu đã thay thế ít nhất một lịch
     */
    private boolean cancelActiveSchedulesOf(Integer applicationId) {
        /*
         * Chỉ thay lịch chưa bắt đầu; lịch đang diễn ra phải giữ nguyên để hội đồng tiếp tục phỏng vấn/chấm điểm.
         * Kết quả trả về dùng chọn trạng thái và loại thông báo cho lịch mới.
         */
        List<InterviewSchedule> activeSchedules = interviewScheduleRepository.findActiveByApplicationId(applicationId);
        LocalDateTime now = LocalDateTime.now();
        for (InterviewSchedule active : activeSchedules) {
            if (!active.getStartTime().isAfter(now)) {
                throw new InterviewStatusException("Hồ sơ này đang có buổi phỏng vấn diễn ra, chưa thể tạo lịch mới.");
            }
            active.transitionTo(InterviewStatus.Cancelled);
            interviewScheduleRepository.save(active);
        }
        return !activeSchedules.isEmpty();
    }

    /**
     * Vai trò trong hội đồng suy từ role tài khoản khi form không chỉ định (CK_InterviewPanel_Role: HR, HM, Interviewer, Director).
     */
    private static RoleInPanel defaultRoleInPanel(User interviewer) {
        String roleName = interviewer.getRole() != null ? interviewer.getRole().getRoleName() : "";
        if ("HR".equalsIgnoreCase(roleName)) {
            return RoleInPanel.HR;
        }
        if ("Director".equalsIgnoreCase(roleName)) {
            return RoleInPanel.Director;
        }
        if ("Interviewer".equalsIgnoreCase(roleName)) {
            return RoleInPanel.Interviewer;
        }
        return RoleInPanel.HM;
    }

    @Override
    @Transactional
    public InterviewScheduleResponse cancelSchedule(Long interviewId, String reason, Integer hrUserId) {
        /*
         * Hủy giữ lại lịch và hội đồng để truy vết; không đồng nghĩa loại hồ sơ.
         * Lý do hủy được chuyển vào thông báo và email.
         */
        log.info("HR ID {} đang yêu cầu hủy lịch phỏng vấn ID {}. Lý do: {}", hrUserId, interviewId, reason);
        completeEndedSchedules();

        InterviewSchedule schedule = interviewScheduleRepository.findByIdWithDetails(interviewId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lịch phỏng vấn với ID: " + interviewId));

        // Kiểm tra Rule GBR-01 trước khi chuyển sang Cancelled
        if (!schedule.getInterviewStatus().canTransitionTo(InterviewStatus.Cancelled)) {
            throw new InterviewStatusException(schedule.getInterviewStatus(), InterviewStatus.Cancelled,
                    "Business Rule GBR-01: Lịch phỏng vấn đang ở trạng thái [" + schedule.getInterviewStatus() +
                    "], không thể hủy (chỉ hủy được khi ở trạng thái Scheduled hoặc Rescheduled).");
        }

        schedule.transitionTo(InterviewStatus.Cancelled);
        InterviewSchedule cancelledSchedule = interviewScheduleRepository.save(schedule);
        // hoangnh bổ sung: báo hội đồng và ứng viên
        notificationService.notifyInterviewCancelled(cancelledSchedule, userRepository.findById(hrUserId).orElse(null), reason);
        log.info("Hủy lịch phỏng vấn ID {} thành công", interviewId);

        return InterviewScheduleResponse.fromEntity(cancelledSchedule);
    }

    /**
     * Chi tiết dùng cho quản lý lịch; phạm vi quyền được kiểm tra ở bên gọi.
     */
    @Override
    @Transactional
    public InterviewScheduleResponse getScheduleDetail(Long interviewId) {
        completeEndedSchedules();
        InterviewSchedule schedule = interviewScheduleRepository.findByIdWithDetails(interviewId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lịch phỏng vấn với ID: " + interviewId));
        return InterviewScheduleResponse.fromEntity(schedule);
    }

    /**
     * Danh sách đầy đủ cho HR/Director, đồng bộ lịch hết giờ trước khi trả dữ liệu.
     */
    @Override
    @Transactional
    public List<InterviewScheduleResponse> getAllForHR() {
        completeEndedSchedules();
        return interviewScheduleRepository.findAllWithDetailsForHr().stream()
                .map(InterviewScheduleResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public Page<InterviewScheduleResponse> getAllForHRPaged(Pageable pageable) {
        completeEndedSchedules();
        return interviewScheduleRepository.findAllPagedForHr(pageable)
                .map(InterviewScheduleResponse::fromEntity);
    }

    @Override
    @Transactional
    public List<InterviewScheduleResponse> getSchedulesByApplicationForHR(Integer applicationId) {
        completeEndedSchedules();
        return interviewScheduleRepository.findByApplicationIdWithDetailsForHr(applicationId).stream()
                .map(InterviewScheduleResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public List<InterviewScheduleResponse> getMySchedulesForInterviewer(Integer interviewerId) {
        completeEndedSchedules();
        if (interviewerId == null) {
            throw new IllegalArgumentException("Interviewer ID không được để trống.");
        }
        // Rule GBR-05: Chỉ lấy lịch phỏng vấn mà Interviewer được phân công
        return interviewScheduleRepository.findAllAssignedToInterviewer(interviewerId).stream()
                .map(InterviewScheduleResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public Page<InterviewScheduleResponse> getMySchedulesForInterviewerPaged(Integer interviewerId, Pageable pageable) {
        completeEndedSchedules();
        if (interviewerId == null) {
            throw new IllegalArgumentException("Interviewer ID không được để trống.");
        }
        // Rule GBR-05 (Phân trang)
        return interviewScheduleRepository.findAssignedToInterviewerPaged(interviewerId, pageable)
                .map(InterviewScheduleResponse::fromEntity);
    }

    @Override
    @Transactional
    public InterviewScheduleResponse getMyScheduleDetailForInterviewer(Long interviewId, Integer interviewerId) {
        /*
         * Chỉ đọc chi tiết nếu người xem được phân công vào lịch này.
         */
        completeEndedSchedules();
        if (interviewId == null || interviewerId == null) {
            throw new IllegalArgumentException("Interview ID và Interviewer ID không được để trống.");
        }

        // Rule GBR-05: Kiểm tra quyền truy cập ở tầng Database
        InterviewSchedule schedule = interviewScheduleRepository.findByIdAndAssignedInterviewer(interviewId, interviewerId)
                .orElseThrow(() -> new InterviewStatusException(
                        "Business Rule GBR-05: Bạn không có quyền truy cập hoặc không được phân công vào lịch phỏng vấn này."));

        return InterviewScheduleResponse.fromEntity(schedule);
    }

    @Override
    @Transactional
    public List<InterviewScheduleResponse> getSchedulesForCandidate(Integer candidateUserId) {
        /*
         * Lấy lịch của mọi hồ sơ thuộc cùng tài khoản ứng viên,
         * không giới hạn riêng một hồ sơ đang được chọn trên giao diện.
         */
        completeEndedSchedules();
        if (candidateUserId == null) {
            throw new IllegalArgumentException("Candidate User ID không được để trống.");
        }
        return interviewScheduleRepository.findAllByCandidateUserId(candidateUserId).stream()
                .map(InterviewScheduleResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public List<InterviewScheduleResponse> getSchedulesForDepartment(Integer departmentId, Integer managerUserId) {
        /* Dùng phạm vi phòng ban/người quản lý khi dựng danh sách dành cho Hiring Manager. */
        completeEndedSchedules();
        if (departmentId == null) {
            throw new IllegalArgumentException("Department ID không được để trống.");
        }
        return interviewScheduleRepository.findAllByDepartmentId(departmentId, managerUserId).stream()
                .map(InterviewScheduleResponse::fromEntity)
                .toList();
    }
}
