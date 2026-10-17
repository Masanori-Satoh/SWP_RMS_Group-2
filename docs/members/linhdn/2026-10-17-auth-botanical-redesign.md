# Login / Register — Botanical Minimalism

## Phạm vi và giao diện

Login, Register và bước xác minh đăng ký dùng cùng lớp `auth-botanical`. Cột giới thiệu đặt cao hơn, giữ nội dung và CTA `/#roles`, thêm ba điểm nhấn tiếng Việt. Khung trắng, font Lora / Source Sans 3 đã có, nền sáng từ token hiện tại. Không sửa token, layout nội bộ, logo dùng chung hoặc module khác.

Register giữ đủ năm trường và tên binding cũ:

1. Họ và tên | Tên đăng nhập.
2. Email chiếm cả hàng.
3. Mật khẩu | Xác nhận mật khẩu.
4. Nút gửi mã chiếm cả hàng.

Helper tên đăng nhập rút gọn thành “Tên đăng nhập không thể thay đổi.” Khi không đủ chiều rộng, các trường về một cột. Input cao 48px, mật khẩu có nút mắt và nhãn trợ năng Hiện/Ẩn. Không có scrollbar riêng trong card; màn thấp cuộn trang tự nhiên.

Theo phản hồi cuối, **chỉ card Login được căn giữa theo chiều dọc của cột desktop**, phần giới thiệu vẫn nằm cao. Dưới 768px card trở về vị trí đầu hàng; Register và OTP không áp dụng căn giữa này. Login giữ chiều cao tự nhiên theo số trường.

## Minh họa và chuyển động

- `auth-botanical-branch.png`: dùng nguyên ảnh cành lá người dùng cung cấp, 1000 × 561px, 384.910 byte. Không dùng screenshot toàn trang làm nền, không chỉnh sửa ảnh gốc. Ảnh có kích thước khai báo để tránh dịch bố cục, trang trí `aria-hidden` / `alt=""`.
- Chữ trang trí là “Con người tạo nên giá trị.” Ba điểm nhấn: Khám phá cơ hội / Kết nối tài năng / Cùng tạo giá trị. Icon SVG nhỏ, dùng fragment auth chung, không thêm thư viện.
- Nền radial-gradient nhẹ; nền và hai lá nhỏ dịch 3–5px bằng transform, chu kỳ 16–20 giây, không đi qua form. Chỉ animate trên desktop khi thiết bị báo cập nhật nhanh và người dùng không yêu cầu giảm chuyển động; tạm dừng khi tab ẩn. Trang trí không bắt pointer và không làm đổi kích thước layout.
- Đối chiếu [MDN radial-gradient](https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/Values/gradient/radial-gradient) và [MDN prefers-reduced-motion](https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/At-rules/@media/prefers-reduced-motion). Không tải package từ Haikei/Lucide.

## Tiếng Việt và hợp đồng auth

Title được render bằng tiếng Việt ngay từ server. Thêm placeholder tiếng Việt, giữ nhãn/tooltip Hiện/Ẩn và label OTP. Thông báo validation trình duyệt của ba trang được đặt bằng tiếng Việt, không đổi điều kiện required/email/độ dài/pattern. Sửa fragment dịch để không thay thông báo NotBlank/Size tiếng Việt từ message bundle thành lỗi chung.

Giữ action/method/name, `th:field`, CSRF, liên kết lỗi và focus error summary; password không bị chặn paste/autocomplete. OTP vẫn sáu chữ số, gửi lại/thời gian hết hạn dựa trên backend; không sửa thời hạn hoặc tạo tài khoản trước xác minh. Forgot/Reset giữ body class và story cũ, không nhận minh họa/lưới mới; JS nút mật khẩu vẫn có nhánh text cũ cho các trang này.

## File thay đổi

- `src/main/resources/templates/auth/{login,register,register-otp,fragments}.html`.
- `src/main/resources/static/css/auth.css`: phần mới scope theo `.auth-botanical`; một selector Login riêng để căn card.
- `src/main/resources/static/js/auth.js`: giữ icon khi đổi Hiện/Ẩn, native validation tiếng Việt và pause trang trí khi tab ẩn.
- `src/main/resources/static/images/auth-botanical-branch.png`.
- `src/test/java/com/group2/rms/auth/AuthBotanicalWebTests.java`.

Không đổi Java production/controller/service/repository, database hoặc dependency; không commit/push.

## Kiểm chứng và giới hạn

- **6 test passed, 0 failure/error/skipped** ở lượt cuối. MockMvc chạy template và Spring Security thật, mock các thao tác gửi email/tạo tài khoản: kiểm tra năm field, CSRF, lỗi tiếng Việt, POST Register/OTP/resend, đăng nhập Candidate và HR, render Forgot/Reset. Không kết nối DB hoặc gửi email thật. `node --check auth.js` và `git diff --check` qua.
- Xem browser trên HTML **do Thymeleaf render trong test**, phục vụ bởi server preview chỉ đọc trên loopback. Preview chặn POST và không phải backend auth đang chạy. Server app 8081 không khả dụng trong lượt kiểm tra.
- Viewport được mô phỏng bằng iframe có kích thước CSS thật, sau đó thu nhỏ để chụp trong cửa sổ kiểm tra: 1440×900, 1366×768, 1920×1080, 390×844. Register hai cột ở desktop, một cột ở 390; scrollWidth đo được lần lượt 1425, 1351, 1920, 375px (đã trừ scrollbar), không tràn ngang. 1440×900 thấy đủ form và submit; 1366×768 cần cuộn để xem phần dưới; mobile cuộn trang tự nhiên.
- Đã xem Login/OTP ở 1440×900, kiểm tra nút mắt đổi aria-pressed và nhãn “Ẩn mật khẩu”; xác nhận lại card Login căn giữa sau phản hồi. Chưa có kiểm chứng SMTP/OTP email thật, thiết bị vật lý, hoặc benchmark thiết bị yếu. Reduced-motion được cấu hình trong CSS; chưa đổi tùy chọn hệ điều hành để kiểm thử thực tế.
- Preview/harness, HTML render và log nằm dưới `target/`, không đưa vào Git. Kết quả lượt cuối được ghi trong log `target/auth-botanical-tests-confirmed.log`.
