# Sửa rủi ro và dọn code Offer

Ngày 07/11/2026. Nền: `64f44a8c`. Chỉ sửa module Offer, không sửa nghiệp vụ/source Application, notification chung, layout, schema hoặc database ứng dụng. Job hết hạn giữ tắt; chưa thêm phát hành lại.

## Phase 1: điều kiện UI và thông báo lỗi

- Candidate chỉ được hiện thao tác phản hồi khi thư mời Sent_Candidate còn hạn và Application vẫn Offered, đúng điều kiện UPDATE hiện hữu.
- Gom nhãn/badge/quyền phản hồi của danh sách và chi tiết vào một helper nội bộ; vẫn cho xem thư mời đã gửi của đúng chủ.
- Hồ sơ không còn Offered hiển thị “Thư mời không còn nhận phản hồi”; không coi việc ẩn nút là bằng chứng thư mời đã hết hạn.
- Lỗi tạo form ngoài nghiệp vụ dùng thông báo chung; chi tiết kỹ thuật chỉ ghi log. Dữ liệu form đã nhập được giữ lại.
- Ba nút chọn cột Excel dùng btn/btn--secondary/btn--sm, wrapper có tiền tố ofw và hỗ trợ xuống dòng. Bỏ CSS nút riêng và dấu phân cách không cần thiết.
- Thêm sáu ca trạng thái hồ sơ cho cả danh sách/chi tiết và một ca MockMvc chứng minh lỗi kỹ thuật không xuất hiện trong HTML, dữ liệu nhập vẫn còn.

Kiểm tra: 258 Maven test, 20 Node test đạt; git diff --check đạt. Chưa kiểm tra trực quan bằng trình duyệt.

Log không commit: scratch/offer-risk-phase1-20261107.log và scratch/offer-risk-phase1-js-20261107.log.
