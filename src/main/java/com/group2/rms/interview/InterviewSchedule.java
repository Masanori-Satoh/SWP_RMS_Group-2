package com.group2.rms.interview;

import com.group2.rms.candidate.Application;
import com.group2.rms.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 3.14 InterviewSchedule - Lịch phỏng vấn ứng viên được HR tạo.
 * Đã loại bỏ hoàn toàn thuộc tính InterviewRound theo đúng refactor/clean-architecture.
 *
 * Business Rules:
 * - endTime phải lớn hơn startTime.
 * - GBR-01: Trạng thái chỉ đi tiến, không lùi (Forward-only state transitions).
 */
@Entity
@Table(name = "InterviewSchedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "InterviewId")
    private Long interviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ApplicationId", nullable = false, referencedColumnName = "ApplicationId")
    private Application application;

    @Enumerated(EnumType.STRING)
    @Column(name = "InterviewFormat", nullable = false, length = 30)
    private InterviewFormat interviewFormat;

    @Column(name = "StartTime", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "EndTime", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "LocationOrLink", length = 500)
    private String locationOrLink;

    @Enumerated(EnumType.STRING)
    @Column(name = "InterviewStatus", nullable = false, length = 30)
    @Builder.Default
    private InterviewStatus interviewStatus = InterviewStatus.Scheduled;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedBy", nullable = false, referencedColumnName = "UserId")
    private User createdBy;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "interviewSchedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<InterviewPanel> interviewPanels = new HashSet<>();

    /**
     * Ràng buộc nghiệp vụ cấp Entity trước khi lưu vào Database:
     * - Tự động thiết lập createdAt nếu chưa có.
     * - endTime phải lớn hơn startTime.
     */
    @PrePersist
    protected void onPersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        validateScheduleBusinessRules();
    }

    @PreUpdate
    protected void onUpdate() {
        validateScheduleBusinessRules();
    }

    private void validateScheduleBusinessRules() {
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new IllegalStateException("Business Rule Violation: EndTime (" + endTime +
                    ") phải lớn hơn StartTime (" + startTime + ")");
        }
    }

    /**
     * Chuyển trạng thái phỏng vấn tuân thủ nghiêm ngặt Business Rule GBR-01 (Forward-only).
     *
     * @param targetStatus Trạng thái mới cần chuyển đến
     * @throws IllegalStateException nếu vi phạm quy tắc đi tiến không lùi
     */
    public void transitionTo(InterviewStatus targetStatus) {
        if (!this.interviewStatus.canTransitionTo(targetStatus)) {
            throw new IllegalStateException("Business Rule GBR-01 Violation: Không thể chuyển trạng thái từ [" +
                    this.interviewStatus + "] sang [" + targetStatus + "]. Trạng thái chỉ được đi tiến, không được đi lùi.");
        }
        this.interviewStatus = targetStatus;
    }

    /**
     * Tiện ích thêm người phỏng vấn vào Hội đồng (đồng bộ quan hệ 2 chiều).
     */
    public void addPanelMember(User interviewer, RoleInPanel roleInPanel) {
        if (interviewer == null || interviewer.getUserId() == null) {
            throw new IllegalArgumentException("Interviewer không được null và phải có UserId hợp lệ.");
        }
        InterviewPanelId panelId = new InterviewPanelId(this.interviewId, interviewer.getUserId());
        InterviewPanel panel = InterviewPanel.builder()
                .id(panelId)
                .interviewSchedule(this)
                .interviewer(interviewer)
                .roleInPanel(roleInPanel)
                .build();
        this.interviewPanels.add(panel);
    }

    /**
     * Tiện ích gỡ người phỏng vấn khỏi Hội đồng.
     */
    public void removePanelMember(InterviewPanel panel) {
        if (panel != null) {
            this.interviewPanels.remove(panel);
            panel.setInterviewSchedule(null);
        }
    }
}
