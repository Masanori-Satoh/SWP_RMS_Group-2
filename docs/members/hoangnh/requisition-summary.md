# BÁO CÁO TỔNG QUAN MODULE JOB REQUISITION (YÊU CẦU TUYỂN DỤNG)

- **Dự án:** Recruitment Management System (SWP_RMS_Group-2)
- **Thành viên phụ trách:** Nguyễn Huy Hoàng (HoangNH)
- **Module:** Yêu cầu Tuyển dụng (Job Requisition) & Thông báo (Notification)
- **Mã chức năng (SRS):** 5.1.14 (List), 5.1.15 (Create/Update/Copy), 5.1.16 (Details/Approve/Reject)
- **Nhánh Git:** `feat/hoang-Requisition-List-Screen`

---

## I. MỤC TIÊU NGHIỆP VỤ (BUSINESS OBJECTIVES)

Module **Job Requisition** là mắt xích đầu tiên và cốt lõi trong quy trình tuyển dụng nội bộ của hệ thống RMS:
1. **Hiring Manager (HM):** Khởi tạo nhu cầu nhân sự của phòng ban, thiết lập bộ tiêu chí sàng lọc tự động (AI Screening Criteria), lưu nháp hoặc gửi đơn phê duyệt lên Giám đốc.
2. **Director:** Xem xét các đơn đang chờ (`Pending_Director`), đưa ra quyết định Phê duyệt (`Approved`) hoặc Từ chối (`Rejected` - bắt buộc kèm lý do giải thích).
3. **HR:** Tiếp nhận các yêu cầu tuyển dụng đã được duyệt (`Approved`) để chuẩn bị đăng tin tuyển dụng (`Job Posting`).
4. **Hệ thống Thông báo (In-App Notification):** Tự động phát sinh thông báo riêng gửi tới Hiring Manager ngay khi đơn bị Giám đốc từ chối để HM kịp thời chỉnh sửa và nộp duyệt lại.

---

## II. KIẾN TRÚC HỆ THỐNG VÀ THIẾT KẾ KỸ THUẬT

Tuân thủ nghiêm ngặt theo quy chuẩn kiến trúc **Package-by-Feature** tại [`docs/architecture/ARCHITECTURE_GUIDE.md`](../../architecture/ARCHITECTURE_GUIDE.md):

### 1. Phân chia Module & Package
* **`com.group2.rms.requisition`**: Module lớn, tổ chức đa tầng phân lớp rõ ràng:
  * `controller/`: Tiếp nhận HTTP requests, xử lý dữ liệu form và mapping Model.
  * `service/`: Triển khai các quy tắc nghiệp vụ tuyển dụng (`RequisitionService`, `RequisitionServiceImpl`, `RequisitionAccess`).
  * `repository/`: Tầng truy cập dữ liệu Spring Data JPA và `JpaSpecificationExecutor`.
  * `entity/`: Các thực thể JPA ánh xạ tới bảng SQL Server.
  * `dto/`: Request/Response truyền tải dữ liệu an toàn ra view, tránh vòng lặp Lazy Loading.
  * `validator/`: Kiểm tra tính toàn vẹn và quy tắc nghiệp vụ chặt chẽ phía server.
  * `exception/`: Chứa `RequisitionValidationException` phục vụ render lỗi chi tiết về form.
* **`com.group2.rms.notification`**: Module dùng chung, tổ chức phẳng (`Flat by default`) theo chuẩn kiến trúc:
  * Chứa đầy đủ `Notification`, `NotificationRepository`, `NotificationService`, `NotificationServiceImpl`, `NotificationController`, `NotificationResponse`.

### 2. Các Thực thể Cơ sở dữ liệu (Database Entities)
* **`JobRequisition`**: Lưu trữ toàn bộ thông tin đơn (Tiêu đề, Phòng ban, Số lượng, Loại hình, Mức lương min/max, Địa điểm, Mô hình làm việc, Hạn thử việc, Ngày bắt đầu, Trạng thái phê duyệt, Concurrency Version `@Version`).
* **`ScreeningCriteria`**: Quan hệ 1–N với `JobRequisition`, lưu danh sách tiêu chí AI (Tên tiêu chí, Loại tiêu chí: Skill/Education/Experience/Knockout, Giá trị yêu cầu, Trọng số điểm Weight %, Cờ bắt buộc isMandatory).
* **`RequisitionApproval`**: Lưu lịch sử các lần Giám đốc phê duyệt hoặc từ chối kèm phản hồi.
* **`RequisitionWorkflowEvent`**: Lưu dòng thời gian (Timeline) các sự kiện luân chuyển trạng thái (Submitted, Approved, Rejected, Withdrawn).
* **`Notification`**: Lưu thông báo in-app cho người dùng (Người nhận, Tiêu đề, Nội dung, Loại sự kiện, Link điều hướng, Trạng thái đã đọc, `eventId` unique chống tạo trùng).

