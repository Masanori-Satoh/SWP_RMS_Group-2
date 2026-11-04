# Task 25C — Khử link CSS trùng trong Offer

Nhánh `refactor/huyenpt/offer-ui-js-to-html-css`, HEAD `4ed4c25426b45a4c186415eb78340bd5c2574384`. Trước thay đổi, index và tracked diff rỗng. Báo cáo 38 và `scratch/` đã có dưới dạng untracked. Phạm vi kế thừa kết quả Task 25 và Task 25A về cơ chế ghép head của Layout Dialect 3.4.0. [PoC Task 25A](38-task-25a-shared-layout-css-deduplication-poc.md)

## Thay đổi

Trong mỗi template dưới đây, bỏ đúng một cặp thẻ bao `<th:block layout:fragment="styles">` và đưa các link hiện có trực tiếp vào `<head>`. Cả sáu wrapper chỉ chứa link stylesheet, không có điều kiện hay biến cục bộ. Các thuộc tính `th:href`, thứ tự link cấp trang, tên tệp, title, script, body, binding form và route được giữ theo phiên bản trước.

| Tệp trong `src/main/resources/templates/offers/` | Link trước → sau | CSS riêng theo thứ tự |
|---|---:|---|
| `list.html` | 13 → 7 | offer-common, offer-hr, flash-toast |
| `form.html` | 13 → 7 | offer-common, offer-hr, flash-toast |
| `detail.html` | 13 → 7 | offer-common, offer-director, flash-toast |
| `director-list.html` | 13 → 7 | offer-common, offer-director, flash-toast |
| `candidate-list.html` | 11 → 7 | candidate-offers, flash-toast |
| `candidate-detail.html` | 11 → 7 | candidate-offers, flash-toast |

Baseline là tokens → global → components → bộ CSS riêng → workspace → candidate-shell nếu có → bộ CSS riêng → bộ CSS riêng. Sau sửa là tokens → global → components → workspace → candidate-shell nếu có → bộ CSS riêng. Thứ tự của lần xuất hiện cuối cùng của từng URL được giữ. Layout chung vẫn có hai slot rỗng. Với Offer, link đi vào head một lần qua cơ chế ghép head của dialect vì trang con không cung cấp fragment cùng tên. Các layout và module khác nằm ngoài phạm vi Task 25C.

## Kiểm chứng trước khi áp dụng

- Probe Spring Thymeleaf dùng head thật của cả sáu template, cùng layout base/internal, chạy 12 ca A/B. Tất cả pass. Probe thay title động bằng title fixture và dùng resolver string để thử riêng head.
- Ứng dụng bản sao dùng cùng Spring Boot, Layout Dialect, controller và asset với bản local gốc. Resolver trỏ vào cây template dưới `target/` và chỉ sáu template Offer khác. Bản sao chạy trên loopback 18081 với SQL init tắt và schema ở chế độ validate. Chrome so HTML trả về với DOM ở mỗi lượt.
- Chrome CDP pass 18 cặp snapshot A/B cho sáu lượt route/role ở 1440, 768 và 390 px. Gồm HR List/Create/Detail, Director List/Detail và Candidate List. Khớp computed styles, kích thước, bề rộng tài liệu, meta, CSRF và script URL. Đã thử Excel modal, Director decision modal, Send dialog, Candidate list dialog và toast bằng thao tác mở/đóng UI. Backdrop được đo là `rgba(37, 41, 35, 0.45)` trên HR/Director Detail và Candidate List. Không có runtime exception hoặc lỗi console API trong lượt đo.
- HR Edit và Candidate Detail pass hai test MockMvc trên resolver của bản sao. Hai test dùng service giả lập để render template thật. Candidate Detail trên server dùng chung có tác dụng đánh dấu thông báo đã đọc. Không dùng route này cho kiểm tra live. HR Edit cũng chưa có xác nhận Draft an toàn để mở bằng Chrome.

Các artifact tái hiện nằm trong `target/`: `Task25CHeadProbe.java`, `task25c-browser.cjs`, `task25c-compare.cjs` và JSONL của các lượt đo. Harness dùng tài khoản fixture local và không ghi token hoặc credential vào kết quả.

## Kiểm chứng trên template đã sửa

Chrome CDP chạy lại trên ứng dụng local 8080 sau khi Maven chép resource mới. 18 cặp snapshot pass. Mỗi trang có 7 URL stylesheet riêng, mỗi URL một link, HTML response khớp thứ tự DOM. So sánh gồm computed style và kích thước của nút, bảng, badge, input, select, header, navigation, modal và toast ở nơi có mặt. Bề rộng tài liệu khớp baseline ở cả ba viewport, ví dụ HR List 1425/753/375 px và Candidate List 1440/768/375 px. Không có lỗi JS được ghi qua runtime exception hoặc console API. Chưa đo pixel diff.

| Route và vai trò | Entry CSS trước → sau | Encoded byte trước → sau | Transfer byte trước → sau |
|---|---:|---:|---:|
| HR List và Create | 10 → 10 | 154.459 → 154.459 | 157.459 → 157.459 |
| HR Detail và Director List | 10 → 9 | 146.598 → 144.335 | 149.598 → 147.035 |
| Director Detail | 10 → 10 | 146.598 → 146.598 | 149.598 → 149.598 |
| Candidate List | 10 → 10 | 134.337 → 134.337 | 137.337 → 137.337 |

Resource Timing được đo khi tắt cache, gồm cả CSS import. Mỗi CSS cấp trang ghi một entry ở cả hai phiên bản. Chênh lệch 2.263 byte encoded và 2.563 byte transferred trùng kích thước một entry import `fonts.css`. Hiện tượng này thay đổi giữa các lượt đo ở Task 25A và trong PoC 25C. Không quy chênh lệch đó cho việc bỏ link CSS trang, và không kết luận tốc độ tải được cải thiện.

Diff tracked gồm đúng sáu template, 16 dòng thêm và 28 dòng xóa, tương ứng bỏ hai dòng wrapper trên mỗi tệp và chỉnh indent các link. `git diff --check` pass. Index rỗng. Báo cáo 38 và `scratch/` có từ trước được giữ nguyên. Server PoC 18081 đã dừng sau kiểm thử.

Maven chạy đủ bảy suite Offer trên các template đã sửa. Exit 0, 223 test, 0 failure, 0 error, 0 skip. Hai lượt MockMvc trên bản sao là kiểm chứng bổ sung trước khi áp dụng và không được cộng vào kết quả 223.

| Hạng mục | Kết quả |
|---|---|
| OFFER-ONLY OPTIMIZATION | COMPLETED |
| OFFER STYLESHEET DUPLICATION | RESOLVED |
| MAVEN | PASS |
| BROWSER REGRESSION | PARTIAL. Sáu màn đại diện pass. HR Edit và Candidate Detail có kiểm chứng MockMvc, chưa có đo Chrome trực tiếp. |
| SHARED FILES MODIFIED | 0 |
| OTHER MODULES MODIFIED | 0 |
| GIT SAFETY | PASS |
