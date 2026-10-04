# Test homepage Mộc sau audit Gemini — 01/10/2026

## Phạm vi

Chỉ `prototype/index.html`: một công ty, filters/detail/hash/contact/responsive/accessibility. Không test Spring Boot, JWT, DB, SMTP hoặc nộp hồ sơ thật. Không thay đổi schema. Mộc/JD/benefits là fixture; email và website `.example` không vận hành thật.

## Chuẩn bị và mở trang

1. Mở `prototype/index.html` bằng Chrome từ Explorer để thử offline. Không cần chạy Spring Boot.
2. Để kiểm tra qua HTTP: mở PowerShell trong repo `SWP_RMS_Group-2`, chạy `py -m http.server 8766 --bind 127.0.0.1 --directory prototype`.
3. Mở `http://127.0.0.1:8766/index.html`; dùng DevTools Responsive Mode với width 1440/1024/768/375, zoom 100%. Các thao tác sau bắt đầu bằng nút/link trên trang.
4. Đóng server bằng Ctrl+C sau khi test. Không gửi CV, thư hoặc nhập thông tin cá nhân vào fixture này.

### Chạy script tự động để nối tiếp

Máy đã có Node 24 và Chrome tại `C:\Program Files\Google\Chrome\Application\chrome.exe`. Khi server 8766 ở trên đang chạy, mở PowerShell thứ hai trong repo và chạy:

```powershell
node --check scripts/check-career-homepage.mjs
node scripts/check-career-homepage.mjs
```

Script dùng CDP, không cần npm/package mới; lưu JSON/ảnh có prefix timestamp `manual-*`, trả exit 1 nếu check FAIL. Tạo profile Chrome tạm, chỉ xóa profile có path đã kiểm tra nằm trực tiếp trong TEMP và tên đúng prefix của task. Cần cổng 9237 trống. Script không gửi thư hoặc gọi backend. Phiên bản lưu đã sửa driver reload/hash; đợt này chỉ xác nhận riêng case đó sau hai round hình ảnh, không tuyên bố đã chạy lại toàn bộ script đã đóng gói.

## Kịch bản thao tác

