package com.group2.rms.requisition.entity;

import com.group2.rms.user.entity.User;

import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="RequisitionWorkflowEvent") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class RequisitionWorkflowEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="EventId") private Long eventId;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="RequisitionId",nullable=false) private JobRequisition requisition;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="ActorId",nullable=false) private User actor;
 @Column(name="EventType",length=20,nullable=false) private String eventType;
 @Column(name="OccurredAt",nullable=false) private java.time.LocalDateTime occurredAt;
 @Column(name="Comment",length=1000) private String comment;
}
