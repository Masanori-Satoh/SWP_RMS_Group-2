package com.group2.rms.interview.repository;

import com.group2.rms.interview.entity.InterviewPanel;
import com.group2.rms.interview.entity.InterviewPanelId;
import com.group2.rms.interview.entity.RoleInPanel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewPanelRepository extends JpaRepository<InterviewPanel, InterviewPanelId> {

    /**
     * Lấy toàn bộ danh sách thành viên Hội đồng phỏng vấn của một buổi phỏng vấn cụ thể,
     * đồng thời JOIN FETCH thông tin Interviewer (User) để tránh N+1 query.
     */
    @Query("SELECT p FROM InterviewPanel p " +
           "JOIN FETCH p.interviewer u " +
           "WHERE p.id.interviewId = :interviewId " +
           "ORDER BY p.roleInPanel ASC")
    List<InterviewPanel> findByInterviewIdWithInterviewer(@Param("interviewId") Long interviewId);

    /**
     * Lấy toàn bộ phân công của một Interviewer cụ thể, đồng thời JOIN FETCH lịch phỏng vấn (InterviewSchedule).
     */
    @Query("SELECT p FROM InterviewPanel p " +
           "JOIN FETCH p.interviewSchedule s " +
           "WHERE p.id.interviewerId = :interviewerId " +
           "ORDER BY s.startTime DESC")
    List<InterviewPanel> findByInterviewerIdWithSchedule(@Param("interviewerId") Integer interviewerId);

    /**
     * Kiểm tra nhanh sự tồn tại của phân công giữa Interview và Interviewer thông qua Composite ID.
     */
    boolean existsById_InterviewIdAndId_InterviewerId(Long interviewId, Integer interviewerId);

    /**
     * Xóa toàn bộ hội đồng phỏng vấn khi cần cấu hình lại danh sách người tham gia.
     */
    @Modifying
    @Query("DELETE FROM InterviewPanel p WHERE p.id.interviewId = :interviewId")
    void deleteByInterviewId(@Param("interviewId") Long interviewId);

    /**
     * Lọc danh sách thành viên hội đồng theo vai trò (HR hoặc HM).
     */
    List<InterviewPanel> findById_InterviewIdAndRoleInPanel(Long interviewId, RoleInPanel roleInPanel);
}
