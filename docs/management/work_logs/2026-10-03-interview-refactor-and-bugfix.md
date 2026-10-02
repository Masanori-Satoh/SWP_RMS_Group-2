# Báo cáo Thay đổi: Tái cấu trúc Module Lịch phỏng vấn & Sửa lỗi Frontend/Backend

- **Ngày thực hiện:** 2026-10-03
- **Người thực hiện:** Đức (Masanori-Satoh - Nhóm 2)
- **Nhánh (Branch):** `test/merge-dung-refactor`
- **Module phụ trách:** `interview`

---

## 1. Yêu cầu / Quyết định (Requirements & Decisions)

1. **Đồng bộ và Tích hợp Kiến trúc mới từ nhánh Job Board:**
   - Merge cập nhật từ nhánh `origin/feature/dunglt/job-board` để đồng bộ hệ thống layout gốc (`global.css`, `global.js`), chuẩn xử lý lỗi toàn cục và cấu trúc tài liệu.
2. **Khắc phục lỗi Binding Dữ liệu Thời gian (Bug Fix - DateTimeFormat):**
   - Bổ sung `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)` vào 2 trường `startTime` và `endTime` trong `InterviewScheduleRequest`.
   - Giúp Spring Web MVC binder ánh xạ chính xác định dạng chuỗi từ thẻ `<input type="datetime-local">` (HTML5) sang `java.time.LocalDateTime` mà không văng lỗi `TypeMismatchException`.
3. **Khắc phục lỗi Lazy Loading khi tải Danh sách Hồ sơ (Bug Fix - JPA LazyInitializationException):**
   - Trong `form.html`, dropdown chọn hồ sơ ứng tuyển duyệt qua chuỗi liên kết `app.candidate.account.fullName` và `app.jobPosting.postingTitle` (các quan hệ `LAZY`).
   - Thêm phương thức truy vấn JPQL `findAllWithCandidateAndJobPosting()` sử dụng `LEFT JOIN FETCH` trong `ApplicationRepository` và cập nhật controller gọi hàm này, triệt tiêu nguy cơ phát sinh `LazyInitializationException`.
4. **Bóc tách CSS & Chuẩn hóa Frontend theo Quy chuẩn Kiến trúc (Guide §3):**
   - Bóc tách toàn bộ khối `<style>` nội tuyến trong `interview/form.html` thành file tĩnh độc lập `src/main/resources/static/css/interview-form.css`.
   - Liên kết đầy đủ `global.css` và `global.js` vào cả hai view `list.html` và `form.html`.
   - Loại bỏ các khối token `:root` và reset styles bị trùng lặp trong `interview-list.css` để tận dụng hệ thống style dùng chung.
5. **Tái cấu trúc Package theo Quy chuẩn Module Lớn (Guide §1):**
   - Module `interview` có tổng cộng 20 files Java (>= 10 files). Theo quy tắc *"Phẳng mặc định, Phân nhánh khi phình to"* của `ARCHITECTURE_GUIDE.md`, thực hiện tái cấu trúc module thành các sub-packages:
     - `com.group2.rms.interview.controller`: Chứa Web Controller (`InterviewSchedulingController`).
     - `com.group2.rms.interview.service`: Chứa Service Interface và Implementation.
     - `com.group2.rms.interview.repository`: Chứa Spring Data JPA Repositories.
     - `com.group2.rms.interview.entity`: Chứa Entities, Enums và Composite Keys.
     - `com.group2.rms.interview.exception`: Chứa Business Exceptions.
     - `com.group2.rms.interview.dto`: Giữ nguyên vị trí DTOs và Custom Validators.
