package com.group2.rms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 3.15 InterviewSchedule - Lịch phỏng vấn được HR tạo.
 * InterviewRound: Round 1 - HR, Round 2 - Technical, Final
 * InterviewFormat: Online_GoogleMeet, Offline_Office
 * InterviewStatus: Scheduled, Completed, Cancelled, Rescheduled
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
    private Integer interviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ApplicationId", nullable = false, referencedColumnName = "ApplicationId")
    private Application application;

    /**
     * Round 1 - HR, Round 2 - Technical, Final
     */
    @Column(name = "InterviewRound", nullable = false, length = 50)
    private String interviewRound;

    /**
     * Online_GoogleMeet, Offline_Office
     */
    @Column(name = "InterviewFormat", nullable = false, length = 30)
    private String interviewFormat;

    @Column(name = "StartTime", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "EndTime", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "LocationOrLink", length = 500)
    private String locationOrLink;

    /**
     * Scheduled, Completed, Cancelled, Rescheduled
     */
    @Column(name = "InterviewStatus", nullable = false, length = 20)
    private String interviewStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedBy", nullable = false, referencedColumnName = "UserId")
    private User createdBy;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onPersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
