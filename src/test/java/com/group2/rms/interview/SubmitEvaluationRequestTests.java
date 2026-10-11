package com.group2.rms.interview;

import com.group2.rms.interview.dto.SubmitEvaluationRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SubmitEvaluationRequestTests {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static Set<String> invalidFields(SubmitEvaluationRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("Phiếu hợp lệ: không có lỗi")
    void validRequest() {
        assertTrue(invalidFields(new SubmitEvaluationRequest(1, 5, 3, "No_Hire", null, null, null)).isEmpty());
    }

    @Test
    @DisplayName("Điểm ngoài 1–5 bị chặn")
    void scoresOutOfRange() {
        assertEquals(Set.of("technicalScore", "softSkillScore"),
                invalidFields(new SubmitEvaluationRequest(6, 0, null, "Hire", null, null, null)));
    }

    @Test
    @DisplayName("Được bỏ trống từng mục điểm (vd HR chỉ chấm kỹ năng mềm / văn hóa)")
    void partialScoresAreAllowed() {
        assertTrue(invalidFields(new SubmitEvaluationRequest(null, 4, 5, "Consider", null, null, null)).isEmpty());
        assertTrue(invalidFields(new SubmitEvaluationRequest(null, null, 3, "Hire", null, null, null)).isEmpty());
    }

    @Test
    @DisplayName("Không chấm mục nào: lỗi anyScoreGiven")
    void atLeastOneScoreIsRequired() {
        assertEquals(Set.of("anyScoreGiven"),
                invalidFields(new SubmitEvaluationRequest(null, null, null, "Hire", null, null, null)));
    }

    @Test
    @DisplayName("Đề xuất phải là Hire / No_Hire / Consider")
    void recommendationMustMatchDatabaseValues() {
        assertEquals(Set.of("recommendation"), invalidFields(new SubmitEvaluationRequest(3, 3, 3, "NO_HIRE", null, null, null)));
        assertEquals(Set.of("recommendation"), invalidFields(new SubmitEvaluationRequest(3, 3, 3, " ", null, null, null)));
    }

    @Test
    @DisplayName("Nhận xét quá 2000 ký tự bị chặn")
    void commentsTooLong() {
        String longText = "a".repeat(2001);
        assertEquals(Set.of("comments"), invalidFields(new SubmitEvaluationRequest(3, 3, 3, "Hire", null, null, longText)));
    }
}
