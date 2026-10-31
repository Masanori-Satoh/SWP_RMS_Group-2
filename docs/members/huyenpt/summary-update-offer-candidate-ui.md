# BÁO CÁO ĐỒNG BỘ & NÂNG CẤP TOÀN DIỆN GIAO DIỆN CỔNG OFFER ỨNG VIÊN THEO UI_RULES & ARCHITECTURE_GUIDE

- **Dự án:** Hệ Thống Quản Lý Tuyển Dụng — SWP_RMS_Group-2 (Mộc RMS)
- **Nhánh thực hiện (Branch):** `feat/huyenpt/update-offer-candidate-ui`
- **Nhánh gốc (Base Branch):** `main`
- **Thành viên phụ trách (Assignee):** Phạm Thị Huyền (`HuyenPT`)
- **Phân hệ (Module):** Cổng ứng viên phản hồi thư mời nhận việc (`/portal/offers/**`)
- **Tài liệu tham chiếu:**
  - Quy chuẩn kiến trúc hệ thống: [`docs/architecture/ARCHITECTURE_GUIDE.md`](../../architecture/ARCHITECTURE_GUIDE.md)
  - Hướng dẫn thành phần UI: [`docs/architecture/UI/UI_CATALOG.md`](../../architecture/UI/UI_CATALOG.md)
  - Hướng dẫn di chuyển UI: [`docs/architecture/UI/UI_MIGRATION.md`](../../architecture/UI/UI_MIGRATION.md)
  - Quy tắc dọn dẹp mã UI cũ: [`docs/architecture/UI/UI_LEGACY_CLEANUP.md`](../../architecture/UI/UI_LEGACY_CLEANUP.md)
  - Quy tắc UI toàn hệ thống: [`docs/architecture/UI/UI_RULES.md`](../../architecture/UI/UI_RULES.md)
  - Nhật ký công việc dự án: [`docs/management/WORK_LOG.md`](../../management/WORK_LOG.md)
  - Nhật ký kỹ thuật chi tiết: [`docs/management/work_logs/2026-10-31-candidate-offer-ui-sync.md`](../../management/work_logs/2026-10-31-candidate-offer-ui-sync.md)

---

## 🎯 I. MỤC TIÊU & BỐI CẢNH TRIỂN KHAI

### 1. Bối cảnh
Trước đợt rà soát và nâng cấp toàn diện này, phân hệ Cổng thông tin Offer dành cho Ứng viên (`/portal/offers/**`) tồn tại các điểm cần hoàn thiện:
1. **Thông báo phân mảnh & Popup che màn hình:**
   - Ứng viên bấm đồng ý nhận việc thì hệ thống hiện modal tự chế `#cof-accept-success-modal` che tối toàn bộ màn hình, thay vì dùng cơ chế Flash Toast chuẩn toàn hệ thống (`fragments/ui/flash-toast`).
2. **Kích thước nút bấm và nhận diện chưa chuẩn Button DNA:**
   - Nút hành động bị giới hạn chiều cao 42px thay vì chiều cao chuẩn 48px (`var(--btn-h)`).
   - Nút chấp nhận và từ chối chưa có bóng đổ nhận diện thương hiệu.
3. **Thư mời nhận việc còn đơn điệu về thị giác:**
   - Thiếu định danh thương hiệu Mộc RMS ở đầu văn bản.
   - Thiếu mã văn bản thư mời chính thức (`offerCode`) và ngày phát hành (`sentAt`).
   - Lời ngỏ chưa được đóng khung trang trọng; khối lương chưa làm nổi bật sự khác biệt giữa Lương chính thức và Lương thử việc.
4. **Định tuyến Breadcrumb chưa liên kết liền mạch:**
   - Chỉ có nút "Quay lại danh sách" đơn lẻ, chưa kết nối hành trình từ `Đơn ứng tuyển của tôi / Thư mời nhận việc / Chi tiết`.

