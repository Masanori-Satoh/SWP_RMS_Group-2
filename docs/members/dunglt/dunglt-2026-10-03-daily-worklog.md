# Báo Cáo Công Việc Chi Tiết (2026-10-03 - 2026-10-04)

* **Người thực hiện:** Dũng (dunglt)
* **Vai trò:** Core Developer / Integration & Architecture Support
* **Không gian lưu trữ:** `docs/members/dunglt/dunglt-2026-10-03-daily-worklog.md`
* **Thời gian ghi nhận:** Ngày 03/10/2026 đến rạng sáng 04/10/2026

---

## I. TỔNG QUAN CÔNG VIỆC TRONG NGÀY
Hôm nay là một ngày làm việc với khối lượng công việc và trách nhiệm rất lớn. Bên cạnh việc trực tiếp phát triển tính năng mới (**User Profile & Change Password**), tôi đóng vai trò là "chốt chặn kỹ thuật" của nhóm: liên tục rà soát, dọn dẹp rò rỉ kiến trúc, giải cứu các nhánh bị xung đột git nặng ("Git Hell"), đưa toàn bộ các pull request của các thành viên về trạng thái kiểm thử thành công trước khi hòa vào nhánh `main`.

---

## II. CHI TIẾT CÁC ĐỢT SỬA LỖI, XỬ LÝ CONFLICT VÀ TẠO PR

```
[Sáng 03/10] PR #4 & PR #5 hòa main gây xung đột kiến trúc ---> Tạo & merge PR #7 (Career Unification)
[Chiều 03/10] Cấu hình môi trường & Docs phân mảnh       ---> Tạo & merge PR #8 (Config & Docs Refactor)
[Tối 03/10]   PR #9 (Offer) phát sinh rò rỉ kiến trúc   ---> Tạo & merge PR #10 (Core Exception Cleanup)
[Đêm 03/10]   PR #6 (Đức - Interview) kẹt xung đột nặng  ---> Fix Lazy Loading, vá Security & Merge PR #6
```

### 1. Đợt 1: Xử lý va chạm kiến trúc sau PR #4 & PR #5 -> Tạo PR #7 (`fix/pr4-pr5-career-unification`)
* **Bối cảnh & Nguyên nhân xung đột:**
  * Sáng sớm ngày 03/10, hai PR lớn được merge vào `main` gần như đồng thời: PR #4 (`feature/dunglt/job-board`) và PR #5 (`feature/update-architure`).
  * Việc này làm xáo trộn kiến trúc: Có hai bộ Controller và Service cùng xử lý trang việc làm công khai (`CareerController` vs `CareerPortalController`, `JobPostingService` vs `CareerPortalService`), đường dẫn import DTO bị thay đổi khiến **52 file** trong mã nguồn (controller, service, test suite) bị gãy đỏ.
* **Các commit thực hiện:**
  * `cbb5665`: Tái quy hoạch package `com.group2.rms.career`, di chuyển DTOs vào `career/dto`, refactor và fix lỗi import trên toàn bộ 52 file test và service liên quan (`AccountListQueryTests`, `SecurityFlowTests`, `DashboardDatabaseTests`,...).
  * `4e0f83f`: Xóa bỏ các class trùng lặp thừa thãi (`CareerController`, `CareerService`), chuẩn hóa các response DTO (`PublicJobDetailResponse`, `PublicJobListResponse`), sửa bug giao diện tại `job-board.html` và `job-detail.html`.
* **Kết quả:** Mở **Pull Request #7**, xác nhận test pass toàn bộ và merge vào `main` (commit `1517775` lúc 12:20).

---

### 2. Đợt 2: Tối ưu Cấu hình Môi trường, Bộ Test & Tài liệu -> Tạo PR #8 (`dunglt/chore/refactor-config-docs`)
* **Bối cảnh & Vấn đề:**
  * Cấu hình biến môi trường (`application.properties`) bị phân mảnh giữa cấu hình chung và local.
  * Bộ test `CareerFlowTests` bị lỗi logic xác thực sau khi Linh cập nhật luồng Security.
  * Thư mục `docs/` của nhóm chưa được quy hoạch, tài liệu cá nhân để lẫn lộn trong thư mục chung dẫn đến nguy cơ xung đột git rất cao khi push tài liệu cuối ngày.
