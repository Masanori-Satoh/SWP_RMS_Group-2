# Login Sequence Diagram — theo ảnh mẫu

Diagram `Login` được cập nhật trực tiếp trong `login.vpp`, giữ project ID `lDXo7oWAUITI6gBZ` và diagram ID `lgd6531eb8b9b841`. Nội dung đăng nhập và xác thực nằm trong cùng một diagram, không có diagram hoặc ref `Authenticate Credentials` riêng.

- [Project VPP](login.vpp)
- [Ảnh PNG](login.png)

Diagram có 6 participant và 15 message, với một `alt` cho thất bại/thành công và một `opt` cho hai truy vấn khi identifier sau trim không rỗng. Mũi tên return nét đứt; không có `break`, `ref`, `loop` hay self message.

| Participant | Thành phần thực tế |
|---|---|
| User | Người dùng |
| Browser / Login Page | Giao diện và browser gửi form, theo redirect và hiển thị kết quả |
| UsernamePasswordAuthenticationFilter | Spring Security nhận POST `/login` |
| DaoAuthenticationProvider | Provider kiểm tra authentication qua Spring Security |
| DatabaseUserDetailsService | Service tìm user, map role, tạo UserDetails |
| UserRepository | Truy vấn username và email không phân biệt hoa thường |

ProviderManager, success/failure handler và BCrypt được gộp để bố cục gọn. Provider và UserDetailsService vẫn thể hiện riêng vì đây là hai thành phần riêng trong project. Ghi chú của diagram nêu BCrypt, các trường hợp thất bại chính, CSRF và redirect. Các lần GET tiếp theo được tóm tắt trong message hiển thị của browser.

Luồng chính: nhập thông tin → nhấn Login → POST `/login` → authenticate → `loadUserByUsername` → tìm username và email → trả UserDetails hoặc lỗi → trả kết quả xác thực → `alt` redirect lỗi hoặc redirect thành công → browser hiển thị trang sau GET.

Các điều kiện đối chiếu source:

- Identifier null/rỗng/sau trim rỗng, không tìm thấy hoặc khớp username và email của hai user khác nhau: thất bại xác thực.
- Role không hợp lệ/thiếu; trạng thái khác chính xác `Active`; hoặc password không khớp BCrypt: thất bại xác thực. Bản login trả redirect `/login?error` và giao diện dùng cùng thông báo chung.
- Không dựng self message `Validate input` ở Login Page. Không thêm validator định dạng email, độ dài hoặc độ mạnh không có trong login; HTML5 required không được dùng làm một bước validate nghiệp vụ riêng.
- Request thiếu/sai CSRF nhận HTTP 403 trước filter đăng nhập. Đây không phải redirect `/login?error`.
- Thành công redirect đến saved URL hoặc `/dashboard`. Nếu đích là `/dashboard`, `DashboardController` chuyển Candidate đến `/portal/applications`. Quyền của trang đích được kiểm tra ở request tiếp theo.

Thông báo lỗi theo `auth/login.html`: “Thông tin đăng nhập không đúng hoặc tài khoản không hoạt động. Vui lòng thử lại hoặc đặt lại mật khẩu.”

Nguồn: `SecurityConfig`, `AuthController`, `DatabaseUserDetailsService`, `RoleAuthorities`, `UserRepository`, `DashboardController`, `templates/auth/login.html`, `static/js/auth.js`, và bytecode Spring Security của dependency local. Không thay đổi source hay database.

Đã kiểm tra bằng runtime Visual Paradigm: load/save file `.vpp` gốc, export lại PNG/XML, kiểm tra SQLite integrity và các endpoint của message; các model ngoài Login được đối chiếu với backup. Đây là kiểm tra source và artifact, không phải chạy login HTTP/DB thực tế.

PNG được Visual Paradigm Modeler bản Evaluation xuất nên có watermark của ứng dụng.
