# Đồng Bộ & Nâng Cấp Toàn Diện Giao Diện Cổng Đề Xuất Nhận Việc Của Ứng Viên (Candidate Offer Portal) Theo Chuẩn UI_RULES & ARCHITECTURE_GUIDE

- **Ngày:** 31/10/2026.
- **Người thực hiện:** HuyenPT.
- **Yêu cầu đã làm rõ:** Rà soát và nâng cấp toàn diện luồng Offer của Ứng viên (từ Thư mời nhận việc, Danh sách offer, Chi tiết offer, tới Màn hình từ chối) theo `docs/architecture/UI/UI_RULES.md` và `docs/architecture/ARCHITECTURE_GUIDE.md`. Chuẩn hóa hệ thống thông báo Flash Toast, tuân thủ Buttons DNA, loại bỏ mã hex hardcoded, nâng cấp thị giác trang trọng cho Thư mời nhận việc (Letterhead Header, Executive Greeting, Khối Lương & Đãi ngộ cao cấp, Breadcrumb định tuyến liền mạch), bổ sung đường link trực tiếp truy cập offer trong email gửi ứng viên.
- **Phạm vi:** 
  - Templates Thymeleaf: `candidate-list.html`, `candidate-detail.html`, `candidate-decline.html`.
  - Static CSS/JS: `candidate-offers.css`, `candidate-offers.js`.
  - Backend Service: `OfferMailSender.java`.
  - Kiểm thử: `OfferControllerBugTests`, `OfferIntegrationTests`, `OfferServiceTests` (148/148 passed, BUILD SUCCESS).

---

## 1. Danh Sách File Thay Đổi

| Loại | File | Nội dung thay đổi |
| --- | --- | --- |
| **Sửa** | `src/main/resources/templates/offers/candidate-detail.html` | Nhúng Flash Toast; nâng cấp Breadcrumb định tuyến cao cấp; thiết kế lại Top Action Bar với các meta badge (hạn chót, đếm ngược thời gian còn lại); chuẩn hóa Buttons DNA cao 48px, bo 8px; nâng cấp Header thư mời kèm nhận diện Mộc RMS và mã văn bản `offerCode`; đóng khung Lời mở đầu trang trọng; nâng cấp 2 thẻ Lương chính thức và Lương thử việc; loại bỏ popup tự chế `#cof-accept-success-modal`. |
| **Sửa** | `src/main/resources/templates/offers/candidate-list.html` | Bổ sung Breadcrumb định tuyến; nhúng Flash Toast fragment; loại bỏ inline alerts `.alert-success` / `.alert-danger` phân mảnh; chuẩn hóa các nút xem chi tiết và phản hồi theo Buttons DNA. |
| **Sửa** | `src/main/resources/templates/offers/candidate-decline.html` | Bổ sung Breadcrumb định tuyến; nhúng Flash Toast fragment; thay thế HTML entity thô `&larr;` bằng inline SVG arrow; chuẩn hóa các nút xem lại và xác nhận từ chối. |
| **Mở rộng** | `src/main/resources/static/css/pages/candidate-offers.css` | Bổ sung CSS cho Breadcrumb pill sang trọng (`.cof-breadcrumb__inner`, `.cof-breadcrumb__link`, `.cof-breadcrumb__current`); nâng cấp `.cof-top-bar` và các meta badges (`.cof-meta-badge`); chuẩn hóa `.cof-action-btn` (cao 48px, bo 8px); nâng cấp header thư mời (`.cof-letterhead-brand`, `.cof-paper-meta-row`, `.cof-paper-rule`); đóng khung `.cof-paper-greeting-box`; làm nổi bật khối lương `.cof-salary-card--official` và `.cof-salary-card--probation`; dọn sạch CSS modal cũ. |
| **Tối ưu** | `src/main/resources/static/js/pages/candidate-offers.js` | Dọn dẹp hoàn toàn đoạn script lắng nghe và hiển thị modal cũ `#cof-accept-success-modal`, đồng bộ với luồng Flash Toast toàn hệ thống. |
| **Mở rộng** | `src/main/java/com/group2/rms/offer/service/OfferMailSender.java` | Inject configurable `@Value("${app.base-url:${APP_BASE_URL:http://localhost:8080}}") baseUrl`; chèn trực tiếp đường dẫn xem chi tiết Thư mời nhận việc và phản hồi (`baseUrl + "/portal/offers/" + offer.getOfferId()`) vào nội dung email. |
| **Cập nhật** | `docs/management/WORK_LOG.md` | Ghi nhận thay đổi vào lịch sử dự án. |
| **Cập nhật** | `docs/members/huyenpt/summary-update-offer-candidate-ui.md` | Báo cáo chi tiết công việc của thành viên HuyenPT cho nhánh `feat/huyenpt/update-offer-candidate-ui`. |

