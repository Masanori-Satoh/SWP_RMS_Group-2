# Talent Pool Selection UI — 17/10/2026

## Phạm vi và quy tắc

Tinh gọn luồng HM mở Job Posting → kho tiềm năng → chọn ứng viên/CV nguồn → kiểm tra danh sách. Áp dụng `docs/architecture/ARCHITECTURE_GUIDE.md`, bộ `docs/architecture/UI/` và giao diện mộc. RMS hiện có; tham khảo Impeccable/UI UX Pro Max.

Không thay đổi schema, entity, repository, dependency, bảo mật chung, layout, sidebar hay token toàn hệ thống. Không commit/push. Giữ nguyên các thay đổi tài liệu đã có sẵn trong working tree của người dùng.

## Điểm lệch backend cần xử lý trước khi cho phép hoàn tất

`TalentPoolService.suggest()` hiện tạo `Application` mới với trạng thái `HM_Suggested` bằng `applications.saveAndFlush(...)`. Đây là cách lưu đề xuất hiện có, trái với yêu cầu mới “không tạo Application”.

Vì vậy:

- Giao diện mới **không gửi tới** `/talent-pool/jobs/{id}/suggest`.
- Nút “Thêm X ứng viên” mở danh sách để kiểm tra; nút “Xác nhận thêm” bị vô hiệu hóa và có thông báo chức năng chưa sẵn sàng.
- Trang hồ sơ trong kho bỏ form đề xuất trực tiếp cũ, dẫn HM về danh sách đang chọn.
- Giữ nguyên endpoint/service legacy để không tự xây lại nghiệp vụ hoặc tác động các nơi gọi khác. Đây không phải thay đổi hợp đồng của endpoint legacy.
- Không báo thêm thành công, không tạo đề xuất giả trong trình duyệt và không cập nhật danh sách HR bằng dữ liệu giả.

Tích hợp tối thiểu còn thiếu: backend lưu quan hệ đề xuất riêng giữa Candidate–Job Posting, tham chiếu Application/CV nguồn, người đề xuất và trạng thái chờ HR liên hệ; ràng buộc chống trùng và danh sách cho HR đọc từ quan hệ này. Cần quyết định/ủy quyền riêng cho thay đổi backend và schema đó. Luồng hiện tại chưa thể kiểm chứng “HR thấy ứng viên vừa thêm” vì không thực hiện bước ghi dữ liệu này.

## UI/UX đã triển khai

- Heading “Chọn ứng viên tiềm năng” khi HM vào từ một tin; banner nhỏ ghi rõ tin tuyển dụng đích và link danh sách đã đề xuất.
- Bộ lọc trong một card thống nhất, giữ tên trường và ID `keyword`, `markedBy`, `sourceJobId`; giữ bộ lọc/job đích trên URL qua fragment phân trang sẵn có.
- Checkbox ở đầu hàng, chọn tất cả ứng viên có CV trên trang, trạng thái hàng xanh nhẹ. Bỏ badge Tiềm năng lặp lại trên mọi hàng.
- Cột “CV nguồn”, có tooltip giải thích, tên vị trí/ngày nộp, tooltip tên đầy đủ và phần mở rộng để đọc tên dài. Dropdown chọn CV lịch sử, không upload hoặc sao chép CV.
- Không có CV nguồn: hiển thị rõ lý do, vô hiệu hóa checkbox và không có nút preview.
- Icon mắt mở đúng CV đang chọn bằng endpoint `/talent-pool/{candidateId}/cv/{applicationId}` và modal CV hiện có. Đổi dropdown cập nhật cả preview lẫn mapping được chọn.
- Icon ghi chú mở lịch sử dùng chung, gồm nội dung/người viết/thời gian; có trạng thái tải, trống, lỗi, thử lại và link hồ sơ chi tiết.
- Thanh chọn sticky ở cuối vùng nội dung, đếm tổng lựa chọn qua nhiều trang, hiển thị job đích, bỏ chọn tất cả và mở tổng kết.
- Modal tổng kết thể hiện từng ứng viên/CV nguồn và cho bỏ từng người, kể cả người được chọn ở trang khác. Có nút quay lại, đóng bằng X/Escape và kiểm tra lại.
- Trang kho chung khi không có job đích vẫn là danh sách đọc CV/ghi chú; không hiện các điều khiển thêm vào job.
- Giới hạn 20 ứng viên/lần theo DTO backend hiện có. Không thay đổi quy tắc giới hạn này.

