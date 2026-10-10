package com.group2.rms.candidate.service;

import java.math.BigDecimal;

/**
 * Kết quả chấm của {@link AiScreeningClient}.
 *
 * @param matchScore điểm phù hợp 0.00–100.00, lưu vào {@code AIScreeningResult.AIMatchScore}
 */
public record AiScreeningOutcome(BigDecimal matchScore) {
}
