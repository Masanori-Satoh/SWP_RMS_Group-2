package com.group2.rms.candidate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.SplittableRandom;

/**
 * Bản giả lập: không đọc CV, không xét tiêu chí. Điểm phân bố hình chuông quanh 70 (giống dữ liệu seed)
 * và cố định theo {@code applicationId}, nên chấm lại cùng một đơn luôn ra cùng một điểm.
 */
@Slf4j
@Component
public class StubAiScreeningClient implements AiScreeningClient {

    static final double MEAN = 70;
    static final double STD_DEV = 14;
    static final double MIN_SCORE = 35;
    static final double MAX_SCORE = 95;

    @Override
    public AiScreeningOutcome score(AiScreeningInput input) {
        double raw = new SplittableRandom(input.applicationId().longValue()).nextGaussian(MEAN, STD_DEV);
        BigDecimal score = BigDecimal.valueOf(Math.clamp(raw, MIN_SCORE, MAX_SCORE))
                .setScale(2, RoundingMode.HALF_UP);
        log.info("[AI stub] Application {} scored {} (giả lập, không đọc CV)", input.applicationId(), score);
        return new AiScreeningOutcome(score);
    }
}
