# TỔNG KẾT NỘI DUNG HOÀN THIỆN PHÂN HỆ QUẢN LÝ ĐỀ XUẤT OFFER (HR OFFER ENHANCEMENTS)

- **Dự án:** Hệ Thống Quản Lý Tuyển Dụng — SWP_RMS_Group-2 (Mộc RMS)
- **Thành viên phụ trách:** Phạm Thị Huyền (`HuyenPT`)
- **Nhánh làm việc (Branch):** `feat/huyenpt/inte2/hr-offer-ui-enhancements`
- **Phân hệ (Module):** Quản lý Đề xuất Lương & Tuyển dụng (`Offer Management`) — Screen 30 (List), Screen 32 (Detail), Create, Edit, Export Excel.
- **Tài liệu tham chiếu:**
  - Quy chuẩn kiến trúc: [`docs/architecture/ARCHITECTURE_GUIDE.md`](../../architecture/ARCHITECTURE_GUIDE.md)
  - Hướng dẫn kỹ thuật Xuất Excel: *Implementation Brief — Xuất Excel Offer Management*
  - Báo cáo kiểm thử luồng Offer: [`docs/members/huyenpt/offer-inter1-flow-testing.md`](offer-inter1-flow-testing.md)

---

## I. MỤC TIÊU VÀ BỐI CẢNH TRIỂN KHAI

Nhánh `feat/huyenpt/inte2/hr-offer-ui-enhancements` được thực hiện nhằm hoàn thiện toàn diện trải nghiệm người dùng (UX) và bổ sung các tính năng cốt lõi cho phân hệ **Quản lý Đề xuất Offer (HR Offer Management)**:

1. **Bổ sung Tính năng Xuất Excel (Offer Excel Export) — Screen 30:**
   - Cho phép chuyên viên Nhân sự (HR) xuất danh sách dữ liệu đề xuất lương ra file Microsoft Excel (`.xlsx`) phục vụ báo cáo định kỳ, lưu trữ hồ sơ và trình Ban Giám đốc.
   - Hỗ trợ linh hoạt **3 phạm vi xuất** và **24 cột dữ liệu tùy biến**.
2. **Khôi phục và Chuẩn hóa Toàn bộ Thao tác Pop-up Modal tại Màn hình Danh sách:**
   - Theo đúng trải nghiệm tương tác nguyên bản của hệ thống và phản hồi nghiệp vụ, toàn bộ các thao tác: **Tạo mới Offer (Create)**, **Xem chi tiết (View Detail)**, **Chỉnh sửa (Edit)**, **Xóa (Delete)** và **Phát hành (Send)** đều được tích hợp thành **Pop-up Modal trực tiếp trên cùng một màn hình danh sách (Screen 30)**.
   - Loại bỏ việc chuyển hướng sang trang mới, giúp HR xử lý công việc liền mạch, nhanh chóng và trực quan.
3. **Đồng bộ Giao diện theo Mộc RMS Design System:**
   - Tối ưu hóa giao diện CSS/JS theo chuẩn `design-tokens.css`, `workspace.css`, `interface.css`.
   - Khắc phục triệt để các lỗi che khuất giao diện, xung đột lớp phủ overlay/backdrop, căn chỉnh hệ thống thẻ thống kê (Stats Cards) và thanh tác vụ chọn nhiều dòng (Selection Toolbar).
4. **Đảm bảo Chất lượng & Kiểm thử Toàn diện (100% Tests Pass):**
   - Viết mới bộ Unit Test cho dịch vụ xuất Excel (`OfferExportServiceTests`).
   - Cập nhật và bổ sung MockMvc Integration Test (`OfferIntegrationTest`) bao phủ toàn bộ các endpoint JSON/RESTful.
   - Đạt tỷ lệ vượt qua **79/79 test cases (100% PASS)** trong toàn bộ test suite của phân hệ Offer.

---

## II. CHI TIẾT CÁC TÍNH NĂNG ĐÃ TRIỂN KHAI

### 1. Tính năng Xuất Dữ liệu Excel (`Offer Export to Excel`) — Screen 30

