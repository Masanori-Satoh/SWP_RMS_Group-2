package com.group2.rms.interview.service;

import com.group2.rms.interview.dto.InterviewScheduleRequest;
import com.group2.rms.interview.entity.InterviewSchedule;
import com.group2.rms.interview.entity.InterviewStatus;
import com.group2.rms.interview.exception.InterviewStatusException;
import com.group2.rms.interview.repository.InterviewScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Service chuyên trách kiểm tra các ràng buộc về thời gian và tính hợp lệ của trạng thái
 * cho toàn bộ quy trình Xem, Tạo mới và Cập nhật Lịch phỏng vấn.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewTimeValidationService {

    private final InterviewScheduleRepository interviewScheduleRepository;

    /**
     * Kiểm tra toàn bộ ràng buộc thời gian và trạng thái khi TẠO MỚI lịch phỏng vấn.
     * Quy tắc:
     * 1. MSG26: endTime > startTime.
     * 2. Trạng thái tạo mới: Chỉ cho phép Scheduled (Đã lên lịch) hoặc Completed (Đã hoàn thành).
     *    Tuyệt đối không cho phép tạo mới với trạng thái Rescheduled (Đã đổi lịch) hoặc Cancelled (Đã hủy).
     * 3. Nếu Scheduled: startTime phải ở trong tương lai.
     * 4. Kiểm tra xung đột trùng lịch: Ứng viên và người phỏng vấn không được có lịch khác trùng giờ.
     */
    public void validateForCreate(InterviewScheduleRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu yêu cầu lịch phỏng vấn không được để trống.");
        }

        LocalDateTime startTime = request.getStartTime();
        LocalDateTime endTime = request.getEndTime();

        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Thời gian bắt đầu và kết thúc phỏng vấn không được để trống.");
        }

        // 1. Rule MSG26: endTime > startTime
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Lỗi Rule MSG26: Thời gian kết thúc (" + endTime +
                    ") phải lớn hơn thời gian bắt đầu (" + startTime + ").");
        }

        // 2. Ràng buộc trạng thái khi tạo mới: Chỉ Scheduled hoặc Completed
        InterviewStatus status = request.getInterviewStatus();
        if (status == null) {
            status = InterviewStatus.Scheduled;
        }

        if (status == InterviewStatus.Rescheduled || status == InterviewStatus.Cancelled) {
            throw new InterviewStatusException(
                    "Quy tắc nghiệp vụ: Không thể tạo mới một lịch phỏng vấn với trạng thái '" +
                    status.getDisplayName() + "'. Lịch mới chỉ có thể tạo ở trạng thái 'Đã lên lịch' hoặc 'Đã hoàn thành'.");
        }

        // 3. Nếu là Scheduled: startTime phải ở trong tương lai
        if (status == InterviewStatus.Scheduled && !startTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Lịch phỏng vấn mới ở trạng thái 'Đã lên lịch' phải có thời gian bắt đầu ở trong tương lai.");
        }

        // 4. Kiểm tra trùng lịch ứng viên
        if (request.getApplicationId() != null) {
            long appConflicts = interviewScheduleRepository.countConflictingSchedulesForApplication(
                    request.getApplicationId(), startTime, endTime, null);
            if (appConflicts > 0) {
                throw new IllegalArgumentException(
                        "Hồ sơ ứng viên này đã có một lịch phỏng vấn khác đang hoạt động trùng vào khoảng thời gian đã chọn.");
            }
        }

        // 5. Kiểm tra trùng lịch hội đồng phỏng vấn
        Set<Integer> interviewerIds = request.getInterviewerIds();
        if (interviewerIds != null) {
            for (Integer interviewerId : interviewerIds) {
                if (interviewerId != null) {
                    long intConflicts = interviewScheduleRepository.countConflictingSchedulesForInterviewer(
                            interviewerId, startTime, endTime, null);
                    if (intConflicts > 0) {
                        throw new IllegalArgumentException(
                                "Thành viên hội đồng (ID: " + interviewerId +
                                ") đã có một lịch phỏng vấn khác trùng vào khoảng thời gian này.");
                    }
                }
            }
        }
    }

    /**
     * Kiểm tra toàn bộ ràng buộc thời gian và trạng thái khi CẬP NHẬT lịch phỏng vấn.
     */
    public void validateForUpdate(Long interviewId, InterviewScheduleRequest request, InterviewSchedule existingSchedule) {
        if (request == null || existingSchedule == null) {
            throw new IllegalArgumentException("Dữ liệu cập nhật hoặc lịch phỏng vấn hiện tại không hợp lệ.");
        }

        LocalDateTime startTime = request.getStartTime() != null ? request.getStartTime() : existingSchedule.getStartTime();
        LocalDateTime endTime = request.getEndTime() != null ? request.getEndTime() : existingSchedule.getEndTime();

        // 1. Rule MSG26: endTime > startTime
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Lỗi Rule MSG26: Thời gian kết thúc (" + endTime +
                    ") phải lớn hơn thời gian bắt đầu (" + startTime + ").");
        }

        // 2. Rule GBR-01: Kiểm tra trạng thái chỉ đi tiến, không lùi
        InterviewStatus targetStatus = request.getInterviewStatus();
        if (targetStatus != null && !existingSchedule.getInterviewStatus().canTransitionTo(targetStatus)) {
            throw new InterviewStatusException(existingSchedule.getInterviewStatus(), targetStatus,
                    "Business Rule GBR-01: Không thể chuyển trạng thái từ [" +
                    existingSchedule.getInterviewStatus() + "] về [" + targetStatus + "]. Trạng thái chỉ đi tiến, không lùi.");
        }

        // 3. Nếu chuyển sang Rescheduled: thời gian mới phải ở trong tương lai
        if (targetStatus == InterviewStatus.Rescheduled && startTime != null && !startTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Khi đổi lịch phỏng vấn (Đã đổi lịch), thời gian bắt đầu mới phải ở trong tương lai.");
        }

        // 4. Kiểm tra trùng lịch ứng viên (loại trừ chính lịch đang cập nhật)
        if (existingSchedule.getApplication() != null && startTime != null && endTime != null) {
            long appConflicts = interviewScheduleRepository.countConflictingSchedulesForApplication(
                    existingSchedule.getApplication().getApplicationId(), startTime, endTime, interviewId);
            if (appConflicts > 0) {
                throw new IllegalArgumentException(
                        "Hồ sơ ứng viên này đã có một lịch phỏng vấn khác đang hoạt động trùng vào khoảng thời gian cập nhật.");
            }
        }

        // 5. Kiểm tra trùng lịch hội đồng phỏng vấn (loại trừ chính lịch đang cập nhật)
        Set<Integer> interviewerIds = request.getInterviewerIds();
        if (interviewerIds != null && startTime != null && endTime != null) {
            for (Integer interviewerId : interviewerIds) {
                if (interviewerId != null) {
                    long intConflicts = interviewScheduleRepository.countConflictingSchedulesForInterviewer(
                            interviewerId, startTime, endTime, interviewId);
                    if (intConflicts > 0) {
                        throw new IllegalArgumentException(
                                "Thành viên hội đồng (ID: " + interviewerId +
                                ") đã có một lịch phỏng vấn khác trùng vào khoảng thời gian cập nhật.");
                    }
                }
            }
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
