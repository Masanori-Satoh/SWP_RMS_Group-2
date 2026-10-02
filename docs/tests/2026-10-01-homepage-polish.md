# Kiểm thử homepage sau Impeccable polish — 01/10/2026

## Chuẩn bị

Mở [prototype/index.html](../../prototype/index.html) trực tiếp bằng Chrome/Edge. Không cần chạy Spring Boot, DB, login hoặc server HTTP. Công ty Mộc/vị trí/lương đều là minh họa; thao tác ứng tuyển chưa gửi hồ sơ thật.

Trong DevTools chọn Responsive Mode và lần lượt đặt 1440, 1024, 768, 375 px; 320 px là kiểm tra bổ sung cho reflow. Bật bàn phím và thử touch trong chế độ device khi được hỗ trợ.

## Kịch bản từng bước

| ID | Bước test | Kết quả mong đợi | Kết quả thực tế |
| --- | --- | --- | --- |
| POL-01 | Mở trang ở 1440 px. Đọc header, hero và các section; so với ảnh trước polish. | Giữ thương hiệu Mộc, màu/font/minh họa và bố cục một công ty. Search và vị trí đang tuyển là hành động chính; card gọn, chữ phụ đọc rõ. | PASS visual review desktop/mobile. Card nội dung hiện tại 220 → 194px; hero desktop 698 → 672px. |
| POL-02 | Đặt 1024/768/375 px, kéo từ đầu tới footer. Thử thêm 320 px. | Không cuộn ngang, không cắt text/control; tablet search có keyword hàng riêng; mobile xếp dọc. | PASS DOM/layout ở 1440/1024/768/375/320. Visual review toàn trang chỉ desktop 1440 và mobile 375. |
| POL-03 | Reload, bấm Tab đầu tiên rồi Enter ở `Bỏ qua điều hướng`. | Skip link hiện rõ và focus chuyển tới main. | PASS bằng sự kiện bàn phím CDP. |
| POL-04 | Focus từ khóa, gõ `java`, bấm Enter. Sau đó bấm Tab. | Một vị trí Java; trang cuộn đến danh sách, focus ở heading `Vị trí đang tuyển`; Tab đi tiếp tới control của kết quả. | PASS gõ/submit/focus và Tab tới `Xóa bộ lọc`. |
| POL-05 | Bấm `Xóa bộ lọc`. Gõ `zzzz-no-role`, rồi bấm `Xem tất cả vị trí`. | Empty state xuất hiện; reset trả về sáu vị trí và focus ô search. | PASS chuột/typing CDP. |
| POL-06 | Bấm phòng ban `Thiết kế`. | Chỉ Product Designer hiện; focus đến heading kết quả. | PASS chuột CDP. |
| POL-07 | Xóa lọc. Chọn `Đà Nẵng`, rồi chọn thêm `Thiết kế`. | Ban đầu còn Data Analyst; tổ hợp sau không có kết quả và hiển thị cách reset. | PASS qua select change event trên DOM; thao tác native select bằng tay CHƯA TEST. |
| POL-08 | Xóa lọc, bấm `Xem vị trí` trên một card. Dùng Tab/Shift+Tab nhiều lần. | Dialog có tên chức danh, metadata/mô tả; các control nền không nhận focus trong khi modal mở. Nút xem vị trí có tên riêng cho từng chức danh. | PASS accessibility tree, chuột, Tab/Shift+Tab CDP. Native dialog có thể qua body sentinel trước khi quay lại control bên trong; không có background control nhận focus. |
| POL-09 | Bấm khoảng đệm trong dialog. Bấm `Ứng tuyển`, rồi Escape. Mở lại và thử bấm backdrop ngoài khung. | Bấm padding không đóng; ứng tuyển hiện thông báo demo; Escape đóng và trả focus về nút đã mở. Bấm backdrop ngoài khung đóng được. | PASS padding/apply/Escape/focus restore và backdrop ngoài khung bằng CDP. |
| POL-10 | Ở mobile, tap Menu, tap mục vị trí. Mở lại rồi Escape; mở lại rồi tap bên ngoài. | Menu mở/đóng đúng, Escape trả focus về summary, không che thao tác sau điều hướng. | PASS touch CDP; Escape xác nhận riêng sau khi chờ gesture hoàn tất. |
| POL-11 | Tab qua links/input/select/buttons; xem kích thước control và màu chữ trong DevTools. | Target tối thiểu 44×44 CSS px, focus ring rõ, input mobile 16px. Chữ thường/placeholder tối thiểu 4.5:1. | PASS target bounds ở năm kích thước; 134 phần tử chữ và placeholder không có contrast fail, tỷ lệ thấp nhất 4.87:1. |
| POL-12 | Bật Reduce Motion trong DevTools Rendering. | Scroll tự động không smooth và transition giảm. | PASS `scroll-behavior:auto`; rule reduced-motion vẫn giữ. |
| POL-13 | Tạm đổi chức danh trong DevTools thành tên dài, ví dụ `Senior Java Developer — Nền tảng và tích hợp hệ thống doanh nghiệp`. | Card reflow được, không tràn ngang. Reload trả lại nội dung gốc. | PASS tại 375 px; nội dung test không ghi vào file HTML. |
| POL-14 | Dùng NVDA/VoiceOver và zoom trình duyệt 200% trên thiết bị thật. | Label/status/dialog có tên rõ; thứ tự đọc hợp lý; nội dung reflow và điều hướng được. | NOT RUN. Accessibility tree và viewport CSS không thay thế screen reader hoặc zoom/thiết bị thật. |

