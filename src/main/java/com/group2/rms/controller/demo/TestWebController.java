package com.group2.rms.controller.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller này dùng để TEST hiển thị giao diện web (HTML/CSS/Thymeleaf).
 * Phục vụ mục đích làm mẫu cho team xem cách gọi View.
 * (Sau này dự án chốt xong form có thể xóa bỏ package demo này).
 */
@Controller
@RequestMapping("/test-web")
public class TestWebController {

    @GetMapping
    public String testWeb(Model model) {
        // Biến "message" này sẽ được đẩy sang file html
        model.addAttribute("message", "Giao diện web đã chạy hoàn hảo! 🚀");
        return "hello"; // Trả về file src/main/resources/templates/hello.html
    }
}
