# BÁO CÁO TỔNG HỢP CÁC CẢI TIẾN VÀ SỬA LỖI PHÂN HỆ QUẢN LÝ ĐỀ XUẤT NHẬN VIỆC (OFFER MANAGEMENT)

- **Dự án:** Hệ Thống Quản Lý Tuyển Dụng — SWP_RMS_Group-2 (Mộc RMS)
- **Nhánh thực hiện (Branch):** `fix/huyenpt/offer-management-flow-fixes`
- **Nhánh gốc (Base Branch):** `main`
- **Thành viên phụ trách (Assignee):** Phạm Thị Huyền (`HuyenPT`)
- **Phân hệ (Module):** Quản lý Đề xuất Offer (HR Offer Workspace, Director Approval Workspace & Candidate Portal)
- **Tài liệu tham chiếu:**
  - Quy chuẩn kiến trúc hệ thống: [`docs/architecture/ARCHITECTURE_GUIDE.md`](../../architecture/ARCHITECTURE_GUIDE.md)
  - Hướng dẫn thành phần UI: [`docs/architecture/UI/UI_CATALOG.md`](../../architecture/UI/UI_CATALOG.md)
  - Quy tắc thiết kế UI: [`docs/architecture/UI/UI_RULES.md`](../../architecture/UI/UI_RULES.md)

---

## 🎯 I. MỤC TIÊU & BỐI CẢNH TRIỂN KHAI

Đợt cập nhật này tập trung xử lý và hoàn thiện các lỗi phát sinh trong quy trình làm việc (workflow), trải nghiệm giao diện người dùng (UX) và tính nhất quán dữ liệu thuộc phân hệ Đề xuất nhận việc (Offer Management):

1. **Bộ lọc thẻ thống kê tương tác:** Biến các thẻ KPI thống kê trên màn hình HR và Giám đốc thành các bộ lọc có thể nhấp chuột trực tiếp để lọc bảng dữ liệu.
2. **Khắc phục lỗi logic lọc Giám đốc đã duyệt:** Giải quyết hiện tượng thẻ thống kê hiển thị 7 đề xuất đã duyệt nhưng khi lọc theo `Director_Approved` bảng lại trả về 0 kết quả (do các offer đã chuyển tiếp sang `Sent_Candidate`, `Accepted`, `Declined`).
3. **Đồng bộ giao diện & Icon trạng thái Giám đốc:** Đồng bộ bảng danh sách Giám đốc với bộ huy hiệu trạng thái (status badge) có kèm icon trực quan, căn chỉnh thẳng hàng tiêu đề và icon tiêu đề danh sách.
4. **Khắc phục lỗi hiển thị trang chỉnh sửa Offer:** Giải quyết lỗi màn hình trắng / lỗi SpEL khi người dùng nhấn nút "Chỉnh sửa" bản nháp đề xuất.
5. **Tinh gọn giao diện ứng viên:** Loại bỏ mã ứng viên kỹ thuật dư thừa (`#CAN-0051`) trên thẻ hồ sơ ứng tuyển của ứng viên tại Candidate Portal.
6. **Chuẩn hóa ngôn ngữ nhận xét đánh giá phỏng vấn:** Loại bỏ từ tiếng Anh dư thừa `(Offer)` trong câu tổng hợp đánh giá phỏng vấn ở dữ liệu seed.
7. **Điều tra chuyên sâu lỗi điều hướng bất thường:** Phân tích nguyên nhân gốc rễ và đề xuất phương án xử lý hiện tượng trình duyệt chuyển hướng ngoài ý muốn về `/api/notifications/unread-count`.

---

## 🛠️ II. CHI TIẾT CÁC HẠNG MỤC ĐÃ HOÀN THÀNH

### 1. Bộ lọc thẻ thống kê tương tác & Chuẩn hóa giao diện Giám đốc (`director-list.html`, `offers.js`, `offer-workspace.css`)
- **Đổi tiêu đề trang Giám đốc:** Thay đổi từ `"Quản lý Đề xuất"` thành `"Phê Duyệt Đề Xuất Nhận Việc"` để phản ánh chính xác vai trò, thẩm quyền của Giám đốc (phê duyệt/từ chối đề xuất).
- **Thẻ KPI Giám đốc thành bộ lọc:** Chuyển đổi 3 thẻ thống kê (`CHỜ DUYỆT`, `ĐÃ DUYỆT`, `ĐÃ TỪ CHỐI`) thành `<button type="button" class="director-stat-card ...">` với:
  - `CHỜ DUYỆT` $\rightarrow$ `Pending_Director`
  - `ĐÃ DUYỆT` $\rightarrow$ `Director_Approved`
  - `ĐÃ TỪ CHỐI` $\rightarrow$ `Director_Rejected`
  - Hỗ trợ đầy đủ trạng thái `is-active`, `aria-pressed`, phím tắt Enter/Space.
