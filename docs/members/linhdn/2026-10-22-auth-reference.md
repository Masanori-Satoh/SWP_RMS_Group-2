# Giao diện auth theo mẫu ngày 22/10/2026

Nguồn người dùng cung cấp: `D:/dowload/moc_auth_login_giong_anh.html`.

## Phạm vi

Đồng bộ sáu trang Login, Register, Register OTP, Forgot Password, Reset Password OTP và Reset Password. Dùng nền văn phòng theo mẫu, nội dung giới thiệu bên trái, Card trắng bên phải và tab liên kết đăng nhập/đăng ký. Mobile chuyển thành một cột; chữ và control là HTML thật, không thu nhỏ ảnh chụp giao diện.

Giữ navbar/brand chung qua fragment hiện có và các token màu của RMS; chỉ đổi font Arial trong phạm vi auth theo mẫu. Các file layout, navbar, token, component chung và backend không sửa. Các trường form, action, method, CSRF, validation, OTP và countdown giữ nguyên.

## Quyết định triển khai

- Ảnh gốc chứa cả giao diện và chữ; tạo một nền văn phòng sạch để không có form/chữ trùng khi hiển thị HTML thật.
- Tab là liên kết trang thực, dùng `aria-current` thay vì giả lập ARIA tab cho các trang độc lập. Các bước đăng ký vẫn có cho trình đọc màn hình; các màn OTP/khôi phục tiếp tục hiển thị tiến trình.
- Giữ đăng nhập bằng tên đăng nhập hoặc email theo chức năng hiện tại. Chưa thêm checkbox ghi nhớ đăng nhập vì SecurityConfig chưa bật remember-me.
- Thông báo đăng xuất/thành công/lỗi chỉ hiển thị theo trạng thái Thymeleaf thực. Không mang JavaScript giả lập submit của mẫu vào ứng dụng.
- Không hiển thị các vị trí tuyển dụng minh họa được vẽ sẵn trong ảnh như dữ liệu thật. Liên kết khám phá dùng `/jobs`.

## Kiểm chứng

- `AuthBotanicalWebTests`: 6 tests, 0 failures, 0 errors, 0 skipped. Render Thymeleaf và filter bảo mật thật; các service/repository được mock.
- Browser Chrome: 6 màn × 6 viewport (1694×928, 1440×900, 1024×768, 768×1024, 390×844, 320×740) = 36 lượt render. Không tràn ngang/control vượt Card; có CSRF và tab trên mọi màn. Kiểm tra thêm hiện/ẩn mật khẩu, focus bàn phím, nhập đủ sáu số OTP, error summary. Không lỗi JavaScript.
- Detector trên CSS và các template chính trả `[]`.
- Kết quả browser và ảnh: `../.codex-tmp/auth-new-reference/`. Log test: `target/auth-reference-20261022-tests.log`.
- Đây là preview từ MockMvc, không phải E2E với tài khoản/database/email thật. Không gửi OTP hay sửa tài khoản thật.

## Ảnh nền và prompt

Asset dùng trong app: `src/main/resources/static/images/auth-office-scene.png`.

Tạo bằng công cụ Imagegen tích hợp, chế độ sửa ảnh, từ PNG nhúng đầu tiên của HTML mẫu. Bản sinh gốc: `C:/Users/Administrator/.codex/generated_images/01a162d9-6b9e-7602-8562-d29427b61035/exec-aab70155-0b67-4371-8d69-f3e790ba03f2.png`. Không ghi đè các ảnh cũ.

Prompt chính xác:

> Edit target: supplied recruitment login webpage screenshot. Create ONLY the clean photographic background plate for this exact webpage. Preserve the same wide 1694:928 composition and office scene: bright white diffuse daylight left, glass office walls and dark window mullions right, indoor green plants, desk in bottom quarter, open coding laptop bottom center-left, notebook, white coffee mug, office chair. Remove ALL user interface and ALL readable text from the entire image: remove top navigation and logo and top bar, all headline and paragraph and benefits text/icons in left half, floating job cards and application progress cards in center, all glowing digital overlays, and the entire white login form panel on the right. Seamlessly reconstruct the real office scene behind these objects. The left upper half should remain very bright soft pale office window light providing whitespace for real HTML headline to be overlaid. The right half should be darker photographic office glass and plants for a real white login card to overlay. Remove writing on books and mug too. No logos, no words, no buttons, no boxes, no floating interface. Match the source photo angle, atmosphere, subdued greens and cool greys. Photorealistic, not illustration.
