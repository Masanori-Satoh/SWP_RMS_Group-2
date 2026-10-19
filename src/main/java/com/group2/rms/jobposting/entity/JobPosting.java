package com.group2.rms.jobposting.entity;

import com.group2.rms.core.base.BaseEntity;
import com.group2.rms.jobposting.service.JobPostingAccess;
import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 3.10 JobPosting - Tin tuyển dụng công khai (từ JobRequisition đã được duyệt).
 * PostingStatus: Draft, Published, Paused, Closed
 */
@Entity
@Table(name = "JobPosting")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobPosting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "JobPostingId")
    private Integer jobPostingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RequisitionId", nullable = false, referencedColumnName = "RequisitionId")
    private JobRequisition requisition;

    /**
     * Lần đăng thứ mấy của requisition nguồn (UQ_JobPosting_Requisition_PostingNo).
     */
    @Column(name = "PostingNo", nullable = false)
    private Integer postingNo;

    @Column(name = "PostingTitle", nullable = false, length = 200)
    private String postingTitle;

    @Column(name = "JobDescription", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String jobDescription;

    @Column(name = "JobRequirements", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String jobRequirements;

    @Column(name = "Benefits", columnDefinition = "NVARCHAR(MAX)")
    private String benefits;

    @Column(name = "SalaryDisplay", length = 100)
    private String salaryDisplay;

    /**
     * Số lượng hiển thị trên tin công khai; NULL = không hiển thị.
     */
    @Column(name = "NumberOfPositionsDisplay", length = 50)
    private String numberOfPositionsDisplay;

    @Column(name = "WorkLocation", length = 255)
    private String workLocation;

    @Column(name = "PostingDate")
    private LocalDateTime postingDate;

    @Column(name = "ApplicationDeadline")
    private LocalDateTime applicationDeadline;

    /**
     * Draft, Published, Paused, Closed
     */
    @Column(name = "PostingStatus", nullable = false, length = 20)
    private String postingStatus;

    @Column(name = "ClosedAt")
    private LocalDateTime closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedBy", nullable = false, referencedColumnName = "UserId")
    private User createdBy;

    /**
     * Ứng viên thấy được tin này chưa: đang đăng (Published) VÀ đã tới giờ đăng.
     * Tin hẹn đăng (PostingDate ở tương lai) chưa hiện trên trang tuyển dụng, chưa nộp hồ sơ được.
     */
    public boolean isVisibleToCandidates(LocalDateTime now) {
        return JobPostingAccess.STATUS_PUBLISHED.equals(postingStatus) && (postingDate == null || !postingDate.isAfter(now));
    }
}
