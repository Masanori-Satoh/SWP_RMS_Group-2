package com.group2.rms.interview;

import com.group2.rms.interview.dto.InterviewScheduleRequest;
import com.group2.rms.interview.dto.InterviewScheduleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface quản lý nghiệp vụ Lên lịch phỏng vấn (Interview Scheduling - Iteration 1).
 */
public interface InterviewSchedulingService {

    /**
     * Tạo mới một lịch phỏng vấn (Dành cho HR).
     *
     * @param request  Dữ liệu yêu cầu tạo lịch
     * @param hrUserId ID của tài khoản HR đang đăng nhập thực hiện thao tác
     * @return Thông tin chi tiết lịch phỏng vấn vừa tạo
     */
    InterviewScheduleResponse createSchedule(InterviewScheduleRequest request, Integer hrUserId);

    /**
     * Cập nhật thông tin lịch phỏng vấn (Dành cho HR).
     * Tuân thủ Business Rule GBR-01 (không được chuyển lùi trạng thái).
     *
     * @param interviewId ID của lịch phỏng vấn cần cập nhật
     * @param request     Dữ liệu cập nhật mới
     * @param hrUserId    ID của tài khoản HR thực hiện thao tác
     * @return Thông tin chi tiết lịch phỏng vấn sau khi cập nhật
     */
    InterviewScheduleResponse updateSchedule(Long interviewId, InterviewScheduleRequest request, Integer hrUserId);

    /**
     * Hủy lịch phỏng vấn (Dành cho HR).
     * Chuyển trạng thái sang Cancelled tuân thủ Business Rule GBR-01.
     *
     * @param interviewId ID của lịch phỏng vấn cần hủy
     * @param reason      Lý do hủy buổi phỏng vấn
     * @param hrUserId    ID của tài khoản HR thực hiện thao tác
     * @return Thông tin chi tiết lịch phỏng vấn sau khi hủy
     */
    InterviewScheduleResponse cancelSchedule(Long interviewId, String reason, Integer hrUserId);

    /**
     * Lấy thông tin chi tiết một lịch phỏng vấn theo ID (Dành cho HR).
     *
     * @param interviewId ID lịch phỏng vấn
     * @return Thông tin chi tiết đầy đủ
     */
    InterviewScheduleResponse getScheduleDetail(Long interviewId);

    /**
     * Lấy danh sách tất cả lịch phỏng vấn (Dành cho HR).
     * Tránh lỗi N+1 Query thông qua JOIN FETCH.
     *
     * @return Danh sách lịch phỏng vấn
     */
    List<InterviewScheduleResponse> getAllForHR();

    /**
     * Lấy danh sách lịch phỏng vấn có phân trang (Dành cho HR).
     *
     * @param pageable Thông tin phân trang
     * @return Trang danh sách lịch phỏng vấn
     */
    Page<InterviewScheduleResponse> getAllForHRPaged(Pageable pageable);

    /**
     * Lấy danh sách lịch phỏng vấn theo một hồ sơ ứng tuyển Application (Dành cho HR).
     *
     * @param applicationId ID của hồ sơ ứng tuyển
     * @return Danh sách các buổi phỏng vấn của hồ sơ đó
     */
    List<InterviewScheduleResponse> getSchedulesByApplicationForHR(Integer applicationId);

    /**
     * Lấy danh sách lịch phỏng vấn mà Interviewer được phân công (Dành cho Interviewer).
     * Tuân thủ nghiêm ngặt Business Rule GBR-05: Chỉ xem những lịch được phân công rõ ràng.
     *
     * @param interviewerId ID người dùng Interviewer
     * @return Danh sách lịch phỏng vấn được phân công
     */
    List<InterviewScheduleResponse> getMySchedulesForInterviewer(Integer interviewerId);

    /**
     * Lấy danh sách lịch phỏng vấn được phân công có phân trang (Dành cho Interviewer - Rule GBR-05).
     *
     * @param interviewerId ID người dùng Interviewer
     * @param pageable      Thông tin phân trang
     * @return Trang danh sách lịch phỏng vấn
     */
    Page<InterviewScheduleResponse> getMySchedulesForInterviewerPaged(Integer interviewerId, Pageable pageable);

    /**
     * Lấy chi tiết lịch phỏng vấn cho Interviewer (Tuân thủ Rule GBR-05).
     * Nếu Interviewer không nằm trong hội đồng, từ chối truy cập và ném ngoại lệ.
     *
     * @param interviewId   ID lịch phỏng vấn
     * @param interviewerId ID người dùng Interviewer
     * @return Chi tiết lịch phỏng vấn
     */
    InterviewScheduleResponse getMyScheduleDetailForInterviewer(Long interviewId, Integer interviewerId);
}
