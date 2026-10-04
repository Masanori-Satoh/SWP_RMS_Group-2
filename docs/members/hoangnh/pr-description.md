## 📑 Tổng quan (Overview)

Pull Request này tập trung vào 3 mục tiêu chính:

1. **Khắc phục xung đột merge từ nhánh `main` và chuẩn hóa môi trường:** Giải quyết triệt để các xung đột code, đồng bộ cấu hình môi trường local (`application-local.properties`), sửa ánh xạ Entity với SQL Server và bổ sung dependency kiểm thử bảo mật.
2. **Hoàn thiện phân quyền Requisition cho HR (P0 - I2-REQ-01..03):** Ràng buộc phạm vi dữ liệu chặt chẽ để HR **chỉ được xem các đơn đã được duyệt (`Approved`)** trên cả danh sách, tìm kiếm, phân trang và chặn hoàn toàn (HTTP 403) khi cố truy cập URL trực tiếp của các trạng thái khác.
3. **Phát triển Phân hệ Thông báo nội bộ (In-App Notification System - P0 - I2-NOTI-01..06):** Xây dựng module thông báo dùng chung, tự động phát sinh thông báo riêng gửi thẳng đến Hiring Manager kèm lý do phản hồi khi Giám đốc (Director) từ chối đơn yêu cầu, tích hợp cơ chế chống trùng lặp (Idempotent) và nút xem/chỉnh sửa đơn tức thì.
4. **Đồng bộ thanh điều hướng chung (Sidebar Navigation):** Bổ sung mục **Interview Schedule** và **Notifications** vào thanh sidebar làm việc `workspace` chung của hệ thống.

---

## 💅 Chi tiết các thay đổi (Detailed Changes)

### 1. Phân quyền và Phạm vi dữ liệu Requisition (HR Scope & Security)

* **Tầng Service & Access Control (`com.group2.rms.requisition.service`):**
  * `RequisitionServiceImpl`: Tách riêng nhánh `HR` khỏi `Director` trong hàm `scope()`, áp dụng điều kiện bắt buộc `approvalStatus = 'Approved'`. Phạm vi này áp dụng đồng bộ cho toàn bộ các hàm `search()`, `countVisible()`, tính tổng số bản ghi và phân trang. HR truyền tham số tìm kiếm khác trạng thái này sẽ nhận kết quả rỗng.
  * `RequisitionAccess`: Cập nhật hàm `requireView()`, đưa quy tắc kiểm tra vai trò `HR` lên trước quy tắc sở hữu `owns()` (ngăn chặn ngoại lệ đổi role tài khoản). Ném ngoại lệ `AccessDeniedException` (HTTP 403 Forbidden) nếu HR cố tình truy cập xem chi tiết các đơn `Draft`, `Pending_Director` hoặc `Rejected`.

* **Giao diện & Trải nghiệm Người dùng (Frontend UX):**
  * `templates/requisitions/list.html`: Điều chỉnh dropdown bộ lọc trạng thái, chỉ hiển thị duy nhất tùy chọn `Approved` khi vai trò người dùng hiện tại là `HR`.

---

### 2. Phân hệ Thông báo nội bộ (In-App Notification Module)

* **Tầng DTO (`com.group2.rms.notification`):**
  * `NotificationResponse`: DTO dạng Java `record` bất biến (immutability) đóng gói dữ liệu thông báo trả về (ID, tiêu đề, nội dung, loại sự kiện, reference ID, link điều hướng, trạng thái đã đọc, thời gian tạo).

* **Tầng Thực thể & Dữ liệu (`com.group2.rms.notification`):**
  * `Notification`: Entity ánh xạ bảng `Notification`, lưu trữ khóa ngoại `RecipientId` liên kết với `User`, tiêu đề, nội dung phản hồi, `eventType` (`REQUISITION_REJECTED`), đường dẫn `linkUrl`, cờ `isRead` và khóa `eventId` unique.
  * `NotificationRepository`: Hỗ trợ phân trang theo tài khoản `findByRecipient_UserIdOrderByCreatedAtDesc`, đếm số thông báo chưa đọc `countByRecipient_UserIdAndIsReadFalse` và cập nhật đánh dấu đã đọc.

* **Tầng Service & Controller (`com.group2.rms.notification`):**
  * `NotificationService` & `NotificationServiceImpl`:
    * Phương thức `notifyRequisitionRejected()`: Kích hoạt tạo thông báo chi tiết cho Hiring Manager khi đơn bị từ chối.
    * Tích hợp kiểm tra **Idempotency** thông qua `eventId = "REQ_REJECT_{id}_{decidedTime}"`, ngăn chặn tạo trùng thông báo khi người dùng retry hoặc gửi lại request.
    * Xử lý đánh dấu đã đọc (`markAsRead`, `markAllAsRead`) có kiểm tra quyền sở hữu thông báo.
  * `NotificationController`:
    * Route `GET /notifications`: Render giao diện danh sách thông báo của tài khoản đang đăng nhập.
    * Route `POST /notifications/{id}/read`: Đánh dấu đã đọc và chuyển hướng tức thì tới đơn liên quan.
    * Route `POST /notifications/read-all`: Đánh dấu đọc tất cả thông báo.
    * Route `GET /notifications/unread-count`: API trả về số lượng thông báo chưa đọc phục vụ cập nhật badge.

