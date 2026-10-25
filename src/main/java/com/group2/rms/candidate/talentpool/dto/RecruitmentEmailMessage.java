package com.group2.rms.candidate.talentpool.dto;
// set data to send email
public record RecruitmentEmailMessage(
        String entity,
        Number id,
        String kind,
        String to,
        String subject,
        String body) {
}