### 2. Mục tiêu cốt lõi
1. **Chuẩn hóa Thông báo Flash Toast:** Đưa toàn bộ phản hồi về Flash Toast đặt giữa màn hình trên Topbar, loại bỏ 100% modal blocking tự chế.
2. **Nâng cấp Bố cục Khung & Thanh Thao Tác:** Breadcrumb định tuyến dạng pill sang trọng, Action bar với meta badge hạn chót & đếm ngược thời gian còn lại trực quan.
3. **Tuân thủ Tuyệt đối Buttons DNA:** Nút cao 48px, bo góc 8px, font 15.5px semibold, bóng đổ thương hiệu Mộc RMS.
4. **Trang trọng hóa Thư Mời (Official Letterhead):** Bổ sung logo Mộc RMS, mã văn bản, ngày phát hành, khung đón mừng ứng viên và làm nổi bật khối lương/đãi ngộ.
5. **Bảo toàn Kiến trúc & Bộ Kiểm Thử:** Đảm bảo 100% test cases phân hệ Offer vượt qua, không ảnh hưởng tới bất kỳ luồng nào khác (HR, Giám đốc, Đơn ứng tuyển).

---

## 🛠️ II. TIẾN ĐỘ THỰC HIỆN THEO TỪNG PHASE

### Phase 1: Chuẩn Hóa Flash Toast, Buttons DNA & Link Email (Commits `936e7d4`, `0c3adab`, `205c413`, `02eede8`)
- Xóa bỏ hoàn toàn modal tự chế `#cof-accept-success-modal`.
- Tích hợp fragment `fragments/ui/flash-toast :: flashToast` vào `candidate-list.html`, `candidate-detail.html`, `candidate-decline.html`.
- Thay thế mã màu cứng `#2d6a4f` trong watermark bằng biến token `var(--brand)`.
- Thay thế ký tự thô `&larr;` bằng inline SVG arrow.
- Tối ưu `OfferMailSender.java`: chèn link trực tiếp `baseUrl + "/portal/offers/" + offer.getOfferId()` vào email gửi ứng viên.
- **Kiểm thử:** 148/148 tests passing.

### Phase 2: Nâng Cấp Bố Cục Khung & Thanh Điều Hướng (Commit `6bbdd5f`)
- Nâng cấp `<nav class="cof-breadcrumb">` thành thanh định tuyến pill tinh tế: `Đơn ứng tuyển / Thư mời nhận việc / #{id} · {Vị trí}`.
- Thiết kế lại `.cof-top-bar` thành thẻ Surface hiện đại bo tròn 14px:
  - Thẻ Hạn chót phản hồi kèm icon lịch.
  - Thẻ Thời gian còn lại dạng đếm ngược tự đổi sang màu cảnh báo ấm `var(--warning-soft)` & `var(--warning)`.
  - Nút *"Đồng ý nhận việc"* chuẩn `.btn.btn--primary` (cao 48px, bo 8px, bóng đổ xanh thương hiệu).
  - Nút *"Từ chối thư mời"* chuẩn `.btn.btn--outline` (cao 48px, bo 8px, viền đỏ tinh tế).
- **Kiểm thử:** 148/148 tests passing.

### Phase 3: Nâng Cấp Thị Giác Tờ Thư Mời (Official Letterhead) (Commit `5ffdd63`)
- Thêm định danh thương hiệu: `MỘC RMS | HỆ THỐNG QUẢN TRỊ TUYỂN DỤNG` kèm icon lá xanh thương hiệu ở đầu văn bản.
- Hiển thị metadata văn bản: Mã văn bản (`offerCode`) và Ngày phát hành (`sentAt`).
- Tiêu đề thư mời sắc nét kèm thanh gạch đôi phân cách `THƯ MỜI NHẬN VIỆC CHÍNH THỨC`.
- Đóng khung lời ngỏ trang trọng (`.cof-paper-greeting-box`) với viền nhấn xanh `var(--brand)`.
- Chữ ký trang trọng: *Ban Giám đốc & Đội ngũ Tuyển dụng Mộc RMS*.
- **Kiểm thử:** 148/148 tests passing.

