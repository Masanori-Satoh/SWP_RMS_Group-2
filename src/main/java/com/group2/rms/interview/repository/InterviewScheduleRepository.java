package com.group2.rms.interview.repository;

import com.group2.rms.interview.entity.InterviewSchedule;
import com.group2.rms.interview.entity.InterviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Truy vấn theo phạm vi người xem; query chi tiết theo ID không tự kiểm tra quyền.
 * Danh sách phân trang không fetch collection hội đồng để tránh phân trang trong bộ nhớ.
 */
@Repository
public interface InterviewScheduleRepository extends JpaRepository<InterviewSchedule, Long> {


    // =========================================================================
    // DÀNH CHO HR: Xem tất cả lịch phỏng vấn (Giải pháp xử lý N+1 Query Problem)
    // =========================================================================

    /**
     * HR Query: Lấy toàn bộ danh sách lịch phỏng vấn kèm theo Application, Candidate, JobPosting,
     * CreatedBy, và toàn bộ Hội đồng phỏng vấn (InterviewPanel -> Interviewer).
     * Nạp các quan hệ liệt kê bằng JOIN FETCH để giảm truy vấn phát sinh khi ánh xạ DTO.
     * Không mặc định các quan hệ sâu hơn, ví dụ Candidate.account, đã được fetch nếu không có trong query.
     */
    @Query("SELECT DISTINCT s FROM InterviewSchedule s " +
           "LEFT JOIN FETCH s.application a " +
           "LEFT JOIN FETCH a.candidate c " +
           "LEFT JOIN FETCH a.jobPosting jp " +
           "LEFT JOIN FETCH s.createdBy u " +
           "LEFT JOIN FETCH s.interviewPanels p " +
           "LEFT JOIN FETCH p.interviewer i " +
           "ORDER BY s.startTime DESC")
    List<InterviewSchedule> findAllWithDetailsForHr();

    /**
     * HR Query (Phân trang): Sử dụng @EntityGraph để fetch eager Application và CreatedBy.
     * Tránh tình trạng cảnh báo HHH000104 in-memory pagination của Hibernate khi dùng JOIN FETCH collection với Pageable.
     */
    @EntityGraph(attributePaths = {"application", "application.candidate", "application.jobPosting", "createdBy"})
    @Query("SELECT s FROM InterviewSchedule s ORDER BY s.startTime DESC")
    Page<InterviewSchedule> findAllPagedForHr(Pageable pageable);

    /**
     * HR Query: Lọc lịch phỏng vấn theo trạng thái (có eager fetch Application và CreatedBy).
     */
    @EntityGraph(attributePaths = {"application", "createdBy"})
    List<InterviewSchedule> findByInterviewStatusOrderByStartTimeDesc(String status);

    default List<InterviewSchedule> findByInterviewStatusOrderByStartTimeDesc(InterviewStatus status) {
        /* Tầng service dùng enum, còn thuộc tính JPA lưu String nên chuyển sang name trước khi truy vấn. */
        return findByInterviewStatusOrderByStartTimeDesc(status != null ? status.name() : null);
    }

    /**
     * HR Query: Lấy toàn bộ lịch phỏng vấn theo ApplicationId (kèm Application, Candidate, JobPosting, Panel).
     */
    @Query("SELECT DISTINCT s FROM InterviewSchedule s " +
           "LEFT JOIN FETCH s.application a " +
           "LEFT JOIN FETCH a.candidate c " +
           "LEFT JOIN FETCH a.jobPosting jp " +
           "LEFT JOIN FETCH s.createdBy u " +
           "LEFT JOIN FETCH s.interviewPanels p " +
           "LEFT JOIN FETCH p.interviewer i " +
           "WHERE a.applicationId = :applicationId " +
           "ORDER BY s.startTime DESC")
    List<InterviewSchedule> findByApplicationIdWithDetailsForHr(@Param("applicationId") Integer applicationId);