* **Các commit thực hiện:**
  * `c228c3b`: Refactor biến môi trường, phân tách rõ ràng cấu hình mặc định và local properties.
  * `bf1a2b2` & `ca03ecb`: Quy hoạch lại cấu trúc `docs/`, tạo thư mục độc lập `docs/members/{member_name}/` để mỗi thành viên lưu trữ tài liệu riêng, cập nhật `docs/README.md`.
  * `74067d4`: Cập nhật lại bộ test `CareerFlowTests` khớp chuẩn với logic phân quyền người dùng mới của Linh.
* **Kết quả:** Mở **Pull Request #8**, kiểm tra test suite và merge vào `main` (commit `10ab2b6` lúc 16:38).

---

### 3. Đợt 3: Xử lý rò rỉ kiến trúc sau PR #9 của Huyền -> Tạo PR #10 (`refactor/after-huyenpt-pr9`)
* **Bối cảnh & Vấn đề:**
  * Lúc 22:32, PR #9 của Huyên (`feature/huyenpt/offer-proposal`) được merge vào `main`.
  * Quá trình rà soát phát hiện module Offer bị rò rỉ kiến trúc (architectural leakage): file `NotificationService` và implementation bị đặt ngoài package feature, tồn tại class dư thừa `ApiResponseDto` trong module `requisition`, các ngoại lệ xử lý phân tán, chưa ăn khớp với `GlobalExceptionHandler`.
* **Các commit thực hiện:**
  * `301a104`: Di chuyển `NotificationService` về đúng vị trí `com.group2.rms.offer.service`, xóa bỏ `ApiResponseDto` trùng lặp, cập nhật `GlobalExceptionHandler`, sửa các class `OfferServiceImpl` và `OfferServiceTests`.
  * `9cee492` & `fd23841`: Chuyển các file prototype tham chiếu sang `docs/prototype-reference/` để làm sạch codebase.
* **Kết quả:** Mở **Pull Request #10**, hoàn tất rà soát và merge vào `main` (commit `f16ee04` lúc 00:15 ngày 04/10).

---

### 4. Đợt 4: Gỡ rối "Git Hell" & Vá lỗi bảo mật cho PR #6 (Đức - Interview Schedule)
* **Bối cảnh & Vấn đề:**
  * Nhánh `feature/iter1-interview-schedule_duc` của Đức bị tách rời quá lâu từ thời điểm `main` còn ở PR #3. Trong lúc đó `main` đã liên tục tiếp nhận PR #7, #8, #9, #10.
  * Đức gặp xung đột git nghiêm trọng, form đặt lịch phỏng vấn bị crash do lỗi JPA/Hibernate `LazyInitializationException`, đồng thời xuất hiện lỗ hổng phân quyền khi cho phép Candidate/Interviewer truy cập vào module Offer.
* **Các commit thực hiện:**
  * `51fb7b4`: Kéo `main` mới nhất vào nhánh của Đức, trực tiếp phân giải các xung đột code và template phức tạp.
  * `8e19b47`:
    * Khắc phục triệt để lỗi render form bằng cách fetch dữ liệu an toàn trong `InterviewSchedulingController` và bổ sung method truy vấn trong `UserRepository`.
    * Cấu hình lại `SecurityConfig.java`: Giới hạn quyền truy cập `/offers/**` chỉ dành riêng cho `HR` và `Director`, chặn đứng nguy cơ truy cập trái phép.
    * Sửa menu điều hướng `sidebar.html` để ẩn liên kết Offer đối với các quyền không phù hợp.
    * Đưa toàn bộ **78/78 unit/integration test** của module Interview về trạng thái pass (xanh).
* **Kết quả:** Hỗ trợ merge thành công **Pull Request #6** vào `main` (commit `7baf06a` lúc 01:08 ngày 04/10).

---

## III. NÂNG CẤP TẦNG CORE (CORE EXCEPTION & ARCHITECTURE STANDARDS)
Đã hoàn thành và được tích hợp vào `main` thông qua PR #10 (các commit `9cee492`, `fd23841`, `301a104`):

1. **Chuẩn hóa Xử lý Ngoại lệ Toàn cục (`GlobalExceptionHandler.java`):**
   * Mở rộng thêm hơn 85 dòng code xử lý tập trung: Bắt mọi ngoại lệ kế thừa từ `BaseBusinessException`, chuyển đổi mã trạng thái HTTP chuẩn mực thay vì để hệ thống crash văng lỗi 500 hoặc rò rỉ stacktrace ra giao diện.
   * Cơ chế tự động nhận diện request: Phản hồi JSON chuẩn cho các request AJAX/API và điều hướng về trang thông báo lỗi thân thiện đối với các request điều hướng của trình duyệt.
