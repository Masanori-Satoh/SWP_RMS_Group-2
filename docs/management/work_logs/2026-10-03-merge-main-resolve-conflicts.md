# Nhật ký Công việc: Đồng bộ Main (PR7 & PR8) và Giải quyết Xung đột Kiến trúc

- **Ngày thực hiện:** 2026-10-03
- **Người thực hiện:** Đức (Masanori-Satoh)
- **Nhiệm vụ:** Kéo code mới nhất từ nhánh `main` (bao gồm các thay đổi của PR7 & PR8) vào nhánh `feature/iter1-interview-schedule_duc`, giải quyết xung đột Git vật lý và xung đột kiến trúc package-by-feature, thiết lập cấu hình môi trường local an toàn, và chuẩn hóa cấu trúc tài liệu theo quy định repo mới tại [docs/README.md](../../README.md) và [dunglt-2026-10-01-update-quy-trinh.md](../../members/dunglt/dunglt-2026-10-01-update-quy-trinh.md).

---

## 1. Yêu cầu & Quyết định Kỹ thuật

### 1.1 Bối cảnh
- Nhánh `main` vừa merge thành công 2 Pull Request quan trọng:
  - **PR #7 (`fix/pr4-pr5-career-unification`):** Hợp nhất cổng ứng tuyển candidate, chuyển sang `CareerPortalController` (Read-only BFF), chuẩn hóa DTO thành Java Record, và tái cấu trúc module theo chuẩn **package-by-feature** (`candidate.entity.*`, `candidate.repository.*`).
  - **PR #8 (`dunglt/chore/refactor-config-docs`):** Chuyển đổi quản lý biến môi trường sang `application-local.properties` (loại bỏ `.env`), đưa `application.properties` vào quản lý chung, và thiết lập kỷ luật quản lý tài liệu trong `docs/`.
- Nhánh `feature/iter1-interview-schedule_duc` cần đồng bộ với `main` để tránh phân rã mã nguồn và đảm bảo tương thích kiến trúc khi PR ngược lại `main`.

### 1.2 Các xung đột và phương án xử lý
1. **Xung đột vật lý Git (Modify/Delete Conflict):**
   - *Tệp xung đột:* `src/main/java/com/group2/rms/interview/InterviewSchedule.java`.
   - *Nguyên nhân:* Nhánh interview đã chuyển thực thể này vào sub-package `entity/InterviewSchedule.java` và xóa tệp cũ ở gốc module. Nhánh `main` có chỉnh sửa dòng import trong tệp cũ ở gốc module.
   - *Giải pháp:* Loại bỏ tệp cũ ở gốc module (`git rm`), giữ nguyên thực thể chuẩn kiến trúc tại `src/main/java/com/group2/rms/interview/entity/InterviewSchedule.java`.
2. **Xung đột phụ thuộc & import (Compilation / Package-by-Feature):**
   - *Nguyên nhân:* Các lớp `Application` và `ApplicationRepository` ở nhánh `main` đã được di chuyển vào package con `com.group2.rms.candidate.entity` và `com.group2.rms.candidate.repository`.
   - *Giải pháp:* Cập nhật lại đường dẫn import tại 5 tệp trong module `interview`.
3. **Bảo toàn truy vấn nghiệp vụ:**
   - Hàm truy vấn tùy biến `@Query findAllWithCandidateAndJobPosting()` của `ApplicationRepository` được Git tự động gộp (clean auto-merge) vào interface mới tại `com.group2.rms.candidate.repository.ApplicationRepository`, không bị mất mát hay xung đột.

---

## 2. Thay đổi Cơ sở dữ liệu & Cấu hình Môi trường

### 2.1 Cơ sở dữ liệu (Database)
- Không có thay đổi schema SQL hay dữ liệu mẫu (seed data).

### 2.2 Quản lý Cấu hình (Environment Configuration)
- **Sao lưu & Cô lập mật khẩu cá nhân:**
  - Thiết lập tệp cấu hình máy cá nhân [`src/main/resources/application-local.properties`](../../../src/main/resources/application-local.properties) chứa mật khẩu database SQL Server local (`spring.datasource.password=123`).
  - Tệp này nằm trong `.gitignore` (đã được cấu hình ở PR8), đảm bảo an toàn tuyệt đối không đẩy mật khẩu lên GitHub.
- **Nạp cấu hình gốc:**
  - Giữ nguyên tệp cấu hình mặc định chung [`src/main/resources/application.properties`](../../../src/main/resources/application.properties) được nạp từ `main` (`spring.profiles.active=local`).

---

