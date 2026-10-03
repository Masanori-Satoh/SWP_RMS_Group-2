# Nhật Ký Công Việc: Chuẩn Hóa Module Interview Theo ARCHITECTURE_GUIDE.md (Mục 1, 3, 4, 5, 6, 8, 9)

- **Ngày thực hiện:** 2026-10-03
- **Người thực hiện:** Đức (Masanori-Satoh)
- **Nhánh:** `feature/iter1-interview-schedule_duc`
- **Tài liệu quy chuẩn đối chiếu:** [docs/architecture/ARCHITECTURE_GUIDE.md](../../architecture/ARCHITECTURE_GUIDE.md)

---

## 1. Mục tiêu và Bối cảnh

Sau khi hoàn tất merge nhánh `origin/main` (bao gồm PR7 và PR8), tiến hành rà soát mã nguồn module `interview` đối chiếu với tài liệu quy chuẩn kiến trúc của dự án (`ARCHITECTURE_GUIDE.md`). Thực hiện chuẩn hóa toàn diện 7 hạng mục đã được phân tích và đánh giá:
- **Mục 1:** `InterviewStatusException` kế thừa `BaseBusinessException`.
- **Mục 3:** Format text thô từ DB chứa ký tự `\n` sang `<br/>` dùng `th:utext` và `white-space: pre-line;`.
- **Mục 4:** Chuyển đổi Input DTO `InterviewScheduleRequest` thành Java Record (bất biến, có `@Builder` và getter tương thích ngược).
- **Mục 5:** Tái sử dụng Thymeleaf fragment dùng chung (`fragments/head`, `fragments/brand`).
- **Mục 6:** Dọn sạch 100% thuộc tính CSS inline (`style="..."`) trong các view `list.html` và `form.html`.
- **Mục 8:** Đảm bảo `@Transactional(readOnly = true)` trên các thao tác đọc và `@Transactional` trên các thao tác ghi.
- **Mục 9:** Xác nhận không có bất kỳ thẻ `<style>` hay `<script>` nội tuyến nào trong các template.

---

## 2. Chi Tiết Thực Hiện Từng Mục

### Mục 1: Kế thừa BaseBusinessException cho InterviewStatusException
- **File:** [InterviewStatusException.java](../../../src/main/java/com/group2/rms/interview/exception/InterviewStatusException.java)
- **Thực hiện:**
  - Thay đổi kế thừa từ `RuntimeException` sang `BaseBusinessException`.
  - Cung cấp mã lỗi nghiệp vụ chuẩn hóa: `"INTERVIEW_STATUS_ERROR"` và `"INTERVIEW_INVALID_STATUS_TRANSITION"`.
  - Giữ nguyên các getter `currentStatus` và `targetStatus` để phục vụ audit/debug log.

### Mục 3: Xử lý an toàn dữ liệu nhiều dòng từ Database
- **File:** [list.html](../../../src/main/resources/templates/interview/list.html), [interview-list.css](../../../src/main/resources/static/css/interview-list.css)
- **Thực hiện:**
  - Áp dụng class `.multiline-text` (`white-space: pre-line; word-break: break-word;`).
  - Dùng Thymeleaf expression:
    `th:utext="${#strings.replace(#strings.escapeXml(s.locationOrLink), '&#10;', '&lt;br/&gt;')}"`
    để vừa đảm bảo chống tấn công XSS (nhờ escapeXml), vừa tự động chuyển ký tự xuống dòng `\n` thành `<br/>` chuẩn theo hướng dẫn tại dòng 64 của `ARCHITECTURE_GUIDE.md`.

### Mục 4: Chuyển đổi InterviewScheduleRequest sang Java Record
- **File:** [InterviewScheduleRequest.java](../../../src/main/java/com/group2/rms/interview/dto/InterviewScheduleRequest.java), [InterviewSchedulingController.java](../../../src/main/java/com/group2/rms/interview/controller/InterviewSchedulingController.java)
- **Thực hiện:**
  - Chuyển khai báo class thành `public record InterviewScheduleRequest(...)`.
  - Giữ đầy đủ Bean Validation annotations (`@NotNull`, `@Future`, `@Size`, `@NotEmpty`, `@Valid`, `@ValidInterviewTime`, `@AssertTrue`).
  - Thêm Compact Constructor để tự động đồng bộ 2 chiều giữa `interviewerIds` (từ HTML multi-select) và `panelMembers` (List `PanelMemberRequest`).
  - Thêm No-args Constructor `public InterviewScheduleRequest()` khởi tạo rỗng mặc định.
  - Cung cấp các getter JavaBean (`getApplicationId()`, `getStartTime()`, ...) để tương thích ngược 100% với Spring MVC DataBinder, Thymeleaf `th:field`, và `InterviewSchedulingServiceImpl`.
  - Cập nhật các hàm `showCreateForm` và `showEditForm` trong `InterviewSchedulingController` chuyển sang sử dụng `InterviewScheduleRequest.builder()`.

