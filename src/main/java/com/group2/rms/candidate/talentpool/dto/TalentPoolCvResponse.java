package com.group2.rms.candidate.talentpool.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

// List CV
public record TalentPoolCvResponse(
        Integer applicationId,
        String jobTitle,
        LocalDateTime submittedAt,
        String department,
        String employmentType) {

    public TalentPoolCvResponse(Integer applicationId, String jobTitle, LocalDateTime submittedAt) {
        this(applicationId, jobTitle, submittedAt, null, null);
    }

    // relative label shown under the submitted date on the profile page
    public String submittedAgo() {
        if (submittedAt == null)
            return null;
        long days = ChronoUnit.DAYS.between(submittedAt.toLocalDate(), LocalDate.now());
        if (days <= 0)
            return "Hôm nay";
        if (days == 1)
            return "Hôm qua";
        if (days < 30)
            return days + " ngày trước";
        if (days < 365)
            return (days / 30) + " tháng trước";
        return (days / 365) + " năm trước";
    }
}
