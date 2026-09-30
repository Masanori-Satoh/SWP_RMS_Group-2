package com.group2.rms.dto.response;

import java.time.LocalDateTime;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalResponseDto {
    private Integer stepNumber;
    private String approverName;
    private String status;
    private LocalDateTime approvalDate;
    private String comments;
}