| ID | Các bước | Kết quả mong đợi |
|---|---|---|
| HOME-01 | 1. Mở homepage. 2. Xem hero, jobs, footer. 3. So sánh tên công ty trong copy. | Chỉ Mộc; không Browse Companies/Post a Job/employer pricing; không disclaimer lặp hoặc metric/testimonial giả. |
| NAV-01 | 1. Bấm lần lượt nav từ trái sang phải. 2. Quan sát section đích. | Thứ tự positions → about → benefits → life → process; mọi link trỏ tới section hiện có. Không Hồ sơ của tôi. |
| FILTER-01 | 1. Bấm Hero “Xem vị trí đang tuyển”. 2. Quan sát filters và job grid. | Filters ở ngay trên results, không còn form trong Hero; count 6; Xóa bộ lọc ẩn. |
| FILTER-02 | 1. Nhập `nhan su`. 2. Chưa bấm Enter, xem kết quả. 3. Bấm Enter. | Chỉ HR Specialist; count 1; focus ở keyword; Enter không cuộn/teleport. |
| FILTER-03 | 1. Xóa lọc. 2. Nhập `THIẾT KẾ`. 3. Thử `da nang`. | Lần lượt Product Designer và Data Analyst, không phân biệt dấu/hoa thường. |
| FILTER-04 | 1. Xóa lọc. 2. Chọn Đà Nẵng. 3. Bấm Dữ liệu. | Một Data Analyst; Dữ liệu pressed, các pill khác false; counts theo keyword/location. |
| FILTER-05 | 1. Giữ tổ hợp trên. 2. Nhập `Java`. | Count 0, empty state có cách khôi phục; không lưới trắng im lặng. |
| FILTER-06 | 1. Bấm Xóa bộ lọc trong empty state. 2. Quan sát focus và cuộn. | 6 jobs trở lại, clear ẩn; focus keyword trong positions, không về Hero. |
| FILTER-07 | 1. Nhập một keyword có kết quả. 2. Bấm Xóa bộ lọc trên results. | Xóa cả keyword/location/department; giữ vị trí cuộn, không để focus trên nút đã ẩn. |
| ROLE-01 | 1. Bấm title Senior Java Developer. 2. Đóng. 3. Bấm vùng lương/trống trong cùng card. | Cả hai mở cùng dialog/hash `#role/senior-java-developer`; một title link native, không hai hành động khác nhau. |
| ROLE-02 | 1. Mở từng trong sáu card. 2. Đối chiếu facts với card. 3. Đọc các section. | Giữ title/department/city/type/salary/date; có Tổng quan/Trách nhiệm/Yêu cầu/Điểm cộng/Quyền lợi; không chỉ một dòng. |
| ROLE-03 | 1. Mở role. 2. Bấm Sao chép liên kết. 3. Dán vào tab mới. 4. Refresh. | Role đúng mở trực tiếp và title tab cập nhật. Clipboard thành công thật hoặc hướng dẫn copy thanh địa chỉ nếu không được cấp quyền. |
| ROLE-04 | 1. Từ card mở detail. 2. Browser Back. 3. Browser Forward. 4. Escape. | Back đóng, Forward mở lại; Escape đóng và trả focus đúng card, giữ ngữ cảnh danh sách. |
| ROLE-05 | 1. Dán link role vào tab mới. 2. Bấm nút đóng. | Vẫn ở homepage/positions; không Back ra khỏi site và không focus control ở ngoài viewport. |
| ROLE-06 | 1. Copy role URL. 2. Thay slug cuối bằng `not-a-role`. 3. Mở link. | Không dialog rỗng; thông báo không tìm thấy cùng danh sách để chọn lại. |
| ROLE-07 | 1. Mở role. 2. Bấm padding trong dialog. 3. Bấm backdrop ngoài dialog. | Padding không đóng; backdrop đóng. |
| ROLE-08 | 1. Mở detail ở 375px. 2. Cuộn hết JD. 3. Tìm nút đóng. | Facts/JD dễ đọc, không cuộn ngang; đóng được khi đọc nội dung dài. |
| CONTACT-01 | 1. Mở role. 2. Xem đích “Gửi CV qua email”. 3. Xem CTA cuối/footer. | `mailto:tuyendung@moc.example`, subject role tương ứng; không giả upload hoặc thông báo đã nhận CV. Không gửi thư thử. |
| MENU-01 | 1. Ở 375px bấm Menu. 2. Escape. 3. Mở lại và chọn Quyền lợi. 4. Mở lại rồi tap ngoài. | Native expanded state, Escape trả summary, chọn link đến section và đóng, tap ngoài đóng. CAREERS vẫn hiện. |
| KEY-01 | 1. Reload, Tab đầu tiên. 2. Enter skip link. 3. Tab qua filters/card. | Skip link hiện và focus main; thứ tự hợp lý, focus ring nhìn rõ; Enter title mở detail. |
| KEY-02 | 1. Mở detail. 2. Tab/Shift+Tab qua close/apply/copy. 3. Escape. | Không focus control nền; dialog có tên title, không describedby đọc toàn bộ JD; focus trở về nguồn phù hợp. |
| VIS-01 | 1. Kiểm tra 1440/1024/768/375. 2. Xem grid/process/menu/footer. | Job grid 3/2/2/1; process 4/2/2/1; không squeeze/horizontal overflow; inputs 16px, targets ≥44px. |
| VIS-02 | 1. Inspect body/placeholder/control/focus. 2. Bật Reduced Motion trong DevTools Rendering. | Text ≥4,5:1 (large ≥3); control borders ≥3; visible focus; reduced-motion không smooth scroll. Hairline trang trí không tính như control. |
| OFFLINE-01 | 1. Ngắt mạng. 2. Mở file HTML local. 3. Search, mở role/hash, refresh. | Fonts/JS/data nội tuyến vẫn dùng được; không gọi CDN/API. Contact external/clipboard tùy quyền môi trường, không giả thành công. |

## Bằng chứng và kết quả thực tế