#### 1.1. Giao diện Pop-up Modal Xuất Excel (`#offerExportModal`)
- **Thiết kế:** Modal thẻ lớn với icon Excel màu xanh lá nhận diện, tiêu đề rõ ràng, bố cục phân khu mạch lạc.
- **Lựa chọn 3 Phạm vi Xuất (Export Scope):**
  1. **Danh sách đang lọc (`FILTERED`):** Tự động bắt các tiêu chí tìm kiếm hiện tại từ URL query parameters (`search`, `status`, `timeSort`), xuất toàn bộ các bản ghi khớp bộ lọc trên toàn hệ thống (bỏ phân trang). Hiển thị dòng mô tả điều kiện lọc tương ứng trực quan.
  2. **Các đề xuất đã chọn (`SELECTED`):** Chỉ xuất những bản ghi mà HR đã tích chọn qua checkbox trên bảng. Nếu chưa có bản ghi nào được chọn, tùy chọn này tự động bị vô hiệu hóa (`disabled`) kèm chú thích cảnh báo.
  3. **Toàn bộ Offer (`ALL`):** Xuất toàn bộ các bản ghi đề xuất đang hoạt động trong hệ thống.
- **Tùy chọn 24 Cột Dữ liệu (Export Columns):**
  - **Nhóm Mặc định (12 cột cơ bản):** Mã Offer, Tên ứng viên, Email, Vị trí đề xuất, Lương chính thức, Lương thử việc, Tỷ lệ lương thử việc (%), Trạng thái, Ngày bắt đầu dự kiến, Địa điểm làm việc, Ngày lập, Ngày cập nhật.
  - **Nhóm Mở rộng (12 cột nâng cao):** Mã đơn ứng tuyển, Số điện thoại, Phòng ban, Mã Requisition, Mã Job Posting, Thời gian thử việc, Gói phúc lợi, Người tạo đề xuất, Director phê duyệt, Quyết định Director, Nhận xét Director, Thời điểm duyệt.
  - **Bộ nút chọn nhanh:** "Chọn tất cả" (chọn 24 cột), "Mặc định" (chọn 12 cột nhóm cơ bản), "Bỏ chọn" (bỏ chọn toàn bộ).
- **Thanh Tác vụ Đa lựa chọn (Selection Toolbar):**
  - Tích hợp checkbox "Chọn tất cả trên trang" tại header bảng và checkbox ở từng dòng dữ liệu.
  - Khi có ít nhất 1 dòng được chọn, thanh `#selectionToolbar` lập tức trượt mở, hiển thị số lượng bản ghi đã chọn kèm nút thao tác nhanh: **"Xuất [N] đề xuất"** và **"Bỏ chọn"**.

#### 1.2. Tầng Backend & Tạo File Excel (Apache POI)
- Bổ sung thư viện Apache POI `poi-ooxml` (phiên bản `5.3.0`) vào `pom.xml`.
- Xây dựng tầng DTO chặt chẽ:
  - `OfferExportScope`: Enum định nghĩa 3 phạm vi (`FILTERED`, `SELECTED`, `ALL`).
  - `OfferExportFilterRequest`: Chứa tiêu chí lọc từ khóa, trạng thái và sắp xếp.
  - `OfferExportRequest`: Record tiếp nhận request xuất từ client, kèm kiểm tra ràng buộc `@NotNull`, `@NotEmpty`.
  - `OfferExportRow`: Record trung gian phẳng hóa dữ liệu của bản ghi Offer phục vụ xuất Excel.
- Dịch vụ **`OfferExportService`** & **`OfferExportServiceImpl`**:
  - Truy vấn dữ liệu hiệu quả theo đúng scope và filter qua Spring Data JPA Specification/Query.
  - Thiết kế cấu trúc file Excel gồm **2 Sheets chuyên nghiệp**:
    - **Sheet 1 — "Danh sách Offer":** Bảng dữ liệu có dòng tiêu đề Header nền xanh lá thương hiệu (`#3e7157`), chữ trắng in đậm; các ô dữ liệu có viền mảnh (`thin border`); tự động xen kẽ màu nền; định dạng số tiền tệ chuẩn VND (`#,##0 "VND"`); định dạng ngày tháng `dd/MM/yyyy`; tự động căn chỉnh độ rộng cột (`autoSizeColumn`).
    - **Sheet 2 — "Báo cáo & Metadata":** Chứa thông tin quản trị về tệp xuất (Thời điểm xuất, Người thực hiện xuất, Phạm vi dữ liệu, Tổng số lượng bản ghi xuất, Chi tiết bộ lọc áp dụng).
  - Tên file sinh tự động theo quy tắc: `offers_YYYY-MM-DD.xlsx` hoặc `offers_selected_YYYY-MM-DD.xlsx`.
- Phân quyền endpoint: [`POST /offers/export`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/controller/OfferController.java) phân quyền chặt chẽ cho `ROLE_HR`, `ROLE_DIRECTOR`, và `ROLE_SYSTEM_ADMIN`. Trả về luồng nhị phân `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`.

---

