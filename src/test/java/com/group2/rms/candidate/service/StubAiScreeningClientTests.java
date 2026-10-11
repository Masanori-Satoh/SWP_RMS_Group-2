package com.group2.rms.candidate.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bản giả lập phải ổn định (cùng đơn cùng điểm) và có phân bố giống dữ liệu seed.
 */
class StubAiScreeningClientTests {

    private static final BigDecimal MIN = BigDecimal.valueOf(StubAiScreeningClient.MIN_SCORE);
    private static final BigDecimal MAX = BigDecimal.valueOf(StubAiScreeningClient.MAX_SCORE);

    private final StubAiScreeningClient client = new StubAiScreeningClient();

    @Test
    void sameApplicationAlwaysGetsSameScore() {
        BigDecimal first = scoreOf(42);

        assertEquals(first, scoreOf(42));
        assertEquals(first, new StubAiScreeningClient().score(input(42)).matchScore());
    }

    @Test
    void scoresStayInRangeWithTwoDecimals() {
        IntStream.rangeClosed(1, 2000).mapToObj(this::scoreOf).forEach(score -> {
            assertEquals(2, score.scale());
            assertTrue(score.compareTo(MIN) >= 0 && score.compareTo(MAX) <= 0, () -> "out of range: " + score);
        });
    }

    @Test
    void scoresSpreadAroundSeventyLikeSeedData() {
        double[] scores = IntStream.rangeClosed(1, 2000).mapToDouble(id -> scoreOf(id).doubleValue()).toArray();
        double average = Arrays.stream(scores).average().orElseThrow();
        long distinct = Arrays.stream(scores).distinct().count();

        assertTrue(average > 67 && average < 73, () -> "average: " + average);
        assertTrue(distinct > 1500, () -> "distinct scores: " + distinct);
    }

    private BigDecimal scoreOf(int applicationId) {
        return client.score(input(applicationId)).matchScore();
    }

    private static AiScreeningInput input(int applicationId) {
        return new AiScreeningInput(applicationId, "local:cv/1/a.pdf", "Java Developer", List.of());
    }
}