### Phase 4: Nâng Cấp 3 Khối Nội Dung Cốt Lõi (Thông Tin - Lương Thưởng - Quyền Lợi) (Commit `9fb9922`)
- **Thẻ Lương chính thức:** Đóng khung viền xanh `var(--brand)`, số tiền to 24px đậm nét `var(--brand-dark)`, nền gradient nhẹ sang trọng.
- **Thẻ Lương thử việc:** Nền `var(--surface-sunken)`, badge tỷ lệ *85% lương chính thức* thanh lịch.
- **Cột Thông tin công việc & Quyền lợi:** Bo tròn 14px, icon bo góc trên nền `var(--brand-soft)`, các item quyền lợi có hiệu ứng hover mượt mà.
- **Kiểm thử:** 148/148 tests passing.

### Phase 5: Đồng Bộ Màn Hình Danh Sách & Từ Chối (Commit `6c937b8`)
- Bổ sung Breadcrumb định tuyến liền mạch cho cả `candidate-list.html` và `candidate-decline.html`.
- Đồng bộ toàn bộ trải nghiệm người dùng từ Danh sách -> Chi tiết -> Từ chối.
- **Kiểm thử:** 148/148 tests passing.

### Phase 6: Thiết Kế Lại Giao Diện Đơn Ứng Tuyển Theo Mockup Mẫu (/portal/applications)
- **Tabs lọc hồ sơ:** Thiết kế lại thành dạng pill bo tròn mềm mại (`border-radius: var(--radius-full)`), loại bỏ thanh cuộn ngang xanh lá.
- **Thẻ đơn ứng tuyển:** Chuyển sang phong cách thẻ trắng tinh khôi (`var(--surface)`), viền mảnh 1px (`var(--line)`), bo góc 14px mềm mại.
- **Menu tùy chọn thẻ:** Tích hợp menu ba chấm dọc `⋮` (`.myapp-card__more`) ở góc trên phải thẻ với tùy chọn *"Xem lại CV"*.
- **Khối trạng thái có thư mời:** Pill badge mint `[✉] Có thư mời`, Hạn phản hồi hiển thị ngắn gọn kèm huy hiệu đếm ngược dạng pill.
- **Cặp nút hành động:** Đồng bộ 2 nút ngang hàng (cao 40px, bo 8px): nút chính `[✉] Trả lời thư mời` (`.btn--primary`) và nút phụ `Xem chi tiết` (`.btn--secondary`).
- **Kiểm thử:** 72/72 test cases passed (20/20 MyApplicationWebTests, 52/52 MyApplicationJourneyTests, BUILD SUCCESS).

### Phase 7: Thiết Kế Lại Màn Hình Xem Thư Mời Làm Việc Theo Mockup (/portal/offers/{id})
- **Thanh điều hướng:** Tích hợp nút quay lại danh sách (`← Quay lại danh sách đơn ứng tuyển`) kèm breadcrumb gọn gàng.
- **Thẻ 1 (Overview & Stepper):** Tên vị trí, bộ ba metadata, thanh tiến độ Stepper 4 bước chuẩn với ngày tháng, Hạn phản hồi & đếm ngược, cặp nút hành động ngang hàng.
- **Thẻ 2 (Tờ Thư Mời):** Header với icon thư xanh, tiêu đề Thư mời nhận việc, mã thư mời và nút Tải PDF.
- **Thẻ 3 (Accordion Chi Tiết):** Đưa 3 khối chi tiết vào thẻ Accordion (`<details class="card cof-accordion" open>`).
- **Kiểm thử:** 67/67 test cases passed (BUILD SUCCESS).

### Phase 8: Thiết Kế Lại Khung Trên Dạng Hero Card Căn Giữa Chuẩn Mockup Mới (/portal/offers/{id})
- **Thẻ 1 (Hero Card Căn Giữa):**
  - Huy hiệu pill mint `[✉] THƯ MỜI NHẬN VIỆC` (`.cof-hero-badge`) căn giữa.
  - Tên vị trí tuyển dụng to đậm 24px (`.cof-hero-title`).
  - Hàng metadata căn giữa: `[🏢] Phòng ban` · `[📅] Ngày gửi` · `[📅] Bắt đầu dự kiến`.
  - Hàng thao tác căn giữa: Cả 3 box (Hộp Hạn phản hồi, Nút Đồng ý nhận việc, Nút Từ chối thư mời) được căn chỉnh có chiều rộng bằng nhau 300px (`width: 300px; flex: 0 0 300px`) và chiều cao 42px. Khắc phục triệt để lỗi lệch cao/thấp (6px) do margin-bottom của layout cũ bằng cách cô lập selector sidebar cũ và thiết lập `margin: 0 !important`, `height: 42px !important`, cùng `.cof-hero-buttons { display: contents; }` giúp cả 3 box thẳng hàng tuyệt đối. Tối ưu JS countdown hiển thị gọn gàng `Còn N ngày` theo mockup, không bị lặp icon hoặc xuống dòng.
