# 02/11/2026 — Khắc phục lỗi Java trong IDE

## Yêu cầu

- Người thực hiện: LinhDN.
- Người dùng báo VS Code đang hiện lỗi mã nguồn. Cửa sổ dự án thực tế đang mở trong Antigravity IDE, dùng phần mở rộng Java của Red Hat.

## Chẩn đoán và xử lý

- Bảng Problems ban đầu có **2.328 error**, nhiều lỗi `The import ...dto cannot be resolved`, dù các file DTO vẫn có trên đĩa.
- Log JDT báo `Error filtering index locations based on qualifier` và `FileNotFoundException` khi đọc tệp chỉ mục `.index.tmp` trong workspace của Java language server.
- Lần Maven biên dịch sạch ngay trước yêu cầu này đã chạy thành công 170 test liên quan đến Talent Pool/Application Pipeline. Vì vậy ưu tiên khôi phục trạng thái phân tích dự án của IDE.
- Chạy lệnh **Java: Reload Projects** trong IDE và chờ Java biên dịch/kiểm tra lại toàn bộ dự án. Số lỗi giảm dần từ 2.328 xuống 1.007, 707, 407 rồi 0.
- Không chỉnh sửa thêm mã Java, template, POM, thiết lập cảnh báo hoặc cấu hình DB trong lượt khắc phục này. Các thay đổi Việt hóa đang có được giữ nguyên.

## Kiểm chứng

- Sau khi hoàn tất, thanh trạng thái hiện **Java: Ready**.
- Bộ đếm Problems không còn error; giao diện hiển thị **0 lỗi đỏ**, **3.948 warning** và **16 info**. Các cảnh báo vẫn được giữ hiển thị, không bị tắt để che lỗi.
- Không chạy lại bộ test sau thao tác nạp lại IDE vì mã nguồn không thay đổi. Kết quả 170/170 trước đó là kiểm chứng Maven của cùng phần mã Việt hóa, không phải một lượt chạy mới trong tác vụ này.
- `git diff --check`: không có lỗi khoảng trắng.

## Bàn giao

- Các cảnh báo còn lại cần được đánh giá riêng nếu có yêu cầu; lượt này xử lý lỗi đỏ về trạng thái dự án Java.
- Không thay đổi schema hoặc dữ liệu DB. Chưa commit/push.