    /**
     * HR / Detail Query: Xem chi tiết 1 buổi phỏng vấn kèm theo toàn bộ thông tin liên quan (tránh N+1).
     * Dùng trong xem/sửa lịch, nộp phiếu và chốt kết quả; không lọc theo người xem trong query này.
     * Bên gọi kiểm tra quyền sau khi đọc lịch, hoặc dùng query có ràng buộc phân công khi cần giới hạn từ DB.
     */
    @Query("SELECT s FROM InterviewSchedule s " +
           "LEFT JOIN FETCH s.application a " +
           "LEFT JOIN FETCH a.candidate c " +
           "LEFT JOIN FETCH a.jobPosting jp " +
           "LEFT JOIN FETCH s.createdBy u " +
           "LEFT JOIN FETCH s.interviewPanels p " +
           "LEFT JOIN FETCH p.interviewer i " +
           "WHERE s.interviewId = :interviewId")
    Optional<InterviewSchedule> findByIdWithDetails(@Param("interviewId") Long interviewId);

    // =========================================================================
    // DÀNH CHO INTERVIEWER: Tuân thủ Business Rule GBR-05
    // "Interviewers are only allowed to view Applications and Interview Schedules
    // to which they have been explicitly assigned. Không dùng query select all rồi filter bằng Java."
    // =========================================================================

    /**
     * Interviewer Query: CHỈ lấy các lịch phỏng vấn mà người dùng (Interviewer) được phân công tham gia hội đồng.
     * Điều kiện lọc được đẩy xuống tầng Database thông qua JOIN s.interviewPanels myPanel WHERE myPanel.interviewer.userId = :interviewerId.
     * myPanel dùng để chứng minh phân công của người xem, allPanels dùng nạp toàn bộ hội đồng để hiển thị.
     * Không dùng cùng một alias fetch bị lọc theo người xem, vì có thể làm collection hội đồng chỉ còn một người.
     */
    @Query("SELECT DISTINCT s FROM InterviewSchedule s " +
           "JOIN s.interviewPanels myPanel " +
           "LEFT JOIN FETCH s.application a " +
           "LEFT JOIN FETCH a.candidate c " +
           "LEFT JOIN FETCH a.jobPosting jp " +
           "LEFT JOIN FETCH s.createdBy u " +
           "LEFT JOIN FETCH s.interviewPanels allPanels " +
           "LEFT JOIN FETCH allPanels.interviewer i " +
           "WHERE myPanel.interviewer.userId = :interviewerId " +
           "ORDER BY s.startTime DESC")
    List<InterviewSchedule> findAllAssignedToInterviewer(@Param("interviewerId") Integer interviewerId);

    /**
     * Interviewer Query (Phân trang): Dành cho màn hình danh sách của Interviewer kèm phân trang.
     */
    @EntityGraph(attributePaths = {"application", "application.candidate", "application.jobPosting", "createdBy"})
    @Query("SELECT s FROM InterviewSchedule s " +
           "JOIN s.interviewPanels p " +
           "WHERE p.interviewer.userId = :interviewerId " +
           "ORDER BY s.startTime DESC")
    Page<InterviewSchedule> findAssignedToInterviewerPaged(
            @Param("interviewerId") Integer interviewerId,
            Pageable pageable);

    /**
     * Interviewer Query: Xem chi tiết một buổi phỏng vấn NHƯNG CHỈ khi người dùng thực sự nằm trong hội đồng.
     * Ngăn chặn hoàn toàn việc can thiệp URL ID để xem trộm lịch phỏng vấn của người khác (Security Enforcement tại DB).
     */
    @Query("SELECT s FROM InterviewSchedule s " +
           "JOIN s.interviewPanels myPanel " +
           "LEFT JOIN FETCH s.application a " +
           "LEFT JOIN FETCH a.candidate c " +
           "LEFT JOIN FETCH a.jobPosting jp " +
           "LEFT JOIN FETCH s.createdBy u " +
           "LEFT JOIN FETCH s.interviewPanels allPanels " +
           "LEFT JOIN FETCH allPanels.interviewer i " +
           "WHERE s.interviewId = :interviewId AND myPanel.interviewer.userId = :interviewerId")
    Optional<InterviewSchedule> findByIdAndAssignedInterviewer(
            @Param("interviewId") Long interviewId,
            @Param("interviewerId") Integer interviewerId);

