package com.group2.rms.candidate.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dữ liệu gửi cho {@link AiScreeningClient} để chấm một đơn.
 *
 * @param cvUrl    giá trị {@code Application.AppliedCvUrl}
 * @param criteria tiêu chí sàng lọc của requisition; có thể rỗng
 */
public record AiScreeningInput(Integer applicationId, String cvUrl, String jobTitle, List<Criterion> criteria) {

    public AiScreeningInput {
        criteria = List.copyOf(criteria);
    }

    /** Một dòng {@code ScreeningCriteria}; {@code mandatory} là cờ {@code IsMandatory}. */
    public record Criterion(String name, String type, String requiredValue, BigDecimal weight, boolean mandatory) {
    }
}
