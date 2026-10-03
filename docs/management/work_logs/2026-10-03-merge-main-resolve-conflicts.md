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

## 5. Kế hoạch Tiếp theo (Next Steps)
- Tiếp tục hoàn thiện các quy tắc nghiệp vụ và giao diện cho phân hệ Đặt lịch & Đánh giá phỏng vấn (Iteration 1).
- Đồng bộ giao diện `interview/list.html` và `interview/form.html` sử dụng layout fragment chung (`fragments/head`, `fragments/workspace-header`) để đồng nhất trải nghiệm với toàn hệ thống.
- Theo dõi PR tiếp theo của core về việc mở rộng `GlobalExceptionHandler` để gỡ bỏ triệt để các khối `try-catch` cục bộ trong Controller mà vẫn đảm bảo trải nghiệm trả lỗi về Form.
