# PR 01/08 — Tách tài nguyên frontend Offer theo vai trò và loại stylesheet trùng

**Base:** `main` (`1e3d7134`).
**Nhánh:** `refactor/huyenpt/offer-01-frontend-structure`.

## Vấn đề và kết quả

Trước thay đổi, các trang HR, Director và Candidate còn phụ thuộc vào JS/CSS Offer nguyên khối, chứa nhiều phần dành cho vai trò khác. Cấu trúc fragment styles ở template con cũng làm stylesheet bị ghép lặp qua layout Thymeleaf.

PR này chia tài nguyên thành common, HR và Director, cập nhật đường dẫn nạp tại từng trang, bỏ modal Director dư trên danh sách HR và giữ thông báo flash sau thao tác/reload. Sáu template Offer bỏ wrapper styles gây lặp; không sửa base.html hoặc internal.html.

Diff production gồm **16 file HTML/CSS/JS, +4.467/-6.488 dòng, giảm ròng 2.021 dòng**. Có thêm một file báo cáo, tổng cộng 17 file trong PR. JS nội bộ từ một file 2.005 dòng thành ba file tổng cộng 1.245 dòng.

## Danh sách file và nội dung thay đổi

### JavaScript — 4 file

| File | Thay đổi | Mục đích |
|---|---|---|
| `src/main/resources/static/js/offers.js` | Xóa file JS nguyên khối sau khi tách chức năng và cập nhật các trang gọi | Loại mã cũ và tránh nạp toàn bộ xử lý của mọi vai trò |
| `src/main/resources/static/js/pages/offers/offer-common.js` | Thêm phần CSRF, escape HTML, flash toast, toast chờ sau reload và xác nhận/phát hành thư mời | Dùng chung các thao tác xuất hiện ở nhiều trang Offer |
| `src/main/resources/static/js/pages/offers/offer-hr.js` | Thêm xử lý danh sách/bộ lọc/chọn hàng/xóa nháp, form tạo-sửa, gợi ý dữ liệu và popup xuất Excel | Gom hành vi thuộc màn HR; chỉ khởi tạo theo phần tử DOM thực tế |
| `src/main/resources/static/js/pages/offers/offer-director.js` | Thêm xử lý popup quyết định và duyệt/từ chối trên trang chi tiết | Tách phần Director khỏi mã dành cho HR |

### CSS — 6 file

| File | Thay đổi | Mục đích |
|---|---|---|
| `src/main/resources/static/css/offers.css` | Xóa CSS Offer nguyên khối sau khi chuyển các nhóm cần thiết sang thư mục pages/offers | Bỏ nguồn CSS cũ |
| `src/main/resources/static/css/pages/offer-workspace.css` | Xóa nguồn CSS workspace Offer cũ | Thay bằng bộ common/HR/Director |
| `src/main/resources/static/css/pages/offers/offer-common.css` | Thêm nền CSS chung của các màn Offer nội bộ: khung, bảng, bộ lọc và thành phần dùng chung | Tránh chép CSS giữa HR và Director |
| `src/main/resources/static/css/pages/offers/offer-hr.css` | Thêm CSS đặc thù danh sách, form và popup xuất Excel HR | Nạp cùng các màn HR cần dùng |
| `src/main/resources/static/css/pages/offers/offer-director.css` | Thêm CSS danh sách phê duyệt, popup quyết định và phần quyết định trên trang chi tiết | Tách giao diện phê duyệt khỏi CSS HR |
| `src/main/resources/static/css/pages/candidate-offers.css` | Loại phần CSS nội bộ/legacy không còn thuộc trang Candidate sau khi tách | Giữ phạm vi CSS của ứng viên rõ ràng |

### Template — 6 file

| File | Thay đổi |
|---|---|
| `src/main/resources/templates/offers/list.html` | Nạp common + HR CSS/JS, bỏ wrapper styles gây lặp, xóa modal/nút Director dư trong danh sách HR |
| `src/main/resources/templates/offers/form.html` | Nạp common + HR CSS/JS thay các asset cũ; bỏ wrapper styles gây lặp |
| `src/main/resources/templates/offers/detail.html` | Nạp common + Director CSS/JS cho phần thao tác chung và quyết định; bỏ wrapper styles gây lặp |
| `src/main/resources/templates/offers/director-list.html` | Nạp common + Director CSS/JS; bỏ wrapper styles gây lặp |
| `src/main/resources/templates/offers/candidate-list.html` | Giữ URL CSS Candidate và flash toast, chỉ bỏ wrapper styles gây lặp |
| `src/main/resources/templates/offers/candidate-detail.html` | Giữ URL CSS Candidate và flash toast, chỉ bỏ wrapper styles gây lặp |

### Tài liệu — 1 file

`docs/members/huyenpt/pr-review/01-frontend-structure.md`: mô tả vấn đề, manifest, phạm vi và bằng chứng kiểm tra của PR hiện tại.

## Phạm vi nghiệp vụ và khả năng tương thích

- Giữ tên template, route, tên biến backend, field/id/name và dữ liệu form hiện hữu.
- Không thay Java backend, SecurityConfig, database, migration, thông tin đăng nhập hoặc shared layout.
- Không chỉnh source của Application hoặc các module khác. Các trang Candidate trong manifest vẫn thuộc Offer Management.
- Các asset cũ chỉ được xóa sau khi cập nhật tham chiếu trong template; đã kiểm tra không còn template/JS gọi ba đường dẫn asset bị xóa.
- Đây là PR cấu trúc frontend đầu tiên, chưa chứa toàn bộ bản sửa nghiệp vụ, quyền, notification và export ở các PR tiếp theo. Nhánh Offer gốc giữ nguyên toàn bộ bản hoàn thiện.

## Kiểm thử và kiểm tra xung đột

| Kiểm tra | Kết quả |
|---|---|
| OfferIntegrationTests | 34 test đạt |
| CandidateOfferWebTests | 19 test đạt |
| CandidateOfferServiceTests | 35 test đạt |
| OfferServiceTests | 114 test đạt |
| Tổng Maven | **202 test đạt, không lỗi hoặc bỏ qua** |
| Cú pháp JS | `node --check` đạt với cả ba JS mới |
| Tài nguyên | Sáu template có đường dẫn CSS/JS tồn tại, không còn wrapper styles gây lặp |
| Git | Diff check đạt; mô phỏng merge với origin/main không conflict tại thời điểm kiểm tra |

Các test được chạy trực tiếp trong worktree của PR trên nền main, không dùng kết quả của nhánh Offer gốc để thay thế. Lần cập nhật mô tả và tên nhánh không sửa production, nên không chạy lại Maven. Chưa chạy lại browser A/B trong lượt tách PR này.

## Thứ tự các PR tiếp theo

2. Phân quyền và validation nghiệp vụ Offer.
3. Đồng bộ UI, popup, ngôn ngữ và form.
4. Lưu thời gian thử việc: migration 020 và backend liên quan.
5. Hợp đồng export, tối ưu truy vấn/API và workbook.
6. Giao dịch, xử lý đồng thời và notification với link theo người nhận.
7. Expired, migration 021, job tắt mặc định và tích hợp Application.
8. Tài liệu audit, hướng dẫn nghiệp vụ, comment tiếng Việt và bằng chứng kiểm thử.

Mỗi PR tiếp theo dùng nhánh có huyenpt, chỉ được dựng/test/push sau khi người dùng xác nhận đã tạo PR trước. Base là main mới nhất hoặc nhánh PR trước nếu chưa merge.
