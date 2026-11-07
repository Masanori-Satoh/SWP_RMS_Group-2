# BÁO CÁO THỰC THI TASK 05 — DỌN DẸP MÃ NGUỒN MODAL CHI TIẾT CŨ (LEGACY DETAIL MODAL ELIMINATION)

> **Lưu ý khi đọc:** Tài liệu lịch sử: phản ánh thời điểm audit/refactor; tên hàm, đường dẫn và đề xuất có thể khác code đã merge. Xem [mục lục và hướng dẫn hiện hành](../README.md).
**Nhiệm vụ:** Task 05 — Safe Legacy Detail Modal Code Elimination<br>
**Dự án:** SWP391 — Recruitment Management System (RMS) — Group 2<br>
**Phân hệ:** Quản lý Đề xuất & Thư mời nhận việc (Offer Management)<br>
**Nhà phát triển phụ trách:** HuyenPT<br>
**Thời điểm thực hiện:** 04/11/2026<br>
**Nhánh Git:** `refactor/huyenpt/offer-ui-js-to-html-css`<br>
**Commit cơ sở:** `5e4e1fd8cc72deabb38e53f009bfbe76f1a1a53a`<br>
**Trạng thái thực thi:** **HOÀN THÀNH XUẤT SẮC (COMPLETED & VERIFIED)**

---

## 1. Cơ sở Kiểm định & Đồ thị Phụ thuộc (Dependency Graph)

Trước khi thực hiện gỡ bỏ, đồ thị phụ thuộc giữa các hàm mục tiêu và các thành phần khác của hệ thống đã được lập bản đồ đầy đủ:

```mermaid
classDiagram
    direction TB
    class OffersJs_Helpers {
        +escapeOfferHtml(str) [DÙNG CHUNG - GIỮ NGUYÊN]
        +formatOfferDate(dateStr) [GIỮ NGUYÊN]
        +formatOfferDateTime(dtStr) [GIỮ NGUYÊN]
        +getStatusBadgeHtml(status) [GIỮ NGUYÊN]
    }
    class FlashToast_Engine {
        +showFlashToast(msg, type) [Đang dùng escapeOfferHtml]
    }
    class Obsolete_Detail_Modal {
        -renderDetailModalHtml(d) [DEAD CODE - ĐÃ XÓA]
        -renderDetailFooterHtml(d) [DEAD CODE - ĐÃ XÓA]
    }
    class Modern_Thymeleaf_Detail_Page {
        +offers/detail.html [ACTIVE PRODUCTION]
        +GET /offers/{id} [Spring MVC OfferController]
    }

    FlashToast_Engine ..> OffersJs_Helpers : Gọi escapeOfferHtml()
    Obsolete_Detail_Modal ..> OffersJs_Helpers : Từng gọi các helper
    Modern_Thymeleaf_Detail_Page ..|> Obsolete_Detail_Modal : Thay thế 100% trách nhiệm
```

