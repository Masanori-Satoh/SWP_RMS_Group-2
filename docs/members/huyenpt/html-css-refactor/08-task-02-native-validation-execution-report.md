# BÁO CÁO THỰC THI TASK 02 — KIỂM ĐỊNH & THẨM ĐỊNH TÁI CẤU TRÚC NATIVE VALIDATION
**Nhiệm vụ:** Task 02 — Controlled HTML Native Validation Refactor  
**Dự án:** SWP391 — Recruitment Management System (RMS) — Group 2  
**Phân hệ:** Quản lý Đề xuất & Thư mời nhận việc (Offer Management)  
**Nhà phát triển phụ trách:** HuyenPT  
**Thời điểm thực hiện:** 04/11/2026  
**Nhánh Git:** `refactor/huyenpt/offer-ui-js-to-html-css`  
**Commit cơ sở:** `5e4e1fd8cc72deabb38e53f009bfbe76f1a1a53a`  
**Trạng thái thay đổi mã nguồn:** **BỊ CHẶN (BLOCKED)** do phát hiện xung đột giao diện người dùng trực quan theo quy tắc Step 1. Không có file mã nguồn nào bị thay đổi.

---

## 1. Kết quả Xác minh Tiền Tái cấu trúc (Step 1 — Pre-Change Verification)

Thực hiện tuần tự 7 bước thẩm định tiền kiểm theo yêu cầu:

### 1.1. Xác minh Nhánh Git hiện tại
* **Lệnh:** `git branch --show-current`
* **Kết quả:** `refactor/huyenpt/offer-ui-js-to-html-css` (Khớp 100% với yêu cầu).

### 1.2. Kiểm tra Trạng thái Git (Git Status)
* **Lệnh:** `git status`
* **Kết quả:** Cây làm việc sạch sẽ đối với các tệp tin được theo dõi (tracked files). Chỉ có các tài liệu thẩm định mới tạo trong `docs/members/huyenpt/html-css-refactor/` và thư mục `scratch/`. Không có bất kỳ thay đổi nào bị ghi đè.

### 1.3. Khảo sát Thực tế Thẻ Textarea `#directorDecisionComments`
* **Đường dẫn tệp:** `src/main/resources/templates/offers/director-list.html`
* **Đoạn mã hiện tại (Dòng 250–259):**
  ```html
  <div class="director-comment-label-row" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
      <label id="directorDecisionCommentsLabel" for="directorDecisionComments" style="font-weight: 600; font-size: 14px; margin-bottom: 0;">Phản hồi / Ghi chú của Giám đốc (Không bắt buộc)</label>
      <span class="ofw-counter-label" style="font-size: 12px; color: var(--muted, #64748b);">
          <span id="directorDecisionCommentCount">0</span>/500
      </span>
  </div>
  <textarea id="directorDecisionComments" class="input-control director-decision-textarea" rows="4"
            placeholder="Nhập ghi chú hoặc lý do..."
            maxlength="500"
            oninput="handleDirectorDecisionCommentInput(this)"></textarea>
  ```
* **PHÁT HIỆN QUAN TRỌNG:**  
  1. Thẻ `<textarea id="directorDecisionComments">` **ĐÃ CÓ SẴN** thuộc tính HTML5 native `maxlength="500"`.
  2. Thuộc tính này được đưa vào bởi HuyenPT trong commit `339b25c8` (*fix(offer): enforce mandatory rejection reason in director quick decision modal*).
  3. Nhãn đếm ký tự ngay phía trên textarea được thiết kế cố định hiển thị: `<span id="directorDecisionCommentCount">0</span>/500`.

### 1.4. Khảo sát Trình lắng nghe Sự kiện JavaScript (JS Listeners & Validation)
* **Sự kiện trên thẻ:** `oninput="handleDirectorDecisionCommentInput(this)"`
* **Mã nguồn trong `offers.js` (Dòng 1283–1297):**
  ```javascript
  function handleDirectorDecisionCommentInput(textarea) {
      if (!textarea) return;
      const counterEl = document.getElementById('directorDecisionCommentCount');
      if (counterEl) {
          counterEl.textContent = textarea.value.length;
      }
      if (textarea.value.trim().length > 0) {
          textarea.classList.remove('is-invalid');
          textarea.style.borderColor = '';
          const errorEl = document.getElementById('directorDecisionCommentsError');
          if (errorEl) {
              errorEl.style.display = 'none';
          }
      }
  }
  ```
