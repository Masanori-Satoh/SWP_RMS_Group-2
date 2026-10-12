package com.group2.rms.candidate.dto;

import org.springframework.web.multipart.MultipartFile;

/** Form nộp đơn (multipart). File được kiểm tra ở {@code CvFileValidator}, không dùng Bean Validation. */
public record ApplyJobRequest(MultipartFile cvFile) {
}
