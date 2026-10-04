# Test Dashboard Candidate tiếng Việt

Ngày: 04/10/2026. Không chạy script tạo/xóa DB, seed hay thao tác mutation chưa được duyệt.

## Kết quả tự động

| Nhóm | Trạng thái | Bằng chứng |
|---|---|---|
| Compile nối dữ liệu | PASS | `target/candidate-dashboard-compile.log` |
| Service/privacy/query tests | PASS 4/4 | CandidateDashboardServiceTests |
| MockMvc/Thymeleaf/session/CSRF | PASS 5/5 | CandidateDashboardWebTests; `target/candidate-dashboard-confirmation.log` |
| Public Jobs regression | PASS 13/13 | CareerFlowTests |
| Spring Context/JPA/query trên SQL Server, SELECT-only | PASS 1/1 | CandidateDashboardReadOnlyTests; `target/candidate-dashboard-tests-final.log` |
| Browser fixture 1440/1024/768/375 | PASS 52/52 round2 | [JSON](assets/2026-10-04-candidate-dashboard/round2-checks.json) |
| JS syntax / scoped diff check | PASS | node --check, git diff --check cho file thuộc đợt này |
| Live application E2E | NOT TESTED | Lượt audit trước gặp lỗi Tomcat loopback |

## Lệnh kiểm tra

Tại root project, dùng Maven và JDK 21 đã cài:

```powershell
mvn.cmd -q '-Dtest=CandidateDashboardServiceTests,CandidateDashboardWebTests,CareerFlowTests' '-Dspring.jpa.hibernate.ddl-auto=none' '-Dspring.sql.init.mode=never' '-Dspring.jpa.show-sql=false' test

# Chỉ khi môi trường DB đã sẵn sàng: validate schema + SELECT, không seed/update/create.
mvn.cmd -q '-Dtest=CandidateDashboardReadOnlyTests' '-Dspring.jpa.hibernate.ddl-auto=validate' '-Dspring.sql.init.mode=never' '-Dspring.jpa.show-sql=false' test

# Terminal riêng: preview fixture, không phải ứng dụng thật.
py docs/members/linhdn/scripts/candidate-preview-server.py --port 8772

# Terminal khác; có Chrome được cài. Giới hạn hai lượt kiểm tra/polish.
node docs/members/linhdn/scripts/check-candidate-dashboard-ui.mjs http://127.0.0.1:8772 round1
node docs/members/linhdn/scripts/check-candidate-dashboard-ui.mjs http://127.0.0.1:8772 round2
```

Fixtures chỉ chứa dữ liệu kiểm thử từ MockMvc, ở `target/candidate-preview/empty/index.html` và `populated/index.html`. CSRF trong fixture được thay bằng marker. Không dùng fixture POST để chứng minh logout thật.

23 test riêng biệt PASS trong lượt chính. Sau sửa CSS/body class cuối, 22 service/web/career tests được chạy lại và PASS; test DB read-only trước đó vẫn là bằng chứng cho Java/query không thay đổi.

Lịch sử lỗi đã sửa:

- Lượt đầu 8/9 PASS, 1 ERROR: Thymeleaf từ chối biến `application`; đổi thành `item`.
- Browser round1 51/52 PASS: root scrollWidth499 tại viewport375; span sr-only của link interview nằm ngoài containing block bảng. Sửa wrapper `position:relative`; round2 root không tràn ngang, 52/52 PASS.
- Driver SQL Server có warning connection closed trong lượt read-only; suite vẫn exit0 và test PASS. Không chỉnh pool/network/config trong phạm vi UI.

Ảnh kiểm tra: [desktop có dữ liệu](assets/2026-10-04-candidate-dashboard/round2-populated-1440.png), [mobile có dữ liệu](assets/2026-10-04-candidate-dashboard/round2-populated-375.png), [empty desktop](assets/2026-10-04-candidate-dashboard/round2-empty-1440.png). Đây là fixture kiểm thử, không phải dữ liệu production.

Manual C01–C10 bên dưới trên ứng dụng thật: **NOT TESTED** trong đợt này. MockMvc đã chứng minh một phần auth/CSRF và browser đã chứng minh frontend fixture; hai loại này không thay thế live application E2E.

## Điều kiện manual test trên ứng dụng thật

- App đã khởi động; account Candidate Active; có ít nhất hai Candidate A/B để kiểm tra scope.
- Dùng dữ liệu test đã được owner chuẩn bị, không tự tạo offer/phỏng vấn tại DB production.
- Để case có dữ liệu: A có đơn, interview Scheduled/Rescheduled tương lai, offer chính thức; B có dữ liệu riêng. Một offer Draft/Pending_Director/Director_Approved phải tồn tại trong môi trường test để đối chiếu không bị lộ.
- Không ghi password, hash, SMTP secret, CSRF thật hay session cookie vào báo cáo.

## Các case và từng step

### C01 — Guest / đăng nhập / ngôn ngữ

1. Mở `/dashboard` khi chưa login → chuyển `/login`.
2. Đăng nhập Candidate Active → vào Dashboard hoặc mở Dashboard từ navigation.
3. Kiểm tra Tổng quan, Đơn ứng tuyển, Lịch phỏng vấn, Thư mời, Thông báo, Hồ sơ đều có entry point nhìn thấy được.
4. Mong đợi: nhãn Dashboard/header/sidebar tiếng Việt, HTML `lang=vi`; tên vị trí/nội dung DB giữ nguyên. Không có link tài khoản nội bộ/requisition trong Candidate sidebar.

