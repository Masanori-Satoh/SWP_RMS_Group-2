# Bookmark trong phỏng vấn: dùng chung popup ghi chú

Ngày 17/10/2026. Yêu cầu cuối: gộp bookmark và xem/thêm ghi chú thành một nút mở popup như luồng Application cũ. Thay cho phương án bấm trực tiếp để lưu/bỏ lưu ở lượt trước.

## Hành vi cuối cùng

- Giữ một nút bookmark trên trang `/interviews/{id}/result` của HR/HM.
- Chưa lưu: icon outline, **Lưu ứng viên tiềm năng**. Đã lưu: icon filled, nền xanh rêu nhẹ, **Đã lưu tiềm năng**.
- Bấm nút mở đúng popup Application đã có, với cột lịch sử ghi chú và cột nhập ghi chú. Không còn nút “Xem / thêm ghi chú” riêng trên trang phỏng vấn.
- Mở popup chỉ tải dữ liệu, không tự lưu hoặc bỏ lưu. Chưa lưu thì ghi chú tùy chọn; đã lưu thì nút submit thêm ghi chú mới, giữ lịch sử.
- Bỏ lưu dùng hành động và bước xác nhận đã có bên trong popup. Không sinh thêm popup riêng.
- Chỉ cập nhật trạng thái sau response hợp lệ. Khóa bấm lặp khi đang gửi; lỗi giữ trạng thái đã xác nhận và bản nháp, cho tải lại trạng thái trước khi thử lại.
- Bỏ lưu chỉ đổi cờ trong kho chung. Ghi chú cũ, Candidate, Application, CV, kết quả đánh giá, lịch phỏng vấn và Offer được giữ nguyên; có thể lưu lại sau đó.

## Phạm vi

- `templates/talent-pool/potential-candidate-action.html`: rút còn nút mở, thông báo và lời gọi fragment popup.
- `templates/candidate/application-dialogs.html`: thêm tên fragment cho dialog bookmark hiện có để gọi riêng, không kéo popup xem CV vào Interview. Giữ nguyên nội dung và các ID cũ.
- `static/js/pages/applications.js`: dùng chung toàn bộ logic popup/ghi chú/bỏ lưu. Thêm tùy chọn tải trạng thái ban đầu và label cho nút nhúng; mặc định của trang Application giữ nguyên. Kiểm tra response hợp lệ, khóa gửi lặp sau lỗi cho đến khi tải lại trạng thái.
- `templates/interview/result.html`: đổi 2 đường dẫn asset sang CSS/JS Application hiện có. Không thay bố cục hay form Pass/Fail.
- Gỡ `static/js/pages/talent-pool-potential-action.js` và `static/css/pages/talent-pool-potential-action.css` vì không còn nơi chạy tham chiếu. Không tạo production file mới; không thêm CSS mới.

Không sửa layout dùng chung, controller/service Interview, DTO, Java production hoặc schema trong lượt này.

Tái sử dụng các API đã có:

- `GET /talent-pool/applications/{id}/bookmark` để tải trạng thái/ghi chú.
- `POST /talent-pool/applications/{id}/bookmark` để lưu hoặc thêm ghi chú có chủ đích.
- `POST /talent-pool/applications/{id}/bookmark/remove` để bỏ lưu.

Spring Security, CSRF và kiểm tra quyền Application phía server được giữ nguyên; không gửi trạng thái đích do client tự đặt. Không sửa Java production.

## Kiểm chứng

- **39/39 JavaScript test qua**, gồm 13 test dùng trực tiếp script Application chung. Kiểm tra mở popup không mutate, lịch sử, lưu/thêm ghi chú/bỏ lưu, gửi lặp, lỗi API, response chưa xác nhận, retry và giữ nguyên cách hoạt động ở trang Application. Các test preview CV cũ cũng qua.
- **108/108 Java test qua**: 53 ApplicationPipelineWebTests, 13 InterviewPotentialWebTests, 7 TalentPoolBookmarkTests, 10 InterviewResultServiceTests, 22 InterviewResultWebTests, 3 InterviewPotentialIntegrationTests. Xác nhận popup được render đúng cho HR/HM và không xuất hiện cho vai trò khác, giữ nguyên form chốt kết quả.
- 3 integration test dùng SQL Server thật và transaction rollback, kiểm tra HM bỏ lưu sau cả Passed/Failed và HR bỏ lưu. Snapshot Application, InterviewSchedule, InterviewEvaluation, InterviewFinalResult, OfferProposal, JobPosting, JobRequisition và lịch sử TalentPoolNote giữ nguyên.
- Integration dùng `ddl-auto=validate`, không sửa schema. Không commit/push.
- Syntax check JS và `git diff --check` qua.

Kiểm tra trình duyệt desktop trên `/interviews/49/result`: HM thấy một nút bookmark, bấm mở popup Application cũ; hiển thị lịch sử “Oke” của Vũ Anh Tuấn và ô nhập ghi chú bên cạnh, nút bỏ lưu có sẵn trong popup. Mở/đóng không ghi dữ liệu. Chưa thử ghi/bỏ lưu trên dữ liệu người dùng qua browser; bằng chứng ghi thật là HTTP MockMvc + SQL test rollback.

Localhost 8081 ban đầu từ chối kết nối; lần thử preview với `ddl-auto=validate` tự dừng vì cổng đã có app khác khởi động trong lúc chờ. Không dừng app đó và không để lại tiến trình preview; dùng app localhost đang chạy để kiểm tra giao diện.