---

## III. CÁC TÍNH NĂNG VÀ MÀN HÌNH ĐÃ HOÀN THÀNH

### 1. Màn hình Danh sách Requisition (Requisition List Screen — 5.1.14)
- [x] **Hiển thị & Phân trang:** Bảng danh sách chuẩn hóa phân trang (`currentPage`, `totalPages`, `pageSize`, `totalElements`).
- [x] **Tìm kiếm đa trường:** Tìm kiếm từ khóa theo Tiêu đề công việc và Tên phòng ban (hỗ trợ escape ký tự đặc biệt SQL chống lỗi).
- [x] **Bộ lọc đa tiêu chí:** Lọc theo Phòng ban, Hình thức tuyển dụng (Full-time, Part-time, Internship, Contract), Trạng thái phê duyệt.
- [x] **Sắp xếp linh hoạt:** Mới nhất, Cũ nhất, Tên chức danh A–Z, Tên chức danh Z–A.
- [x] **Menu Thao tác nhanh (Floating Action Dropdown):** Tối ưu hóa UI/UX với menu thao tác (Xem chi tiết, Sửa, Rút đơn, Xóa) không bị tràn bảng (`overflow`).
- [x] **Phân quyền HR chặt chẽ (P0 - Done):**
  - Tách scope HR trong `RequisitionServiceImpl.scope()`: HR **chỉ truy vấn đơn có trạng thái `Approved`**.
  - Áp dụng cùng scope cho cả `search()`, `countVisible()`, tổng số bản ghi và phân trang.
  - Dropdown lọc trạng thái trên giao diện `list.html` chỉ hiển thị lựa chọn `Approved` khi viewer là HR.
  - Khi HR cố tình truyền param URL khác (như `?status=Draft`), hệ thống trả về 0 kết quả ngoài phạm vi.

### 2. Màn hình Tạo mới, Chỉnh sửa & Sao chép (Create / Update / Copy Screen — 5.1.15)
- [x] **Form nhập liệu toàn diện:** Đầy đủ các trường thông tin vị trí, chế độ và yêu cầu tuyển dụng.
- [x] **Lưu nháp (Save Draft):** Cho phép Hiring Manager lưu lại tiến độ đang làm dở mà không bắt buộc điền hết mọi trường.
- [x] **Nộp duyệt (Submit to Director):** Server kiểm tra nghiêm ngặt: tiêu đề, phòng ban, số lượng > 0, ngày bắt đầu từ hiện tại trở đi, mô tả, yêu cầu; tổng trọng số tiêu chí AI bắt buộc phải tròn **100%**.
- [x] **Quản lý Tiêu chí AI động:** Thêm/xóa dòng tiêu chí trực tiếp trên giao diện bằng JavaScript, tự động lọc bỏ các dòng trống khi lưu DB.
- [x] **Chỉnh sửa an toàn (Edit):** Cho phép sửa đơn ở trạng thái `Draft` hoặc `Rejected`, kiểm tra Concurrency `@Version` chống xung đột ghi đè.
- [x] **Sao chép đơn (Copy Requisition):** Nhân bản nhanh một đơn cũ sang form mới (tự động reset `version` và toàn bộ `criteriaId` về null).

### 3. Màn hình Chi tiết Requisition (Requisition Details Screen — 5.1.16)
- [x] **Xem chi tiết đa chiều:** Hiển thị thông số tuyển dụng, bảng tiêu chí AI, lịch sử duyệt và Timeline tiến trình trực quan.
- [x] **Phê duyệt / Từ chối (Director Action):**
  - Modal thao tác cho Giám đốc duyệt đơn đang chờ (`Pending_Director`).
  - Khi từ chối (`Reject`): Bắt buộc Giám đốc phải nhập lý do phản hồi (Feedback/Comments) để Hiring Manager biết hướng điều chỉnh.
- [x] **Rút đơn (Withdraw):** Cho phép Hiring Manager rút đơn `Pending_Director` về lại trạng thái `Draft` để tự cập nhật lại thông tin.
- [x] **Xóa đơn bảo vệ (Delete):** Cho phép xóa đơn nháp/bị từ chối; chặn xóa an toàn nếu đơn đã liên kết với Tin tuyển dụng (`JobPosting`).
- [x] **Kiểm soát bảo mật URL (P0 - Done):**
  - Trong `RequisitionAccess.requireView()`: HR truy cập trực tiếp ID của đơn `Draft`, `Pending_Director` hoặc `Rejected` sẽ bị ném ngay `AccessDeniedException` (HTTP 403 Forbidden).

