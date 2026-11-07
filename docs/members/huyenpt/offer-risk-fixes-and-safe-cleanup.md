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

## Phase 4: export chính xác và giảm bộ nhớ workbook

- Sửa lỗi Summary không đếm Expired. Tái sử dụng mapOfferStatusLabel thay vì duy trì switch dịch trạng thái thứ hai; đếm được cả alias cũ và trạng thái khác nếu dữ liệu có.
- Chuyển phần dựng workbook sang SXSSFWorkbook với cửa sổ 100 hàng mỗi sheet và nén file tạm. Theo dõi độ rộng trước khi flush hàng để giữ auto-size, freeze header và định dạng Excel.
- Kiểm tra API POI 5.3.0 đang cài: close() gọi dispose() và đóng workbook gốc; dùng try-with-resources để dọn file tạm kể cả khi lỗi.
- Không thay đổi scope, bộ lọc, khóa cột, tên sheet, quyền export hoặc hợp đồng byte[] của controller. Danh sách entity/DTO và byte[] kết quả vẫn ở RAM; chưa phải streaming toàn bộ từ database đến HTTP.
- 12/12 test export đạt, gồm file 1.001 dòng: dữ liệu đầu/cuối còn nguyên, Expired 500, Accepted 501, tổng 1.001; header đóng băng và độ rộng cột được giữ.
- Kiểm tra cuối: 399/399 service/MockMvc (Offer và hợp đồng Application), cộng 40/40 SQL ở phase 2 và 20/20 Node ở phase 1; không lỗi hoặc bỏ qua trong các lần chạy xác minh cuối. git diff --check đạt.

Log không commit: scratch/offer-risk-export-20261107.log và scratch/offer-risk-final-20261107.log.

## Tổng thay đổi production so với 64f44a8c

8 file production, +109/-269 dòng, giảm ròng **160 dòng**. Không thêm file production hoặc tách thêm package.

| File | Trước | Sau |
|---|---:|---:|
| OfferController.java | 513 | 486 |
| OfferServiceImpl.java | 957 | 912 |
| OfferExportServiceImpl.java | 733 | 729 |
| CandidateOfferServiceImpl.java | 308 | 318 |
| offer-director.css | 324 | 250 |
| offer-hr.css | 804 | 787 |

CandidateOfferDetailResponse.java và offers/list.html là hai file production còn lại. Phần tăng ở candidate service là điều kiện an toàn và helper dùng chung cho danh sách/chi tiết. Test và tài liệu tăng riêng để giữ bằng chứng; không tính chúng vào mức giảm production.

Không sửa JavaScript vì các request dùng chung đã được gom trước đó; không thêm abstraction hoặc file mới chỉ để giảm số dòng. Chưa đổi gửi email sang async vì đang giữ nguyên phản hồi kết quả email. Notification sau commit vẫn là best-effort có log, chưa có outbox/retry bền vững. Chưa kiểm tra trực quan browser hoặc benchmark bộ nhớ bằng MB. Không tự bật job hết hạn.

## Bổ sung: sửa rollback khi candidate nhận hoặc từ chối thư mời

Nền bản sửa: `5dbf1817`. Đợt cách ly notification trước mới xử lý HR/Director, còn bỏ sót nhánh candidate.

- Chẩn đoán trước sửa đã tái hiện trên SQL riêng: HR khác Hiring Manager, UPDATE phản hồi clear persistence context, notification dùng Offer đã detached và truy cập username của HM gây LazyInitializationException; participant notification đánh dấu rollback-only. Commit ném UnexpectedRollbackException, Offer/Application không đổi và controller hiển thị lỗi chung.
- Candidate đăng ký notification sau khi giao dịch phản hồi commit. Hai thao tác đánh dấu thông báo đã đọc và báo HR/HM chạy độc lập trong hai giao dịch REQUIRES_NEW.
- Tải lại Offer của đúng candidate trong giao dịch mới trước khi soạn thông báo; không giữ entity đã bị clear. Lỗi thông báo có log theo OfferId, không rollback quyết định hoặc ngăn thao tác thông báo còn lại.
- Giữ nguyên kiểm tra hạn, chủ sở hữu, UPDATE nguyên tử và chuyển Application Offered sang Hired/Withdrawn. Không sửa Application hoặc NotificationService chung, schema, migration, UI hay dữ liệu thư mời #12.
- Dùng constructor injection qua RequiredArgsConstructor cho Clock và transaction manager; bỏ các constructor overload không có caller production, cập nhật chỗ khởi tạo trong unit test.