## Lựa chọn qua nhiều trang

`talent-pool-selection.js` giữ bản nháp theo Candidate ID thật; mỗi Candidate chỉ có một source Application ID. Khóa `sessionStorage` tách theo User ID và Job Posting ID, không trộn giữa tài khoản/job và không biểu diễn kết quả đã gửi thành công.

- Khi đổi trang/lọc, tải lại đúng CV đã chọn cho những hàng xuất hiện lại.
- Khi CV cũ biến mất khỏi các lựa chọn hiện tại, bỏ lựa chọn của ứng viên đó và thông báo chọn lại.
- Chọn/bỏ chọn tất cả chỉ tác động hàng đủ điều kiện trên trang hiện tại; người ở trang khác vẫn được giữ.
- Lựa chọn cũ được kiểm tra lại trên server khi mở tổng kết, không tin bản nháp trình duyệt là trạng thái nghiệp vụ hiện tại.
- Nếu trình duyệt chặn hoặc hết dung lượng lưu tạm, thông báo rõ và chặn chuyển trang/lọc khi còn lựa chọn để tránh mất âm thầm. Có thể bỏ chọn tất cả để tiếp tục.

## Backend chỉ đọc bổ sung

| Endpoint | Chức năng |
| --- | --- |
| `GET /talent-pool/{id}/notes` | Tái sử dụng `pool.detail(id)` để đọc ghi chú của ứng viên tiềm năng theo quyền hiện có. |
| `POST /talent-pool/jobs/{id}/selection-review` | Nhận JSON DTO `sourceApplicationIds`, kiểm tra danh sách và trả lỗi cụ thể từng hồ sơ; không ghi dữ liệu. |

Endpoint mới dùng `ApiResponse`, validation DTO và header `X-CSRF-TOKEN` theo hướng dẫn kiến trúc. Vai trò được bảo vệ bởi rule `/talent-pool/**` đã có và kiểm tra service; bước review chỉ cho Hiring Manager trong phạm vi tin tuyển dụng.

Các kiểm tra review: tin còn mở; Candidate còn trong kho; Candidate không lặp; CV thuộc hồ sơ đủ điều kiện; CV nguồn thuộc tin khác; Candidate chưa có hồ sơ/đề xuất cho tin đích; CV nội bộ còn tồn tại. CV URL ngoài chỉ kiểm tra kiểu URL theo cơ chế hiện có; không khẳng định tài nguyên ngoài còn truy cập được.

Response luôn có `submissionSupported=false` vì chưa có quan hệ lưu đề xuất riêng. Không gọi `suggest`, review CV, phỏng vấn, email, hoặc thao tác ghi Candidate/Application.

## File sửa/thêm

| Đường dẫn từ project root | Nội dung |
| --- | --- |
| `src/main/resources/templates/talent-pool/list.html` | Heading, filter, bảng, sticky bar, modal ghi chú/tổng kết. |
| `src/main/resources/templates/talent-pool/detail.html` | Nhãn CV, quay về danh sách chọn, bỏ form gửi legacy. |
| `src/main/resources/templates/candidate/application-dialogs.html` | Cho tái sử dụng riêng fragment preview, nút đóng có icon X. |
| `src/main/resources/static/css/pages/talent-pool.css` | CSS bố cục riêng với tiền tố `tp-` và token hiện có. |
| `src/main/resources/static/js/pages/talent-pool.js` | UI chọn, mapping CV, xem ghi chú, review và xử lý lỗi. Giữ handler dialog cũ cho các trang khác. |
| `src/main/resources/static/js/pages/talent-pool-selection.js` | Bản nháp lựa chọn theo tài khoản/job, tối đa 20 ứng viên. |
| `src/main/java/com/group2/rms/candidate/controller/TalentPoolController.java` | Endpoint notes/review và scope bản nháp lấy từ service. |
| `src/main/java/com/group2/rms/candidate/service/TalentPoolService.java` | Scope User ID qua CurrentUserService, kiểm tra selection chỉ đọc. |
| `src/main/java/com/group2/rms/candidate/dto/TalentPoolSelectionReviewResponse.java` | Kết quả kiểm tra theo ứng viên/CV nguồn. |
| `src/test/java/com/group2/rms/candidate/ApplicationPipelineWebTests.java` | Render, URL filter/pagination, endpoint, quyền, validation/CSRF, không nối submit legacy. |
| `src/test/java/com/group2/rms/candidate/service/TalentPoolSelectionReviewTests.java` | Đủ điều kiện, trùng, nguồn sai/thiếu, quyền; xác minh không ghi dữ liệu. |
| `src/test/js/talent-pool-selection.test.cjs` | Bản nháp qua trang, giới hạn, tránh trùng, cách ly scope và lỗi storage. |
| `src/test/js/talent-pool-ui.test.cjs` | Sự kiện chọn/đổi CV thật của script, preview mapping, select-all và bảo vệ điều hướng. |

