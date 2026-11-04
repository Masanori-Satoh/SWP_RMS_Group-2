# Đăng xuất về trang chủ — 2026-11-04

- Người thực hiện: LinhDN (hỗ trợ bởi Codex).
- Yêu cầu: người dùng gặp trang 403 tại `/logout` và muốn đăng xuất về trang chủ.
- Mã nguồn: đổi `SecurityConfig.logoutSuccessUrl` từ `/login?logout` thành `/`. Form dùng POST với `th:action`, được Thymeleaf tích hợp CSRF; giữ bảo vệ CSRF.
- Chẩn đoán: request logout thiếu/sai CSRF bị chặn trước khi đến logout handler. Session/token cũ sau restart là một khả năng; chưa có request/log của lần lỗi trong ảnh để xác nhận nguyên nhân cụ thể.
- Kiểm thử: thêm `LogoutWebTests` cho logout hợp lệ (redirect `/`, xóa authentication, invalidate session), thiếu token và token sai (403, giữ authentication).
- Validation: chạy `mvn -o "-Dtest=LogoutWebTests" test`; compile/testCompile hoàn tất nhưng tiến trình không hoàn tất khởi tạo test sau log Spring/Mockito trong vài phút, đã dừng. Chưa có kết quả pass/fail; chưa kiểm chứng trên trình duyệt. `git diff --check` không báo lỗi khoảng trắng.
- Database: không thay đổi.
- Tồn đọng: chạy lại test khi môi trường khởi tạo ổn định; cần restart app và tải mới trang trước khi thử lại. Nếu vẫn 403, kiểm tra request POST `/logout`, session cookie và CSRF gửi lên để xác định nguyên nhân.
