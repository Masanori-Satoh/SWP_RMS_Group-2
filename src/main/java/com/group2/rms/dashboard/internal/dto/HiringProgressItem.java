package com.group2.rms.dashboard.internal.dto;

/*
 * DTO: một thanh chồng "Tiến độ tuyển theo vị trí" của Hiring Manager (đã tuyển / cần tuyển và tình trạng ứng viên).
 */
public record HiringProgressItem(
        String code,
        String title,
        long hired,
        long needed,
        String pipelineText,
        String href) {

    /** Phần "Còn thiếu" của thanh chồng; tuyển dư thì 0. */
    public long missing() {
        return Math.max(needed - hired, 0);
    }
}
