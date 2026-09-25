package com.group2.rms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

/**
 * Composite Primary Key cho bảng InterviewPanel.
 * Bao gồm: InterviewId + InterviewerId
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class InterviewPanelId implements Serializable {

    @Column(name = "InterviewId")
    private Integer interviewId;

    @Column(name = "InterviewerId")
    private Integer interviewerId;
}
