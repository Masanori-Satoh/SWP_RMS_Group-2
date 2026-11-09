# Tổng quan module Job Requisition

> Đối chiếu code ngày 2026-11-09. Tài liệu này mô tả phiên bản hiện tại; kết quả kiểm thử lịch sử nằm ở các báo cáo test riêng.

## Phạm vi

- Hiring Manager tạo yêu cầu, lưu Draft, gửi Director duyệt, sửa và gửi lại yêu cầu Rejected.
- Director xem các yêu cầu trong phạm vi, phê duyệt hoặc từ chối yêu cầu Pending_Director của người khác. Director tạo yêu cầu của mình có thể phê duyệt trực tiếp qua form.
- HR chỉ xem yêu cầu Approved để tạo Job Posting.
- Candidate, Interviewer và System Admin không có quyền vào các route `/requisitions/**` theo `SecurityConfig` hiện tại.

## Code và dữ liệu

| Phần | Trách nhiệm |
|---|---|
| `RequisitionController` | Nhận request, bind form, chọn view/redirect và flash message |
| `RequisitionListService` | Phạm vi theo vai trò, lọc, tìm kiếm, sắp xếp, phân trang và lỗi URL |
| `RequisitionServiceImpl` | Tạo, sửa, xóa, sao chép và quyết định phê duyệt trong transaction |
| `RequisitionAccess` | Kiểm tra vai trò, sở hữu và trạng thái trước thao tác |
| `RequisitionValidator` | Kiểm tra dữ liệu nhập cho Draft/Submit |
| `RequisitionMapper` và `RequisitionLabels` | Đổi entity sang DTO, cờ thao tác và nhãn tiếng Việt |
| `JobRequisitionRepository` và `RequisitionApprovalRepository` | Đọc/ghi yêu cầu và quyết định |
| `RequisitionAuditLogger` và `NotificationService` | Lưu lịch sử thao tác và phát thông báo |

`JobRequisition.ApprovalStatus` dùng các trạng thái Draft, Pending_Director, Approved, Rejected. Quyết định được lưu ở `RequisitionApproval`; lịch sử hoạt động đọc từ `AuditLog`. Module hiện không có `ScreeningCriteria` hay `RequisitionWorkflowEvent`.

## Màn hình và luồng

- **Danh sách:** `GET /requisitions` tìm theo tên vị trí hoặc phòng ban. Bộ lọc hợp lệ phụ thuộc vai trò; tham số URL sai làm bảng rỗng kèm lỗi. Mặc định ưu tiên yêu cầu chờ Director, sau đó mới nhất.
- **Form:** `GET/POST /requisitions/create`, `GET/POST /requisitions/edit/{id}`. Save Draft hoặc Submit được phân biệt bằng `action`. Submit của HM tạo Pending_Director; Submit của Director phê duyệt trực tiếp.
- **Chi tiết:** `GET /requisitions/{id}` hiển thị thông tin, lịch sử duyệt, audit và các nút theo cờ quyền. `POST /requisitions/{id}/decision` xử lý approve/reject; reject bắt buộc lý do.
- **Sao chép:** `GET /requisitions/copy/{id}` chỉ điền sẵn form từ yêu cầu Approved. Bản mới chỉ được lưu khi người dùng gửi form.
- **Xóa:** `POST /requisitions/delete/{id}` chỉ cho chủ yêu cầu Draft/Rejected đủ điều kiện, chưa có Job Posting và chưa có đợt sau.
- Không còn nút, endpoint, service hoặc notification cho thao tác rút Requisition Pending_Director.

## Phân quyền

| Vai trò | Xem | Sửa/xóa | Duyệt | Sao chép |
|---|---|---|---|---|
| Hiring Manager | Yêu cầu của mình hoặc thuộc phòng ban được quản lý/phụ trách | Của mình khi Draft/Rejected; xóa còn cần các guard dữ liệu | Không | Approved |
| Director | Yêu cầu không phải Draft, cộng Draft do chính mình tạo | Của mình khi Draft/Rejected | Pending_Director của người khác | Approved |
| HR | Chỉ Approved | Không | Không | Không |

Quyền được kiểm tra trong service, không dựa vào việc ẩn nút. Lỗi sai trạng thái của người có quyền xem được trả về trang đã xem kèm toast; lỗi quyền được xử lý theo controller và cấu hình bảo mật.

## Kiểm thử

Các test hiện có nằm trong `src/test/java/com/group2/rms/requisition/` ở các lớp validator, service, controller và integration. Kết quả chạy gần nhất phải lấy từ `target/surefire-reports`; tài liệu này không gán PASS cho ca chưa chạy trên UI/SQL Server.
