# Talent Pool — Chi tiết ứng viên và popup tổng kết, 17/10/2026

## Phạm vi

Triển khai yêu cầu mới trên danh sách `/talent-pool?jobPostingId=...`, popup tổng kết và trang `/talent-pool/{id}`. Tiếp nối báo cáo `2026-10-17-talent-pool-selection-ui.md`; các mô tả dưới đây thay thế phần hướng dẫn mở rộng tên CV và layout chi tiết cũ trong báo cáo đó.

Tuân thủ `docs/architecture/ARCHITECTURE_GUIDE.md` và bộ `docs/architecture/UI/`; áp dụng Impeccable/UI UX Pro Max trong phạm vi design system hiện có. Dùng layout, card, button, alert và token mộc. RMS. CSS riêng tiền tố `tp-`; không sửa layout/sidebar/topbar, token, schema, security hay workflow tuyển dụng. Không commit/push, không thao tác DB thật.

## UI/UX đã cập nhật

- Bỏ phần mở rộng “Tên CV đầy đủ”; vẫn đọc tên đầy đủ trong dropdown/tooltip.
- Bỏ dòng “Chọn ứng viên ở đầu mỗi dòng. Tối đa 20 ứng viên mỗi lần.”; giữ validation giới hạn 20 ở state và backend.
- Popup tổng kết: tiêu đề theo số người chọn, tin tuyển dụng đích, banner số CV nguồn hợp lệ. Mỗi mục gồm tên ứng viên, vị trí nguồn, ngày nộp riêng, icon xem CV và icon bỏ chọn. Danh sách cuộn trong popup; nút quay lại/xác nhận nằm ở footer.
- Icon mắt trong popup mở đúng CV nguồn đã chọn bằng modal preview đang có, không tạo hồ sơ hay thay đổi mapping. Có fallback khi không tải được PDF hoặc định dạng không hỗ trợ.
- Trang chi tiết có profile header xanh rêu nhạt: breadcrumb, initials từ tên thật, tên/email/điện thoại, badge và nút quay lại.
- Workspace hai cột khoảng 57%/43%: lịch sử CV trái; lịch sử ghi chú và form bổ sung phải. Co thành một cột dưới 992px.
- CV lịch sử chỉ đọc: vị trí, ngày nộp và nút xem CV. Không có dropdown chọn nguồn, checkbox, upload hoặc lưu CV nguồn trên trang chi tiết.
- Ghi chú dùng hệ thống cũ, mới nhất trước; hiển thị người viết/thời gian/nội dung, textarea 4 dòng ngay dưới feed. Giữ giới hạn 1.000 ký tự nhưng không hiện đoạn hướng dẫn dư. Nội dung được escape bằng `th:text`.
- Có trạng thái trống CV/ghi chú, thông báo lưu thành công/lỗi; giữ nội dung đang nhập khi validation hoặc nghiệp vụ báo lỗi.

## Nút Back và bảo toàn lựa chọn

Các đường mở hồ sơ từ hàng, popup ghi chú và popup CV mang theo `jobPostingId`, `keyword`, `markedBy`, `sourceJobId`, `page`. Controller dựng lại URL nội bộ cố định từ các trường có kiểu; không nhận URL đích tùy ý và không phụ thuộc `history.back()`.

Form ghi chú và redirect sau POST cũng giữ các trường này. Refresh trang chi tiết vẫn có đủ ngữ cảnh. Truy cập trực tiếp không có query quay về `/talent-pool`.

Bản nháp dùng `sessionStorage`, khóa theo User ID và Job Posting ID. Chỉ lưu `{candidateId, sourceId}`; tên, email, điện thoại và nội dung ghi chú không được lưu vào draft. Bản nháp định dạng cũ được ghi lại chỉ còn ID khi tải danh sách.

Trang chi tiết không nạp script quản lý lựa chọn, vì vậy mở hồ sơ, xem CV hay lưu ghi chú không sửa Candidate ID ↔ source Application ID. Khi quay lại, danh sách khôi phục checkbox, dropdown CV, số lượng và thanh chọn từ draft. Server trả lại tên/vị trí/ngày nguồn khi mở tổng kết, kể cả các ứng viên đã chọn ở trang khác. Nguồn không còn hợp lệ được báo để chọn lại. Các job/tài khoản khác không ghi đè draft của nhau.

## File sửa/thêm trong lượt này

