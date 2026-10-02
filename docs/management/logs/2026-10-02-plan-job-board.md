# Nhật ký Thực thi: Public Job Board & Job Details

**Ngày lập:** 2026-10-02  
**Người thực hiện:** dunglt  
**Branch thực hiện:** `feature/dunglt/job-board`  
**Dựa trên:** Đặc tả SRS (Public JD Board - Screen 48 & JD Details - Screen 49) và Quy chuẩn Kiến trúc `ARCHITECTURE_GUIDE.md`

---

## 1. Yêu cầu & Quyết định Thực thi

- **Mục tiêu:** Xây dựng tính năng xem danh sách việc làm công khai (Job Board) và chi tiết việc làm (Job Details) cho ứng viên.
- **Tuân thủ Kiến trúc:** Toàn bộ code phải theo chuẩn Package-by-Feature, cấm try-catch ở Controller (nhường cho Global Exception), tách CSS/JS ra file riêng (không inline), và DTO phải dùng Java `record`.

## 2. Các Thay đổi & Tính năng đã Triển khai

### 2.1. Cấu trúc Core (Ngoại lệ & Lỗi)
- Tạo `BaseBusinessException` và `ResourceNotFoundException`.
- Tạo `GlobalExceptionHandler` bắt lỗi toàn hệ thống và điều hướng về trang lỗi 500/404.
- Tạo giao diện `404.html` và `500.html` trong `templates/error/`.

### 2.2. Giao diện Dùng chung (Global UI)
- Trích xuất toàn bộ Style cốt lõi (biến màu, typography, thẻ card, button) từ tính năng phỏng vấn sang `static/css/global.css`.
- Trích xuất logic User Dropdown Menu từ trang phỏng vấn sang `static/js/global.js`.
- Cập nhật lại UI của tính năng phỏng vấn để dùng các file global này.

### 2.3. Backend Job Board (Feature: `requisition`)
- **DTO:** Khởi tạo `JobPostingListResponse` và `JobPostingDetailResponse` (chuẩn Java `record`).
- **Repository:** Cập nhật `JobPostingRepository` với `@Query` lấy danh sách công việc `Published`, còn hạn, kết hợp tìm kiếm và lọc.
- **Service:** Tạo `JobPostingService` xử lý nghiệp vụ ánh xạ sang DTO, bắt lỗi `LazyInitializationException` bằng `@Transactional(readOnly = true)`, format lại chuỗi text `\n` sang `<br/>` HTML.
- **Controller:** Tạo `JobPostingController.java` mapped với `/jobs`, không sử dụng `try-catch` thủ công, truyền thẳng model xuống View.

### 2.4. Frontend Job Board
- Dựng `templates/candidate/job-board.html` (kế thừa `global.css`, tích hợp `global.js`).
- Dựng `templates/candidate/job-detail.html` (hiển thị mô tả an toàn với `th:utext`, sticky sidebar cho action).
- Cấu hình style đặc thù trong `static/css/job-board.css`.

## 3. Vấn đề tồn đọng
- Cần tiếp tục triển khai chức năng Nộp hồ sơ (Apply Job) ở bước tiếp theo.
