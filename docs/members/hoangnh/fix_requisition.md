# Danh sách Requisition hiện tại

`GET /requisitions` đi qua `RequisitionController.list`, `RequisitionListService.getListPage` rồi render bằng `requisitions/list.html`.

- Tìm kiếm là form GET thông thường. `q` tìm theo tên vị trí hoặc phòng ban, không phân biệt hoa thường; giới hạn 120 ký tự.
- Bộ lọc theo vai trò: Hiring Manager chọn trạng thái tin đăng và hình thức tuyển dụng; Director chọn phòng ban và người tạo; HR chọn trạng thái tin đăng và phòng ban. Các tab trạng thái chỉ hiển thị khi vai trò được dùng.
- Tham số URL sai giá trị hoặc sai kiểu, khoảng ngày ngược, và trang vượt phạm vi làm bảng rỗng kèm thông báo lỗi. Truy vấn luôn giữ phạm vi xem theo vai trò.
- Mặc định ưu tiên yêu cầu đang chờ Director, sau đó theo ngày tạo mới nhất. URL vẫn hỗ trợ sắp xếp cũ nhất trước.
- Menu thao tác đọc cờ quyền từ `RequisitionMapper` và `RequisitionAccess`. Chỉ người tạo được sửa/xóa yêu cầu Draft hoặc Rejected đủ điều kiện; sao chép chỉ áp dụng cho Approved. Không còn thao tác rút Requisition.

Đối chiếu: `RequisitionController`, `RequisitionListService`, `RequisitionSpecifications`, `RequisitionAccess`, `RequisitionListServiceTests`.