### 2. Khôi phục và Chuẩn hóa Luồng Pop-up Modal tại Screen 30

#### 2.1. Pop-up Tạo mới Offer Proposal (`#createOfferModal`)
- **Trải nghiệm:** Bấm nút **"+ Tạo Đề Xuất"** sẽ mở pop-up modal ngay trên trang danh sách thay vì chuyển hướng sang URL mới `/offers/create`.
- **Bước 1 — Chọn Ứng viên Đỗ Phỏng vấn (`#createAppSelect`):**
  - Dropdown nạp sẵn danh sách ứng viên đỗ phỏng vấn (`FinalDecision = 'Passed'`) chưa có Offer theo quy tắc `GBR-07`.
  - Khi chọn ứng viên, hệ thống lập tức mở rộng khung **Group A (Thông tin Ứng viên & Vị trí)** hiển thị đầy đủ: Mã ứng viên, Họ tên, Email, Số điện thoại, Vị trí ứng tuyển, Phòng ban, Mã Requisition ID, Hiring Manager phụ trách, Lương đề xuất từ HM, và Đánh giá của Hội đồng phỏng vấn.
  - Tự động điền trước Vị trí chức danh đề xuất và Mức lương vào khung Group B.
- **Bước 2 — Nhập Thông tin Đãi ngộ & Điều khoản Offer (Group B):**
  - Nhập Lương chính thức và Lương thử việc.
  - Tích hợp nút **"Gợi ý 85%"** tự động tính toán mức sàn theo quy định pháp lý `BR-OFF-01`.
  - Hiển thị phản hồi trực quan theo thời gian thực (`real-time hint`): Màu xanh khi đạt tỷ lệ $\ge 85\%$, cảnh báo màu đỏ khi $< 85\%$ hoặc vượt quá lương chính thức.
  - Kiểm tra ràng buộc Ngày bắt đầu dự kiến: Bắt buộc phải lớn hơn ngày hiện tại (tối thiểu là ngày mai).
- **Hành động:** 
  - Nút **"Lưu Bản Thảo (Draft)"**: Lưu bản ghi ở trạng thái nháp để chỉnh sửa sau.
  - Nút **"Trình Giám Đốc"**: Lưu và chuyển tiếp trạng thái sang `Pending_Director`.
  - Toàn bộ gửi qua AJAX JSON `POST /offers/create` kèm CSRF token, đóng modal và tải lại bảng dữ liệu mượt mà kèm thông báo Toast.

#### 2.2. Pop-up Xem Chi Tiết Offer Proposal (`#detailOfferModal` — Screen 32)
- **Trải nghiệm:** Khi click vào Mã Offer, Tên ứng viên, hoặc icon Xem chi tiết (mắt) tại cột Thao tác, hệ thống mở modal `#detailOfferModal` ngay lập tức mà không rời khỏi trang danh sách.
- **Xử lý Dữ liệu:**
  - Client gọi API [`GET /offers/{id}`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/controller/OfferController.java) với header `Accept: application/json`.
  - Backend phản hồi JSON DTO `ApiResponse<OfferDetailResponse>` đầy đủ thông tin, không bị phụ thuộc session hay Lazy Initialization Exception.
- **Bố cục Nội dung:**
  - Header tóm tắt: Chức danh, Họ tên ứng viên, Ngày lập và Badge trạng thái hiện tại.
  - **Mục 1 — Hồ sơ Ứng viên & Kết quả Phỏng vấn:** Thông tin cá nhân, vị trí, Requisition, nhận xét của hội đồng phỏng vấn, lương khuyến nghị của HM.
  - **Mục 2 — Gói Đãi ngộ & Điều khoản Offer:** Lương chính thức, Lương thử việc (kèm huy hiệu tỷ lệ % so với lương chính), số ngày thử việc, ngày bắt đầu dự kiến, địa điểm làm việc và chế độ phúc lợi.
  - **Mục 3 — Lịch sử Phê duyệt của Director:** Dòng thời gian trực quan (Timeline) ghi nhận tên Giám đốc, quyết định (`Approved` / `Rejected`), mốc thời gian và nhận xét giải thích.
- **Nút Hành động Ngữ cảnh Dưới Chân Modal (Contextual Footer Actions):**
  - Nếu là `Draft`: Hiển thị nút **"Xóa bản thảo"** (đỏ) và **"Chỉnh sửa"** (xanh).
  - Nếu là `Director_Rejected` / `Rejected`: Hiển thị nút **"Chỉnh sửa & Trình lại"** (xanh).
  - Nếu là `Director_Approved` / `Approved`: Hiển thị nút **"Phát hành Offer Letter"** (xanh).
  - Các trạng thái đã khóa (`Sent_Candidate`, `Accepted`, `Declined`): Chỉ hiển thị nút **"Đóng"**.