* **Tích hợp Luồng Nghiệp vụ Tuyển dụng:**
  * `RequisitionServiceImpl`: Tiêm `NotificationService` và gọi `notifyRequisitionRejected()` bên trong phương thức `decide()` khi Giám đốc chọn `Reject` có phản hồi hợp lệ trong cùng database transaction.

* **Giao diện & Database Migration:**
  * `templates/notifications/list.html`: Giao diện danh sách thông báo theo Design Tokens RMS, hiển thị badge trạng thái "New" / "Read", khung lý do từ chối rõ ràng và nút "View Details" điều hướng một chạm tới Requisition.
  * `static/css/notifications.css`: File định kiểu độc lập, không dùng style nội tuyến.
  * `database/migrations/005_notification.sql`: Script khởi tạo bảng `Notification`, thiết lập ràng buộc khóa ngoại và index tối ưu truy vấn.

---

### 3. Đồng bộ Schema Database và Loại bỏ Trường `Version`

* **Khôi phục tương thích Database dùng chung (`db.sql`):**
  * Gỡ bỏ hoàn toàn trường `@Version private Long version` khỏi entity `JobRequisition`, các DTO (`RequisitionRequest`, `RequisitionResponse`), giao diện form/modal HTML và API controller.
  * Không yêu cầu nhóm thêm cột `Version`. Giữ nguyên `database/schema/db.sql`; những chênh lệch schema khác cần được kiểm tra riêng trước khi merge.

---

### 4. Giải quyết Xung đột Merge `main`, Cấu hình & Giao diện Sidebar

* **Khắc phục lỗi biên dịch & Import:** Cập nhật lại đường dẫn import đúng của module `admin` (`com.group2.rms.admin.dto.ActivityLogResponse`, `admin.entity.AuditLog`, `admin.repository.AuditLogRepository`) sau khi nhánh `main` tái cấu trúc package; dọn sạch các thẻ conflict Git.
* **Đồng bộ Schema Entity:** Ánh xạ lại `@Column(name = "RequiredGender")` trong `JobRequisition.java` khớp với cột trong cơ sở dữ liệu SQL Server.
* **Cấu hình môi trường Local:** Bổ sung `src/main/resources/application-local.properties` ghi đè mật khẩu database cá nhân (`sa/123`) và giữ cổng chạy `8088`.
* **Bổ sung Test Dependency:** Thêm thư viện `spring-security-test` vào `pom.xml` để hỗ trợ kiểm thử `@WithMockUser` và CSRF token.
* **Cập nhật Navigation Sidebar:** Thêm menu **Interview Schedule** (`/interviews`) và **Notifications** (`/notifications`) vào thanh sidebar `workspace` trong `fragments/sidebar.html`; cấu hình route trong `SecurityConfig.java`.

---

## 🧪 Kiểm thử & Đảm bảo Chất lượng (Testing & QA)

* **Unit Tests Phân quyền (`RequisitionAccessTest`):**
  * `hr_canViewApproved`: Xác thực HR truy cập thành công đơn trạng thái `Approved`.
  * `hr_cannotViewDraft / hr_cannotViewPending / hr_cannotViewRejected`: Xác thực HR bị chặn với `AccessDeniedException` 403 khi truy cập các trạng thái còn lại.
  * `hr_evenIfOwner_cannotViewNonApproved`: Xác thực HR từng tạo đơn khi còn là HM vẫn bị chặn nếu đơn chưa `Approved`.
  * `director_viewRules`: Xác thực Giám đốc được xem Pending/Approved và bị chặn xem Draft.
* **Unit Tests Thông báo (`NotificationServiceImplTest`):**
  * `notifyRequisitionRejected_success`: Kiểm tra tạo thông báo đúng người nhận HM, đúng tiêu đề, nội dung lý do và link.
  * `notifyRequisitionRejected_idempotent_skipsDuplicate`: Kiểm tra cơ chế chống tạo trùng lặp với cùng `eventId`.
  * `markAsRead_success`: Kiểm tra cập nhật trạng thái đọc của người dùng.
* **Unit Tests Nghiệp vụ Requisition (`JobRequisitionServiceTest`):**
  * `svc07_reject_notifiesHiringManager`: Xác thực luồng Giám đốc reject đơn kích hoạt gọi `NotificationService`.
  * `svc_hr_searchScope_onlyApproved`: Xác thực luồng tìm kiếm của HR luôn áp dụng bộ lọc chỉ `Approved`.
* **Cập nhật Checklist:** Đánh dấu hoàn thành các đầu việc `I2-REQ-01..03` và `I2-NOTI-01..06` tại `docs/members/hoangnh/checklist.md`.
