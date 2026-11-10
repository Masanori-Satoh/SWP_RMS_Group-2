# Kế hoạch kiểm thử Job Requisition hiện tại

> Đối chiếu source ngày 2026-11-09. Đây là kế hoạch và bản đồ test, không phải biên bản chạy system test.

## Test tự động hiện có

| Lớp | File test | Nội dung cần giữ |
|---|---|---|
| Validation | `RequisitionValidatorTests` | Draft/Submit, trường bắt buộc, Remote/location, lương, probation |
| Quyền và mapping | `RequisitionAccessTests`, `RequisitionMapperTests` | Phạm vi vai trò, sở hữu, trạng thái và cờ thao tác |
| Danh sách | `RequisitionListServiceTests` | Scope, lọc theo role, từ khóa, lỗi URL, trang vượt phạm vi |
| Nghiệp vụ | `RequisitionServiceTests` | Tạo, sửa, duyệt/từ chối, copy, xóa và notification |
| Web | `RequisitionControllerTests` | Route, binding, CSRF, redirect, flash toast và URL sai trạng thái |
| Integration | `RequisitionIntegrationTests` | Repository, transaction và dữ liệu SQL Server khi môi trường sẵn sàng |

Đường dẫn là `src/test/java/com/group2/rms/requisition/`. Các test cần chạy lại sau khi đổi validator, quyền, truy vấn hoặc template. Không dùng test cũ cho `ScreeningCriteria` hoặc Requisition withdrawal vì hai tính năng này không còn trong code.

## Ca system test tối thiểu

| ID | Vai trò và bước chính | Kết quả mong đợi | Actual | Status |
|---|---|---|---|---|
| ST-REQ-01 | HM tạo Draft, sửa rồi Submit | Cùng ID chuyển Draft -> Pending_Director; Director nhận thông báo | Not run | NOT RUN |
| ST-REQ-02 | Director khác người tạo Approve | Trạng thái Approved; có approval/audit và HM notification | Not run | NOT RUN |
| ST-REQ-03 | Director Reject có lý do; HM sửa và Submit lại | Rejected -> Pending_Director cùng ID, giữ lịch sử quyết định | Not run | NOT RUN |
| ST-REQ-04 | HM mở/sửa Pending_Director; người khác sửa Draft | Không đổi dữ liệu; báo lỗi quyền hoặc trạng thái đúng loại | Not run | NOT RUN |
| ST-REQ-05 | HR mở list/detail và gửi filter sai | Chỉ thấy Approved; URL sai báo lỗi, không mở rộng dữ liệu | Not run | NOT RUN |
| ST-REQ-06 | HM copy Approved; xóa Draft/Rejected đủ điều kiện | Copy mở form mới; xóa đúng guard posting/đợt sau | Not run | NOT RUN |

Khi chạy, bổ sung URL, ID, snapshot trước/sau ở SQL Server, ảnh màn hình, thời điểm, Actual Result và trạng thái. Không đổi `NOT RUN` chỉ vì code đã được review hoặc unit test đã PASS.
