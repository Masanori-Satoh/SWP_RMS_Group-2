# Đồng bộ Login theo Register — 21/10/2026

> Các mục đầu ghi lại lần sửa Login riêng. Phần **Cập nhật phạm vi các màn auth** cuối tài liệu mô tả thay đổi cuối cùng theo yêu cầu tiếp theo, thay thế các nhận định phạm vi cũ.

## Kết quả và nguyên nhân

Login và Register vốn dùng chung Grid, chiều rộng Card, typography, token và các fragment nền. Không cần tăng width riêng cho Login. Điểm lệch là `.auth-card` căn giữa theo cả cột giới thiệu kèm ảnh, làm Card Login ngắn bị đẩy xuống; link Quên mật khẩu có chiều cao tối thiểu 32px làm khoảng cách label–input lớn hơn Register.

## File và selector đã sửa

Chỉ thay đổi giao diện tại `src/main/resources/static/css/auth.css`:

- `.auth-login .auth-card { align-self: start; }`: đưa Card Login lên cùng điểm bắt đầu của cột form Register, gần vùng nội dung chữ bên trái. Không thêm chiều cao, khoảng trống giả hoặc offset.
- `.auth-login .auth-label-row { align-items: flex-start; }`: căn label và link trong hàng Mật khẩu.
- `.auth-login .auth-label-row a { min-height: var(--s6); }`: giảm vùng link từ 32px xuống 24px, để khoảng cách label–input bằng Register. Giữ focus indicator và thao tác bàn phím.

File báo cáo mới: `docs/members/linhdn/2026-10-21-login-register-sync.md`.

Không thay HTML, JavaScript, ảnh, CSS nền hoặc các selector đang áp dụng cho Register/Forgot Password. Không sửa stylesheet/layout dùng chung của toàn ứng dụng.

## Thuộc tính được đồng bộ và tái sử dụng

Tiếp tục dùng chung `.auth-stage`, `.auth-shell`, `.auth-card`, `.auth-form`, `.form-label`, `.form-input`, `.btn--primary`, token màu/font, fragment `story`, `scene`, `artwork` và năm lá cố định hiện có. Giữ minh họa botanical, tòa nhà kính và chữ trong ảnh; không tạo ảnh khác hoặc animation.

Browser xác nhận hai trang có cùng chiều rộng và mép trái Card; padding, border, radius, shadow, nền Card, font heading/label/input, màu chữ, chiều cao input và khoảng cách label–input bằng nhau tại cùng viewport/zoom.

Ví dụ tại 1440×900, zoom 100%:

| Thông số | Login trước | Login sau | Register giữ nguyên |
|---|---:|---:|---:|
| Chiều rộng Card | 575px | 575px | 575px |
| Mép trên Card | 323,58px | 152px | 152px |
| Chiều cao Card | 518,19px | 511,44px | 772,20px |
| Khoảng cách label Mật khẩu–input | 10,75px | 4px | 4px |

Font đang dùng: Inter, Source Sans 3 và các fallback sans-serif; không thêm serif hoặc font ngoài.

## Zoom và khác biệt có chủ đích

Giữ nguyên Grid/breakpoint đang có của Register; Login dùng cùng cột theo chiều rộng CSS thật khi browser zoom. Không thêm `transform: scale()`, margin lớn, width theo viewport hoặc định vị ảnh mới. Trên tablet/mobile vẫn một cột.

Login ngắn hơn tự nhiên, có hai trường nhập, không có bước xác minh đăng ký. Register giữ năm trường và indicator bước. Minh họa và khối giới thiệu ở cùng vị trí trên Desktop; không kéo Card Login dài để chạm đáy ảnh. Không thay đổi nội dung nghiệp vụ.

## Kiểm thử

- Native Chrome zoom xác nhận bằng `chrome.tabs.setZoom/getZoom`.
- Desktop 1920×1080, 1440×900, 1366×768, mỗi kích thước ở 67%, 80%, 100%, 125%, 150%.
- Tablet 768×1024; mobile 390×844 và 320×740 ở 100%.
- 18 tổ hợp × 3 màn Login/Register/Forgot × trước/sau = **108 lần render đạt**. Không tràn ngang, input/nút không vượt Card, không lỗi JS. Sau sửa, các thuộc tính chung và khoảng cách field của Login bằng Register.
- **36 cặp screenshot Register/Forgot trước–sau giống từng byte**, cùng các số đo DOM/style không đổi. Không thay đổi giao diện hai trang này.
- Nút hiện/ẩn mật khẩu, aria-pressed, focus bằng Tab, required validation, Enter phát sinh submit, action `/login`, CSRF và các href giữ nguyên. Submit trong browser QA được chặn trước khi gửi.
- Mô phỏng khối thông báo lỗi/thành công để kiểm tra co giãn bố cục; đây không phải phản hồi đăng nhập thật.
- `AuthBotanicalWebTests`: **BUILD SUCCESS, 6 test đạt**. MockMvc chạy Security thật với repository/service mock; có POST đăng nhập Candidate và HR, redirect `/dashboard`, CSRF và hợp đồng form. Không chứng minh E2E với tài khoản/database thật.
- Đối chiếu 50 phần tử form/input/button/label và các điều kiện Thymeleaf không đổi; `git diff --check` đạt.