| File từ project root | Nội dung |
| --- | --- |
| `src/main/resources/templates/talent-pool/list.html` | Bỏ hai phần thừa, truyền ngữ cảnh hồ sơ, bố cục popup. |
| `src/main/resources/templates/talent-pool/detail.html` | Profile header, hai cột, preview CV và form ghi chú. |
| `src/main/resources/static/css/pages/talent-pool.css` | CSS riêng cho profile/workspace/popup và responsive. |
| `src/main/resources/static/js/pages/talent-pool.js` | Dựng tổng kết, ngày nguồn, icon preview/bỏ chọn, status hợp lệ. |
| `src/main/resources/static/js/pages/talent-pool-selection.js` | Draft chỉ chứa ID, chuyển đổi bản nháp cũ. |
| `src/main/resources/static/js/pages/applications.js` | Event delegation cho icon CV sinh động; ẩn link mở lại chính hồ sơ đang xem. |
| `src/main/java/com/group2/rms/candidate/controller/TalentPoolController.java` | Back nội bộ, giữ query sau lưu ghi chú và giữ draft khi lỗi. |
| `src/main/java/com/group2/rms/candidate/dto/TalentPoolSelectionReviewResponse.java` | Thêm ngày nộp của nguồn vào response chỉ đọc. |
| `src/main/java/com/group2/rms/candidate/service/TalentPoolService.java` | Lấy ngày nộp từ hồ sơ nguồn thật trong bước review. |
| `src/test/java/com/group2/rms/candidate/ApplicationPipelineWebTests.java` | Render, Back/fallback, query encode, note PRG, quyền/CSRF. |
| `src/test/java/com/group2/rms/candidate/service/TalentPoolSelectionReviewTests.java` | Đối chiếu ngày nguồn; giữ kiểm tra không ghi dữ liệu. |
| `src/test/js/talent-pool-selection.test.cjs` | Kiểm tra draft chỉ ID, cùng các trường hợp state trước đó. |
| `src/test/js/talent-pool-ui.test.cjs` | Popup lấy thông tin từ response, ngày, preview và bỏ người ở trang khác. |
| `src/test/js/application-preview.test.cjs` | Preview động, đúng URL nguồn, fallback, ẩn/hiện link hồ sơ. |

## Kiểm thử

- 120 test Java qua: 39 MVC, 7 selection review, 7 bookmark, 56 access, 11 application status; không failure/error/skipped.
- 17 test JavaScript qua bằng Node runner: 9 state, 5 UI, 3 preview.
- Java test render Thymeleaf thực tế bằng MockMvc; backend nghiệp vụ dùng mocks, không xác minh lưu dữ liệu trên SQL Server thật.
- Kiểm tra cú pháp ba script, ID template không trùng, không inline CSS/JS, màu dùng token và các token tồn tại; `git diff --check` qua.
- Trình duyệt tự động không mở được `http://localhost:8081/talent-pool` (`ERR_BLOCKED_BY_CLIENT`). Chưa xác minh pixel/layout thực tế, PDF viewer trình duyệt, hoặc thao tác người dùng đầu-cuối trên ứng dụng đang chạy.

Lệnh kiểm tra từ project root:

```powershell
mvn.cmd -q "-Dtest=TalentPoolSelectionReviewTests,ApplicationPipelineWebTests,TalentPoolBookmarkTests,ApplicationAccessTests,ApplicationStatusServiceTests" test
node --test src/test/js/talent-pool-selection.test.cjs src/test/js/talent-pool-ui.test.cjs src/test/js/application-preview.test.cjs
```

## Giới hạn nghiệp vụ đang giữ nguyên

Nút xác nhận cuối vẫn bị vô hiệu hóa, có thông báo chưa sẵn sàng. Endpoint legacy `/suggest` tạo Application `HM_Suggested`, trái yêu cầu không tạo Application của luồng mới; giao diện này chỉ gọi `/selection-review` để đọc/kiểm tra. Lượt sửa layout không mở rộng backend lưu đề xuất hoặc schema. Không báo thêm thành công khi chưa có backend đáp ứng hợp đồng mới.

## Kiểm tra thủ công trên ứng dụng

1. Đăng nhập HM, mở Talent Pool từ tin tuyển dụng được phép; lọc và sang trang 2.
2. Chọn ba ứng viên ở nhiều trang, đổi CV nguồn của một người. Kiểm tra thanh chọn đếm đúng.
3. Mở hồ sơ bằng icon; kiểm tra profile, hai cột, tên/ngày CV và ghi chú thật.
4. Mở từng CV, đóng lại; mapping lựa chọn không thay đổi. Thử nguồn không tải được để kiểm tra fallback.
5. Lưu ghi chú; kiểm tra thông báo và ghi chú mới ở đầu feed. Refresh rồi bấm Back: đúng job, bộ lọc, trang và ba lựa chọn cùng CV nguồn.
6. Mở tổng kết: tên/vị trí/ngày khớp server; icon mắt mở đúng nguồn; X bỏ riêng một người kể cả người ở trang khác; số đếm cập nhật.
7. Mở trực tiếp `/talent-pool/{id}`: Back về kho chung. Thử thêm query `returnUrl=https://example.org`: không chuyển ra ngoài.
8. Kiểm tra job khác không trộn lựa chọn; Candidate không được xem trang/ghi chú nội bộ; HR chỉ dùng chức năng đúng quyền.
9. Kiểm tra desktop và viewport dưới 992px: hai cột co thành một, popup không tràn, nút đóng/quay lại và focus bàn phím hoạt động.
