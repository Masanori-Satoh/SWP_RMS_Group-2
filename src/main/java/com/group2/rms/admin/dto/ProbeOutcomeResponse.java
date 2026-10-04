package com.group2.rms.admin.dto;

public record ProbeOutcomeResponse(boolean success,
        String detail,
        long elapsedMs) {

}
