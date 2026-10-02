# Audit Gemini — homepage Mộc, 01/10/2026

## Phạm vi và bằng chứng trước khi sửa

Đọc toàn bộ review trong attachment `71597c48-833d-43cf-91df-90bab39c7577`, đối chiếu HTML/CSS/JS tại `prototype/index.html`, PRODUCT.md và design system hiện có. Ảnh `docs/tests/assets/2026-10-01-homepage-polish/after-*` khớp phiên bản trước đợt này. Review là đầu vào đánh giá, không phải nguồn yêu cầu nghiệp vụ. Không thể kết luận tác giả là AI chỉ từ vẻ ngoài.

Trang thuộc một công ty hư cấu Mộc. Người dùng cho phép bổ sung nội dung mẫu cụ thể và đổi media/font trong đợt này. Không sửa Spring Boot, database, security hoặc Thymeleaf.

## Phân loại khuyến nghị

| Nhận xét chính | Phân loại | Bằng chứng / quyết định |
|---|---|---|
| Card trông clickable nhưng chỉ nút cuối hoạt động; tiêu đề không phải link | Confirmed | `h3` thuần văn bản, handler chỉ gắn nút. Dùng một title anchor phủ vùng card, tránh hai tab stop trùng. |
| Detail thiếu lương, ngày đăng, nội dung và URL | Confirmed | Dialog chỉ có title/meta/summary. Thêm cấu trúc mô tả, metadata đầy đủ, hash slug ổn định. |
| Search ở Hero, xa results; reset chuyển focus ngược lên | Confirmed | Form trong hero, `clearSearch()` gọi keyword.focus(). Chuyển bộ lọc vào positions và giữ focus theo ngữ cảnh. |
| Clear luôn hiện; department filter trùng và thiếu state/count | Confirmed | Reset luôn hiển thị; section riêng không có aria-pressed. Hợp nhất phòng ban vào bộ lọc có số lượng và pressed state. |
| Hai SVG người không đủ chất lượng | Confirmed | Hai cảnh hình người SVG tự dựng. Bỏ, dùng composition hình học và preview văn bản công việc; không stock photo. |
| Disclaimers prototype xuất hiện nhiều lần | Confirmed | Hero/results/culture/footer/dialog có copy lặp. Giữ sự thật về nội dung mẫu trong docs và comment, bỏ copy lặp khỏi UI. |
| Mọi section có cùng bố cục | Partially confirmed | Eyebrow lặp, nhưng split/list/figure đã khác nhau. Bỏ eyebrow dư, giữ các bố cục hữu ích, thêm benefit rows. |
| Company story mơ hồ, thiếu benefits | Confirmed | Không có working policy cụ thể. Thêm nội dung hư cấu vừa phải; docs liệt kê chính xác các giá trị mẫu. |
| Menu sai thứ tự, account là dead end | Confirmed | Nav khác thứ tự DOM; account mở info giả. Đồng bộ positions/about/benefits/life/process, bỏ account action. |
| Tablet squeeze và CAREERS mất ở mobile | Confirmed | Reasons/departments 3 cột tới 580px, process 4 tới 768px; CAREERS ẩn ở 580px. Sửa breakpoint và giữ wordmark. |
| Serif 600 là synthetic; tracking có thể ảnh hưởng dấu | Partially confirmed | Georgia không có 600; chưa có bằng chứng clipping dấu. Dùng Lora 600 thật + Source Sans 3 400–600, tracking nhẹ. |
| Spacing nhiều giá trị lẻ | Confirmed | Có 17/19/23/29… Chuẩn hóa scale chủ đạo 4/8/12/16/24/32/48/64/80. Không ép kích thước font/radius vào spacing scale. |
| Viền control 2,65:1 | Not applicable với con số review | Công thức WCAG cho #7b8b7d trên #e7f0e8 cho 3,091:1. Vẫn kiểm tra control mới riêng, hairline trang trí không bắt buộc 3:1. |
| Low contrast của SVG trang trí là lỗi nontext | Not applicable | Trang trí không truyền thông tin thao tác. SVG người được bỏ vì chất lượng thiết kế, không gán lỗi WCAG tùy tiện. |
| aria-describedby hiện tại sai hoàn toàn | Partially confirmed | Mô tả ngắn hiện tại không tự làm mất DOM. Với detail nhiều section, bỏ describedby để người đọc điều hướng nội dung thay vì đọc một chuỗi dài. |
| Native mobile details thiếu semantics nên cần thay | Not applicable | details/summary đã có state native. Giữ, sửa thứ tự, focus/close và kích thước. |
| Footer cần link thật thay fake modal | Confirmed | Privacy/terms/FAQ/contact/account hiện là button info-dialog. Bỏ các trang chưa có; dùng contact mailto và company URL miền reserved. |
| Thu nhỏ footer target xuống 32–36px | Not applicable | Giữ vùng thao tác tối thiểu 44px theo yêu cầu accessibility. Giảm số cột/copy, không giảm hit area. |
| Bottom CTA lặp đường đi | Confirmed | CTA trở lại roles/life. Đổi thành gửi CV qua email, không mô phỏng upload/backend. |
| Thêm số người thật, ảnh thật, địa chỉ đường phố, application tracking | Not applicable trong phạm vi | Không có nguồn doanh nghiệp đã duyệt và không có backend prototype. Không bịa bằng chứng employer branding. |

