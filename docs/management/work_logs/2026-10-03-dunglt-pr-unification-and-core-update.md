# Báo Cáo Thay Đổi Qua Các PR & Nâng Cấp Tầng Core (2026-10-03)

* **Người thực hiện:** Dũng (dunglt)
* **Ngày thực hiện:** 2026-10-03 (đến rạng sáng 2026-10-04)
* **Các PR liên quan:** PR #7, PR #8, PR #10, hỗ trợ PR #6, và nhánh tính năng `feature/dunglt/user-profile`

---

## 1. Yêu cầu & Quyết định Kỹ thuật
Trong ngày 03/10, dự án diễn ra nhiều đợt tích hợp lớn dẫn đến xung đột kiến trúc và phân rã mã nguồn giữa các module độc lập. Nhằm bảo đảm tính toàn vẹn của nhánh `main` và chuẩn bị cho mốc bàn giao Iteration 1:
1. **Khắc phục xung đột PR #4 & PR #5 (PR #7):** Hợp nhất các phiên bản trùng lặp của trang Public Career Portal, khôi phục 52 file test/service bị lỗi import.
2. **Chuẩn hóa cấu hình và tài liệu (PR #8):** Phân định biến môi trường và cô lập không gian tài liệu theo từng thành viên (`docs/members/{name}`) nhằm triệt tiêu nguy cơ xung đột git.
3. **Dọn rò rỉ kiến trúc & Nâng cấp Core Exception (PR #10):** Đưa xử lý ngoại lệ tập trung qua `GlobalExceptionHandler`, xóa bỏ DTO thừa, tạo trang lỗi `403.html`.
4. **Hỗ trợ gỡ rối & vá bảo mật cho module Phỏng vấn (PR #6):** Sửa lỗi `LazyInitializationException` khi render form lịch phỏng vấn, cấu hình lại Spring Security để chặn Candidate/Interviewer truy cập module Offer.
5. **Phát triển tính năng Hồ sơ cá nhân & Đổi mật khẩu (`feature/dunglt/user-profile`):** Xây dựng module User Profile chuẩn Package-by-Feature.

---

## 2. Thay đổi Cơ sở dữ liệu (Database)
* Không thay đổi schema DB; tái sử dụng các bảng `users`, `roles`, `departments` sẵn có.

---

## 3. Thay đổi Mã nguồn & Tài liệu qua từng đợt PR

### A. PR #7: Thống nhất Module Career Portal (`fix/pr4-pr5-career-unification`)
* **Vấn đề giải quyết:** PR #4 và PR #5 merge đồng thời làm xuất hiện hai controller/service trùng lặp và gây lỗi import 52 file.
* **Mã nguồn thay đổi:**
  * Di chuyển toàn bộ tính năng cổng việc làm vào package `com.group2.rms.career`.
  * Xóa bỏ class trùng: `CareerController`, `CareerService`; thống nhất sử dụng `CareerPortalController` và `CareerPortalService`.
  * Chuẩn hóa DTO: `PublicJobDetailResponse`, `PublicJobListResponse`.
  * Fix toàn bộ 52 file bị gãy import (các luồng test `AccountListQueryTests`, `SecurityFlowTests`, `DashboardDatabaseTests`,...).

### B. PR #8: Tái cấu trúc Cấu hình & Phân vùng Tài liệu (`dunglt/chore/refactor-config-docs`)
* **Vấn đề giải quyết:** Tránh xung đột git khi cả nhóm cùng commit tài liệu; cập nhật test khớp logic bảo mật.
* **Mã nguồn & Tài liệu:**
  * Tách bạch cấu hình `application.properties` và biến môi trường.
  * Cập nhật `CareerFlowTests` thích ứng với luồng phân quyền của Linh.
  * Quy hoạch thư mục `docs/members/` riêng biệt cho từng thành viên (`duc`, `dunglt`, `hoangnh`, `linhdn`).

### C. PR #10: Nâng cấp Core Exception & Khắc phục Rò rỉ sau PR #9 (`refactor/after-huyenpt-pr9`)
* **Vấn đề giải quyết:** Sau PR #9 của Huyên, module Offer để sót service ngoài package và ngoại lệ chưa được xử lý đồng bộ.
* **Mã nguồn thay đổi:**
  * **Core Exception:** Mở rộng [GlobalExceptionHandler.java](file:///d:/D_Workspace-Active/FA26-TMP/SWP-Project/RMS-G2/src/main/java/com/group2/rms/core/exception/GlobalExceptionHandler.java) xử lý tập trung mọi `BaseBusinessException`, tự động phân tách response JSON (cho AJAX) hoặc View lỗi (cho trình duyệt).
  * **Error Page:** Bổ sung trang lỗi [403.html](file:///d:/D_Workspace-Active/FA26-TMP/SWP-Project/RMS-G2/src/main/resources/templates/error/403.html) theo chuẩn giao diện RMS.
  * **Package boundary:** Chuyển `NotificationService` về đúng gói `offer/service`, loại bỏ `ApiResponseDto` dư thừa trong module `requisition`.

### D. Hỗ trợ PR #6: Phỏng vấn & Lịch phỏng vấn (Đức - `feature/iter1-interview-schedule_duc`)
* **Vấn đề giải quyết:** Gỡ xung đột git sau thời gian dài nhánh của Đức bị trôi xa so với `main`.
* **Mã nguồn thay đổi:**
  * Khắc phục lỗi `LazyInitializationException` khi mở form đặt lịch trong `InterviewSchedulingController`.
  * Bổ sung query method trong `UserRepository` phục vụ dropdown chọn người phỏng vấn.
  * Tăng cường bảo mật trong [SecurityConfig.java](file:///d:/D_Workspace-Active/FA26-TMP/SWP-Project/RMS-G2/src/main/java/com/group2/rms/core/config/SecurityConfig.java): Giới hạn quyền truy cập `/offers/**` chỉ dành cho HR và Director.
  * Đưa bộ test 78/78 test case về trạng thái xanh.

### E. Module đang phát triển: Hồ sơ người dùng & Đổi mật khẩu (`feature/dunglt/user-profile`)
* **Mã nguồn:**
  * DTOs: `UserProfileResponse`, `UpdateProfileRequest`, `ChangePasswordRequest`.
  * Backend: `UserProfileService` và `UserProfileController` (hỗ trợ đọc profile, update profile và đổi mật khẩu an toàn với BCrypt `PasswordEncoder`).
  * Frontend: `profile.html`, `profile.js`, `profile.css` (Dirty check form state, Modal popup xác nhận, Đổi mật khẩu AJAX không reload trang kèm Toast notification, validation Unicode tiếng Việt).

---

## 4. Kiểm thử (Testing)
* **Unit & Integration Tests:** Chạy toàn bộ test suite dự án đảm bảo các PR #7, #8, #10 không gây hồi quy (regression) mã nguồn của các thành viên khác.
* **Manual Test module Profile:** Kiểm tra luồng xem thông tin, chỉnh sửa họ tên/SĐT/avatar và đổi mật khẩu trên giao diện web chạy ổn định.

---

## 5. Kế hoạch tiếp theo
1. Merge nhánh `main` mới nhất vào `feature/dunglt/user-profile` và xử lý conflict nhỏ ở menu điều hướng `sidebar.html`.
2. Bổ sung Unit Test cho `UserProfileService` và `UserProfileController`.
3. Mở Pull Request cho module User Profile để hoàn tất Iteration 1.