---

## 2. Các Phase Thực Hiện Và Kết Quả Kiểm Thử

### Phase 1: Chuẩn Hóa Flash Toast, Buttons DNA & Link Email (Commits `936e7d4`, `0c3adab`, `205c413`, `02eede8`)
- Thay thế popup blocking `#cof-accept-success-modal` bằng Flash Toast căn giữa màn hình trên Topbar (`flash-toast.css` / `flash-toast.js`).
- Thay mã hex hardcoded `#2d6a4f` bằng biến token `var(--brand)`.
- Thay ký tự HTML thô `&larr;` bằng SVG icon mũi tên.
- Dọn dẹp CSS và JS mồ côi của modal cũ.
- Inject `baseUrl` và tự động gắn link trực tiếp `/portal/offers/{id}` vào nội dung email gửi ứng viên.
- **Kiểm thử:** 148/148 test cases passed (BUILD SUCCESS).

### Phase 2: Nâng Cấp Bố Cục Khung & Thanh Điều Hướng (Commit `6bbdd5f`)
- **Breadcrumb:** Thiết kế lại `<nav class="cof-breadcrumb">` thành thanh định tuyến dạng pill sang trọng: icon hồ sơ SVG, đường dẫn `Đơn ứng tuyển / Thư mời nhận việc / #{id} · {Vị trí}`.
- **Top Action Bar:** Nâng cấp thành thẻ Surface cao cấp:
  - Thẻ Hạn chót phản hồi kèm icon lịch.
  - Thẻ Thời gian còn lại dạng đếm ngược realtime chuyển tone màu ấm `var(--warning-soft)` & `var(--warning)`.
  - Nút *"Đồng ý nhận việc"* chuẩn `.btn.btn--primary` (cao 48px, bo 8px, font 15.5px, icon tick, bóng đổ xanh thương hiệu).
  - Nút *"Từ chối thư mời"* chuẩn `.btn.btn--outline` (cao 48px, bo 8px, viền đỏ tinh tế).
- **Kiểm thử:** 148/148 test cases passed (BUILD SUCCESS).

### Phase 3: Nâng Cấp Thị Giác Tờ Thư Mời (Official Letterhead) (Commit `5ffdd63`)
- Bổ sung định danh thương hiệu trang trọng: `MỘC RMS | HỆ THỐNG QUẢN TRỊ TUYỂN DỤNG` kèm icon lá xanh thương hiệu ở đầu thư.
- Bổ sung metadata văn bản chính thức: Mã văn bản (`offerCode`) và Ngày phát hành (`sentAt`).
- Tiêu đề thư mời sắc nét với thanh gạch đôi phân cách tinh tế `THƯ MỜI NHẬN VIỆC CHÍNH THỨC`.
- Lời mở đầu thư ngỏ được đóng khung đón mừng trang trọng (`.cof-paper-greeting-box`) với viền nhấn xanh `var(--brand)` và làm nổi bật tên ứng viên.
- Chữ ký đại diện trang trọng: *Ban Giám đốc & Đội ngũ Tuyển dụng Mộc RMS*.
- **Kiểm thử:** 148/148 test cases passed (BUILD SUCCESS).

### Phase 4: Nâng Cấp 3 Khối Nội Dung Cốt Lõi (Job Info, Salary, Benefits) (Commit `9fb9922`)
- **Thẻ Lương chính thức:** Đóng khung viền xanh `var(--brand)`, số tiền to 24px đậm nét `var(--brand-dark)`, nền gradient nhẹ sang trọng.
- **Thẻ Lương thử việc:** Nền `var(--surface-sunken)`, badge tỷ lệ *85% lương chính thức* thanh lịch.
- **Cột Thông tin công việc & Quyền lợi:** Bo tròn 14px, icon bo góc trên nền `var(--brand-soft)`, các item quyền lợi có hiệu ứng hover mượt mà.
- **Kiểm thử:** 148/148 test cases passed (BUILD SUCCESS).

### Phase 5: Đồng Bộ Màn Hình Danh Sách & Từ Chối (Commit `6c937b8`)
- Thêm Breadcrumb định tuyến liền mạch cho cả `candidate-list.html` và `candidate-decline.html`.
- Đồng bộ toàn bộ trải nghiệm người dùng từ Danh sách -> Chi tiết -> Từ chối.
- **Kiểm thử:** 148/148 test cases passed (0 failures, 0 errors, BUILD SUCCESS).