- Cú pháp JavaScript: `node --check` PASS.
- Chrome headless CDP round1: 61 checks PASS, 2 FAIL — Escape bị browser hash-navigation trả focus về body; favicon 404. Có lưu JSON/ảnh thật; không che lỗi.
- Harness ban đầu chạy trước khi DOM tải xong: TypeError getComputedStyle(null), sửa cơ chế chờ DOM; bằng chứng tại `assets/2026-10-01-homepage-audit/harness-startup-failure.json`. Đây là lỗi driver, không dùng làm kết luận page FAIL/PASS.
- Round2: **75 checks PASS / 1 driver FAIL**. Native hash/history navigation đã được sửa trả focus sau default action; sticky close qua kiểm tra cuộn; metadata hai cột; CAREERS 11px; favicon inline. File local, actual CDP touch, keyboard, Back/Forward, sáu role metadata, AX names, targets và contrast PASS.
- Case FAIL còn lại là `Direct URL close focuses visible positions control`: `Page.navigate` chỉ đổi fragment trong cùng document, giữ returnFocus từ card trước; đó không phải tab mới/reload như test muốn mô phỏng. Đã sửa test driver buộc reload. Kiểm chứng hẹp bằng **hai tab mới độc lập 1440/375px PASS**: initial role đúng, `openedByLink=false`, đóng về `#roles`, keyword nhận focus và nằm trong viewport. Không sửa thêm UI hoặc chụp round thứ ba.
- **Kết luận flow đã kiểm chứng: PASS; chưa có lỗi page còn mở từ các checks này.** Giữ raw FAIL để phân biệt lỗi driver, không chỉnh JSON thành xanh.
- Browser không có runtime/network error trong round2. Không cuộn ngang tại 1440/1024/768/375 và kiểm tra thêm 600/581/320. Job columns 3/2/2/1, process 4/2/2/1. Card ~205px desktop/tablet, ~189px mobile. Font faces nội tuyến được tải; mở file local/search/hash PASS.
- Text DOM đo 122 phần tử, minimum 5,94:1; placeholder 6,25:1; control border trên white 3,60:1. Không thay bằng chứng này cho NVDA/VoiceOver hoặc toàn bộ WCAG audit.
- Impeccable hai agent độc lập, một CLI scan: 13 warning trước batch cuối; CAREERS được sửa, 12 padding false positives có computed evidence. Không rerun scan, không ghi “detector sạch”. Native CUA fail sandbox; Chrome fallback chạy mutation preflight, live overlay **không inject** vì helper không parse cổng start output dù server trả JSON hợp lệ. Live server đã stop; không có overlay người dùng nhìn thấy.
- [Round1 results](assets/2026-10-01-homepage-audit/round1-results.json), [desktop](assets/2026-10-01-homepage-audit/round1-1440.png), [mobile](assets/2026-10-01-homepage-audit/round1-375.png), [detail mobile](assets/2026-10-01-homepage-audit/round1-detail-375.png).
- [Round2 results](assets/2026-10-01-homepage-audit/round2-results.json), [fresh URL focus](assets/2026-10-01-homepage-audit/fresh-url-focus-results.json), [final desktop](assets/2026-10-01-homepage-audit/round2-1440.png), [final mobile top](assets/2026-10-01-homepage-audit/round2-375-top.png), [final mobile filters](assets/2026-10-01-homepage-audit/round2-375-positions.png), [final mobile detail](assets/2026-10-01-homepage-audit/round2-detail-375.png).
- [Assessment B và cleanup](assets/2026-10-01-homepage-audit/assessment-b.json), [CLI raw](assets/2026-10-01-homepage-audit/assessment-b-cli.json), [browser raw](assets/2026-10-01-homepage-audit/assessment-b-runtime.json). Token localhost đã được redacted trong durable evidence.

## Chưa xác minh / giới hạn

Chưa dùng NVDA/VoiceOver, Safari/Firefox, thiết bị iOS/Android thật, browser zoom 200%, OS mail client, SMTP/mailbox hoặc website doanh nghiệp thật. Không thể coi AX tree và Chrome emulation là screen-reader/device E2E. Không test backend vì trang không kết nối backend. JS-disabled có contact fallback nhưng không mở detail. `.example` cố ý không phải contact live.

## Tiếp tục

Thay fixture/contact bằng thông tin doanh nghiệp duyệt trước khi công bố; tích hợp endpoint/application journey là nhiệm vụ riêng. Lưu thêm kết quả test thủ công vào tài liệu này, không nâng trạng thái chỉ từ build/scan.
