# Sửa lỗi phân trang khi số trang vượt giới hạn

## Hiện tượng

Mở `/talent-pool?page=9999` khi kho có 54 ứng viên và mỗi trang có 20 dòng. Giao diện hiển thị `10000 / 3`, báo danh sách rỗng và kéo dài thanh phân trang với hàng nghìn nút.

## Nguyên nhân

Chỉ số `page` trên URL bắt đầu từ 0. Với `page=9999`, fragment tính trang bắt đầu là `9997` và trang kết thúc là `2`. Thymeleaf tạo dãy giảm dần khi điểm bắt đầu lớn hơn điểm kết thúc. Talent Pool đồng thời đưa trang rỗng ngoài phạm vi vào view dù truy vấn đếm vẫn tìm thấy ứng viên.

## Hành vi sau khi sửa

| Tình huống | Kết quả |
| --- | --- |
| Talent Pool có 54 kết quả và mở `?page=9999` | Hiển thị dữ liệu trang 3/3 và các nút 1, 2, 3. |
| Talent Pool đang có bộ lọc và yêu cầu vượt trang cuối | Tải trang cuối theo cùng từ khóa, người đánh dấu và tin tuyển dụng nguồn. |
| Bộ lọc Talent Pool có 0 kết quả | Hiển thị trạng thái trống và ẩn phân trang. |
| Danh sách khác truyền trang vượt phạm vi vào fragment | Hiện thông báo trang yêu cầu không tồn tại và cung cấp liên kết đến các trang hợp lệ. |

Khi Talent Pool tải trang cuối thay cho trang yêu cầu, địa chỉ trình duyệt giữ giá trị `page` ban đầu. Các nút phân trang sử dụng chỉ số hợp lệ và giữ tham số lọc trên URL.

[Nhật ký triển khai và kiểm chứng](../../management/work_logs/2026-11-08-pagination-out-of-range.md)