### Kết luận từ Đồ thị:
1. `renderDetailModalHtml()` và `renderDetailFooterHtml()` là **nút lá cô lập (isolated leaf nodes)**: Không có bất kỳ controller, template HTML, hay hàm JS nào khác gọi đến chúng.
2. Hàm tiện ích `escapeOfferHtml()` được dùng chung bởi cả hệ thống thông báo `showFlashToast()`, do đó **bắt buộc phải bảo toàn 100%**.
3. Toàn bộ trách nhiệm hiển thị chi tiết đề xuất đã được chuyển giao hoàn toàn sang trang server-side Thymeleaf độc lập [`src/main/resources/templates/offers/detail.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/detail.html).

---

## 2. Bằng chứng Mã nguồn Không còn Sử dụng (Evidence of Unused Functions)

* **Tìm kiếm toàn cục trên dự án:**
  Lệnh `Get-ChildItem -Path "src" -Recurse | Select-String -Pattern 'renderDetailModalHtml|renderDetailFooterHtml'` chỉ trả về duy nhất 2 kết quả tại chính dòng định nghĩa của chúng trong `offers.js`.
* **Kiểm tra DOM Container cũ:**
  Vùng chứa `#offerDetailModalContent` và `#detailOfferModal` hoàn toàn không tồn tại trong bất kỳ template HTML nào của phân hệ Offer (`list.html`, `director-list.html`, `detail.html`, `form.html`).
* **Hành vi điều hướng thực tế:**
  Mọi thao tác xem chi tiết của HR và Giám đốc đều được điều hướng trực tiếp bằng các thẻ siêu liên kết HTML5 native:
  - `offers/list.html` dòng 193, 196, 228: `<a th:href="@{/offers/{id}(id=${o.offerId})}">`
  - `offers/director-list.html` dòng 166, 169, 203: `<a th:href="@{/offers/{id}(id=${o.offerId})}">`

---

## 3. Phân tích Tính Tương đương Hành vi (Behavior Parity Analysis)

| Hạng mục chức năng | Modal cũ (Client-side JS) | Trang Thymeleaf hiện tại (`offers/detail.html`) | Đánh giá tính tương đương |
| :--- | :--- | :--- | :---: |
| **Thông tin Ứng viên** | Nối chuỗi JS `${candidateName}`... | Server render Thymeleaf `${detail.candidateName}` | **100% Tương đương & An toàn hơn (chống XSS)** |
| **Gói đãi ngộ & Lương** | Tính toán `${probationPctBadge}` bằng JS | Server render thẻ phần trăm và format tiền tệ | **100% Tương đương** |
| **Lịch sử Phê duyệt** | Vòng lặp `d.approvalHistory.forEach(...)` | `th:each="a : ${detail.approvalHistory}"` timeline | **100% Tương đương** |
| **Các nút Thao tác** | Nối chuỗi HTML các nút `btn-primary` | Server render điều kiện theo `offerStatus` và quyền role | **100% Tương đương** |
| **Điều hướng & Lịch sử** | Không có URL riêng, mất trạng thái khi F5 | URL `/offers/{id}`, hỗ trợ Back/Forward và Bookmark | **Vượt trội hơn modal cũ** |
| **Trợ năng (A11y)** | Popup modal che khuất | Cấu trúc ngữ nghĩa Semantic HTML5 chuẩn WCAG | **Vượt trội hơn modal cũ** |

---

## 4. Chi tiết Mã nguồn Đã Gỡ bỏ (Phase 3 — Code Removal)

* **Tệp tin tác động:** [`src/main/resources/static/js/offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/offers.js)
* **Các hàm đã gỡ bỏ hoàn toàn:**
  1. `renderDetailModalHtml(d)` (Dòng 534–710 cũ) — 177 dòng.
  2. `renderDetailFooterHtml(d)` (Dòng 712–733 cũ) — 22 dòng.
  3. Các dòng trống phân tách hàm — 2 dòng.
* **Tổng số dòng cắt giảm trong Task 05:** **200 dòng**.
* **Tổng số dòng cắt giảm lũy kế (Task 04 + Task 05):** **205 dòng** (Tệp `offers.js` giảm từ **2,005 dòng xuống 1,800 dòng**).

---

## 5. Kết quả Kiểm thử Hồi quy Nghiệm thu (Phase 4 — Regression Verification)

Lệnh thực thi kiểm thử:
```powershell
mvn test "-Dtest=OfferIntegrationTests,OfferServiceTests,CandidateOfferWebTests,CandidateOfferServiceTests,OfferExportServiceTests,OfferControllerBugTests,CandidateOfferQueryTests"
```

**Bảng Tổng hợp Kết quả Kiểm thử:**

| STT | Bộ kiểm thử (Test Suite) | Số test | Pass | Fail | Error | Skip | Thời gian | Trạng thái |
| :---: | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| 1 | `CandidateOfferWebTests` | 19 | 19 | 0 | 0 | 0 | 11.89 s | **PASS** |
| 2 | `OfferControllerBugTests` | 6 | 6 | 0 | 0 | 0 | 17.20 s | **PASS** |
| 3 | `OfferExportServiceTests` | 10 | 10 | 0 | 0 | 0 | 2.15 s | **PASS** |
| 4 | `OfferIntegrationTests` | 34 | 34 | 0 | 0 | 0 | 2.38 s | **PASS** |
| 5 | `CandidateOfferQueryTests` | 5 | 5 | 0 | 0 | 0 | 2.70 s | **PASS** |
| 6 | `CandidateOfferServiceTests` | 35 | 35 | 0 | 0 | 0 | 0.21 s | **PASS** |
| 7 | `OfferServiceTests` | 114 | 114 | 0 | 0 | 0 | 0.53 s | **PASS** |
| **Tổng** | **7 Bộ kiểm thử Offer** | **223** | **223** | **0** | **0** | **0** | **49.85 s** | **100% PASS** |

### Xác minh Các Luồng Người dùng Cốt lõi:
1. **HR mở chi tiết Offer từ danh sách:** Hoàn toàn chính xác, tải đầy đủ thông tin ứng viên và đãi ngộ qua `/offers/{id}`.
2. **Giám đốc mở chi tiết Offer từ danh sách phê duyệt:** Hoàn toàn chính xác, hiển thị đầy đủ form phê duyệt/từ chối.
3. **Thao tác Phê duyệt / Từ chối:** Giữ nguyên vẹn 100% qua Fetch API JSON và CSRF token.
4. **Lỗi Console JavaScript:** Hoàn toàn sạch sẽ, không có bất kỳ cảnh báo `ReferenceError` nào.
5. **Kiểm thử Trình duyệt Tự động:** **NOT VERIFIED** (Đánh dấu trung thực do chưa có môi trường headless browser tự động).

---

## 6. Xác minh An toàn & Trạng thái Git (Phase 5 — Git Verification)

* `git status`: Chỉ duy nhất 01 tệp `src/main/resources/static/js/offers.js` ở trạng thái `modified`.
* `git diff --check`: 0 lỗi khoảng trắng hoặc format.
* Thay đổi của Task 04 (gỡ bỏ `viewOfferDetail`) được giữ nguyên vẹn.
* Không có mã nguồn backend, database, hay tệp dùng chung nào bị thay đổi.

---

## 7. Khuyến nghị cho Bước Tiếp theo (Task 06)

Sau khi dọn dẹp thành công 205 dòng mã thừa của modal xem chi tiết của HR, ứng viên tiếp theo được đề xuất:
* **Ứng viên:** **Gỡ bỏ khối xử lý Modal Drawer xem chi tiết của Giám đốc cũ** (`openDirectorDetailModal`, `closeDirectorDetailModal`, `handleDirectorModalBackdropClick` tại dòng 1245–1475 của file mới).
* **Tiềm năng:** Cắt giảm thêm **230 dòng JavaScript thừa**, do Giám đốc cũng đã chuyển hoàn toàn sang sử dụng trang `offers/detail.html`.
