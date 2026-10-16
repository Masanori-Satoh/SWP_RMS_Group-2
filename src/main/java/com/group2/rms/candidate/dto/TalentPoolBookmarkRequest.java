package com.group2.rms.candidate.dto;

import jakarta.validation.constraints.Size;

/** Optional shared note when saving a candidate from an application. */
public record TalentPoolBookmarkRequest(
        @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự.") String content) {
}