## Kế hoạch đã chốt trước khi sửa

1. P0/P1: đường đi card → detail → contact, metadata/URL, bộ lọc gần kết quả, bỏ hành động giả.
2. P1: media không có người, benefits/copy cụ thể, nav/responsive/type.
3. P2: spacing, rhythm, footer, giảm decoration và kiểm tra focus/contrast.
4. Giữ palette trắng ấm/xanh rừng, hairline, restrained shadows, cỡ chữ gọn, salary, accent-insensitive search, skip link/focus/reduced motion/live status và native semantics.

## Nội dung mẫu được dùng

- Công ty: phần mềm workflow giúp lập kế hoạch, phối hợp tác vụ và lưu quyết định dự án.
- Policy hư cấu: hybrid theo nhóm (tối đa hai ngày từ xa/tuần), thứ Hai–thứ Sáu 09:00–18:00, 12 ngày phép/năm, bảo hiểm theo quy định, ngân sách học tập 3 triệu đồng/năm, laptop và thiết bị, review mỗi sáu tháng. Cần doanh nghiệp duyệt trước khi công bố thật.
- Giữ nguyên sáu chức danh, lương, phòng ban, thành phố và ngày đăng. Nội dung trách nhiệm/yêu cầu mới là mô tả mẫu, không phải JD được duyệt.
- Email `tuyendung@moc.example`, website `https://moc.example`: miền reserved cho ví dụ, không nhận hồ sơ thật. Không gửi thư hoặc xác nhận thành công; không bịa địa chỉ đường phố, testimonial hoặc số nhân sự.
- Không thêm trang privacy/terms/FAQ không có nội dung. Không tạo upload, login, application persistence hoặc SPA.

## Skill và xác minh

UI/UX Pro Max: focused searches typography, contextual live count và touch spacing. Cặp Lora + Source Sans 3 là lựa chọn theo brief; dataset không trả đúng cặp này, không coi gợi ý wellness/monochrome là phù hợp để áp nguyên xi. Font chính thức từ Google Fonts, subset Latin/Latin-ext/Vietnamese nhúng offline, giấy phép tại `prototype/FONT_LICENSES.txt`.

Impeccable: polish/craft floor, sau implementation chạy critique độc lập và detector. Người dùng đã cho phép hai agent review. Kết quả sẽ bổ sung ở tài liệu critique và test; không suy diễn scan sạch thành production readiness.