## 3. Danh sách Tệp thay đổi & Di chuyển

### 3.1 Cập nhật mã nguồn Java (Fix imports)
- [`src/main/java/com/group2/rms/interview/controller/InterviewSchedulingController.java`](../../../src/main/java/com/group2/rms/interview/controller/InterviewSchedulingController.java): Cập nhật import `Application` và `ApplicationRepository`.
- [`src/main/java/com/group2/rms/interview/dto/InterviewScheduleResponse.java`](../../../src/main/java/com/group2/rms/interview/dto/InterviewScheduleResponse.java): Cập nhật import `Application`.
- [`src/main/java/com/group2/rms/interview/entity/InterviewSchedule.java`](../../../src/main/java/com/group2/rms/interview/entity/InterviewSchedule.java): Cập nhật import `Application`.
- [`src/main/java/com/group2/rms/interview/service/InterviewSchedulingServiceImpl.java`](../../../src/main/java/com/group2/rms/interview/service/InterviewSchedulingServiceImpl.java): Cập nhật import `Application` và `ApplicationRepository`.
- [`src/test/java/com/group2/rms/interview/InterviewSchedulingServiceTests.java`](../../../src/test/java/com/group2/rms/interview/InterviewSchedulingServiceTests.java): Cập nhật import `Application` và `ApplicationRepository`.

### 3.2 Chuẩn hóa vị trí Tài liệu theo Kỷ luật Repo
- **Di chuyển Mockup:** Di chuyển `docs/mockup/mockup_design.html` về đúng không gian cá nhân tại [`docs/members/duc/mockup/mockup_design.html`](../../members/duc/mockup/mockup_design.html) (tuân thủ quy định không tạo thư mục lạ ở gốc `docs/`).
- **Lưu trữ báo cáo phân tích:** Lưu báo cáo đánh giá PR8 tại [`docs/members/duc/duc-2026-10-03-review-pr8.md`](../../members/duc/duc-2026-10-03-review-pr8.md).

---

## 4. Kiểm thử & Xác minh Chất lượng (Testing)

1. **Kiểm tra Biên dịch toàn dự án:**
   - Lệnh: `.\mvnw.cmd test-compile`
   - Kết quả: **BUILD SUCCESS** (101 source files chính + 15 test files biên dịch thành công 100%, không còn lỗi `cannot find symbol`).
2. **Kiểm tra Unit Test Module Interview:**
   - Lệnh: `.\mvnw.cmd test -Dtest=InterviewSchedulingServiceTests`
   - Kết quả: **Tests run: 5, Failures: 0, Errors: 0, Skipped: 0** — PASS 100%.
3. **Kiểm tra Trạng thái Git:**
   - Commit merge: `de6b5f8 Merge branch 'origin/main' into feature/iter1-interview-schedule_duc`.
   - Trạng thái cây làm việc: Clean hoàn toàn.

---

## 5. Nhược điểm Phát hiện qua Quá trình Review & Merge

Trong quá trình phân tích chuyên sâu mã nguồn `main` và thực hiện đồng bộ, đã phát hiện một số điểm bất cập mang tính hệ thống cần được team lưu ý:

1. **Lỗ hổng xử lý Ngoại lệ trong `GlobalExceptionHandler`:**
   - Handler `@ExceptionHandler(Exception.class)` hiện đang "nuốt" toàn bộ các exception HTTP chuẩn của Spring MVC (`ResponseStatusException`, `NoResourceFoundException`, `AccessDeniedException`), biến các mã lỗi hợp lệ (404 Not Found, 403 Forbidden) thành lỗi hệ thống HTTP 500.
   - Handler `Exception.class` hoàn toàn không ghi log (`log.error`), che giấu nguyên nhân gốc (root cause) khiến việc truy vết lỗi runtime hoặc kiểm thử gặp nhiều khó khăn.
   - Handler `@ExceptionHandler(BaseBusinessException.class)` chỉ trả về ModelAndView `error/500` mà không khai báo `@ResponseStatus`, dẫn đến việc Spring Boot mặc định phản hồi HTTP status code là `200 OK` cho các lỗi nghiệp vụ hoặc lỗi bảo mật (như lỗi `"UNAUTHORIZED"`).
2. **Rủi ro cấu hình `ddl-auto=update` trong tệp dùng chung:**
   - Tệp `application.properties` gốc đang bật `spring.jpa.hibernate.ddl-auto=update`. Khi các thành viên chạy nhánh của mình trên cơ sở dữ liệu chung hoặc local, Hibernate có thể tự ý chỉnh sửa cấu trúc bảng (`ALTER TABLE`), đi ngược lại quy tắc yêu cầu `validate` đã ban hành trong `README.md`.
