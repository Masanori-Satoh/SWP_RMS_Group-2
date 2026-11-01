# 01/11/2026 — Dọn dấu vết AI screening trong Admin

## Yêu cầu và phạm vi được duyệt

Người dùng yêu cầu dọn các dấu vết AI screening còn sót trong code và phải hỏi trước khi ảnh hưởng đến module thành viên khác. Sau khi xem phạm vi đề xuất, người dùng chọn: **“Chỉ dọn phần Admin, giữ nguyên 9 file còn lại”**. Người dùng nhắc lại yêu cầu cập nhật mọi thay đổi vào work log.

Code triển khai AI screening đã được gỡ trong nhánh main trước lượt này. Lượt này dọn fixture test và comment còn sót thuộc Admin/Admin Dashboard.

## Thay đổi theo file

| File | Thay đổi |
|---|---|
| `src/main/java/com/group2/rms/admin/service/ApiMonitoringService.java` | Xóa comment lịch sử nhắc dòng AI CV Screening đã gỡ. Danh sách dịch vụ vẫn gồm nội bộ và email. |
| `src/main/java/com/group2/rms/dashboard/admin/service/AdminDashboardService.java` | Xóa comment lịch sử nhắc lối tắt cấu hình AI đã gỡ. |
| `src/test/java/com/group2/rms/admin/service/AuditLogTargetRepositoryTests.java` | Đổi dữ liệu mock cấu hình từ `AI_SCREENING_ENDPOINT` sang `SMTP_HOST`; giữ kiểm tra khóa chuỗi và việc truy vấn không đọc `ConfigValue`. |
| `src/test/java/com/group2/rms/dashboard/admin/AdminDashboardViewTests.java` | Bỏ dòng giám sát AI giả khỏi fixture; đổi tên test theo danh sách giám sát hiện tại. Trường hợp SMTP chưa cấu hình vẫn được kiểm tra riêng. |

## Kiểm chứng

- `AuditLogTargetRepositoryTests`: 4/4 qua.
- `AdminDashboardViewTests`: 3/3 qua, render bằng Spring MVC với service mock.
- `EmailMonitoringServiceTests`: 12/12 qua.
- Lệnh Maven chỉ chạy ba lớp trên kết thúc thành công: **19 test, 0 failure, 0 error**.
- `git diff --check`: không có lỗi khoảng trắng.
- Đối chiếu hash trước/sau để xác nhận 9 file được giữ lại chưa bị chỉnh sửa.

Lượt kiểm tra ban đầu có chạy thêm `EmailMonitoringWebTests`; **7 test tích hợp bị lỗi kết nối SQL Server trước khi mở được transaction**. Đây là giới hạn kiểm chứng DB, không tính là test đã qua. SMTP probe trong lớp test này được mock.

## Phần được giữ nguyên theo lựa chọn của người dùng

1. `src/main/java/com/group2/rms/dashboard/internal/service/HrDashboardService.java`
2. `src/main/java/com/group2/rms/jobposting/service/JobPostingServiceImpl.java`
3. `src/test/java/com/group2/rms/candidate/application/pipeline/controller/ApplicationPipelineWebTests.java`
4. `src/test/java/com/group2/rms/candidate/application/pipeline/service/ApplicationReviewServiceTests.java`
5. `src/test/java/com/group2/rms/candidate/application/portal/repository/MyApplicationQueryTests.java`
6. `src/test/java/com/group2/rms/candidate/domain/rule/ApplicationAccessTests.java`
7. `src/test/java/com/group2/rms/jobposting/service/JobPostingServiceTests.java`
8. `src/test/java/com/group2/rms/requisition/validator/RequisitionValidatorTests.java`
9. `src/test/java/com/group2/rms/SecurityFlowTests.java`

Bản đề xuất cho các file này chỉ được lưu bên ngoài repository để xem xét, chưa áp dụng. Các tham chiếu AI trong migration gỡ dữ liệu cũ và tài liệu/sơ đồ không thuộc phạm vi thay đổi đã duyệt. Không đổi luật duyệt hồ sơ, chữ ký service/repository hay schema trong lượt này.

## Ghi nhận

Đã thêm mục vào `docs/management/WORK_LOG.md`, tạo báo cáo thành viên và liên kết từ README của LinhDN. Thay đổi đang ở working tree, chưa commit/push.