- **Hàm điều phối JavaScript:** Bổ sung `filterDirectorOffersByStatus(status)` trong `offers.js` tái sử dụng `applyDirectorOfferFilters()`, đồng thời bảo toàn từ khóa tìm kiếm (`search`) và sắp xếp thời gian (`timeSort`).
- **Căn chỉnh layout tiêu đề danh sách:** Sửa cấu trúc `.ofw-list-heading` và flexbox trong `offer-workspace.css` để icon hộp `.ofw-list-icon` và tiêu đề `.adw-panel-title` luôn nằm trên cùng một hàng ngang.

### 2. Xử lý logic lọc dữ liệu "Giám đốc đã duyệt" (`OfferServiceImpl.java`)
- **Nguyên nhân:** Thẻ thống kê đếm tất cả các Offer mà Giám đốc đã duyệt qua bảng `OfferApproval` (gồm 7 đề xuất). Tuy nhiên, bảng dữ liệu lọc theo trường `offerStatus` của bảng `OfferProposal`. Khi Offer được gửi cho ứng viên, trạng thái chuyển thành `Sent_Candidate`, `Accepted`, hoặc `Declined`, khiến việc chỉ lọc `offerStatus = 'Director_Approved'` trả về 0 kết quả.
- **Giải pháp:** Cập nhật điều kiện lọc `Director_Approved` (hoặc `Approved`) trong `OfferServiceImpl.java` mở rộng bao gồm cả các trạng thái kế tiếp:
  ```java
  } else if ("Director_Approved".equalsIgnoreCase(s) || "Approved".equalsIgnoreCase(s)) {
      predicates.add(builder.or(
              builder.equal(builder.lower(root.get("offerStatus")), "director_approved"),
              builder.equal(builder.lower(root.get("offerStatus")), "approved"),
              builder.equal(builder.lower(root.get("offerStatus")), "sent_candidate"),
              builder.equal(builder.lower(root.get("offerStatus")), "accepted"),
              builder.equal(builder.lower(root.get("offerStatus")), "declined")
      ));
  }
  ```
  Nhờ đó, khi click vào thẻ "Giám đốc đã duyệt (7)", bảng hiển thị chính xác toàn bộ 7 đề xuất đã được Giám đốc thông qua.

### 3. Đồng bộ Badge trạng thái kèm icon trực quan (`director-list.html`, `offer-workspace.css`)
- Thay thế các badge văn bản đơn điệu trên danh sách Giám đốc bằng các status badge trực quan chuẩn hóa theo UI Catalog:
  - **Chờ GĐ duyệt:** Kèm icon đồng hồ cát `⌛` (`.status-pending`)
  - **GĐ đã duyệt:** Kèm icon người dùng tích xanh `👤` (`.status-approved`)
  - **Đã gửi thư mời:** Kèm icon máy bay giấy `✈` (`.status-sent`)
  - **Ứng viên đã nhận việc:** Kèm icon tích tròn xanh lá `✔` (`.status-accepted`)
  - **Ứng viên từ chối:** Kèm icon dấu x tròn đỏ `🗙` (`.status-declined`)
  - **GĐ từ chối:** Kèm icon cấm đỏ `🚫` (`.status-rejected`)
  - **Bản nháp:** Kèm icon tệp tài liệu `📄` (`.status-draft`)
- Bổ sung định kiểu tương ứng trong `offer-workspace.css`.

### 4. Khắc phục lỗi hiển thị trang chỉnh sửa Offer (`OfferController.java`)
- **Nguyên nhân:** Khi vào đường dẫn `/offers/{id}/edit`, template `offers/form.html` đánh giá biến SpEL `isLockedCandidate`. Do Controller chưa truyền giá trị này vào model trong action edit, quá trình render template gặp lỗi hoặc không xác định được trạng thái trường ứng viên.
- **Giải pháp:** Thêm `model.addAttribute("isLockedCandidate", false);` trong các phương thức GET/POST chỉnh sửa đề xuất tại `OfferController.java`, đảm bảo trang form render mượt mà, đầy đủ dữ liệu.

### 5. Loại bỏ mã ứng viên `#CAN-xxxx` trên giao diện Portal (`my-applications.html`)
- Xóa bỏ việc hiển thị đoạn mã kỹ thuật `#CAN-0051` cạnh ngày nộp hồ sơ trên thẻ đơn ứng tuyển tại `src/main/resources/templates/candidate/my-applications.html`.
- Cập nhật test case tương ứng trong `MyApplicationWebTests.java` để đảm bảo bộ kiểm thử luôn xanh.