### 4. Hệ thống Thông báo In-App (Notification System — P0 - Done)
- [x] **Tự động gửi thông báo từ chối:** Khi Giám đốc từ chối đơn với feedback hợp lệ trong `RequisitionServiceImpl.decide()`, hệ thống tự động phát sinh thông báo riêng gửi thẳng đến tài khoản của Hiring Manager sở hữu đơn trong cùng transaction.
- [x] **Nội dung thông báo chi tiết:** Nêu rõ tên đơn tuyển dụng, Giám đốc từ chối, thời điểm và lý do từ chối.
- [x] **Liên kết một chạm:** Nút "View Details" dẫn thẳng về trang chỉnh sửa Requisition để HM cập nhật và nộp duyệt lại.
- [x] **Cơ chế Idempotent:** Sử dụng trường `eventId` unique chống tạo trùng thông báo khi người dùng retry hoặc gửi lại request.
- [x] **Màn hình Danh sách Thông báo (`/notifications`):**
  - Giao diện `notifications/list.html` và CSS `notifications.css` đồng bộ chuẩn tokens RMS.
  - Hiển thị badge "New", thời gian tạo, phân trang và thao tác đánh dấu đã đọc ("Mark as read", "Mark all as read").
  - API đếm thông báo chưa đọc (`GET /notifications/unread-count`).
- [x] **Cập nhật Sidebar:** Thêm liên kết **Interview Schedule** (`/interviews`) và **Notifications** (`/notifications`) vào thanh sidebar `workspace`.
- [x] **Migration Script:** Cung cấp file migration SQL [`database/migrations/005_notification.sql`](../../database/migrations/005_notification.sql).

---

## IV. BẢO MẬT VÀ PHÂN QUYỀN (ACCESS MATRIX)

| Vai trò (Role) | Xem Danh sách | Xem Chi tiết | Tạo / Sửa / Copy | Approve / Reject | Rút đơn (Withdraw) | Xóa đơn |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **Hiring Manager** | Đơn của chính mình | Đơn của chính mình | Tạo mới; sửa Draft/Rejected của mình | ❌ Không | Đơn Pending của mình | Đơn Draft của mình |
| **Director** | Mọi đơn trừ Draft | Mọi đơn trừ Draft | ❌ Không | Duyệt/Từ chối đơn Pending (kèm lý do) | ❌ Không | ❌ Không |
| **HR** | **Chỉ đơn Approved** | **Chỉ đơn Approved** (khác: 403) | ❌ Không | ❌ Không | ❌ Không | ❌ Không |
| **System Admin** | Toàn quyền xem | Toàn quyền xem | Sửa Draft/Rejected | ❌ Không tự duyệt | Đơn Pending | Đơn Draft |
| **Candidate / Guest** | ❌ Chặn 403 | ❌ Chặn 403 | ❌ Chặn 403 | ❌ Chặn 403 | ❌ Chặn 403 | ❌ Chặn 403 |

---

## V. KẾT QUẢ KIỂM THỬ (TESTING & QUALITY ASSURANCE)

1. **Validator Tests (`RequisitionValidatorTest`):** 147 test cases Passed bao phủ mọi trường hợp biên của việc lưu nháp và nộp duyệt.
2. **Access Control Tests (`RequisitionAccessTest`):** Đã viết bộ test riêng biệt kiểm tra ma trận phân quyền:
   - HR xem đơn `Approved` -> Thành công.
   - HR xem đơn `Draft`, `Pending_Director`, `Rejected` -> Ném `AccessDeniedException` 403.
   - HR dù là người tạo đơn cũ nhưng role hiện tại là HR -> Vẫn bị chặn nếu đơn chưa `Approved`.
   - Director xem `Pending`/`Approved` -> Cho phép; xem `Draft` -> Chặn.
   - Hiring Manager xem đơn người khác -> Chặn.
3. **Service & Notification Tests (`JobRequisitionServiceTest`, `NotificationServiceImplTest`):**
   - Kiểm tra Director reject -> Gửi notification đúng cho HM kèm feedback và link.
   - Kiểm tra tính Idempotent của Notification (không tạo duplicate khi cùng `eventId`).
   - Kiểm tra đánh dấu đã đọc và đếm số lượng chưa đọc.
4. **Cấu hình Kiểm thử Security:** Bổ sung dependency `spring-security-test` vào `pom.xml` hỗ trợ kiểm thử `@WithMockUser` và CSRF token đầy đủ.