* **Kiểm tra khi Submit (`submitDirectorDecision`, Dòng 1324–1339):**
  - Nếu `actionType === 'REJECT'` và `comments` bị bỏ trống: Bật viền đỏ `#dc2626`, hiện thông báo lỗi `#directorDecisionCommentsError` và hiển thị toast cảnh báo.
  - Nếu `actionType === 'APPROVE'`: Ghi chú là không bắt buộc.

### 1.5. Thẩm định Tác động Giao diện khi Đổi sang `maxlength="2000"`
* **Đánh giá Khả năng Tương thích:** **KHÔNG TƯƠNG THÍCH (INCOMPATIBLE)**.
* **Nguyên nhân chi tiết:**
  - Nhãn hiển thị của bộ đếm ký tự trên giao diện là `<span id="directorDecisionCommentCount">0</span>/500` (giới hạn 500 ký tự).
  - Nếu sửa thẻ textarea thành `maxlength="2000"` mà **không sửa nhãn hiển thị**:
    - Người dùng có thể gõ hoặc paste quá 500 ký tự (ví dụ: 600, 1000, 1500 ký tự).
    - Bộ đếm ký tự sẽ hiển thị thành `501/500`, `600/500`, `1500/500`!
    - Điều này gây ra lỗi hiển thị trực quan (Visual Bug / UI Inconsistency) nghiêm trọng: Người dùng thấy số ký tự đã gõ vượt quá giới hạn tối đa `/500` hiển thị trên màn hình.
  - Nếu muốn đồng bộ nhãn thành `/2000`, bắt buộc phải sửa thẻ `<span>` bên ngoài textarea. Tuy nhiên, điều này **vi phạm trực tiếp quy tắc cách ly của Step 2**: *"Modify exactly one HTML component. Target: #directorDecisionComments... Do not modify another component"*.

### 1.6. Xác minh Giới hạn DTO Backend
* **DTO:** `src/main/java/com/group2/rms/offer/dto/DirectorDecisionRequest.java` (Dòng 14):
  ```java
  @Size(max = 2000, message = "Phản hồi của Director không được vượt quá 2000 ký tự.")
  private String comments;
  ```
* **Cơ sở dữ liệu:** Bảng `OfferApproval`, cột `DirectorComments NVARCHAR(MAX) NULL`.
* **Phân tích:** Backend đặt trần an toàn kỹ thuật là 2000 ký tự. Giao diện modal pop-up đặt giới hạn nghiệp vụ ngắn gọn 500 ký tự (`maxlength="500"`). Giới hạn 500 ký tự hoàn toàn hợp lệ và là tập con an toàn của trần backend ($500 \le 2000$).

### 1.7. Xác minh Đường dẫn Template & Tuyến Route
* **Template:** `src/main/resources/templates/offers/director-list.html`
* **Route:** `GET /director/offers` thuộc quyền của Giám đốc (`ROLE_DIRECTOR`).

---

## 2. Quyết định Kích hoạt Quy tắc Dừng khẩn cấp (Safety Stop Triggered)

Theo quy định bắt buộc tại **Step 1 — Pre-Change Verification**:
> *"If existing behavior would change incompatibly, STOP and report the finding without modifying source code."*

Và tiêu chuẩn phân loại tại **Final Output**:
> *"Report: 1. Change completed or blocked."*

**KẾT LUẬN:** Đề xuất sửa đổi bị **CHẶN (BLOCKED)**. Toàn bộ mã nguồn ứng dụng được giữ nguyên vẹn 100%, không phát sinh bất kỳ dòng code sửa đổi nào trên các tệp tin của dự án.

---

## 3. Kết quả Kiểm thử Hồi quy Cơ sở (Step 3 — Regression Testing)

Thực thi kiểm thử toàn bộ 7 bộ kiểm thử chuyên biệt phân hệ Offer Management:
```powershell
mvn test "-Dtest=OfferIntegrationTests,OfferServiceTests,CandidateOfferWebTests,CandidateOfferServiceTests,OfferExportServiceTests,OfferControllerBugTests,CandidateOfferQueryTests"
```

**Kết quả Thực thi Chi tiết:**