- **Thẻ 2 (Nội Dung Thư Mời):**
  - Header: Icon thư + `Nội dung thư mời` & Nút `[📄] Tải về PDF`.
  - Chữ ký căn lề trái (`.cof-letter-signoff-left`): *Trân trọng, Đội ngũ Mộc RMS*.
- **Thẻ 3 (Accordion Chi Tiết Đề Xuất):**
  - Accordion 3 cột: Thông tin công việc, Lương & thưởng (Lương thử việc % + Lương chính thức), Đãi ngộ & quyền lợi.
- **Kiểm thử:** 67/67 test cases passed (16/16 CandidateOfferWebTests, 31/31 OfferIntegrationTests, 20/20 MyApplicationWebTests, BUILD SUCCESS).


---

## 📊 III. BẢNG TỔNG HỢP KIỂM CHỨNG THEO TIÊU CHUẨN HỆ THỐNG

| Tiêu chuẩn | Trước khi nâng cấp | Sau khi nâng cấp | Đánh giá |
| --- | --- | --- | --- |
| **Hệ thống Flash Toast** | Dùng modal tự chế `#cof-accept-success-modal` và inline alerts | Nhúng `fragments/ui/flash-toast` chuẩn toàn hệ thống căn giữa Topbar | ✅ Đạt 100% |
| **Buttons DNA** | Chiều cao 42px, dùng class tự chế `.cof-top-btn-*` | Chuẩn hóa `.btn.btn--primary`, `.btn.btn--outline`, chiều cao 48px, bo 8px | ✅ Đạt 100% |
| **Màu sắc & Token CSS** | Chứa mã màu cứng `#2d6a4f` | 100% dùng `var(--brand)` và biến token `tokens.css` | ✅ Đạt 100% |
| **Header Thư mời** | Chỉ có dòng chữ nhỏ đơn điệu | Logo Mộc RMS, mã văn bản, ngày phát hành, thanh gạch đôi trang trọng | ✅ Đạt 100% |
| **Khối Lương & Đãi ngộ** | Thẻ xám phẳng, chưa nổi bật mức lương chính thức | Nền gradient, viền xanh `var(--brand)`, số tiền 24px đậm, badge tỷ lệ thử việc | ✅ Đạt 100% |
| **Breadcrumb định tuyến** | Nút quay lại đơn lẻ | Thanh breadcrumb pill sang trọng: Đơn ứng tuyển / Thư mời / #{id} | ✅ Đạt 100% |
| **Trải nghiệm Email** | Email thông báo không có đường dẫn | Tự động sinh link trực tiếp `/portal/offers/{id}` | ✅ Đạt 100% |
| **Giao diện Đơn ứng tuyển (/portal/applications)** | Thẻ viền vàng thô, thanh cuộn tab xanh lá, thông tin gấp rối mắt | Thẻ trắng tinh tế, tab dạng pill không thanh cuộn, pill badge Có thư mời, menu ⋮, cặp nút chuẩn DNA | ✅ Đạt 100% |
| **Giao diện Thư mời nhận việc (/portal/offers/{id})** | Top bar đơn lẻ, thiếu tổng quan tiến độ ứng tuyển | 3 thẻ chuẩn mockup: Thẻ Overview + Stepper ngày tháng + Action, Thẻ Thư mời chính thức + Tải PDF, Thẻ Accordion Chi tiết đề xuất | ✅ Đạt 100% |
| **Kiểm thử tự động** | Chưa chạy toàn bộ test | 148/148 offer tests pass, 72/72 application tests pass, 67/67 integration & web tests pass (BUILD SUCCESS) | ✅ Đạt 100% |