Kiểm thử mới: hai ca unit commit/rollback, một ca lỗi đánh dấu đã đọc vẫn báo HR/HM; hai ca SQL nhận/từ chối với notification logic và repository thực, HR khác HM; bốn ca SQL lỗi rollback ở từng thao tác notification cho cả nhận/từ chối.

Kết quả: **46 CandidateOfferServiceTests + 19 CandidateOfferWebTests + 46 SQL regression = 111 test đạt**, không lỗi hoặc bỏ qua. SQL xác minh quyết định được lưu, trạng thái hồ sơ đúng, notification cũ đã đọc và hai người nhận có thông báo riêng. Các ca lỗi notification vẫn giữ nguyên quyết định đã commit.

Database tạm OfferRegression_e90d70a500fe4d3f988999f4f635e4dc đã được xóa và xác minh không còn. Log giữ trong scratch/offer-candidate-response-unit-20261107.log và scratch/offer-candidate-response-sql-20261107.log, không stage. Không khởi động lại ứng dụng đang chạy của người dùng hoặc bấm nhận/từ chối trên database ứng dụng; cần nạp lại code mới trước khi thử ở trình duyệt.

## Bổ sung: link chi tiết trong thông báo phản hồi candidate

Người dùng đã duyệt mở rộng đúng phần tạo notification Offer trong NotificationServiceImpl và cập nhật hai URL cũ trên database local. Không thay đổi thông báo của nghiệp vụ khác hoặc quyền truy cập.

- Cả nhận và từ chối: HR vào /offers/{offerId}; Hiring Manager vào /applications/{applicationId}. Mã tham chiếu và EventId vẫn theo OfferId để giữ chống trùng.
- Không đưa HM vào /offers/{id}, vì SecurityConfig chỉ cho HR/Director xem Offer. HM xem hồ sơ theo ApplicationAccess hiện hữu; đã xác minh HM của hồ sơ 58 thuộc đúng phòng ban.
- 170 test unit/MockMvc đạt (49 notification, 46 candidate service, 19 candidate web, 56 Offer web) và hai test SQL nhận/từ chối dùng notification/repository thực đạt. Test SQL xác minh URL riêng cho từng người nhận.
- Database kiểm thử OfferRegression_2f691df5ddea46b1bb70cbc8585d7d2f đã được dọn và xác minh không còn.
- Database local chỉ đổi LinkUrl của notification 19 và 20 sau kiểm tra EventId, người nhận và liên kết Offer 12/Application 58: /offers/12 và /applications/58. Giữ nguyên IsRead/ReadAt, nội dung, EventId và trạng thái Accepted/Hired.
- Backup metadata hai bản ghi: scratch/offer-notification-links-before-20261107.csv. Script riêng cho đúng hai ID: scratch/offer-notification-links-repair-20261107.sql; không phải migration dùng chung cho môi trường khác.
- Lần sửa dữ liệu đầu bị thiếu QUOTED_IDENTIFIER nên rollback, không đổi hàng nào. Đã bổ sung SET options cần cho filtered index và cập nhật đúng hai hàng thành công; log cuối: scratch/offer-notification-links-repair-20261107-v2.log.
- Không sửa schema, giao diện chuông, Application hoặc SecurityConfig. Chưa kiểm tra click trực tiếp bằng trình duyệt; cần khởi động lại app để notification mới dùng code mới và reload trang để lấy link đã sửa.

Log kiểm thử không commit: scratch/offer-notification-links-unit-20261107.log và scratch/offer-notification-links-sql-20261107.log.