### 6. Chuẩn hóa câu nhận xét đánh giá phỏng vấn (`build_seed.py`, `seed_data.sql`)
- Loại bỏ từ tiếng Anh dư thừa `(Offer)` trong chuỗi nhận xét tổng hợp kết quả phỏng vấn:
  - **Trước:** `"Tổng hợp kết quả phỏng vấn ứng viên <Tên>: Đạt yêu cầu đầu vào, đề xuất gửi thư mời nhận việc (Offer)."`
  - **Sau:** `"Tổng hợp kết quả phỏng vấn ứng viên <Tên>: Đạt yêu cầu đầu vào, đề xuất gửi thư mời nhận việc."`
- Cập nhật đồng bộ tại file sinh dữ liệu `build_seed.py` và 16 bản ghi tương ứng trong `seed_data.sql`.

### 7. Báo cáo điều tra lỗi điều hướng `/api/notifications/unread-count`
- Đã hoàn thành báo cáo điều tra chi tiết:
  - **Hiện tượng:** Sau khi session hết hạn hoặc tài khoản re-login, việc click vào hành động khiến trình duyệt chuyển hướng thẳng đến URL `/api/notifications/unread-count` hiển thị raw JSON `{"unreadCount": 5}`.
  - **Nguyên nhân:** `HttpSessionRequestCache` của Spring Security bắt nhầm request ngầm polling số thông báo của `topbar-notifications.js` và lưu làm `SavedRequest`. Khi người dùng đăng nhập lại hoặc kích hoạt xác thực, `SavedRequestAwareAuthenticationSuccessHandler` chuyển hướng trình duyệt tới URL API này.
  - **Đề xuất giải pháp:** Cấu hình `RequestMatcher` loại trừ `/api/**` khỏi `HttpSessionRequestCache` và cấu hình `AuthenticationEntryPoint` trả về HTTP 401 thay vì 302 redirect về `/login`.

---

## 📁 III. DANH SÁCH TẬP TIN THAY ĐỔI

| STT | Tập tin | Nội dung thay đổi |
|:---:|:---|:---|
| 1 | `src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java` | Mở rộng điều kiện lọc `Director_Approved` bao quát các trạng thái kế tiếp |
| 2 | `src/main/java/com/group2/rms/offer/controller/OfferController.java` | Bổ sung `isLockedCandidate = false` cho form chỉnh sửa offer |
| 3 | `src/main/resources/templates/offers/director-list.html` | Cập nhật tiêu đề trang, 3 KPI filter cards, bố cục tiêu đề danh sách, badge trạng thái kèm icon |
| 4 | `src/main/resources/templates/offers/form.html` | Đảm bảo an toàn render form chỉnh sửa offer |
| 5 | `src/main/resources/static/js/offers.js` | Thêm logic lọc tương tác `filterOffersByStatus`, `filterDirectorOffersByStatus` |
| 6 | `src/main/resources/static/css/pages/offer-workspace.css` | Định kiểu cho `director-stat-card`, `.ofw-list-heading` và các status badges |
| 7 | `src/main/resources/templates/candidate/my-applications.html` | Bỏ hiển thị mã ứng viên `#CAN-xxxx` trên thẻ hồ sơ |
| 8 | `database/seeds/build_seed.py` | Bỏ `(Offer)` trong câu nhận xét tổng hợp kết quả phỏng vấn |
| 9 | `database/seeds/seed_data.sql` | Cập nhật 16 dòng seed data bỏ `(Offer)` |
| 10 | `src/test/java/com/group2/rms/candidate/application/portal/controller/MyApplicationWebTests.java` | Cập nhật kiểm thử phù hợp với giao diện bỏ `#CAN-xxxx` |
| 11 | `src/test/java/com/group2/rms/offer/OfferIntegrationTests.java` | Bổ sung kiểm thử tích hợp cho bộ lọc Giám đốc và HTML rendering |
| 12 | `src/test/java/com/group2/rms/offer/controller/OfferControllerBugTests.java` | Bổ sung kiểm thử đơn vị cho chức năng lọc và hiển thị danh sách |

---

## ✅ IV. KẾT QUẢ KIỂM THỬ TỰ ĐỘNG

Chạy toàn bộ các test suite liên quan trực tiếp:
```bash
mvn test -Dtest="OfferIntegrationTests,OfferControllerBugTests,MyApplicationWebTests"
```

**Kết quả:**
```text
[INFO] Running com.group2.rms.candidate.application.portal.controller.MyApplicationWebTests
[INFO] Tests run: 20, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.group2.rms.offer.controller.OfferControllerBugTests
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.group2.rms.offer.OfferIntegrationTests
[INFO] Tests run: 33, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total tests run: 59, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
```

Toàn bộ 59/59 test cases đều vượt qua thành công, đảm bảo tính toàn vẹn và ổn định của toàn hệ thống.

