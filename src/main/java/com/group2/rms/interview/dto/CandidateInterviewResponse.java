package com.group2.rms.interview.dto;

import com.group2.rms.interview.entity.InterviewFormat;
import com.group2.rms.interview.entity.InterviewStatus;

import java.net.URI;
import java.time.LocalDateTime;

/** Candidate-facing projection. Deliberately excludes panel, evaluations and internal notes. */
public record CandidateInterviewResponse(
        Long interviewId,
        String jobPostingTitle,
        LocalDateTime startTime,
        LocalDateTime endTime,
        InterviewFormat interviewFormat,
        InterviewStatus interviewStatus,
        String locationOrLink) {

    public String meetingLink() {
        if (interviewFormat != InterviewFormat.Online_GoogleMeet || locationOrLink == null) {
            return null;
        }
        try {
            URI uri = URI.create(locationOrLink.trim());
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null ? uri.toASCIIString() : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
