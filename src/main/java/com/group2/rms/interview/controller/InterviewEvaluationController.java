package com.group2.rms.interview.controller;

import com.group2.rms.core.dto.ApiResponse;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.interview.dto.SubmitEvaluationRequest;
import com.group2.rms.interview.exception.InterviewEvaluationException;
import com.group2.rms.interview.service.InterviewEvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Submit Evaluation Form (SRS 5.1.29): pop-up trên trang kết quả, gửi bằng AJAX (JSON + X-CSRF-TOKEN).
 * Lỗi nhập liệu và vi phạm nghiệp vụ trả ApiResponse(false) + 400 với lỗi theo từng ô trong data;
 * không tìm thấy / không có quyền để GlobalExceptionHandler xử lý.
 */
@RestController
@RequestMapping("/interviews")
@RequiredArgsConstructor
public class InterviewEvaluationController {

    private final InterviewEvaluationService interviewEvaluationService;
    private final CurrentUserService currentUserService;

    @PostMapping(value = "/{id}/evaluations",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> submit(
            @PathVariable("id") Long id,
            @Valid @RequestBody SubmitEvaluationRequest request,
            BindingResult errors) {

        if (errors.hasErrors()) {
            Map<String, String> fieldErrors = new LinkedHashMap<>();
            for (FieldError error : errors.getFieldErrors()) {
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
            }
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Vui lòng kiểm tra lại các ô được đánh dấu.", fieldErrors));
        }

        Integer evaluatorId = currentUserService.requireUser().getUserId();
        try {
            interviewEvaluationService.submit(id, request, evaluatorId);
        } catch (InterviewEvaluationException e) {
            Map<String, String> fieldErrors = e.getField() != null ? Map.of(e.getField(), e.getMessage()) : Map.of();
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), fieldErrors));
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Đã gửi phiếu đánh giá.", Map.of()));
    }
}
