package com.group2.rms.admin.dto;

import java.time.LocalDateTime;

public record MonitorRowResponse(String id,
        String category,
        String name,
        String endpoint,
        String status,
        String statusLabel,
        Long averageResponseMs,
        Integer errorRatePercent,
        Integer recentErrors,
        Integer lastHttpStatus,
        LocalDateTime lastCheckedAt,
        int sampleCount,
        boolean canProbe) {

}
