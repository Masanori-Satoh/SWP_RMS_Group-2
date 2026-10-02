package com.group2.rms.requisition.dto;

import java.time.LocalDateTime;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalResponse {
    private Integer stepNumber;
    private String approverName;
    private String status;
    private LocalDateTime approvalDate;
    private String comments;
}