### C02 — Scope và GBR-02

1. Login A, đối chiếu title/ngày/trạng thái hồ sơ của A; không có đơn B.
2. Đối chiếu lịch A; không có interview B, lịch Cancelled/Completed hay lịch quá khứ.
3. Đối chiếu offer A đã gửi; không có offer B hoặc offer còn ở nội bộ.
4. Kiểm tra rendered HTML, không chỉ phần đang mở: không có AI match score, OverallScore, HR/HM reviewer comments, đánh giá phỏng vấn, HRResponseNotes/director comments hoặc credentials.
5. Login B trong session khác và lặp lại. Mong đợi dữ liệu tách biệt.

### C03 — Empty state

1. Login Candidate chưa có hồ sơ/interview/offer.
2. Mong đợi count 0 từ backend, thông báo rỗng riêng từng mục; không metric giả.
3. Nhấn “Tìm vị trí phù hợp” / “Xem vị trí đang tuyển” → `/jobs` thật.
4. Thông báo ghi Chưa khả dụng, không số unread giả; profile là account hiện tại.

### C04 — Filter tại chỗ

1. Tại Đơn ứng tuyển, gõ `ky su` với row “Kỹ sư phần mềm” → match không dấu.
2. Gõ `dieu phoi` với “Điều phối dự án” → match Đ/đ.
3. Chọn status → chỉ giữ rows title + status phù hợp; live count thay đổi, không chuyển focus.
4. Gõ keyword không tồn tại → thông báo không có kết quả, không viewport overflow.
5. Tab tới Xóa bộ lọc, Enter → list khôi phục; focus/scroll không chuyển sang Hero hay đầu trang.
6. Rời button → button biến mất nếu không còn filter. Lặp lại ở Thư mời; hai nhóm filter độc lập.
7. Mong đợi filters chỉ áp dụng cho 20 hồ sơ/20 thư mời đã tải; tổng count không bị đổi thành count của filter.

### C05 — Lịch phỏng vấn / link

1. Kiểm tra thời gian bắt đầu/kết thúc, hình thức và địa điểm rõ ràng.
2. Online URL http(s) hợp lệ: nút Tham gia mở tab mới, có `noopener noreferrer` và accessible text thông báo tab mới.
3. URL thiếu/sai/javascript: không có link executable, hiện lời nhắc thông tin đang chờ cập nhật.
4. Offline: địa điểm dạng text, không link hóa chuỗi tùy ý.

### C06 — Offer chính thức

1. Kiểm tra title, lương/tháng, ngày bắt đầu và status.
2. Tab tới Xem chi tiết, Enter/Space → mở lương thử việc, địa điểm, quyền lợi; thao tác lại đóng.
3. Mong đợi dữ liệu đến từ offer đã gửi; null hiển thị “Chưa xác định/Chưa có thông tin”. Không lấy CreatedAt làm Offer Date.
4. Benefits chứa HTML phải xuất hiện dưới dạng text escaped; không chạy script.
5. Accept/Decline/Negotiate disabled và có giải thích, không tạo request mutation hay thông báo thành công giả.

### C07 — Phần chưa khả dụng

1. Kiểm tra Rút đơn, Thêm nhắc nhở, Lưu hồ sơ, Đổi mật khẩu đều disabled.
2. Mong đợi mỗi nhóm có lý do rõ; không link 404, không modal giả, không sửa DB qua JS.
3. User email/role/department không phải form có thể sửa; không hiển thị password/hash.

### C08 — Logout / CSRF

1. Bấm Đăng xuất → dialog có title/description Việt, focus **Ở lại**.
2. Bấm Ở lại hoặc Escape → đóng, vẫn login, focus về trigger.
3. Bấm lại → xác nhận Đăng xuất → POST `/logout` có CSRF; về `/login?logout`.
4. Mở lại Dashboard → yêu cầu login. POST thiếu CSRF phải HTTP403.
5. Tắt JS → form vẫn POST logout trực tiếp, không treo chức năng.

### C09 — Responsive / keyboard / zoom

1. Kiểm tra 1440,1024,768,375px. Không scroll ngang toàn trang; bảng chỉ cuộn trong region.
2. <=768px: Danh mục mở/đóng sidebar, aria-expanded cập nhật, Escape đóng.
3. Tab từ skip link → main/navigation/filters/details/logout; focus rõ, không bị header che.
4. Controls tối thiểu44px, label/select có tên, ARIA refs tồn tại, trạng thái không chỉ phân biệt bằng màu.
5. Zoom200%, title/email/location dài, prefers-reduced-motion; vẫn đọc và thao tác được.

### C10 — Regression internal / tài khoản disabled

1. Login HR/Admin → Dashboard cũ vẫn dùng English/shared layout, không nạp candidate-dashboard.js.
2. Candidate Inactive/Blocked không login; session đang dùng bị thu hồi ở protected request theo guard hiện có.
3. Không phát sinh route/schema/JWT mới; các error audit ngoài phạm vi vẫn ghi riêng, không bị che.

## Flow và điểm nối tiếp

[Luồng từng class/package/hàm](../flows/candidate-dashboard-ui-flow.md) · [Quá trình và lý do sửa](../update%20architecture/2026-10-04-candidate-dashboard-ui.md).