3. **Mâu thuẫn tài liệu hướng dẫn khởi chạy trong `README.md` gốc:**
   - Phần hướng dẫn trong `README.md` (mục 2.2, 2.3 và dòng 62) chưa được cập nhật đồng bộ với PR8: vẫn hướng dẫn đổi tên `.env.example` (trong khi tệp này đã bị xóa) và nói tệp `application.properties` bị ignore (trong khi tệp này đã được đưa vào Git quản lý). Người mới kéo code về rất dễ cấu hình nhầm.
4. **Xung đột giữa Quy chuẩn cấm `try-catch` trong Controller và Trải nghiệm người dùng (UX Form):**
   - Quy chuẩn kiến trúc yêu cầu Controller không được tự `try-catch` mà phải để `GlobalExceptionHandler` xử lý. Tuy nhiên, `GlobalExceptionHandler` hiện chỉ có trang lỗi 500 toàn trang, chưa hỗ trợ cơ chế trả lỗi nghiệp vụ về lại Form giao diện kèm thông báo và dữ liệu đã nhập.
   - Nếu ép buộc gỡ bỏ `try-catch` trong các Controller nhập liệu ngay lúc này (ví dụ: màn hình tạo/hủy lịch phỏng vấn), người dùng khi nhập trùng lịch sẽ bị chuyển hướng sang trang lỗi 500 thay vì nhận thông báo lỗi trực quan trên Form.
5. **Thư mục quy hoạch `prototype-reference/` chưa đồng bộ:**
   - Tài liệu `docs/README.md` mới ban hành có nêu tên phân khu `prototype-reference/`, nhưng trên nhánh `main` thư mục này chưa thực sự tồn tại (các bản prototype trước đó đã bị di chuyển vào `docs/members/linhdn/`).

---

## 6. Đề xuất & Mong muốn Team Cải thiện (Wishes for Next PRs)

Để hệ thống hoàn thiện và đồng bộ hơn ở các chu kỳ tiếp theo, khuyến nghị team thực hiện các cải tiến sau:

1. **Về xử lý Ngoại lệ (Nhờ Dũng / Team Core - Cần PR riêng):**
   - Tinh chỉnh `GlobalExceptionHandler`: Thêm handler hoặc loại trừ cho `ResponseStatusException`, `NoResourceFoundException`, `AccessDeniedException` để trả đúng mã HTTP (404, 403).
   - Thêm lệnh `log.error("Unhandled system exception: ", ex);` vào handler bắt `Exception.class`.
   - Gắn annotation `@ResponseStatus(HttpStatus.BAD_REQUEST)` cho `BaseBusinessException`.
   - Nghiên cứu giải pháp Controller Advice hoặc cơ chế chuẩn để trả lỗi validation/nghiệp vụ về lại Form giao diện trước khi bắt buộc gỡ bỏ hoàn toàn `try-catch` ở các Controller.
2. **Về Cấu hình Hệ thống (Nhờ Dũng):**
   - Đổi `spring.jpa.hibernate.ddl-auto` trong `application.properties` gốc về `validate` (hoặc `none`). Thành viên nào cần tự sinh bảng local có thể ghi đè `update` trong tệp `application-local.properties`.
   - Cập nhật mục 2.2 - 2.4 trong root `README.md`: loại bỏ các tham chiếu đến `.env` và hướng dẫn rõ quy trình copy từ `application-local.properties.example`.
3. **Về Tổ chức Thư mục Docs:**
   - Tạo thư mục `docs/prototype-reference/.gitkeep` để các thành viên có nơi lưu trữ thống nhất các bản mockup giao diện tĩnh dùng chung.

---

## 7. Kế hoạch Tiếp theo của Nhánh Interview (Next Steps)
- Tiếp tục hoàn thiện các quy tắc nghiệp vụ và giao diện cho phân hệ Đặt lịch & Đánh giá phỏng vấn (Iteration 1).
- Tạm thời giữ lại các khối `try-catch` cục bộ trong `InterviewSchedulingController` để đảm bảo thông báo lỗi nghiệp vụ hiển thị mượt mà trên Form, chờ đến khi `GlobalExceptionHandler` có giải pháp chuyển tiếp Form chuẩn mực.
- Đồng bộ giao diện `interview/list.html` và `interview/form.html` sử dụng layout fragment chung (`fragments/head`, `fragments/workspace-header`) để đồng nhất trải nghiệm với toàn hệ thống.

