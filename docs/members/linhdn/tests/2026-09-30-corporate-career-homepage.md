# Kiểm thử prototype trang tuyển dụng của một công ty — 2026-09-30

## Phạm vi và cách chạy

- Trang cần mở: [`../../prototype/index.html`](../../prototype/index.html). Đây là **một file HTML độc lập**; dữ liệu Mộc và sáu vị trí là minh họa, không kết nối ứng dụng Spring Boot hay database.
- Có thể mở file trực tiếp bằng trình duyệt. Để kiểm tra bằng HTTP local, đứng tại thư mục gốc repo và chạy `py -m http.server 8766 --bind 127.0.0.1`, rồi mở `http://127.0.0.1:8766/prototype/index.html`.
- Dùng DevTools Responsive Mode tại 1440, 1024, 768 và 375 px. Không cần tài khoản hoặc dữ liệu seed.
- PASS nghĩa là đạt đúng điều kiện ghi ở từng bước; các thao tác chỉ kiểm tra prototype, **không xác nhận nộp hồ sơ thật**.

## Kịch bản thao tác thủ công

| ID | Bước thực hiện | Kết quả mong đợi | Kiểm tra lần này |
| --- | --- | --- | --- |
| HOME-01 | Mở trang, xem header, hero và phần đầu danh sách việc làm. | Chỉ có thương hiệu Mộc; mô tả công ty, lời mời xem vị trí, search và minh họa người làm việc rõ ràng; không có "Post a Job", danh bạ công ty hoặc số liệu giả. | Visual QA tại 1440 px và khoảng 500 px: PASS. |
| HOME-02 | Nhập `java` vào ô từ khóa. | Còn một card `Senior Java Developer`; bộ đếm là một. | Chrome headless smoke: PASS. |
| HOME-03 | Thay từ khóa bằng `zzzz-no-role`. | Không còn card hiện; thông báo chưa có vị trí phù hợp xuất hiện. | Chrome headless smoke: PASS. |
| HOME-04 | Bấm `Xem tất cả vị trí` trong trạng thái rỗng. | Bộ lọc được xóa, sáu card xuất hiện. | Chrome headless smoke: PASS. |
| HOME-05 | Ở `Khám phá theo phòng ban`, bấm `Thiết kế`. | Cuộn đến danh sách và chỉ còn `Product Designer`. | Chrome headless smoke: PASS. |
| HOME-06 | Ở card đang hiện, bấm `Xem vị trí`. | Dialog hiển thị đúng tên, phòng ban, địa điểm, loại hình và mô tả của vị trí; đóng được bằng nút `Đóng` hoặc Escape. | Mở dialog: PASS tự động. Đóng bằng Escape: CHƯA TEST thủ công. |
| HOME-07 | Trong dialog, bấm `Ứng tuyển`. | Thấy thông báo chưa gửi hồ sơ vào hệ thống; không có lời xác nhận nộp thật. | Chrome headless smoke: PASS. |
| HOME-08 | Chọn phòng ban và địa điểm trên form rồi bấm `Tìm vị trí`; thử một tổ hợp có và không có kết quả. | Card và bộ đếm phản ánh giao của các bộ lọc; nút xóa trả về sáu card. | CHƯA TEST đầy đủ tổ hợp thủ công. |
| HOME-09 | Mở menu mobile, chọn `Vị trí đang tuyển`; thử các nút `Hồ sơ của tôi`, `Liên hệ`, `Quyền riêng tư`. | Menu/anchor hoạt động; các mục chưa có backend hoặc nội dung pháp lý chỉ hiện thông báo demo trung thực. | CHƯA TEST thủ công. |
| HOME-10 | Dùng Tab/Shift+Tab, Enter và Escape qua search, nút lọc và dialog; bật Reduce Motion. | Focus thấy rõ, nhãn form đọc được, dialog thao tác bàn phím được, không có chuyển động bắt buộc. | CHƯA TEST bằng bàn phím/reader thực tế. |
| HOME-11 | Đặt viewport lần lượt 375, 768, 1024 và 1440 px; xem header, form, ảnh và các card. | Không cuộn ngang; search và card xếp cột phù hợp màn hình. | Kiểm tra DOM `scrollWidth <= viewport + 1` cả bốn kích thước: PASS. Visual QA chỉ tại 1440 và khoảng 500 px. |
| HOME-12 | Ngắt mạng rồi mở file `index.html` trực tiếp. | Layout, SVG, CSS và JavaScript vẫn hiện vì không tải asset/CDN ngoài. | Kiểm tra mã: không có dependency ngoài; mở offline thực tế: CHƯA TEST. |

## Bằng chứng tự động và giới hạn

- Tách nội dung `<script>` trong `prototype/index.html` và chạy `node --check`: **PASS**.
- Chạy Chrome headless với một iframe cùng origin: sáu vị trí ban đầu; lọc từ khóa; trạng thái rỗng; reset; lọc phòng ban; dialog chi tiết; thông báo ứng tuyển; không tràn ngang ở 375/768/1024/1440 px: **PASS** (`SMOKE_PASS initial keyword empty reset department detail apply widths:375,768,1024,1440`). Bộ harness HTML tạm đã được xóa sau khi chạy.
- Ảnh chụp Chrome headless tại 1440 px và khoảng 500 px đã được xem để kiểm tra bố cục. Đây là visual QA, không thay thế test bàn phím, screen reader hoặc E2E ứng tuyển.
- Không chạy Maven, Spring Context, database hoặc API vì prototype này không tích hợp vào ứng dụng. Những kiểm tra đó không phải bằng chứng của trang HTML độc lập.

## Cách ghi kết quả khi test lại

Ghi ngày, trình duyệt/phiên bản, kích thước màn hình và PASS/FAIL cho từng HOME-01…12. Nếu FAIL, thêm bước tái hiện và ảnh chụp màn hình. Không nhập thông tin ứng viên thật vào bản mẫu.