2. **Trang lỗi truy cập trái phép (`403.html`):**
   * Xây dựng giao diện trang lỗi 403 Forbidden đồng bộ theo ngôn ngữ thiết kế của RMS để xử lý khi người dùng truy cập các URL ngoài phạm vi phân quyền.
3. **Bảo vệ tính đóng gói kiến trúc (Package-by-Feature Enforcement):**
   * Ngăn chặn tình trạng các feature import chéo bừa bãi vào implementation của nhau. Mỗi feature phải tự quản lý DTO và Service của mình.

---

## IV. TÍNH NĂNG ĐANG PHÁT TRIỂN: USER PROFILE & CHANGE PASSWORD
Tính năng được triển khai trên nhánh `feature/dunglt/user-profile` (commit `1b1f8f9` lúc 19:22 ngày 03/10):

### 1. Những phần đã hoàn thành
* **Tầng DTO (`com.group2.rms.user.dto`):**
  * `UserProfileResponse`: Đóng gói dữ liệu hiển thị hồ sơ cá nhân (username, fullName, email, roleName, departmentName, phoneNumber, avatarUrl,...).
  * `UpdateProfileRequest`: Nhận dữ liệu cập nhật (fullName, phoneNumber, avatarUrl) kèm validation đầy đủ.
  * `ChangePasswordRequest`: Nhận thông tin đổi mật khẩu (currentPassword, newPassword, confirmPassword).
* **Tầng Service & Controller (`com.group2.rms.user`):**
  * `UserProfileService`: Trích xuất user từ `SecurityContextHolder`, cập nhật thông tin người dùng, kiểm tra mật khẩu hiện tại bằng Spring Security `PasswordEncoder.matches()`, mã hóa và lưu mật khẩu mới.
  * `UserProfileController`: Cung cấp route `GET /profile` render view và `POST /profile/change-password` hỗ trợ AJAX phản hồi nhanh.
* **Giao diện & Trải nghiệm Người dùng (`profile.html`, `profile.js`, `profile.css`):**
  * Giao diện chia thành 2 card: Thông tin tài khoản (chỉ đọc) và Thông tin cá nhân (chỉnh sửa).
  * **Cơ chế Dirty Check:** Nút "Save Profile" ở trạng thái disable, chỉ tự động kích hoạt khi người dùng thực sự thay đổi dữ liệu trên form. Bổ sung nút "Discard Changes" để hoàn tác dữ liệu ban đầu.
  * **Modal xác nhận:** Hiển thị popup xác nhận trước khi submit form cập nhật hồ sơ để tránh thao tác nhầm.
  * **Đổi mật khẩu bảo mật qua Modal & AJAX:** Người dùng đổi mật khẩu trực tiếp trong modal không reload trang; bắt lỗi tức thời (mật khẩu cũ không chính xác, mật khẩu mới không khớp, độ dài không hợp lệ) và thông báo qua Toast notification.
  * **Ràng buộc dữ liệu nghiêm ngặt:** Regex họ tên Unicode tiếng Việt (`^[\p{L}][\p{L}\s.'-]*$`) chấp nhận đầy đủ dấu tiếng Việt mà không cho phép ký tự đặc biệt phá vỡ hiển thị; regex kiểm tra format SĐT và URL avatar hợp lệ.
  * Đã tích hợp link My Profile vào thanh điều hướng `sidebar.html`.

### 2. Các hạng mục còn dở & Kế hoạch hoàn thiện
1. **Đồng bộ với `main`:** Do `main` vừa merge PR #6, #9, #10, nhánh `feature/dunglt/user-profile` cần merge `origin/main` vào và xử lý **1 conflict nhỏ tại `sidebar.html`** (sắp xếp vị trí menu My Profile cạnh Offer Management).
2. **Bổ sung Unit Test:** Viết bộ test case cho `UserProfileService` và `UserProfileController` (kiểm tra cập nhật thành công, đổi mật khẩu đúng, đổi mật khẩu sai mật khẩu cũ,...).
3. **Mở Pull Request:** Mở PR chính thức lên `main` để hoàn tất Iteration 1 cho module Profile.