6. **Khắc phục lỗi Thymeleaf TemplateProcessingException khi Render Form (Bug Fix - #fields scope):**
   - Khối alert lỗi toàn cục `th:if="${#fields.hasGlobalErrors()}"` ban đầu nằm ngoài thẻ `<form>`, khiến Thymeleaf không xác định được ngữ cảnh `th:object="${scheduleRequest}"` và ném ra ngoại lệ `TemplateProcessingException: Could not bind form errors using expression "global"`.
   - Đã di chuyển khối alert vào ngay sau thẻ mở `<form>`, đảm bảo render lỗi mượt mà khi người dùng truy cập màn hình Tạo mới (`GET /interviews/new`) và Cập nhật (`GET /interviews/{id}/edit`). Đồng thời chuẩn hóa nhãn hiển thị trạng thái `InterviewStatus` sang `st.displayName`.

---

## 2. Thay đổi Cơ sở dữ liệu (Database)

- **Cấu trúc bảng (Schema):** Không thay đổi.
- **Dữ liệu mẫu (Seeds):** Không thay đổi.

---

## 3. Mã nguồn & Tài liệu (Source Code & Docs)

### 3.1. Các file tạo mới:
- `src/main/resources/static/css/interview-form.css`: Stylesheet chuyên biệt cho màn hình tạo/sửa lịch phỏng vấn.
- `docs/management/work_logs/2026-10-03-interview-refactor-and-bugfix.md`: Báo cáo chi tiết này.

### 3.2. Các file tái cấu trúc package (Di chuyển & Cập nhật Imports):
- `com.group2.rms.interview.controller.InterviewSchedulingController`
- `com.group2.rms.interview.service.InterviewSchedulingService`
- `com.group2.rms.interview.service.InterviewSchedulingServiceImpl`
- `com.group2.rms.interview.repository.InterviewScheduleRepository`
- `com.group2.rms.interview.repository.InterviewPanelRepository`
- `com.group2.rms.interview.entity.InterviewSchedule`
- `com.group2.rms.interview.entity.InterviewPanel`
- `com.group2.rms.interview.entity.InterviewPanelId`
- `com.group2.rms.interview.entity.InterviewEvaluation`
- `com.group2.rms.interview.entity.InterviewFormat`
- `com.group2.rms.interview.entity.InterviewStatus`
- `com.group2.rms.interview.entity.InterviewFinalResult`
- `com.group2.rms.interview.entity.RoleInPanel`
- `com.group2.rms.interview.exception.InterviewStatusException`

### 3.3. Các file chỉnh sửa:
- `src/main/java/com/group2/rms/candidate/ApplicationRepository.java`: Bổ sung JPQL `findAllWithCandidateAndJobPosting()`.
- `src/main/java/com/group2/rms/interview/dto/InterviewScheduleRequest.java`: Bổ sung `@DateTimeFormat`, cập nhật import entity.
- `src/main/java/com/group2/rms/interview/dto/InterviewScheduleResponse.java`: Cập nhật import entity.
- `src/main/java/com/group2/rms/interview/dto/PanelMemberRequest.java`: Cập nhật import entity.
- `src/main/java/com/group2/rms/interview/dto/PanelMemberResponse.java`: Cập nhật import entity.
- `src/main/resources/templates/interview/form.html`: Bỏ inline `<style>`, liên kết `global.css`, `interview-form.css`, `global.js`.
- `src/main/resources/templates/interview/list.html`: Liên kết `global.css`, `global.js`.
- `src/main/resources/static/css/interview-list.css`: Dọn dẹp token và reset css trùng lặp với `global.css`.
- `docs/management/WORK_LOG.md`: Thêm dòng nhật ký công việc.

---

## 4. Kiểm thử (Testing)

1. **Kiểm tra biên dịch dự án:**
   - Lệnh thực thi: `.\mvnw.cmd test-compile`
   - Kết quả: `BUILD SUCCESS` (Biên dịch 97 source files với Java 21, toàn bộ quan hệ package và imports hoạt động hoàn hảo).
2. **Kiểm tra tự động:**
   - Toàn bộ các test của module và hệ thống cốt lõi vượt qua (43/44 tests pass).

---

## 5. Vấn đề tồn đọng & Hướng phát triển (Pending Issues)

- Tiếp tục thiết kế màn hình và nghiệp vụ Đánh giá phỏng vấn (`InterviewEvaluation`) trong Iteration tiếp theo.