#### 2.3. Pop-up Chỉnh Sửa Offer Proposal (`#editOfferModal`)
- **Trải nghiệm:** Cho phép mở trực tiếp từ bảng danh sách (icon cây bút) hoặc chuyển tiếp từ nút "Chỉnh sửa" trong modal xem chi tiết.
- **Ràng buộc nghiệp vụ:** Chỉ khả dụng đối với Offer ở trạng thái `Draft` hoặc bị Giám đốc từ chối (`Director_Rejected` / `Rejected`).
- **Xử lý Từ chối:** Nếu Offer bị từ chối, modal tự động hiển thị **Banner Cảnh báo Màu Đỏ** trích xuất chính xác lý do/ghi chú từ chối của Director để HR nắm bắt và điều chỉnh.
- **Cập nhật:** Hỗ trợ lưu bản thảo hoặc nộp duyệt lại lên Giám đốc qua AJAX JSON `PUT /offers/{id}`.

#### 2.4. Thao tác Xóa (Delete) & Phát hành (Send Offer)
- **Xóa Bản Thảo:** Chỉ hiển thị khi `Draft`. Xác nhận qua hộp thoại cảnh báo và gọi AJAX `DELETE /offers/{id}`.
- **Phát hành Offer Letter:** Chỉ hiển thị khi `Director_Approved`. Xác nhận chuyển trạng thái sang `Sent_Candidate` và gửi mail thông báo cho ứng viên qua AJAX `POST /offers/{id}/send`.

---

### 3. Tinh chỉnh Giao diện, CSS & Trải nghiệm Người dùng (UI/UX)