    /**
     * Interviewer Query: Lấy các buổi phỏng vấn sắp tới (Upcoming) mà Interviewer được phân công.
     */
    @Query("SELECT DISTINCT s FROM InterviewSchedule s " +
           "JOIN s.interviewPanels p " +
           "LEFT JOIN FETCH s.application a " +
           "WHERE p.interviewer.userId = :interviewerId " +
           "AND s.startTime >= :fromTime " +
           "AND s.interviewStatus IN (:statuses) " +
           "ORDER BY s.startTime ASC")
    List<InterviewSchedule> findUpcomingAssignedInterviews(
            @Param("interviewerId") Integer interviewerId,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("statuses") List<String> statuses);

    default List<InterviewSchedule> findUpcomingAssignedInterviewsWithEnums(
            Integer interviewerId,
            LocalDateTime fromTime,
            List<InterviewStatus> statuses) {
        /* Chuẩn hóa danh sách enum thành giá trị DB; danh sách thiếu được chuyển thành tập lọc rỗng. */
        return findUpcomingAssignedInterviews(interviewerId, fromTime,
                statuses != null ? statuses.stream().map(Enum::name).toList() : List.of());
    }

    /**
     * Kiểm tra quyền bảo mật: Interviewer có được phân công vào lịch phỏng vấn cụ thể này hay không.
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM InterviewPanel p " +
           "WHERE p.interviewSchedule.interviewId = :interviewId " +
           "AND p.interviewer.userId = :interviewerId")
    boolean isInterviewerAssigned(
            @Param("interviewId") Long interviewId,
            @Param("interviewerId") Integer interviewerId);

    // =========================================================================
    // DÀNH CHO CANDIDATE & HIRING MANAGER (PHÒNG BAN) & KIỂM TRA TRÙNG LỊCH
    // =========================================================================

    /**
     * Candidate Query: Chỉ lấy các lịch phỏng vấn của chính ứng viên đang đăng nhập.
     */
    @Query("SELECT DISTINCT s FROM InterviewSchedule s " +
           "LEFT JOIN FETCH s.application a " +
           "LEFT JOIN FETCH a.candidate c " +
           "LEFT JOIN FETCH a.jobPosting jp " +
           "LEFT JOIN FETCH s.createdBy u " +
           "WHERE c.account.userId = :candidateUserId " +
           "ORDER BY s.startTime DESC")
    List<InterviewSchedule> findAllByCandidateUserId(@Param("candidateUserId") Integer candidateUserId);

    /**
     * Hiring Manager Query: Lấy các lịch phỏng vấn thuộc về phòng ban của Hiring Manager.
     * Điều kiện OR lấy cả phòng ban trên tài khoản và các phòng ban mà user đang là manager.
     * Phạm vi này phục vụ danh sách; quyền chốt từng lịch vẫn được RequisitionAccess kiểm tra riêng.
     */
    @Query("SELECT DISTINCT s FROM InterviewSchedule s " +
           "LEFT JOIN FETCH s.application a " +
           "LEFT JOIN FETCH a.candidate c " +
           "LEFT JOIN FETCH a.jobPosting jp " +
           "LEFT JOIN FETCH jp.requisition req " +
           "LEFT JOIN FETCH s.createdBy u " +
           "LEFT JOIN FETCH s.interviewPanels p " +
           "LEFT JOIN FETCH p.interviewer i " +
           "LEFT JOIN req.department d " +
           "LEFT JOIN d.manager m " +
           "WHERE d.departmentId = :departmentId OR m.userId = :managerUserId " +
           "ORDER BY s.startTime DESC")
    List<InterviewSchedule> findAllByDepartmentId(@Param("departmentId") Integer departmentId,
                                                  @Param("managerUserId") Integer managerUserId);

