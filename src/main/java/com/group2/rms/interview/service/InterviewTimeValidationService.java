package com.group2.rms.interview.service;

import com.group2.rms.candidate.domain.entity.Application;
import com.group2.rms.interview.dto.InterviewScheduleRequest;
import com.group2.rms.interview.entity.InterviewSchedule;
import com.group2.rms.interview.entity.InterviewStatus;
import com.group2.rms.interview.repository.InterviewScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Service chuyên trách kiểm tra các ràng buộc về thời gian và tính hợp lệ của trạng thái
 * cho toàn bộ quy trình Xem, Tạo mới và Cập nhật Lịch phỏng vấn.
 * Trạng thái (Scheduled/Rescheduled/Completed/Cancelled) do hệ thống tự suy, người dùng không
 * tự chọn được nên lớp này chỉ còn kiểm tra thời gian và trùng lịch.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewTimeValidationService {

    public static final Duration MIN_DURATION = Duration.ofMinutes(15);
    public static final Duration MAX_DURATION = Duration.ofHours(4);

    private final InterviewScheduleRepository interviewScheduleRepository;

    /**
     * Kiểm tra toàn bộ ràng buộc thời gian khi TẠO MỚI lịch phỏng vấn.
     * Quy tắc:
     * 1. endTime phải lớn hơn startTime, buổi phỏng vấn dài 15 phút đến 4 giờ.
     * 2. startTime phải ở trong tương lai (lịch mới luôn ở trạng thái hoạt động).
     * 3. Kiểm tra xung đột trùng lịch: ứng viên (theo người, không chỉ theo hồ sơ) và người phỏng vấn
     *    không được có lịch khác đang hoạt động trùng giờ.
     */
    public void validateForCreate(InterviewScheduleRequest request, Application application) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu yêu cầu lịch phỏng vấn không được để trống.");
        }

        LocalDateTime startTime = request.getStartTime();
        LocalDateTime endTime = request.getEndTime();

        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Thời gian bắt đầu và kết thúc phỏng vấn không được để trống.");
        }

        validateDuration(startTime, endTime);

        // startTime phải ở trong tương lai: lịch mới tạo luôn ở trạng thái hoạt động (Scheduled/Rescheduled)
        if (!startTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Thời gian bắt đầu phỏng vấn phải ở trong tương lai.");
        }

        // Kiểm tra trùng lịch của ứng viên (theo người, gồm mọi hồ sơ khác của cùng ứng viên)
        if (application != null && application.getCandidate() != null) {
            long candidateConflicts = interviewScheduleRepository.countConflictingSchedulesForCandidate(
                    application.getCandidate().getCandidateId(), startTime, endTime, null);
            if (candidateConflicts > 0) {
                throw new IllegalArgumentException(
                        "Ứng viên này đã có một lịch phỏng vấn khác đang hoạt động trùng vào khoảng thời gian đã chọn.");
            }
        }

        // Kiểm tra trùng lịch hội đồng phỏng vấn
        Set<Integer> interviewerIds = request.getInterviewerIds();
        if (interviewerIds != null) {
            for (Integer interviewerId : interviewerIds) {
                if (interviewerId != null) {
                    long intConflicts = interviewScheduleRepository.countConflictingSchedulesForInterviewer(
                            interviewerId, startTime, endTime, null);
                    if (intConflicts > 0) {
                        throw new IllegalArgumentException(
                                "Một thành viên hội đồng đã có lịch phỏng vấn khác trùng vào khoảng thời gian này.");
                    }
                }
            }
        }
    }

    /**
     * Kiểm tra toàn bộ ràng buộc thời gian khi CẬP NHẬT lịch phỏng vấn.
     * Lịch đã Hoàn thành hoặc Đã hủy không được sửa (kiểm tra ở service, GBR-01).
     * Mọi lần sửa thành công đưa lịch về trạng thái "Đã đổi lịch", nên thời gian mới luôn phải ở tương lai.
     */
    public void validateForUpdate(Long interviewId, InterviewScheduleRequest request, InterviewSchedule existingSchedule) {
        if (request == null || existingSchedule == null) {
            throw new IllegalArgumentException("Dữ liệu cập nhật hoặc lịch phỏng vấn hiện tại không hợp lệ.");
        }

        LocalDateTime startTime = request.getStartTime() != null ? request.getStartTime() : existingSchedule.getStartTime();
        LocalDateTime endTime = request.getEndTime() != null ? request.getEndTime() : existingSchedule.getEndTime();

        if (startTime != null && endTime != null) {
            validateDuration(startTime, endTime);
        }

        // Sửa lịch luôn đưa lịch về "Đã đổi lịch" (còn hoạt động): thời gian bắt đầu mới phải ở tương lai
        if (startTime != null && !startTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Thời gian bắt đầu mới của lịch phỏng vấn phải ở trong tương lai.");
        }

        // Kiểm tra trùng lịch của ứng viên (loại trừ chính lịch đang cập nhật)
        Application application = existingSchedule.getApplication();
        if (application != null && application.getCandidate() != null && startTime != null && endTime != null) {
            long candidateConflicts = interviewScheduleRepository.countConflictingSchedulesForCandidate(
                    application.getCandidate().getCandidateId(), startTime, endTime, interviewId);
            if (candidateConflicts > 0) {
                throw new IllegalArgumentException(
                        "Ứng viên này đã có một lịch phỏng vấn khác đang hoạt động trùng vào khoảng thời gian cập nhật.");
            }
        }

        // Kiểm tra trùng lịch hội đồng phỏng vấn (loại trừ chính lịch đang cập nhật)
        Set<Integer> interviewerIds = request.getInterviewerIds();
        if (interviewerIds != null && startTime != null && endTime != null) {
            for (Integer interviewerId : interviewerIds) {
                if (interviewerId != null) {
                    long intConflicts = interviewScheduleRepository.countConflictingSchedulesForInterviewer(
                            interviewerId, startTime, endTime, interviewId);
                    if (intConflicts > 0) {
                        throw new IllegalArgumentException(
                                "Một thành viên hội đồng đã có lịch phỏng vấn khác trùng vào khoảng thời gian cập nhật.");
                    }
                }
            }
        }
    }

    /** endTime phải sau startTime và buổi phỏng vấn dài 15 phút đến 4 giờ. */
    private static void validateDuration(LocalDateTime startTime, LocalDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu.");
        }
        Duration duration = Duration.between(startTime, endTime);
        if (duration.compareTo(MIN_DURATION) < 0) {
            throw new IllegalArgumentException("Buổi phỏng vấn phải kéo dài ít nhất 15 phút.");
        }
        if (duration.compareTo(MAX_DURATION) > 0) {
            throw new IllegalArgumentException("Buổi phỏng vấn không được kéo dài quá 4 giờ.");
        }
    }

    /**
     * Xác định trạng thái thời gian hiển thị (View helper) dựa trên mốc thời gian thực tế.
     */
    public String resolveTimeStatusDisplay(InterviewSchedule schedule) {
        if (schedule == null || schedule.getInterviewStatus() == null) {
            return "N/A";
        }
        if (schedule.getInterviewStatus() == InterviewStatus.Cancelled) {
            return "Đã hủy";
        }
        if (schedule.getInterviewStatus() == InterviewStatus.Completed) {
            return "Đã hoàn thành";
        }
        LocalDateTime now = LocalDateTime.now();
        if (schedule.getStartTime() != null && now.isBefore(schedule.getStartTime())) {
            return "Sắp tới";
        }
        if (schedule.getStartTime() != null && schedule.getEndTime() != null
                && !now.isBefore(schedule.getStartTime()) && !now.isAfter(schedule.getEndTime())) {
            return "Đang diễn ra";
        }
        if (schedule.getEndTime() != null && now.isAfter(schedule.getEndTime())) {
            return "Quá hạn (Cần cập nhật)";
        }
        return schedule.getInterviewStatus().getDisplayName();
    }
}
