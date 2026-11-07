# Hoàn thiện ngôn ngữ và kiểm tra hết hạn thư mời

Ngày: 07/11/2026. Nền trước thay đổi: `d6723566`, đã tích hợp main `1e3d7134`.

## Phạm vi đã chốt

- Chỉ hoàn thiện hết hạn; chưa thêm phát hành lại hoặc nhiều phiên bản thư mời.
- Không sửa Application, layout, sidebar, CSS chung hoặc database ứng dụng.
- Director chỉ duyệt/từ chối; HR giữ quyền tạo, sửa/xóa bản nháp và phát hành.
- Giữ job hết hạn tắt mặc định; chưa bật trên môi trường ứng dụng.

## Phase 1: ngôn ngữ hiển thị

- Dùng “Thư mời nhận việc” trong tiêu đề tạo/sửa/phê duyệt; dùng “thư mời” khi đã rõ ngữ cảnh.
- Đồng bộ nút, popup, tooltip, nhãn chọn hàng, bộ lọc phạm vi xuất, toast, lỗi nghiệp vụ và tiêu đề cột Excel.
- Giữ tên route, biến, trạng thái, id/name, field binding và khóa cột export. Tên tab kỹ thuật Excel `Offers`/`Summary` giữ tương thích.
- Giữ “vị trí đề xuất” và câu đánh giá “đề xuất gửi thư mời” khi mô tả đúng nội dung trường/hành động.
- Không đổi CSS, bố cục, điều kiện quyền hoặc quy tắc chuyển trạng thái.
- Ba kỳ vọng chuỗi trong test cũ được cập nhật theo câu chữ mới; kỳ vọng tiêu đề cột Excel cũng được đồng bộ.

Kiểm tra: Maven service/MockMvc Offer và Application thành công; Node 20/20; `git diff --check` thành công. Không chạy test SpringBootTest dùng database ứng dụng trong phase này. Chưa kiểm tra trực quan bằng trình duyệt.

Log cục bộ (không đưa vào commit): `scratch/offer-completion-ui-20261107-v2.log`, `scratch/offer-completion-js-20261107.log`.

## Hợp đồng hết hạn và phần phối hợp

- Candidate nhìn thấy quá hạn theo thời gian kể cả khi trạng thái còn `Sent_Candidate`; chỉ xem, không phản hồi. Backend tiếp tục kiểm tra nguyên tử.
- Job chỉ ghi `Expired`; không đổi Application, không đóng tin. Hồ sơ giữ `Offered` khi chưa có hành động nghiệp vụ khác.
- HR không được tạo lại trên cùng hồ sơ trong phạm vi này. Không hướng dẫn xóa/sửa đè bản hết hạn hoặc đóng toàn bộ tin để lách quy tắc.
- HR theo dõi và phối hợp Dũng LT xử lý riêng hồ sơ qua nghiệp vụ Application được nhóm duyệt. Chưa triển khai thêm thao tác kết thúc hồ sơ tại Offer.
- Nhãn danh sách/chi tiết Offer đã nhận biết quá hạn theo thời gian; bước chi tiết hồ sơ HR vẫn có thể chờ ứng viên đến khi job ghi `Expired`.
- Migration 021 chỉ mở rộng CHECK trạng thái; môi trường đã áp dụng phải kiểm tra trước, không giả định môi trường khác cũng đã áp dụng.
- Chỉ bật `app.offer.expiration.enabled=true` sau khi hai bên kiểm thử tích hợp và xác nhận. Không có tin nhắn được gửi tự động.