    /**
     * Kiểm tra trùng lịch của Ứng viên (ApplicationId) trong khoảng thời gian [startTime, endTime].
     * Chỉ tính giao nhau thực sự: lịch kết thúc đúng lúc lịch mới bắt đầu không bị coi là trùng.
     * excludeInterviewId loại lịch đang sửa khỏi phép kiểm tra.
     * Ví dụ lịch 09:00–10:00 không trùng 10:00–11:00, nhưng trùng 09:30–10:30.
     * Method này giới hạn trong một hồ sơ; luồng validation chính dùng bản theo CandidateId
     * bên dưới để chặn cả hai hồ sơ khác nhau của cùng một người.
     */
    @Query("SELECT COUNT(s) FROM InterviewSchedule s " +
           "WHERE s.application.applicationId = :applicationId " +
           "AND (:excludeInterviewId IS NULL OR s.interviewId != :excludeInterviewId) " +
           "AND s.interviewStatus IN ('Scheduled', 'Rescheduled') " +
           "AND s.startTime < :endTime AND s.endTime > :startTime")
    long countConflictingSchedulesForApplication(
            @Param("applicationId") Integer applicationId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeInterviewId") Long excludeInterviewId);

    /**
     * Kiểm tra trùng lịch của một Người phỏng vấn (InterviewerId) trong khoảng thời gian [startTime, endTime].
     * Chỉ lịch Scheduled/Rescheduled chiếm chỗ; lịch đã hủy/hoàn thành không chặn lịch mới.
     */
    @Query("SELECT COUNT(s) FROM InterviewSchedule s " +
           "JOIN s.interviewPanels p " +
           "WHERE p.interviewer.userId = :interviewerId " +
           "AND (:excludeInterviewId IS NULL OR s.interviewId != :excludeInterviewId) " +
           "AND s.interviewStatus IN ('Scheduled', 'Rescheduled') " +
           "AND s.startTime < :endTime AND s.endTime > :startTime")
    long countConflictingSchedulesForInterviewer(
            @Param("interviewerId") Integer interviewerId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeInterviewId") Long excludeInterviewId);

    /**
     * Kiểm tra trùng lịch của Ứng viên (theo CandidateId, không chỉ riêng hồ sơ đang xếp) trong khoảng
     * thời gian [startTime, endTime]. Một ứng viên có thể có nhiều hồ sơ (nhiều vị trí) cùng lúc,
     * nên phải kiểm tra theo người, không chỉ theo từng ApplicationId riêng lẻ.
     */
    @Query("SELECT COUNT(s) FROM InterviewSchedule s " +
           "WHERE s.application.candidate.candidateId = :candidateId " +
           "AND (:excludeInterviewId IS NULL OR s.interviewId != :excludeInterviewId) " +
           "AND s.interviewStatus IN ('Scheduled', 'Rescheduled') " +
           "AND s.startTime < :endTime AND s.endTime > :startTime")
    long countConflictingSchedulesForCandidate(
            @Param("candidateId") Integer candidateId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeInterviewId") Long excludeInterviewId);

    /**
     * Các lịch còn hoạt động (Scheduled/Rescheduled) của một hồ sơ.
     * Tạo lịch mới cho hồ sơ này nghĩa là đổi lịch, nên các lịch này sẽ bị thay thế.
     */
    @Query("SELECT s FROM InterviewSchedule s " +
           "WHERE s.application.applicationId = :applicationId " +
           "AND s.interviewStatus IN ('Scheduled', 'Rescheduled')")
    List<InterviewSchedule> findActiveByApplicationId(@Param("applicationId") Integer applicationId);

    /**
     * Đã qua giờ kết thúc mà lịch vẫn còn hoạt động thì chuyển sang Completed.
     * Bao gồm đúng mốc EndTime; đây là cập nhật hàng loạt nên không đi qua callback của từng entity.
     * Chỉ sửa InterviewStatus, không tạo phiếu đánh giá hay InterviewFinalResult.
     * Bên gọi cần transaction ghi. Sau bulk update, entity đã nạp trong persistence context
     * không tự được refresh; vì vậy các luồng chính gọi bước này trước khi đọc lịch cần xử lý.
     * Gọi ở đầu các luồng xem / ghi lịch, trước khi đọc lịch, nên không cần job chạy nền.
     * Không dùng clearAutomatically: nó tách cả tài khoản đăng nhập khỏi session và gây LazyInitializationException.
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE InterviewSchedule s SET s.interviewStatus = 'Completed' " +
           "WHERE s.interviewStatus IN ('Scheduled', 'Rescheduled') AND s.endTime <= :now")
    int completeEndedSchedules(@Param("now") LocalDateTime now);
}
