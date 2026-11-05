# Báo cáo làm sạch thông tin công cụ trong tài liệu

- Ngày: 05/11/2026.
- Người thực hiện: LinhDN.
- Yêu cầu: rà soát toàn bộ thư mục `docs` và xóa các cụm từ liên quan tới công cụ hỗ trợ được chỉ định.

## Kết quả thực hiện

Đã làm sạch 11 file, gồm 6 file được Git theo dõi và 5 file tạm bị Git bỏ qua:

- Bốn báo cáo/work log: bỏ phần chú thích hỗ trợ bên cạnh tên người thực hiện.
- Hai sơ đồ `.drawio`: bỏ thuộc tính metadata `agent` chứa thông tin công cụ.
- Ba script `.cjs`: đổi đường dẫn thư viện riêng trên máy thành `require('playwright')`; khi chạy cần môi trường Node có cài Playwright.
- Hai báo cáo test `.xml`: bỏ các mục đường dẫn công cụ khỏi thuộc tính `java.library.path`, giữ các mục môi trường còn lại và kết quả test đã lưu.

Không thay đổi mã nguồn sản phẩm, schema hoặc dữ liệu nghiệp vụ. Tạo mới báo cáo này và work log riêng, bổ sung liên kết vào chỉ mục nhật ký.

## Kiểm chứng

- Quét văn bản không phân biệt hoa/thường trên toàn bộ `docs`, bao gồm file ẩn, file bị bỏ qua và chế độ đọc nhị phân: không còn từ khóa yêu cầu sau khi làm sạch.
- Kiểm tra tên file, chuỗi UTF-16 và nội dung giải nén của các file `.docx`, `.xlsx`, `.jar`: không phát hiện từ khóa yêu cầu.
- Kiểm tra cú pháp XML/sơ đồ và cú pháp ba script đã sửa; không chạy lại các kịch bản trình duyệt hoặc các test nghiệp vụ.
- `git diff --check`: qua.

[Work log chi tiết](../../management/work_logs/2026-11-05-docs-cleanup.md).
