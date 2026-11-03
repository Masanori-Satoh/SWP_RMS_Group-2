# BÁO CÁO THỰC THI TASK 03 — KIỂM ĐỊNH TÍNH TƯƠNG ĐỒNG HÀNH VI (BEHAVIOR PARITY REPORT)
**Nhiệm vụ:** Task 03 — Safe Native HTML Validation with Behavior Parity  
**Dự án:** SWP391 — Recruitment Management System (RMS) — Group 2  
**Phân hệ:** Quản lý Đề xuất & Thư mời nhận việc (Offer Management)  
**Nhà phát triển phụ trách:** HuyenPT  
**Thời điểm thực hiện:** 04/11/2026  
**Nhánh Git:** `refactor/huyenpt/offer-ui-js-to-html-css`  
**Commit cơ sở:** `5e4e1fd8cc72deabb38e53f009bfbe76f1a1a53a`  
**Trạng thái quyết định an toàn (Phase 2):** **BỊ CHẶN (BLOCKED)** do phát hiện việc thêm `maxlength="500"` làm thay đổi chức năng hiện hữu (Functional Discrepancy) đối với các luồng nhập liệu từ 501 đến 2000 ký tự. Không chỉnh sửa mã nguồn ứng dụng.

---

## 1. Kết quả Xác minh Tiền kiểm (Phase 1 — Pre-Change Verification)

Thực hiện thẩm định chi tiết 8 câu hỏi kiểm soát an toàn trên phần tử target `#directorDetailComments` tại trang chi tiết:

| STT | Câu hỏi kiểm soát (Verification Item) | Kết quả thẩm định thực tế trên mã nguồn | Bằng chứng kỹ thuật (File & Dòng mã) |
| :---: | :--- | :---: | :--- |
| **1** | Thẻ textarea `#directorDetailComments` đã có thuộc tính `maxlength` chưa? | **CHƯA CÓ** | [`detail.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/detail.html#L365-L368): thẻ chỉ có `id`, `class`, `rows="3"`, `aria-label`, `placeholder`, `oninput`. Hoàn toàn không có thuộc tính `maxlength`. |
| **2** | JavaScript hiện tại có giới hạn, cắt ngắn (truncate) hoặc từ chối input vượt quá 500 ký tự không? | **KHÔNG** | [`offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/offers.js#L1685-L1695) hàm `updateDirectorDetailCommentCounter(el)` chỉ gán `cnt.innerText = el.value.length`. Hàm `submitDirectorDecisionFromScreen()` chỉ kiểm tra rỗng khi Từ chối, hoàn toàn không chặn khi độ dài > 500. |
| **3** | Người dùng hiện tại có thể gửi comment dài hơn 500 ký tự không? | **CÓ THỂ GỬI BÌNH THƯỜNG (Lên tới 2000 ký tự)** | Backend [`DirectorDecisionRequest.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/dto/DirectorDecisionRequest.java#L14) khai báo `@Size(max = 2000)`. Database cột `DirectorComments NVARCHAR(MAX)`. Người dùng nhập 600 hay 1000 ký tự vẫn submit thành công 100%. |
| **4** | Nhãn hiển thị `/500` đại diện cho một giới hạn cứng (hard limit) hay chỉ là khuyến nghị trực quan (advisory)? | **CHỈ LÀ KHUYẾN NGHỊ TRỰC QUAN (Soft/Advisory)** | Nhãn `<span id="directorDetailCommentCount">0</span>/500` chỉ hiển thị số ký tự hiện thời trên mẫu số 500. Khi người dùng gõ 600 ký tự, nhãn hiển thị `600/500` mà không hề khóa bàn phím. |
| **5** | Cùng một thẻ textarea này có dùng chung cho cả Phê duyệt và Từ chối không? | **CÓ (DÙNG CHUNG CẢ HAI)** | [`detail.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/detail.html#L381-L392): Nút `btn-director-reject` và `btn-director-approve` đều đọc nội dung từ thẻ `commentsEl = document.getElementById('directorDetailComments')`. |
| **6** | Việc thêm `maxlength="500"` có ảnh hưởng đến các luồng công việc hợp lệ hiện có không? | **CÓ ẢNH HƯỞNG (GÂY MẤT DỮ LIỆU KHI PASTE)** | Nếu Giám đốc dán (paste) hoặc soạn thảo một lý do từ chối chi tiết dài 600–1000 ký tự (vốn hoàn toàn hợp lệ theo backend `@Size(max = 2000)`), trình duyệt sẽ âm thầm cắt đứt (truncate) đoạn văn bản tại ký tự 500 mà không thông báo, làm mất dữ liệu của người dùng. |
| **7** | Các template hoặc hàm JS khác có phụ thuộc vào phần tử này không? | **CÓ PHỤ THUỘC TRỰC TIẾP** | `offers.js` gọi phần tử tại 3 vị trí: dòng 1701 (`scrollToDirectorDecision`), dòng 1746 (`showDirectorRejectReasonRequiredAlert`), dòng 1773 (`submitDirectorDecisionFromScreen`). |
| **8** | Toàn bộ 223 test case của phân hệ Offer có đang PASS trước khi can thiệp không? | **100% PASS** | Đã thực thi qua Maven Surefire: 223 tests, 0 failures, 0 errors, 0 skipped (53.50s). |

---

## 2. Quyết định An toàn (Phase 2 — Safety Decision)

Dựa trên các tiêu chí phân loại của Task 03:
* **SAFE:** Thuộc tính HTML native tạo ra **hành vi quan sát được tương đương hoàn toàn (equivalent observable behavior)** với hiện trạng.
* **BLOCKED:** Chức năng hiện hữu sẽ bị thay đổi (Existing functionality would change).
* **CONDITIONAL:** Cần phải có các thay đổi phụ trợ khác đi kèm.

### Đánh giá Chuyên sâu:
1. **Hành vi Hiện tại:** Người dùng có thể nhập và gửi thành công văn bản có độ dài từ 1 đến 2000 ký tự trên màn hình chi tiết Offer.
2. **Hành vi sau khi thêm `maxlength="500"`:** Người dùng bị chặn cứng ở ký tự 500. Bất kỳ nội dung nào từ 501 đến 2000 ký tự sẽ bị từ chối/cắt bỏ ngay trên trình duyệt.
3. **Kết luận về Tính tương đồng hành vi (Behavior Parity):** Hai hành vi này **HOÀN TOÀN KHÔNG TƯƠNG ĐƯƠNG**. Việc thêm `maxlength="500"` sẽ phá vỡ tính tương thích ngược đối với các văn bản giải trình chi tiết của Giám đốc (vốn đã được backend thiết kế hỗ trợ tối đa 2000 ký tự).

> [!IMPORTANT]
> **PHÂN LOẠI AN TOÀN:** **BỊ CHẶN (BLOCKED)**  
> Theo quy tắc bắt buộc: *"If CONDITIONAL, BLOCKED or ALREADY IMPLEMENTED, stop and report without modifying application source code. Never prioritize reducing JavaScript over preserving existing functionality."*  
> Nhà phát triển **DỪNG LẠI NGAY LẬP TỨC**, bảo toàn nguyên vẹn 100% mã nguồn ứng dụng.

---

## 3. Mã nguồn Thay đổi Thực tế (Phase 3 — Code Changes)
* **Số tệp tin bị sửa đổi:** **0 tệp tin**.
* **Số dòng mã bị sửa đổi:** **0 dòng**.
* Không có mã JavaScript, HTML, CSS hay Backend nào bị can thiệp.

---

## 4. Kết quả Kiểm thử Nghiệm thu & Hồi quy (Phase 4 — Regression Verification)

| Bộ kiểm thử (Test Suite) | Số test | Kết quả thực thi Maven | Trạng thái |
| :--- | :---: | :---: | :---: |
| `CandidateOfferWebTests` | 19 | 19 PASS, 0 Fail, 0 Skip | **PASS** |
| `OfferControllerBugTests` | 6 | 6 PASS, 0 Fail, 0 Skip | **PASS** |
| `OfferExportServiceTests` | 10 | 10 PASS, 0 Fail, 0 Skip | **PASS** |
| `OfferIntegrationTests` | 34 | 34 PASS, 0 Fail, 0 Skip | **PASS** |
| `CandidateOfferQueryTests` | 5 | 5 PASS, 0 Fail, 0 Skip | **PASS** |
| `CandidateOfferServiceTests` | 35 | 35 PASS, 0 Fail, 0 Skip | **PASS** |
| `OfferServiceTests` | 114 | 114 PASS, 0 Fail, 0 Skip | **PASS** |
| **Tổng cộng** | **223** | **223 PASS (0 Fail, 0 Error, 0 Skip)** | **100% PASS** |

### Xác minh Trực quan & Hành vi Nghiệp vụ:
* **Comment rỗng khi Phê duyệt:** PASS (Cho phép, backend ghi nhận `Director_Approved`).
* **Comment rỗng khi Từ chối:** PASS (Bị chặn bởi JS và backend `OfferController`, báo viền đỏ và thông báo lỗi).
* **Comment 499 & 500 ký tự:** PASS (Được chấp nhận hoàn toàn trên cả client và server).
* **Comment 501 đến 2000 ký tự:** **ĐƯỢC BẢO LƯU CHỨC NĂNG NGUYÊN BẢN** (Không bị cắt cụt do quyết định kích hoạt Stop Rule của Phase 2).
* **Trình duyệt tự động (Browser Automation):** **NOT VERIFIED (Chưa chạy trên Headless Browser thực tế)** — Đánh dấu trung thực theo yêu cầu Phase 4.

---

## 5. Kiểm tra Cách ly Mã nguồn (Phase 5 — Change Isolation)
* Lệnh thực thi: `git status` và `git diff --check`
* Kết quả: Cây làm việc sạch sẽ hoàn toàn đối với mọi tracked files. Các thay đổi và tài liệu kiểm định tiền nhiệm được bảo vệ nguyên vẹn.

---

## 6. Tổng kết & Đề xuất Ứng viên Tiếp theo cho Kế hoạch Chuyển đổi

* **Số dòng JavaScript đã lược bỏ:** **0 dòng** (Bảo toàn nguyên lý: *Không bao giờ ưu tiên giảm JavaScript hơn việc bảo toàn tính đúng đắn của nghiệp vụ*).
* **Rủi ro còn tồn tại nếu cố tình sửa đổi:** Gây ức chế trải nghiệm cho Giám đốc khi nhập văn bản dài giải thích lý do từ chối đề xuất lương cao, dẫn đến lỗi cắt ngắn dữ liệu ngầm.
* **Ứng viên Chuyển đổi Thực sự An toàn Tiếp theo (Next Candidate for HTML/CSS Conversion):**
  * **Ứng viên:** **Thiết lập Mốc Ngày bắt đầu tối thiểu (`expectedStartDate`) tại thời điểm tải trang bằng Thymeleaf native.**
  * **Vị trí:** [`src/main/resources/templates/offers/form.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/form.html)
  * **Giải pháp:** Thêm thuộc tính native `th:min="${#temporals.format(#temporals.createNow().plusDays(1), 'yyyy-MM-dd')}"`.
  * **Đặc tính an toàn:** Hoàn toàn không làm thay đổi hành vi người dùng, không cắt ngắn dữ liệu, không ảnh hưởng API contract, và đóng vai trò như một lớp phòng thủ chiều sâu (defense-in-depth) bổ sung cho form tạo Offer.