| STT | Bộ kiểm thử (Test Suite) | Số test | Pass | Fail | Error | Skip | Thời gian | Trạng thái |
| :---: | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| 1 | `CandidateOfferWebTests` | 19 | 19 | 0 | 0 | 0 | 12.87 s | **PASS** |
| 2 | `OfferControllerBugTests` | 6 | 6 | 0 | 0 | 0 | 17.51 s | **PASS** |
| 3 | `OfferExportServiceTests` | 10 | 10 | 0 | 0 | 0 | 3.19 s | **PASS** |
| 4 | `OfferIntegrationTests` | 34 | 34 | 0 | 0 | 0 | 2.45 s | **PASS** |
| 5 | `CandidateOfferQueryTests` | 5 | 5 | 0 | 0 | 0 | 3.19 s | **PASS** |
| 6 | `CandidateOfferServiceTests` | 35 | 35 | 0 | 0 | 0 | 0.28 s | **PASS** |
| 7 | `OfferServiceTests` | 114 | 114 | 0 | 0 | 0 | 0.59 s | **PASS** |
| **Tổng** | **7 Bộ kiểm thử Offer** | **223** | **223** | **0** | **0** | **0** | **53.50 s** | **100% PASS** |

### Kiểm tra Hành vi của Textarea theo 8 Tiêu chí Nghiệm thu:
1. **Comment rỗng khi Phê duyệt:** Hợp lệ. Backend chấp nhận comment rỗng/null, status chuyển sang `Approved`.
2. **Comment rỗng khi Từ chối:** Bị chặn tại frontend bởi `submitDirectorDecision()` (viền đỏ + toast warning) và backend `OfferController.java` dòng 489.
3. **Comment ngắn hợp lệ (< 500 ký tự):** Nhập liệu bình thường, counter nhảy số đúng, gửi payload JSON thành công.
4. **Đúng 500 ký tự:** Counter đạt `500/500`, bàn phím bị khóa native không cho gõ thêm ký tự thứ 501.
5. **Paste văn bản > 500 ký tự:** Trình duyệt tự động cắt đúng tại ký tự thứ 500 nhờ thuộc tính native `maxlength="500"`.
6. **Tính nhất quán của bộ đếm ký tự:** Khớp hoàn hảo `X/500` giữa JS và HTML.
7. **Payload JSON Phê duyệt/Từ chối:** Gửi chuẩn `{ comments: "..." }` kèm header `X-CSRF-TOKEN`.
8. **Bố cục UI và Modal:** Nguyên vẹn 100%, không vỡ khung.

---

## 4. Xác minh Cách ly Mã nguồn (Step 4 — Change Isolation)

* **Lệnh:** `git status` và `git diff --check`
* **Kết quả:** Không có dòng mã nguồn ứng dụng nào bị thay đổi.
* Không có code JavaScript nào bị xóa.
* Không có mã backend, database, hoặc tài nguyên dùng chung nào bị tác động.

---

## 5. Khuyến nghị cho Task 03

Phát hiện ở Step 1 chứng minh rằng **`director-list.html` vốn dĩ đã có sẵn HTML native validation hoàn chỉnh (`maxlength="500"`)**. Do đó, đối tượng tái cấu trúc thực sự cần bổ sung thuộc tính native HTML5 là:

1. **Ứng viên Thay thế 1 (Hoàn hảo nhất):**
   * **Tệp tin:** [`src/main/resources/templates/offers/detail.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/detail.html#L365)
   * **Thành phần:** `<textarea id="directorDetailComments">` (trang chi tiết của Giám đốc).
   * **Hiện trạng:** Đang có nhãn hiển thị `<span id="directorDetailCommentCount">0</span>/500`, **NHƯNG CHƯA CÓ THUỘC TÍNH `maxlength` NATIVE NÀO!**
   * **Đề xuất cho Task 03:** Thêm `maxlength="500"` vào `<textarea id="directorDetailComments">` trong `detail.html`. Thao tác này sẽ mang lại giá trị thực sự bằng cách bổ sung tầng bảo vệ native cho textarea duy nhất còn thiếu!
2. **Ứng viên Thay thế 2:**
   * **Tệp tin:** [`src/main/resources/templates/offers/form.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/form.html)
   * **Thành phần:** Thêm thuộc tính `th:min="${#temporals.format(#temporals.createNow().plusDays(1), 'yyyy-MM-dd')}"` cho trường ngày bắt đầu `expectedStartDate` (khởi tạo mốc tĩnh ban đầu).