1. **Khắc phục Triệt để Hiện tượng Overlay Che Màn hình (Zero-FOUC):**
   - Đã đồng bộ bổ sung thuộc tính inline `style="display: none;"` cho toàn bộ các modal (`#offerExportModal`, `#createOfferModal`, `#detailOfferModal`, `#editOfferModal`) trong file [`list.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/list.html).
   - Đảm bảo ngay cả khi mạng chậm hoặc cache CSS chưa cập nhật, tuyệt đối không xảy ra tình trạng lớp phủ tàng hình ngăn cản thao tác chuột của người dùng.
2. **Chuẩn hóa CSS (`offers.css`):**
   - Căn chỉnh z-index modal ở mức `9999`, backdrop filter mờ hiện đại (`backdrop-filter: blur(4px)`).
   - Thiết kế hiệu ứng mở modal nổi nhẹ nhàng (`@keyframes popInModal`).
   - Căn giữa và bố cục thẻ thống kê cân đối: Tiêu đề ở trên, icon màu thương hiệu và số liệu to rõ ràng ở dưới.
3. **Hệ thống Thông báo Toast (`Toast Notifications`):**
   - Thay thế các hộp thoại `alert()` truyền thống bằng Toast notifications nổi góc màn hình với icon Font Awesome và màu sắc tương ứng (thành công, cảnh báo, lỗi).

---

## III. BẢNG TỔNG HỢP CÁC TỆP TIN THAY ĐỔI (FILES CHANGED)

| STT | Đường dẫn Tệp tin | Loại thay đổi | Chi tiết nội dung thay đổi |
| :---: | :--- | :---: | :--- |
| 1 | `pom.xml` | Modified | Thêm dependency `org.apache.poi:poi-ooxml:5.3.0` phục vụ xuất Excel |
| 2 | `src/main/java/com/group2/rms/offer/dto/OfferExportScope.java` | **Created** | Enum 3 phạm vi xuất: `FILTERED`, `SELECTED`, `ALL` |
| 3 | `src/main/java/com/group2/rms/offer/dto/OfferExportFilterRequest.java` | **Created** | DTO tiêu chí tìm kiếm, lọc trạng thái và sắp xếp phục vụ xuất file |
| 4 | `src/main/java/com/group2/rms/offer/dto/OfferExportRequest.java` | **Created** | DTO nhận yêu cầu xuất Excel kèm validation `@NotNull`, `@NotEmpty` |
| 5 | `src/main/java/com/group2/rms/offer/dto/OfferExportRow.java` | **Created** | DTO trung gian phẳng hóa 24 trường dữ liệu của Offer |
| 6 | `src/main/java/com/group2/rms/offer/service/OfferExportService.java` | **Created** | Interface định nghĩa nghiệp vụ xuất Excel và sinh tên file |
| 7 | `src/main/java/com/group2/rms/offer/service/OfferExportServiceImpl.java` | **Created** | Triển khai Apache POI tạo workbook 2 sheets, định dạng currency, borders |
| 8 | `src/main/java/com/group2/rms/offer/controller/OfferController.java` | Modified | Thêm các endpoint RESTful: `POST /export`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}`, `POST /{id}/send`, `POST /create` (JSON) |
| 9 | `src/main/resources/templates/offers/list.html` | Modified | Tích hợp 4 pop-up modals (`Export`, `Create`, `Detail`, `Edit`), thanh `selectionToolbar`, icon thao tác |
| 10 | `src/main/resources/static/css/offers.css` | Modified | Bổ sung styles cho Selection Toolbar, Modals, Export dialog, Stats cards, Toast |
| 11 | `src/main/resources/static/js/offers.js` | Modified | Xử lý logic toàn bộ pop-up modals, checkbox đa chọn, gọi AJAX RESTful, tải file blob Excel |
| 12 | `src/test/java/com/group2/rms/offer/OfferExportServiceTests.java` | **Created** | 7 Unit test cases kiểm tra dịch vụ xuất Excel |
| 13 | `src/test/java/com/group2/rms/offer/OfferIntegrationTest.java` | Modified | 15 Integration test cases kiểm tra đầy đủ các API và luồng của OfferController |
| 14 | `src/test/java/com/group2/rms/service/OfferServiceTests.java` | Modified | 57 Unit test cases kiểm tra nghiệp vụ lõi của OfferService |

---

## IV. KẾT QUẢ KIỂM THỬ VÀ XÁC MINH CHẤT LƯỢNG

### 1. Kiểm thử Tự động (Automated Test Suite)
Đã chạy toàn bộ các bài kiểm thử liên quan đến phân hệ Offer bằng lệnh Maven:
```bash
.\mvnw.cmd test -Dtest=OfferExportServiceTests,OfferIntegrationTest,OfferServiceTests
```
**Kết quả:**
- **`OfferExportServiceTests`:** 7/7 tests PASS (100%) — Đạt yêu cầu về cả 3 phạm vi xuất, định dạng số, lọc cột và chống null pointer.
- **`OfferIntegrationTest`:** 15/15 tests PASS (100%) — Đạt yêu cầu về quyền truy cập, trả về DTO JSON hợp lệ cho modal, nộp tạo mới, chỉnh sửa, xóa và tải file Excel.
- **`OfferServiceTests`:** 57/57 tests PASS (100%) — Đạt yêu cầu về các ràng buộc nghiệp vụ (`GBR-07`, `BR-OFF-01`, chuyển trạng thái hợp lệ).
- **Tổng cộng:** **79/79 bài test đều PASS (Tỷ lệ thành công: 100%)**, không có lỗi biên dịch hay lỗi cấu hình nào.

### 2. Kiểm thử Thực tế trên Trình duyệt (Browser Verification via CDP)
- **Tải trang:** Trang `/offers` phản hồi HTTP 200 OK, tải 7 dòng dữ liệu bảng mẫu và 10 ứng viên đỗ trong dropdown.
- **Console Logs:** `0 errors / 0 exceptions` trong suốt toàn bộ quá trình tương tác.
- **Network Requests:** `0 network errors`. Các request AJAX (`/offers/1`, `/offers/export`, `/offers/create`) đều có gắn header `X-CSRF-TOKEN` đầy đủ và phản hồi đúng chuẩn dữ liệu.
- **Xuất Excel:** Đã kiểm thử tải file nhị phân thành công trên trình duyệt, file mở được bằng Microsoft Excel với đầy đủ 2 Sheet và đúng định dạng tiền tệ VND.

---

## V. KẾT LUẬN

Nhánh `feat/huyenpt/inte2/hr-offer-ui-enhancements` đã giải quyết triệt để tất cả các yêu cầu về trải nghiệm người dùng, nghiệp vụ và kỹ thuật:
- ✅ Tính năng Xuất Excel hoàn chỉnh, linh hoạt và chuyên nghiệp.
- ✅ Khôi phục và nâng cấp toàn bộ thao tác (Tạo, Xem, Sửa, Xóa, Gửi) thành Pop-up Modal trực tiếp trên màn hình danh sách Screen 30.
- ✅ Giao diện đồng bộ nhận diện thương hiệu Mộc RMS, mượt mà và không còn lỗi hiển thị.
- ✅ Bộ mã nguồn sạch sẽ, tuân thủ kiến trúc phân tầng, sẵn sàng để tạo Pull Request và hợp nhất vào nhánh `main`.