## Kiểm tra

- Nhóm Java liên quan: 118 test qua, không failure/error, gồm 37 MVC, 7 selection review, 7 bookmark, 56 quyền truy cập và 11 trạng thái.
- JavaScript: 12 test qua bằng Node test runner, không thêm dependency.
- Kiểm tra cú pháp JS; cấu trúc HTML/ID; không inline CSS/JS, không hardcode màu, token CSS tồn tại, không định nghĩa lại selector component chung.
- Test dùng MVC/service mocks và dữ liệu kiểm thử, không kết nối/cập nhật SQL Server thật.
- Trình duyệt tự động mở localhost vẫn gặp `ERR_BLOCKED_BY_CLIENT`; chưa có bằng chứng thao tác trên ứng dụng thật hoặc HR nhận một đề xuất mới.

Lệnh kiểm tra:

```powershell
mvn.cmd -q "-Dtest=TalentPoolSelectionReviewTests,ApplicationPipelineWebTests,TalentPoolBookmarkTests,ApplicationAccessTests,ApplicationStatusServiceTests" test
node --test src/test/js/talent-pool-selection.test.cjs src/test/js/talent-pool-ui.test.cjs
node --check src/main/resources/static/js/pages/talent-pool.js
git diff --check
```

## Kiểm tra thủ công trên ứng dụng thật

1. Khởi động lại app, đăng nhập HM, vào Job Posting 8 → Thêm ứng viên tiềm năng.
2. Kiểm tra tên tin đích, bộ lọc, số kết quả và URL phân trang vẫn giữ `jobPostingId`/các điều kiện lọc.
3. Chọn một người, đổi CV nguồn; bấm mắt và đối chiếu PDF với vị trí/ngày đang chọn. Thử tên nguồn dài và CV không còn tồn tại.
4. Chọn nhiều người/chọn tất cả trang; đổi trang hoặc lọc rồi quay lại. Kiểm tra số lượng và CV nguồn được giữ chính xác.
5. Kiểm tra người không có CV bị vô hiệu hóa; chọn vượt 20 phải báo rõ và không làm mất người đã chọn.
6. Mở ghi chú, kiểm tra tác giả/ngày/nội dung, trạng thái trống và lỗi mạng; Candidate không được đọc endpoint này.
7. Mở tổng kết, đối chiếu từng Candidate–CV, bỏ người đã chọn từ trang khác. Kiểm tra lỗi trùng job/hồ sơ hoặc Candidate đã bỏ khỏi kho.
8. Kiểm tra Tab/Escape, focus khi đóng/bỏ chọn và thanh sticky không che pagination/hàng cuối ở các cỡ desktop/tablet.
9. Xác nhận nút cuối bị khóa, không phát sinh `POST .../suggest`, Application, email hay thay đổi trạng thái. Đây là giới hạn backend đã nêu, không phải kết quả thêm thành công.
10. Chỉ kiểm tra việc ghi đề xuất mới/HR nhìn thấy sau khi backend lưu quan hệ riêng được thiết kế, phê duyệt và triển khai.
