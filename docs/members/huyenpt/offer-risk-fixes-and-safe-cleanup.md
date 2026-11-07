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

## Phase 2: cách ly lỗi notification

- Đã tái hiện trên SQL Server riêng: participant REQUIRED của notification lỗi và đánh dấu rollback-only; code cũ bắt exception nhưng commit phát hành vẫn ném UnexpectedRollbackException.
- Gom bốn chỗ tạo thông báo (tạo, sửa, phát hành, quyết định Director) vào notifyAfterCommit. Chỉ gọi sau commit và mở giao dịch REQUIRES_NEW, đọc lại Offer để truy cập LAZY an toàn.
- Lỗi thông báo được ghi log, không rollback nội dung thư mời hoặc trạng thái hồ sơ đã lưu. Không sửa NotificationService chung và không đổi hợp đồng email/flash.
- Test SQL chứng minh phát hành và Application Offered còn nguyên khi participant notification rollback. Test unit chứng minh thông báo chưa chạy trước commit và giao dịch riêng được yêu cầu.
- 40/40 SQL test và 132/132 OfferServiceTests đạt, không bỏ qua. Log trước sửa có một lỗi được chủ ý tái hiện; log sau sửa đã đạt.
- Chỉ dùng database tạm OfferRegression_651bda581e3b42d4ad10c199cbaf3235, đã xóa và xác minh không còn. Không đổi database ứng dụng, schema, migration hoặc thông tin đăng nhập.

Log không commit: scratch/offer-risk-notification-before-20261107.log, scratch/offer-risk-notification-after-20261107.log, scratch/offer-risk-notification-unit-20261107.log.

## Phase 3: dọn phần trùng lặp có bằng chứng

- Gom ba nhánh dựng lại form tạo lỗi vào renderCreateForm; giữ tên model, BindingResult, trường đã nhập và hồ sơ cố định. Bỏ biến nhận kết quả tạo không sử dụng.
- Xóa hai tập trạng thái/hai helper public cũ không có caller trong source/test. Các kiểm tra trạng thái tại service ghi và SQL vẫn giữ nguyên.
- Xóa 10 rule CSS nút Director không có tham chiếu từ template hoặc JavaScript; không sửa CSS/layout chung.
- Production giảm 135 dòng: Controller -27, Service -34, CSS Director -74. Không thêm file production hoặc đổi cấu trúc module.
- Bổ sung ba ca MockMvc cho lỗi validation/nghiệp vụ/kỹ thuật khi tạo theo hồ sơ cố định. Test cuối đạt 56 OfferIntegrationTests và 132 OfferServiceTests; CandidateOfferWebTests 19, CandidateOfferServiceTests 43 và OfferExportServiceTests 11 cũng đạt trong lần chạy phase này.

Log không commit: scratch/offer-risk-phase3-20261107.log và scratch/offer-risk-phase3-20261107-v2.log. Lần đầu test mới thiếu Content-Type form; đã sửa fixture và chạy lại các nhóm bị ảnh hưởng thành công.