### Mục 5: Tích hợp Thymeleaf Fragment Dùng Chung
- **File:** [head.html](../../../src/main/resources/templates/fragments/head.html), [list.html](../../../src/main/resources/templates/interview/list.html), [form.html](../../../src/main/resources/templates/interview/form.html)
- **Thực hiện:**
  - Bổ sung fragment `globalHead(title, pageStyles)` trong `fragments/head.html` phục vụ các trang giao diện chuẩn Mộc Careers sử dụng `global.css`.
  - Tích hợp `<head th:replace="~{fragments/head :: globalHead(..., ~{::pageStyles})}">` vào `list.html` và `form.html`.
  - Tích hợp `<a th:replace="~{fragments/brand :: wordmark(...)}"></a>` cho logo brand.
  - Chuẩn hóa toàn bộ liên kết điều hướng sang cú pháp Spring `th:href="@{...}"`.

### Mục 6: Bóc tách toàn bộ Inline Styles (`style="..."`)
- **File:** [interview-list.css](../../../src/main/resources/static/css/interview-list.css), [interview-form.css](../../../src/main/resources/static/css/interview-form.css), [list.html](../../../src/main/resources/templates/interview/list.html), [form.html](../../../src/main/resources/templates/interview/form.html)
- **Thực hiện:**
  - Chuyển `style="display:contents;"` trên logout form sang class `.logout-form`.
  - Chuyển inline font/color `h1` sang class `.page-title`.
  - Chuyển `style="display:none;"` trên button clear filters và empty state sang CSS rules.
  - Chuyển `style="word-break: break-all; max-width: 200px;"` và link màu brand sang `.meta-value-location` và `.meta-link`.
  - Chuyển `style="color:var(--muted); font-size:13px; font-style:italic;"` sang `.panel-empty`.
  - Chuyển `style="display:inline;"` trên cancel form sang `.action-form-inline`.
  - Chuyển inline breadcrumb separator và current page text trong `form.html` sang `.breadcrumb-separator` và `.breadcrumb-current`.
  - Kết quả: Không còn bất kỳ thuộc tính `style="..."` nào trong thư mục `templates/interview/`.

### Mục 8 & Mục 9: Kiểm tra Transactional và Tách biệt Style/Script
- **File:** [InterviewSchedulingServiceImpl.java](../../../src/main/java/com/group2/rms/interview/service/InterviewSchedulingServiceImpl.java)
- **Thực hiện:**
  - Xác nhận class có `@Transactional(readOnly = true)` ở cấp độ class (bảo vệ session Hibernate tránh `LazyInitializationException` khi map DTO).
  - Các hàm thay đổi dữ liệu (`createSchedule`, `updateSchedule`, `cancelSchedule`) đều có `@Transactional` độc lập.
  - Xác nhận không có bất kỳ thẻ `<style>` hay thẻ `<script>` nội tuyến nào trong mã HTML của feature.

---

## 3. Kết Quả Kiểm Thử (Verification)

1. **Biên dịch mã nguồn (Maven Compilation):**
   ```powershell
   .\mvnw.cmd test-compile
   # -> BUILD SUCCESS
   ```
2. **Kiểm thử đơn vị và tích hợp (Unit & Service Tests):**
   ```powershell
   .\mvnw.cmd test -Dtest=InterviewSchedulingServiceTests
   # -> Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
   # -> BUILD SUCCESS (100% Pass)
   ```
3. **Rà soát mã tĩnh:**
   - Ripgrep không tìm thấy thuộc tính `style="` trong `templates/interview/`.
   - Ripgrep không tìm thấy thẻ `<style>` hay `<script>` nội tuyến trong `templates/interview/`.
