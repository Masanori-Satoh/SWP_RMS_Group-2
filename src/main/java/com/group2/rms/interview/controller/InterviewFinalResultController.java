package com.group2.rms.interview.controller;

import com.group2.rms.core.dto.ApiResponse;
import com.group2.rms.core.security.CurrentUserService;
import com.group2.rms.interview.dto.SubmitFinalResultRequest;
import com.group2.rms.interview.exception.InterviewFinalResultException;
import com.group2.rms.interview.service.InterviewFinalResultService;
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
 * Hiring Manager chốt kết quả phỏng vấn: pop-up trên trang kết quả, gửi bằng AJAX (JSON + X-CSRF-TOKEN).
 * Cùng khuôn với InterviewEvaluationController.
 */
@RestController
@RequestMapping("/interviews")
@RequiredArgsConstructor
public class InterviewFinalResultController {

    private final InterviewFinalResultService interviewFinalResultService;
    private final CurrentUserService currentUserService;

    @PostMapping(value = "/{id}/final-result",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> decide(
            @PathVariable("id") Long id,
            @Valid @RequestBody SubmitFinalResultRequest request,
            BindingResult errors) {

        if (errors.hasErrors()) {
            Map<String, String> fieldErrors = new LinkedHashMap<>();
            for (FieldError error : errors.getFieldErrors()) {
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
            }
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Vui lòng kiểm tra lại các ô được đánh dấu.", fieldErrors));
        }

        try {
            interviewFinalResultService.decide(id, request, currentUserService.requireUser());
        } catch (InterviewFinalResultException e) {
            Map<String, String> fieldErrors = e.getField() != null ? Map.of(e.getField(), e.getMessage()) : Map.of();
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), fieldErrors));
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Đã chốt kết quả phỏng vấn.", Map.of()));
    }
}
