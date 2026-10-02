package com.group2.rms.interview.service;

import com.group2.rms.candidate.Application;
import com.group2.rms.candidate.ApplicationRepository;
import com.group2.rms.interview.dto.InterviewScheduleRequest;
import com.group2.rms.interview.dto.InterviewScheduleResponse;
import com.group2.rms.interview.dto.PanelMemberRequest;
import com.group2.rms.interview.entity.*;
import com.group2.rms.interview.exception.InterviewStatusException;
import com.group2.rms.interview.repository.InterviewPanelRepository;
import com.group2.rms.interview.repository.InterviewScheduleRepository;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service implementation cho nghiệp vụ Lên lịch phỏng vấn (Interview Scheduling).
 * Tuân thủ đầy đủ các Business Rules:
 * - Rule MSG26: endTime phải lớn hơn startTime.
 * - Rule GBR-01: Trạng thái lịch phỏng vấn chỉ đi tiến, không lùi.
 * - Rule GBR-05: Interviewer chỉ được xem các lịch phỏng vấn mà họ được phân công tham gia hội đồng.
 * - Rule HR Isolation: HR không tham gia hội đồng phỏng vấn, ngăn chặn HR tự gán mình vào hội đồng.
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

    @Override
    @Transactional
    public InterviewScheduleResponse createSchedule(InterviewScheduleRequest request, Integer hrUserId) {
        log.info("HR ID {} đang tạo lịch phỏng vấn cho Application ID {}", hrUserId, request.getApplicationId());

        // 1. Kiểm tra tồn tại của tài khoản HR tạo lịch
        User hrUser = userRepository.findById(hrUserId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản HR với ID: " + hrUserId));

        // 2. Kiểm tra tồn tại của hồ sơ ứng tuyển
        Application application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hồ sơ ứng tuyển với ID: " + request.getApplicationId()));

        // 3. Kiểm tra ràng buộc thời gian (Rule MSG26: endTime > startTime)
        if (request.getStartTime() == null || request.getEndTime() == null || !request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("Lỗi Rule MSG26: Thời gian kết thúc (" + request.getEndTime() +
                    ") phải lớn hơn thời gian bắt đầu (" + request.getStartTime() + ").");
        }

        // 4. Validate danh sách Hội đồng phỏng vấn (InterviewPanel)
        List<PanelMemberRequest> panelRequests = request.getPanelMembers();
        if (panelRequests == null || panelRequests.isEmpty()) {
            throw new IllegalArgumentException("Hội đồng phỏng vấn phải có ít nhất một người phỏng vấn.");
        }

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

            // Quy tắc: Ngăn chặn HR tự gán mình vào hội đồng
            if (interviewerId.equals(hrUserId)) {
                throw new InterviewStatusException("Quy tắc nghiệp vụ: HR tạo lịch không được phép tự gán mình vào Hội đồng phỏng vấn.");
            }

            User interviewer = userRepository.findById(interviewerId)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người phỏng vấn với ID: " + interviewerId));

            // Quy tắc: HR không còn tham gia phỏng vấn - Kiểm tra role hệ thống của người dùng
            if (interviewer.getRole() != null && "HR".equalsIgnoreCase(interviewer.getRole().getRoleName())) {
                throw new InterviewStatusException("Người dùng " + interviewer.getFullName() + " (ID: " + interviewerId +
                        ") có vai trò HR, không được phép tham gia Hội đồng phỏng vấn.");
            }

            RoleInPanel roleInPanel = memberReq.roleInPanel();
            if (roleInPanel == RoleInPanel.HR) {
                throw new InterviewStatusException("Không cho phép gán vai trò HR trong Hội đồng phỏng vấn. Chỉ gán vai trò HM hoặc phỏng vấn chuyên môn.");
            }
            if (roleInPanel == null) {
                roleInPanel = RoleInPanel.HM; // Mặc định là HM (Hiring Manager / Technical Interviewer)
            }

            validatedInterviewers.add(interviewer);
            validatedRoles.add(roleInPanel);
        }

        // 5. Khởi tạo và lưu InterviewSchedule
        InterviewSchedule schedule = InterviewSchedule.builder()
                .application(application)
                .interviewFormat(request.getInterviewFormat())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .locationOrLink(request.getLocationOrLink())
                .interviewStatus(InterviewStatus.Scheduled)
                .createdBy(hrUser)
                .build();

        InterviewSchedule savedSchedule = interviewScheduleRepository.save(schedule);

        // 6. Gán các thành viên vào Hội đồng phỏng vấn
        for (int i = 0; i < validatedInterviewers.size(); i++) {
            savedSchedule.addPanelMember(validatedInterviewers.get(i), validatedRoles.get(i));
        }

        InterviewSchedule fullySavedSchedule = interviewScheduleRepository.save(savedSchedule);
        log.info("Tạo lịch phỏng vấn thành công với Interview ID {}", fullySavedSchedule.getInterviewId());

        return InterviewScheduleResponse.fromEntity(fullySavedSchedule);
    }

    @Override
    @Transactional
    public InterviewScheduleResponse updateSchedule(Long interviewId, InterviewScheduleRequest request, Integer hrUserId) {
        log.info("HR ID {} đang cập nhật lịch phỏng vấn ID {}", hrUserId, interviewId);

        InterviewSchedule schedule = interviewScheduleRepository.findByIdWithDetails(interviewId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lịch phỏng vấn với ID: " + interviewId));

        // 1. Kiểm tra Business Rule GBR-01: Trạng thái chỉ đi tiến, không lùi
        InterviewStatus targetStatus = request.getInterviewStatus();
        if (targetStatus != null && !schedule.getInterviewStatus().canTransitionTo(targetStatus)) {
            throw new InterviewStatusException(schedule.getInterviewStatus(), targetStatus,
                    "Business Rule GBR-01: Không thể chuyển trạng thái từ [" +
                    schedule.getInterviewStatus() + "] về [" + targetStatus + "]. Trạng thái chỉ đi tiến, không lùi.");
        }

        // 2. Kiểm tra ràng buộc thời gian (Rule MSG26)
        if (request.getStartTime() != null && request.getEndTime() != null) {
            if (!request.getEndTime().isAfter(request.getStartTime())) {
                throw new IllegalArgumentException("Lỗi Rule MSG26: Thời gian kết thúc (" + request.getEndTime() +
                        ") phải lớn hơn thời gian bắt đầu (" + request.getStartTime() + ").");
            }
            schedule.setStartTime(request.getStartTime());
            schedule.setEndTime(request.getEndTime());
        }

        // 3. Cập nhật các trường thông tin cơ bản
        if (request.getInterviewFormat() != null) {
            schedule.setInterviewFormat(request.getInterviewFormat());
        }
        schedule.setLocationOrLink(request.getLocationOrLink());

        if (targetStatus != null && targetStatus != schedule.getInterviewStatus()) {
            schedule.transitionTo(targetStatus);
        }

        // 4. Cập nhật Hội đồng phỏng vấn (nếu có danh sách mới)
        if (request.getPanelMembers() != null && !request.getPanelMembers().isEmpty()) {
            Set<Integer> uniqueIds = new HashSet<>();
            List<User> newInterviewers = new ArrayList<>();
            List<RoleInPanel> newRoles = new ArrayList<>();

            for (PanelMemberRequest memberReq : request.getPanelMembers()) {
                Integer interviewerId = memberReq.interviewerId();
                if (!uniqueIds.add(interviewerId)) {
                    throw new IllegalArgumentException("Trùng lặp người phỏng vấn trong hội đồng: ID " + interviewerId);
                }

                if (interviewerId.equals(hrUserId)) {
                    throw new InterviewStatusException("HR không được phép tự gán mình vào Hội đồng phỏng vấn.");
                }

                User interviewer = userRepository.findById(interviewerId)
                        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người phỏng vấn với ID: " + interviewerId));

                if (interviewer.getRole() != null && "HR".equalsIgnoreCase(interviewer.getRole().getRoleName())) {
                    throw new InterviewStatusException("Người dùng " + interviewer.getFullName() +
                            " có vai trò HR, không được tham gia Hội đồng phỏng vấn.");
                }

                RoleInPanel role = memberReq.roleInPanel() == RoleInPanel.HR || memberReq.roleInPanel() == null
                        ? RoleInPanel.HM
                        : memberReq.roleInPanel();

                newInterviewers.add(interviewer);
                newRoles.add(role);
            }

            // Làm mới danh sách hội đồng
            schedule.getInterviewPanels().clear();
            interviewScheduleRepository.saveAndFlush(schedule);

            for (int i = 0; i < newInterviewers.size(); i++) {
                schedule.addPanelMember(newInterviewers.get(i), newRoles.get(i));
            }
        }

        InterviewSchedule updated = interviewScheduleRepository.save(schedule);
        log.info("Cập nhật lịch phỏng vấn ID {} thành công", interviewId);

        return InterviewScheduleResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public InterviewScheduleResponse cancelSchedule(Long interviewId, String reason, Integer hrUserId) {
        log.info("HR ID {} đang yêu cầu hủy lịch phỏng vấn ID {}. Lý do: {}", hrUserId, interviewId, reason);

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
        log.info("Hủy lịch phỏng vấn ID {} thành công", interviewId);

        return InterviewScheduleResponse.fromEntity(cancelledSchedule);
    }

    @Override
    public InterviewScheduleResponse getScheduleDetail(Long interviewId) {
        InterviewSchedule schedule = interviewScheduleRepository.findByIdWithDetails(interviewId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy lịch phỏng vấn với ID: " + interviewId));
        return InterviewScheduleResponse.fromEntity(schedule);
    }

    @Override
    public List<InterviewScheduleResponse> getAllForHR() {
        return interviewScheduleRepository.findAllWithDetailsForHr().stream()
                .map(InterviewScheduleResponse::fromEntity)
                .toList();
    }

    @Override
    public Page<InterviewScheduleResponse> getAllForHRPaged(Pageable pageable) {
        return interviewScheduleRepository.findAllPagedForHr(pageable)
                .map(InterviewScheduleResponse::fromEntity);
    }

    @Override
    public List<InterviewScheduleResponse> getSchedulesByApplicationForHR(Integer applicationId) {
        return interviewScheduleRepository.findByApplicationIdWithDetailsForHr(applicationId).stream()
                .map(InterviewScheduleResponse::fromEntity)
                .toList();
    }

    @Override
    public List<InterviewScheduleResponse> getMySchedulesForInterviewer(Integer interviewerId) {
        if (interviewerId == null) {
            throw new IllegalArgumentException("Interviewer ID không được để trống.");
        }
        // Rule GBR-05: Chỉ lấy lịch phỏng vấn mà Interviewer được phân công
        return interviewScheduleRepository.findAllAssignedToInterviewer(interviewerId).stream()
                .map(InterviewScheduleResponse::fromEntity)
                .toList();
    }

    @Override
    public Page<InterviewScheduleResponse> getMySchedulesForInterviewerPaged(Integer interviewerId, Pageable pageable) {
        if (interviewerId == null) {
            throw new IllegalArgumentException("Interviewer ID không được để trống.");
        }
        // Rule GBR-05 (Phân trang)
        return interviewScheduleRepository.findAssignedToInterviewerPaged(interviewerId, pageable)
                .map(InterviewScheduleResponse::fromEntity);
    }

    @Override
    public InterviewScheduleResponse getMyScheduleDetailForInterviewer(Long interviewId, Integer interviewerId) {
        if (interviewId == null || interviewerId == null) {
            throw new IllegalArgumentException("Interview ID và Interviewer ID không được để trống.");
        }

        // Rule GBR-05: Kiểm tra quyền truy cập ở tầng Database
        InterviewSchedule schedule = interviewScheduleRepository.findByIdAndAssignedInterviewer(interviewId, interviewerId)
                .orElseThrow(() -> new InterviewStatusException(
                        "Business Rule GBR-05: Bạn không có quyền truy cập hoặc không được phân công vào lịch phỏng vấn này."));

        return InterviewScheduleResponse.fromEntity(schedule);
    }
}
