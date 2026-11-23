package com.group2.rms.interview.entity;

import com.group2.rms.candidate.entity.Application;
import com.group2.rms.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


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
    @JdbcTypeCode(SqlTypes.INTEGER)
    private Long interviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ApplicationId", nullable = false, referencedColumnName = "ApplicationId")
    private Application application;

    /* DB lưu chuỗi đúng CHECK constraint; các accessor bên dưới chuyển sang enum cho tầng nghiệp vụ. */
    @Column(name = "InterviewFormat", nullable = false, length = 30)
    private String interviewFormat;

    @Column(name = "StartTime", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "EndTime", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "LocationOrLink", length = 500)
    private String locationOrLink;

    @Column(name = "InterviewStatus", nullable = false, length = 30)
    private String interviewStatus = "Scheduled";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedBy", nullable = false, referencedColumnName = "UserId")
    private User createdBy;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /* Lịch sở hữu vòng đời hội đồng: bỏ phần tử khỏi collection sẽ xóa dòng liên kết khi flush. */
    @OneToMany(mappedBy = "interviewSchedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<InterviewPanel> interviewPanels = new HashSet<>();

    public Set<InterviewPanel> getInterviewPanels() {
        if (this.interviewPanels == null) {
            this.interviewPanels = new HashSet<>();
        }
        return this.interviewPanels;
    }

    public InterviewFormat getInterviewFormat() {
        /* Dữ liệu chuỗi không khớp enum trả null để tầng gọi xử lý thay vì làm hỏng quá trình dựng trang. */
        if (this.interviewFormat == null) {
            return null;
        }
        try {
            return InterviewFormat.valueOf(this.interviewFormat);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void setInterviewFormat(InterviewFormat interviewFormat) {
        this.interviewFormat = (interviewFormat != null) ? interviewFormat.name() : null;
    }

    public void setInterviewFormat(String interviewFormat) {
        this.interviewFormat = interviewFormat;
    }

    public InterviewStatus getInterviewStatus() {
        /* Chuỗi trạng thái cũ/không hợp lệ không được coi là một trạng thái nghiệp vụ hợp lệ. */
        if (this.interviewStatus == null) {
            return null;
        }
        try {
            return InterviewStatus.valueOf(this.interviewStatus);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void setInterviewStatus(InterviewStatus interviewStatus) {
        this.interviewStatus = (interviewStatus != null) ? interviewStatus.name() : null;
    }

    public void setInterviewStatus(String interviewStatus) {
        this.interviewStatus = interviewStatus;
    }

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
        /* Kiểm tra ở vòng đời JPA để cả những luồng lưu ngoài service vẫn giữ quy tắc EndTime > StartTime. */
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
        InterviewStatus current = getInterviewStatus();
        if (current == null || !current.canTransitionTo(targetStatus)) {
            throw new IllegalStateException("Business Rule GBR-01 Violation: Không thể chuyển trạng thái từ [" +
                    this.interviewStatus + "] sang [" + targetStatus + "]. Trạng thái chỉ được đi tiến, không được đi lùi.");
        }
        this.interviewStatus = targetStatus.name();
    }

    /**
     * Tiện ích thêm người phỏng vấn vào Hội đồng (đồng bộ quan hệ 2 chiều).
     */
    public void addPanelMember(User interviewer, RoleInPanel roleInPanel) {
        /* Khóa ghép và @MapsId gắn mỗi tài khoản với lịch; gán cả chủ lịch và người phỏng vấn trên bản ghi nối. */
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
        if (this.interviewPanels == null) {
            this.interviewPanels = new HashSet<>();
        }
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

    /* Builder nhận cả enum và chuỗi để các nơi khởi tạo dùng cùng cách lưu giá trị với setter của entity. */
    public static class InterviewScheduleBuilder {
        public InterviewScheduleBuilder interviewFormat(InterviewFormat interviewFormat) {
            this.interviewFormat = (interviewFormat != null) ? interviewFormat.name() : null;
            return this;
        }

        public InterviewScheduleBuilder interviewFormat(String interviewFormat) {
            this.interviewFormat = interviewFormat;
            return this;
        }

        public InterviewScheduleBuilder interviewStatus(InterviewStatus interviewStatus) {
            this.interviewStatus = (interviewStatus != null) ? interviewStatus.name() : null;
            return this;
        }

        public InterviewScheduleBuilder interviewStatus(String interviewStatus) {
            this.interviewStatus = interviewStatus;
            return this;
        }
    }
}
