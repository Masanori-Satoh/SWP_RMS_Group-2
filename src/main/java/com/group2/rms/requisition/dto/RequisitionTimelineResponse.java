package com.group2.rms.requisition.dto;

public record RequisitionTimelineResponse(String action,String actor,java.time.LocalDateTime occurredAt,String description) {}
