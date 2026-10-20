# Áp dụng mẫu Auth — Mộc RMS (20/10/2026)

## Mẫu và phạm vi

Tham khảo `docs/prototype-reference/moc_register_single_progress.html`; đối chiếu UI Rules, CSS Guide, Catalog và ngoại lệ auth tại `UI_MIGRATION.md` mục 5. Giữ `auth/fragments`, không sửa các master layout, token, font hoặc component dùng chung.

Áp dụng cho sáu màn: đăng nhập, đăng ký, xác minh đăng ký, quên mật khẩu, xác minh khôi phục và đặt mật khẩu mới.

## Thay đổi

- Bố cục hai cột ở desktop, một cột khi màn hình hẹp; cột giới thiệu và form có khoảng cách theo mẫu. Login/Recovery căn card theo chiều dọc, Register giữ chiều cao tự nhiên. Màn thấp cuộn toàn trang, không có scrollbar riêng trong form.
- Dùng minh họa gốc nhúng trong prototype ở đáy trang, nền xanh rất nhẹ và các điểm nhấn nhỏ. Không tạo mới nội dung quảng cáo hoặc dữ liệu nghiệp vụ.
- Tiêu đề/form dùng `var(--sans)` và font offline hiện có. Bỏ Google Fonts và bộ token legacy khỏi phần auth; dùng `tokens.css`, `components.css` và style logo từ `topbar.css`, không sửa các file này.
- Tái sử dụng `.card`, `.btn`, `.form-input`, `.form-label`, `.form-hint`, `.form-error`, `.alert`; CSS riêng chỉ chứa bố cục và thành phần auth đặc thù. Không có mã màu trực tiếp, inline style, `!important` hoặc ghi đè token root trong CSS auth mới.
- Register giữ đủ năm trường; hai cột trên desktop, email chiếm hàng riêng. Khi hẹp, chuyển một cột.
- Một đường tiến trình ngang cho hai bước đăng ký và ba bước khôi phục, giữ `aria-current="step"`. Các bước chỉ báo tiến trình, không tạo điều hướng giả.
- Nút chính có mũi tên SVG cùng hệ icon auth. Script chỉ thay nhãn khi đang gửi để giữ icon; khôi phục nhãn khi quay lại trang.
- Giữ nút hiện/ẩn mật khẩu, label/aria, focus-visible, error summary liên kết field, nhập/dán sáu chữ số OTP và input dự phòng khi không có JavaScript.

## File sửa/tạo

| File | Nội dung |
|---|---|
| `src/main/resources/templates/auth/login.html` | Card, input/button theo catalog, scene và icon submit |
| `src/main/resources/templates/auth/register.html` | Như trên, giữ lưới năm trường và cooldown |
| `src/main/resources/templates/auth/register-otp.html` | Scene, card/form và icon submit |
| `src/main/resources/templates/auth/forgot-password.html` | Đồng bộ khôi phục bước 1 |
| `src/main/resources/templates/auth/reset-password-otp.html` | Đồng bộ khôi phục bước 2 |
| `src/main/resources/templates/auth/reset-password.html` | Đồng bộ khôi phục bước 3 |
| `src/main/resources/templates/auth/fragments.html` | Assets chuẩn, scene auth, SVG mũi tên và dữ liệu hiển thị tiến trình |
| `src/main/resources/static/css/auth.css` | Thay các override cũ bằng CSS auth dùng token và catalog |
| `src/main/resources/static/js/auth.js` | Cập nhật nhãn submit mà không xóa icon |
| `src/main/resources/static/images/auth-register-scene.png` | Trích nguyên ảnh PNG từ prototype, 900 × 339, 356486 byte |

Ảnh có SHA-256 `2F6E37EB0ED263B8C32E4850117A239225DF1340840A8DC55795F5CD3D30F309`; không chỉnh sửa nội dung ảnh. Ảnh cũ được giữ nguyên.

## Kiểm chứng

Lệnh kiểm thử cuối:

```powershell
mvn.cmd '-Dmaven.repo.local=C:\Users\Administrator\.m2\repository' '-DforkCount=0' '-Djacoco.skip=true' '-Dauth.preview=true' '-Dtest=AuthBotanicalWebTests' test
```

- **BUILD SUCCESS; 6 test passed; 0 failure/error/skipped.** MockMvc render Thymeleaf và chạy Spring Security thật, mock repository/service gửi email/đăng ký/khôi phục. Có kiểm tra CSRF, POST đăng ký/OTP/resend, thông báo lỗi tiếng Việt và đăng nhập Candidate/HR.
- Đối chiếu với HEAD: **50 phần tử form/input/button/label giữ nguyên mọi thuộc tính ngoài class**; giữ nguyên các điều kiện/vòng lặp backend trong sáu template, các tên tham số, giới hạn ký tự, endpoint và method.
- Chrome headless mở HTML do Thymeleaf render, phục vụ qua preview chỉ đọc trên loopback; mọi POST tới preview bị chặn.
- **36 tổ hợp**: sáu màn × 1440×1000, 1366×768, 1920×1080, 768×1024, 390×844 và 320×740. Không tràn ngang cấp trang, không có input vượt card hoặc scrollbar riêng trong card. H1 dùng font token Sans-serif.
- Kiểm tra nút mắt và aria-label/aria-pressed, focus bằng Tab, OTP sáu ô và nhập tự động đủ mã, error summary nhận focus/liên kết về field, trạng thái đang gửi và khôi phục nút/giữ SVG, input OTP khi tắt JS, reduced motion. Không có lỗi JavaScript.
- `node --check auth.js` và `git diff --check` đạt.

Ảnh trước/sau, ảnh từng màn, kết quả browser JSON và log nằm tại `../.codex-tmp/` (không đưa vào Git). Bản HTML render nằm tại `target/auth-botanical-preview/`.

## Giới hạn và bảo toàn

Đây là kiểm chứng UI và hợp đồng auth qua MockMvc, không phải E2E với tài khoản/email/database thật. Chưa gửi OTP thật, tạo tài khoản thật hoặc đổi mật khẩu thật trong đợt này. Không thay đổi Controller, Service, DTO, Security, schema, database hoặc dependency.

Các thay đổi đã có trước trong `docs/prototype-reference/` được giữ nguyên. Không commit, push, merge hoặc deploy.