### Phase 6: Thiết Kế Lại Giao Diện Đơn Ứng Tuyển Theo Mockup Mẫu (/portal/applications)
- **Tabs lọc hồ sơ:** Thiết kế lại thành dạng pill bo tròn mềm mại (`border-radius: var(--radius-full)`), loại bỏ hoàn toàn viền gạch chân thô và thanh cuộn xanh lá (scrollbar). Tab đang chọn hiển thị nền mint `var(--brand-soft)` với chữ xanh đậm `var(--brand-dark)` và badge số đếm tinh tế.
- **Thẻ đơn ứng tuyển:** Chuyển sang phong cách thẻ trắng tinh khôi (`var(--surface)`), viền mảnh 1px (`var(--line)`), bo góc 14px mềm mại, loại bỏ hoàn toàn viền trái dày và viền vàng thô ráp.
- **Menu tùy chọn thẻ:** Tích hợp menu ba chấm dọc `⋮` (`.myapp-card__more`) ở góc trên phải thẻ với dropdown đóng mở nhẹ nhàng, chứa liên kết *"Xem lại CV"* (`data-cv-open`).
- **Khối trạng thái có thư mời:**
  - Chuyển thành pill badge mint `[✉] Có thư mời` (`.myapp-status-pill.myapp-status-pill--offer`).
  - Dòng hạn chót hiển thị ngắn gọn, rõ ràng: `Hạn phản hồi: dd/MM/yyyy`.
  - Huy hiệu đếm ngược gấp gọn gàng dạng pill `[🕒] Còn N ngày/giờ`.
- **Cặp nút hành động:** Đồng bộ 2 nút ngang hàng nhau (cao 40px, bo 8px): nút chính `[✉] Trả lời thư mời` (`.btn--primary`) và nút phụ `Xem chi tiết` (`.btn--secondary`).
- **Banner đơn gấp:** Chỉ hiển thị khi có từ 2 việc gấp trở lên để không gây trùng lặp, rối mắt khi ứng viên chỉ có 1 hồ sơ.
- **Kiểm thử:** 72/72 test cases passed (20/20 MyApplicationWebTests, 52/52 MyApplicationJourneyTests, BUILD SUCCESS).

### Phase 7: Thiết Kế Lại Màn Hình Xem Thư Mời Làm Việc Theo Mockup (/portal/offers/{id})
- **Thanh điều hướng:** Tích hợp nút quay lại danh sách (`← Quay lại danh sách đơn ứng tuyển`) kèm thanh breadcrumb gọn gàng (`› Đơn ứng tuyển / Thư mời nhận việc / #{id}`).
- **Thẻ 1 (Overview & Stepper & Thao tác):**
  - Cột trái: Tên vị trí tuyển dụng đậm nét, bộ ba metadata kèm icon (Phòng ban, Ngày nộp hồ sơ, Mã #CAN-xxxx), thanh tiến độ Stepper 4 bước chuẩn với ngày tháng hiển thị dưới mỗi mốc.
  - Cột phải: Pill badge trạng thái `Có thư mời`, Hạn phản hồi kèm đếm ngược thời gian thực, cặp nút hành động ngang hàng: `[✉] Đồng ý nhận việc` (`.btn--primary`) và `[✕] Từ chối thư mời` (`.btn--outline`).
- **Thẻ 2 (Tờ Thư Mời Nhận Việc Chính Thức):**
  - Header trang trọng với icon thư xanh, tiêu đề `Thư mời nhận việc`, `Ngày gửi | Mã thư mời: OFFR-2026-xxxx` và nút `[📄] Tải về PDF`.
  - Nội dung thư chúc mừng, đón chào chuẩn doanh nghiệp và chữ ký đại diện Đội ngũ Tuyển dụng Mộc RMS căn phải tinh tế.
- **Thẻ 3 (Accordion Chi Tiết Đề Xuất):**
  - Chuyển 3 khối thông tin (Thông tin công việc, Lương & thưởng, Quyền lợi) vào thẻ Accordion đóng/mở tiện lợi (`<details class="card cof-accordion">`), mở mặc định với icon cặp tài liệu và mũi tên chevron xoay mượt mà.
- **Kiểm thử:** 67/67 test cases passed (16/16 CandidateOfferWebTests, 31/31 OfferIntegrationTests, 20/20 MyApplicationWebTests, BUILD SUCCESS).
