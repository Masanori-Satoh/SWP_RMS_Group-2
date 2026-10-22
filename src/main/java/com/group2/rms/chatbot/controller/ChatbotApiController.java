package com.group2.rms.chatbot.controller;

import com.group2.rms.chatbot.dto.ChatRequest;
import com.group2.rms.chatbot.dto.ChatResponse;
import com.group2.rms.chatbot.service.ChatbotService;
import com.group2.rms.core.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/*
 * API JSON của widget chatbot (AJAX, khung chat ở lại trang). Mở cho cả khách (SecurityConfig: permitAll).
 * Controller chỉ kiểm tra dữ liệu gửi lên và cho service biết người hỏi đã đăng nhập chưa.
 */
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotApiController {

    private final ChatbotService chatbotService;

    /** Một lượt hỏi. Tin nhắn quá dài → 400 + ApiResponse(false, lý do), không gọi service. */
    @PostMapping("/message")
    public ResponseEntity<ApiResponse<ChatResponse>> message(@Valid @RequestBody ChatRequest request,
            BindingResult errors, Principal principal) {
        if (errors.hasErrors()) {
            String reason = errors.getAllErrors().get(0).getDefaultMessage();
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, reason));
        }
        // Security: khách không có Principal; ứng viên lấy từ phiên, không nhận id người dùng từ request
        boolean signedIn = principal != null;
        ChatResponse response = chatbotService.reply(request, signedIn);
        return ResponseEntity.ok(new ApiResponse<>(true, null, response));
    }
}