Ảnh và JSON: `login-sync-*.png`, `login-register-sync-results.json`; log Maven `login-register-sync-tests.log`. Browser dùng HTML Thymeleaf do MockMvc render qua preview chỉ đọc, bỏ qua loader chat trong QA. Chưa kiểm tra phiên ứng dụng thật hoặc email/database thật.

## Phạm vi bảo toàn

Không sửa Controller, Service, authentication, Security, database, endpoint, input name, CSRF, redirect hoặc nghiệp vụ. Không thêm thư viện; không commit/push. Các thay đổi chỉ ảnh hưởng Login nhờ tiền tố `.auth-login`.

## Cập nhật phạm vi các màn auth

Theo yêu cầu tiếp theo, giữ Register làm chuẩn và áp dụng cho Login, Quên mật khẩu, OTP đăng ký, OTP khôi phục và Đặt mật khẩu mới. Vẫn chỉ sửa `src/main/resources/static/css/auth.css`:

- Chuyển căn đầu form vào `.auth-card` dùng chung; bỏ bản riêng cho Login. Các màn ngắn cùng mép trên/mép trái/chiều rộng với Register, chiều cao theo nội dung.
- Đưa căn hàng label và chiều cao link Quên mật khẩu vào `.auth-label-row` dùng chung. Register không có hàng này.
- Trên desktop, `.auth-botanical:not(.auth-register)` đưa minh họa lên 24px và dịch ảnh trong figure từ margin-left -20% thành -12%. Tái sử dụng vùng alpha của PNG để ảnh nối qua cột form, tránh kết thúc đúng tại một đường dọc bên dưới form ngắn. Giữ ảnh rộng 120%, không thêm mask/khung/ảnh mới; Register và breakpoint mobile không nhận thay đổi này.
- Theo yêu cầu mới nhất, bỏ border dưới nhãn của `.auth-steps li`, chỉ giữ rail ngang `::before` bên cạnh số bước. Áp dụng cho cả hai bước đăng ký và ba bước khôi phục; giữ số, nhãn và `aria-current`.

Các thuộc tính Card, typography, field, nút, khoảng cách, năm lá và nền wave vẫn dùng chung các token/fragment hiện có. Giữ navbar chuẩn. Ô OTP tiếp tục có cỡ chữ và chiều cao riêng để nhập sáu số.

### Xác minh lần cập nhật

- Ma trận layout: 20 tổ hợp gồm desktop 1920×1080, 1440×900, 1366×768 tại zoom Chrome thật 67/80/100/125/150%; 960×720 tại 67/100%; tablet 768×1024 và mobile 390×844, 320×740 tại 100%.
- 40 render baseline Register/Login và 120 render sau sửa của sáu màn đã đạt kiểm tra layout. Không tràn ngang, input/nút không vượt Card, các form cùng cột và mép trên, ảnh không đè chữ giới thiệu.
- Trước yêu cầu bỏ thanh thứ hai, 20 cặp screenshot Register trước/sau giống từng byte. Yêu cầu mới nhất chủ động thay đổi phần chỉ báo bước trên Register; các phần còn lại vẫn giữ nguyên.
- Kiểm tra riêng sau khi bỏ thanh: 20 render trên năm màn có bước, tại 1440px zoom 67/100/150% và mobile 390px/100%; mỗi bước có một rail 2px, không border dưới nhãn, một bước hiện hành, không tràn ngang.
- Browser kiểm tra required, hiện/ẩn mật khẩu, Tab/focus, Enter submit (chặn trước khi gửi), CSRF, nhập sáu số OTP đồng bộ vào trường backend và khối phản hồi mô phỏng: đạt. Lần kiểm tra OTP đầu dùng nhầm selector của ô số đầu; sửa bộ kiểm tra sang `#otp-value` và chạy riêng thao tác, không sửa JavaScript ứng dụng.
- `AuthBotanicalWebTests`: 6/6 đạt với Spring Security/Thymeleaf thật và dịch vụ dữ liệu/email được mock; không chứng minh luồng DB/email thực tế. Kiểm tra tĩnh 50 hợp đồng form/field và `git diff --check`: đạt.

Ảnh kiểm tra và script nằm ở thư mục QA cục bộ của workspace cha; đây là preview Thymeleaf cục bộ, không phải bằng chứng môi trường đang triển khai. Không đổi template, JavaScript, bitmap, backend, nghiệp vụ hoặc các file khung dùng chung. Không commit/push.
