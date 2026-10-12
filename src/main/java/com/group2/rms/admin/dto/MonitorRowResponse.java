package com.group2.rms.admin.dto;

import java.time.LocalDateTime;
//row data display  dashboard page
public record MonitorRowResponse(
               
                String id,
        
                String name,
                String category,
                String status,
                Long responseTimeMs,
                LocalDateTime lastCheckedAt) {

}
