# Hướng dẫn test module Thông báo (Notification)

Cập nhật 2026-10-18, sau đợt viết lại test notification theo `CLAUDE.md` Phần E (mục 23). Cách viết từng tầng xem thêm `requisition-testing-guide.md` (cùng mẫu).

## 1. Lệnh chạy

```bash
# Toàn bộ test notification (NotificationIntegrationTests cần SQL Server đang chạy)
./mvnw clean test -Dtest='Notification*Tests*'

# Máy thiếu RAM: heap thấp, tắt jacoco
./mvnw clean test -Dtest='Notification*Tests*' '-DargLine=-Xmx512m -XX:+UseSerialGC' -Djacoco.skip=true

# Một nhóm @Nested / một hàm (class có @Nested phải thêm *, nếu không sẽ báo "Tests run: 0")
./mvnw test -Dtest='NotificationServiceImplTests*#markAsRead_shouldKeepFirstReadAt_whenAlreadyRead'
```

Trên Windows PowerShell dùng `.\mvnw.cmd` và đặt mỗi tham số `-D...` trong dấu nháy kép.
Gặp "Unresolved compilation problems" → chạy lại với `clean` (VS Code cũng biên dịch vào `target/`).

## 2. Class test và tầng

| Class test | Tầng | Class được test | Spring / DB |
|---|---|---|---|
| `NotificationServiceImplTests` | Unit — Service (Mockito) | `NotificationServiceImpl` (nội dung từng loại thông báo, chống trùng, phân trang, đánh dấu đã đọc) | Không / Không |
| `NotificationControllerTests` | Controller (`@WebMvcTest`) | `NotificationController` (trang `/notifications`: view / redirect / flash, 404, CSRF, chặn open redirect) | Web / Không |
| `NotificationApiControllerTests` | Controller (`@WebMvcTest`) | `NotificationApiController` (JSON cho chuông trên topbar) | Web / Không |
| `NotificationIntegrationTests` | Integration (`@SpringBootTest`) | 2 câu `@Modifying @Query`, truy vấn theo người nhận, unique index, NVARCHAR, rollback | Đủ / **Có** |

Service test dựng service bằng tay: `new NotificationServiceImpl(repositoryMock, Clock.fixed(2026-10-18 10:00 VN))`.
Phía gọi (`RequisitionServiceTests`, `JobPostingServiceTests`) đã kiểm tra: thành công → `notify…` gọi đúng 1 lần; bị chặn → `verifyNoInteractions(notificationService)`.

## 3. Hàm nào đã có test

| Hàm | Test (tên rút gọn) |
|---|---|
| `notifyRequisitionRejected` | đúng HM / actor / link / `REQ_REJECT_{approvalId}`, cắt khoảng trắng lý do, lý do null / rỗng / khoảng trắng → "Không có lý do cụ thể", thiếu Giám đốc + ngày (không lặp chữ "Giám đốc"), thiếu mã → `REQ-{id}`, rejection null / không có HM → không lưu, eventId đã có → bỏ qua, từ chối lần 2 → thông báo mới |
| `notifyRequisitionApproved` | đúng HM / actor / link / `REQ_APPROVE_{approvalId}`, có ý kiến (cắt khoảng trắng) → dòng "Ý kiến", ý kiến null / rỗng / khoảng trắng → không có dòng "Ý kiến", approval null / không có HM → không lưu, eventId đã có → bỏ qua |
| `notifyJobPostingPublished` | đăng ngay, hẹn đăng, ngày đăng = hiện tại (biên → đăng ngay), chưa có ngày đăng, không hạn nộp + không rõ HR, đổi lịch → eventId mới, eventId đã có → bỏ qua, tin / yêu cầu / HM null → không lưu |
| `notifyOfferLetterSent` | đúng ứng viên / link `/portal/offers/{id}`, tên vị trí trống → "Vị trí tuyển dụng", thiếu tài khoản / hồ sơ / offer null → không lưu, eventId đã có → bỏ qua |
| `notifyOfferResponseFromCandidate` | nhận offer → `OFFER_ACCEPTED`, từ chối có lý do (đã cắt khoảng trắng), từ chối không lý do → không có đoạn "Lý do", ứng viên không có tài khoản → tên "Ứng viên", không rõ HR / offer null → không lưu, eventId đã có → bỏ qua |
| `getNotificationsForUser` | map sang DTO, chỉnh page / size (trang 0, âm, size 0, âm, đúng `MAX_PAGE_SIZE`, `MAX_PAGE_SIZE + 1`, rất lớn), user null → trang rỗng không gọi repository |
| `getUnreadCount / markAllAsRead / markOfferNotificationRead` | trường hợp thường (giờ lấy từ `Clock`), user / id null → không gọi repository |
| `markAsRead` | đánh dấu đúng giờ hiện tại, đã đọc giữ thời điểm đầu, của người khác / không tồn tại → `ResourceNotFoundException`, id / user null |
| `NotificationController` | danh sách + số chưa đọc, trạng thái rỗng, page / size chuyển cho service, chưa đăng nhập → login, đánh dấu đọc → flash, "Xem chi tiết" → link nội bộ, link `//…` / `https://…` / `javascript:` / không có `/` / null → ở lại trang (chặn open redirect), của người khác → 404, thiếu CSRF → 403, đọc tất cả + thiếu CSRF, chưa đăng nhập không gọi service |
| `NotificationApiController` | `unread-count` (thường, service lỗi → 0, tài khoản khóa → đăng xuất, chưa đăng nhập), `recent` (đúng trang 1 cỡ 5, cấu trúc JSON, service lỗi → rỗng), `{id}/read` (của mình / người khác → 400), `read-all` (thường / service lỗi → 400) |
| `NotificationRepository` (JPQL + DB) | `markAllAsReadByUserId` chỉ đổi dòng chưa đọc của đúng người, `markOfferNotificationRead` chỉ đúng loại + đúng offer, tìm theo id + người nhận, danh sách mới nhất trước + đếm chưa đọc theo người, unique (RecipientId, EventId) chặn trùng / cho phép khác người nhận, tiếng Việt + `\n`, rollback nghiệp vụ → không ghi thông báo |

## 4. Kết quả lần chạy gần nhất (2026-10-18)

- Notification: 97 test PASS (service 63, controller 16, API 10, integration 8).
- Toàn dự án: 1275 test, 5 lỗi đều có từ trước (`CareerFlowTests` 2, `CareerReadOnlyTests` 1, `SidebarMenuTests` 1, `SidebarRoleWebTests` 1).

## 5. Ghi chú hành vi (test đang khóa theo hành vi hiện tại)

- API `/api/notifications/{id}/read` trả **400** (không phải 404) khi thông báo không thuộc người xem: controller bắt mọi exception.
- `SecurityConfig` bỏ CSRF cho `/api/**` → POST của API không cần token (không có test "thiếu CSRF → 403" cho API).
- Trang `/notifications` chưa hiện thanh phân trang (controller có `currentPage` / `totalPages` nhưng template không dùng) → chỉ xem được 10 thông báo mới nhất.
