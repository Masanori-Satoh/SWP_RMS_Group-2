# Sửa lỗi Maven compile sau refactor Dashboard

Ngày: 28/10/2026. Không thay database/schema, không đổi OTP lifetime, không commit/push.

## Nguyên nhân

Log `mvn clean compile spring-boot:run` dừng ở compiler với 30 lỗi import/type tại `dashboard/DashboardService.java`. Module Dashboard hiện đã tách thành `admin`, `internal`, `shared`; repository và DTO cũ không còn trong package gốc. Commit `e03acee` lại thêm `dashboard/DashboardService.java` cũ vào source, khiến clean build gặp các import đã mất. Đây là lỗi source sau khi gộp refactor, khác với lỗi bytecode RegistrationController không đồng bộ ở lượt trước.

## Sửa đổi

- Gỡ `src/main/java/com/group2/rms/dashboard/DashboardService.java` cũ. Các controller/service production hiện dùng `DashboardEntryController`, `AdminDashboardService` và `InternalDashboardService`; không còn nơi gọi service cũ. Những tên cũ còn trong comment/test đã comment không phải dependency đang chạy.
- Sửa `dashboard/admin/service/AdminDashboardService.java`: truyền đúng năm thành phần cho `AccountSummary` (`title`, `url`, `total`, `active`, `inactive`), bỏ đối số Blocked còn sót. Giữ các controller, DTO mới, quyền truy cập và logic Dashboard từng role của refactor.
- Không phục hồi package DTO cũ, không thêm shim/service trùng chức năng, không sửa module Interview/Candidate/Auth hoặc cấu hình lifetime của người dùng.

## Kiểm chứng

- `mvn.cmd -B -DskipTests clean compile`: **BUILD SUCCESS**, biên dịch lại 351 file source. Còn một warning Lombok ở `InterviewSchedule`, không phải lỗi build và không sửa ngoài phạm vi.
- `javap` xác nhận `RegistrationController.register` và `verifyOtp` dùng các DTO đầy đủ package `com.group2.rms.auth.dto`, không còn signature unresolved type trong build mới.
- **52 test hiện có của Dashboard mới qua, 0 failure/error/skipped**: AdminDashboardAnalyticsTests, AdminDashboardViewTests, DashboardEntryRedirectTests, InternalDashboardServiceTests và service HR/HM/Director/Interviewer. Toàn bộ test source cũng được Maven biên dịch thành công. Không bổ sung test lặp lại implementation.
- OTP lifetime và resend delay của RegistrationVerificationService/PasswordRecoveryService giữ nguyên giá trị người dùng đã đặt.

- Đã chạy `spring-boot:run` trên cổng kiểm thử tự chọn **56973** với `ddl-auto=validate`, xác nhận Spring Boot khởi động thành công và GET `/login`, `/register`, `/forgot-password` đều HTTP **200**. Không gửi OTP/email hoặc chạy thao tác ghi dữ liệu.
- Phiên kiểm thử PID 2724 đã được dừng sau kiểm tra; không chiếm cổng 8081 để người dùng chạy lại bằng terminal của mình.
- Runtime kiểm thử đặt riêng `jdk.net.unixdomain.tmpdir=target/nio-sockets` và tắt DevTools restart qua JVM args để tránh lỗi temp Unix-domain socket của môi trường tool Windows. Không thay source cấu hình hoặc cài đặt hệ thống.
- `git diff --check`: qua. Log build/test/startup tại `%TEMP%/rms-dashboard-refactor-*.log`.

Người dùng có thể chạy lại tại checkout hiện tại:

```powershell
mvn.cmd clean compile spring-boot:run
```
