package com.group2.rms.candidate.talentpool.dto;
// set data to send email; html = null thì gửi thư văn bản thuần (body), có html thì gửi thư nhiều phần (body + html)
public record RecruitmentEmailMessage(
        String entity,
        Number id,
        String kind,
        String to,
        String subject,
        String body,
        String html) {

    /** Thư văn bản thuần (không có bản HTML). */
    public RecruitmentEmailMessage(String entity, Number id, String kind, String to, String subject, String body) {
        this(entity, id, kind, to, subject, body, null);
    }
}
