package com.group2.rms.dashboard.internal.dto;

/*
 * DTO: một dòng "Tiến độ tuyển theo vị trí" của Hiring Manager (đã tuyển / cần tuyển và tình trạng ứng viên).
 */
public record HiringProgressItem(
        String code,
        String title,
        long hired,
        long needed,
        String stateLabel,
        String stateTone,
        String pipelineText,
        String href) {
}
