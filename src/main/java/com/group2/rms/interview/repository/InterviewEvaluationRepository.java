package com.group2.rms.interview.repository;

import com.group2.rms.interview.entity.InterviewEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Query trả cả phiếu cũ của người đã bị gỡ. Khi xét đủ phiếu,
 * service phải đối chiếu người nộp với hội đồng hiện tại.
 */
@Repository
public interface InterviewEvaluationRepository extends JpaRepository<InterviewEvaluation, Integer> {

    /**
     * Chặn trùng phiếu: mỗi thành viên hội đồng chỉ nộp một phiếu cho một lịch phỏng vấn.
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END " +
           "FROM InterviewEvaluation e " +
           "WHERE e.interviewSchedule.interviewId = :interviewId " +
           "AND e.interviewer.userId = :interviewerId")
    boolean existsByScheduleIdAndInterviewerId(
            @Param("interviewId") Long interviewId,
            @Param("interviewerId") Integer interviewerId);

    /**
     * Lấy toàn bộ phiếu đánh giá của một lịch, JOIN FETCH người đánh giá để tránh N+1.
     * Thứ tự từ thời điểm nộp sớm đến muộn; query không tự giới hạn chỉ phiếu của người đang xem.
     * Không dùng số lượng list trực tiếp làm số thành viên hội đồng đã nộp nếu hội đồng từng được thay đổi.
     */
    @Query("SELECT e FROM InterviewEvaluation e " +
           "JOIN FETCH e.interviewer u " +
           "WHERE e.interviewSchedule.interviewId = :interviewId " +
           "ORDER BY e.evaluatedAt ASC")
    List<InterviewEvaluation> findByScheduleIdWithInterviewer(@Param("interviewId") Long interviewId);

    /**
     * Phiếu của một người trên nhiều lịch, phục vụ nhãn chấm điểm cá nhân trên trang danh sách.
     * Đọc theo lô để tránh một truy vấn riêng cho mỗi thẻ lịch.
     */
    List<InterviewEvaluation> findByInterviewer_UserIdAndInterviewSchedule_InterviewIdIn(
            Integer interviewerId, java.util.Collection<Long> interviewIds);
}