## Bằng chứng và kết quả

- [Ảnh desktop sau polish](assets/2026-10-01-homepage-polish/after-1440.png), [ảnh mobile sau polish](assets/2026-10-01-homepage-polish/after-375.png).
- [Ảnh desktop trước polish](assets/2026-10-01-homepage-polish/before-1440.png), [ảnh mobile trước polish](assets/2026-10-01-homepage-polish/before-375.png).
- [Kết quả trước polish](assets/2026-10-01-homepage-polish/before-results.json), [vòng xác nhận](assets/2026-10-01-homepage-polish/after-results.json), [xác minh riêng bàn phím](assets/2026-10-01-homepage-polish/keyboard-results.json).
- Vòng xác nhận có hai kết quả `false` cho Enter và Escape. Chẩn đoán xác định driver Enter chưa gửi text carriage-return và driver touch kiểm tra quá sớm. Test riêng đã gửi Enter đúng và chờ gesture hoàn tất: submit/focus kết quả, Escape/menu focus và Shift+Tab containment đều PASS; không sửa source để né kết quả test. Giữ raw JSON để nhìn thấy lần kiểm tra này.
- Kiểm tra dùng Chrome headless/CDP với sự kiện chuột, phím và touch thật vào browser engine trên file local. Không phải E2E backend hoặc thao tác thủ công trên thiết bị thật. Tool browser tương tác của phiên làm việc không khởi động được, nên dùng CDP.
- Kiểm tra cuối: `node --check` nội dung script PASS; không có script/stylesheet/fetch ngoài; `git diff --check` PASS; các link bằng chứng tồn tại. Harness và captures tạm đã dọn; file browser profile nằm trong Temp. Chỉ có hai vòng chụp desktop/mobile; lần chẩn đoán riêng không chụp thêm hoặc tiếp tục chỉnh thẩm mỹ.

Không chạy Maven/Spring/DB vì thay đổi nằm trong prototype HTML độc lập. Chưa chạy Safari/Firefox, screen reader hoặc thiết bị mobile thật. Khi test thủ công, điền browser/version, viewport, ID case và ảnh/lỗi nếu có; không nhập hồ sơ cá nhân thật.
