package com.group2.rms.candidate.application.pipeline.dto;

import com.group2.rms.candidate.domain.rule.ApplicationStage;

import java.util.List;
import java.util.Map;

/**
 * Khối lượng việc của một người cầm bóng, cho dashboard: số hồ sơ mỗi giai đoạn (khớp các tab của /applications)
 * và chỉ những dòng người đó đang cầm bóng.
 *
 * @param stageCounts số hồ sơ mỗi giai đoạn, đủ mọi giai đoạn (giai đoạn trống = 0)
 * @param ownedRows   dòng do người đó cầm bóng theo giai đoạn, đúng thứ tự của danh sách /applications
 */
public record OwnerWorkloadResponse(
        Map<ApplicationStage, Long> stageCounts,
        Map<ApplicationStage, List<ApplicationPipelineResponse>> ownedRows) {

    public long count(ApplicationStage stage) {
        return stageCounts.getOrDefault(stage, 0L);
    }

    public List<ApplicationPipelineResponse> owned(ApplicationStage stage) {
        return ownedRows.getOrDefault(stage, List.of());
    }
}
