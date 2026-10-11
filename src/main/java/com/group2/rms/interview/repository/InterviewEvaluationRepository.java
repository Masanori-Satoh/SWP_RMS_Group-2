package com.group2.rms.interview.repository;

import com.group2.rms.interview.entity.InterviewEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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
     */
    @Query("SELECT e FROM InterviewEvaluation e " +
           "JOIN FETCH e.interviewer u " +
           "WHERE e.interviewSchedule.interviewId = :interviewId " +
           "ORDER BY e.evaluatedAt ASC")
    List<InterviewEvaluation> findByScheduleIdWithInterviewer(@Param("interviewId") Long interviewId);
}
