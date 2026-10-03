package com.group2.rms.admin;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogResponse {
    private Long auditLogId;
    private String action;       // CREATE, UPDATE, DELETE
    private String performedBy;  // Tên người thực hiện
    private String description;  // Mô tả ngắn hành động
    private LocalDateTime timestamp;
}
