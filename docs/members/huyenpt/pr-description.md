# BÁO CÁO MERGE PULL REQUEST (PR DESCRIPTION)

- **Nhánh thực hiện (Branch):** `feat/huyenpt/candidate-offer-response-deadline`
- **Nhánh đích (Target Branch):** `main`
- **Thành viên phụ trách (Assignee):** Phạm Thị Huyền (`HuyenPT`)
- **Phân hệ (Module):** Quản lý Đề xuất Tuyển dụng (`Offer Proposal Management`), Luồng Phê duyệt của Giám đốc (`Director Review & Decision`), và Cổng Phản hồi Ứng viên (`Candidate Offer Response Portal`)
- **Tiêu đề PR đề xuất:** `feat(offer): support response deadline, redesign director offer detail view and enhance flash notifications`
- **Tài liệu tham chiếu:** [ARCHITECTURE_GUIDE.md](../../architecture/ARCHITECTURE_GUIDE.md) · [model.md](../../database/model.md) · [summary-director-offer-approval.md](summary-director-offer-approval.md) · [summary-candidate-offer-response.md](summary-candidate-offer-response.md)

---

## 📌 TỔNG QUAN & MỤC TIÊU CỦA BRANCH (PULL REQUEST OVERVIEW)

Nhánh `feat/huyenpt/candidate-offer-response-deadline` hoàn thiện trọn vẹn 3 mục tiêu cốt lõi phục vụ quy trình phê duyệt và phát hành thư mời nhận việc chuyên nghiệp theo chuẩn doanh nghiệp thực tế:

1. **Thiết lập Thời hạn phản hồi Thư mời nhận việc (`ResponsePeriodDays`) & Kiểm soát Hạn chót (`ResponseDeadline`):**
   - Cho phép HR cấu hình số ngày ứng viên có hiệu lực phản hồi (1 - 60 ngày, mặc định 5 ngày).
   - Tự động tính toán mốc thời gian hạn chót chính xác khi HR phát hành Offer (`issueOfferToCandidate`).
   - Khóa tương tác Đồng ý/Từ chối trên Cổng ứng viên và hiển thị thông báo quá hạn rõ ràng khi quá hạn.
2. **Tái thiết kế Toàn diện Màn hình Xem chi tiết & Phê duyệt Offer của Giám đốc (Screen 32 - `offers/detail.html`):**
   - Chuyển đổi giao diện sang chuẩn Mockup hành chính lãnh đạo hiện đại: Tối ưu khối thông tin ứng viên thành thanh ngang tinh gọn, bổ sung nút pill "Xem CV" dẫn trực tiếp đến `/applications/{id}`.
   - Đưa thông tin Người phụ trách (Hiring Manager) lên ngang hàng với tiêu đề Nhóm 2.
   - Trưng bày 2 thẻ KPI Lương nổi bật (Lương chính thức & Lương thử việc kèm huy hiệu tỷ lệ % trực quan), 3 ô gạch thời gian (`date-tile`), khối địa điểm & đãi ngộ mềm.
   - Khu vực Quyết định của Giám đốc với nhận diện xanh lục đậm `#0f5132`, huy hiệu "CẤP DUYỆT CUỐI", bộ đếm ký tự `0/500` và modal SweetAlert2 xác nhận phê duyệt/từ chối.
3. **Chuẩn hóa Trải nghiệm Thông báo (Flash Notifications & Confirmation Modals):**
   - Thông báo Flash Alert dạng thanh trôi căn giữa phía trên màn hình, tự động hiển thị trong 2 giây và biến mất mượt mà với hiệu ứng slide-up fade out.
   - Thay thế toàn bộ hộp thoại `window.confirm()` thô sơ bằng modal giao diện tùy biến (gửi Offer cho ứng viên và từ chối đề xuất).

---

## 🚀 CHI TIẾT CÁC THAY ĐỔI CỐT LÕI (KEY CHANGES IMPLEMENTATION)

### 1. Cơ chế Thời hạn phản hồi & Tự động Tính Hạn chót Offer
- **Cơ sở dữ liệu (Database Schema & Seed Data):**
  - Bổ sung 2 cột mới vào bảng `OfferProposal` trong [database/schema/db.sql](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/database/schema/db.sql):
    ```sql
    ResponsePeriodDays INT NULL CONSTRAINT CK_OfferProposal_ResponsePeriodDays
    CHECK (
        ResponsePeriodDays IS NULL
        OR ResponsePeriodDays > 0
    ),
    ResponseDeadline DATETIME2 NULL,
    ```
  - Cập nhật [build_seed.py](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/database/seeds/build_seed.py) và [seed_data.sql](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/database/seeds/seed_data.sql) khởi tạo giá trị mẫu (`ResponsePeriodDays = 5`, `ResponseDeadline` tính toán theo ngày gửi thư mời).
- **Thực thể Backend & Data Transfer Objects (Entity & DTOs):**
  - Cập nhật [OfferProposal.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/entity/OfferProposal.java): Thêm trường `responsePeriodDays` và `responseDeadline`.
  - Cập nhật [CreateOfferRequest.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/dto/CreateOfferRequest.java) & [UpdateOfferRequest.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/dto/UpdateOfferRequest.java):
    - Khai báo trường `responsePeriodDays` với Bean Validation: `@Min(value = 1, message = "Thời hạn phản hồi tối thiểu là 1 ngày")`, `@Max(value = 60, message = "Thời hạn phản hồi tối đa là 60 ngày")`. Mặc định khởi tạo 5 ngày.
  - Cập nhật [OfferDetailResponse.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/dto/OfferDetailResponse.java) & [CandidateOfferDetailResponse.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/dto/CandidateOfferDetailResponse.java): Chuyển tải dữ liệu thời hạn và hạn chót phản hồi lên các giao diện HR, Director và Candidate.
- **Tầng Nghiệp vụ (Service Logic):**
  - Trong [OfferServiceImpl.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java):
    - Gán `responsePeriodDays` khi tạo mới hoặc cập nhật đề xuất.
    - Tại phương thức `issueOfferToCandidate`: Khi HR nhấn gửi thư mời, tự động tính mốc `responseDeadline = LocalDateTime.now().plusDays(periodDays)`.
  - Trong [CandidateOfferServiceImpl.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/CandidateOfferServiceImpl.java):
    - Bổ sung kiểm tra điều kiện hết hạn trước khi cho phép thực hiện thao tác chấp nhận (`acceptOffer`) hoặc từ chối (`declineOffer`): Kiểm tra cả `expectedStartDate < today` và `responseDeadline < now`.
    - Trả về thông báo lỗi thân thiện khi đề xuất đã quá hạn phản hồi.

---

### 2. Tái cấu trúc Toàn diện Giao diện Chi tiết & Duyệt Offer của Giám đốc (Screen 32)
- **Cập nhật Template [detail.html](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/detail.html):**
  - **Khối 1: Thông tin ứng viên (Candidate Header Card):**
    - Thiết kế lại thành thanh thông tin ngang tinh giản: Họ và tên, Email, Điện thoại, Mã hồ sơ `#APP-xxx`, Vị trí ứng tuyển.
    - Tích hợp nút dạng viên thuốc (pill button) **"Xem CV"** liên kết trực tiếp tới đường dẫn `/applications/{id}` (mở trang chi tiết hồ sơ ứng tuyển, CV và đánh giá phỏng vấn).
  - **Khối 2: Nội dung đề xuất Offer (Proposal Content Card):**
    - Đưa thông tin **Người phụ trách (Hiring Manager)** lên ngang hàng với tiêu đề Card: `2. NỘI DUNG ĐỀ XUẤT OFFER | Người phụ trách: [Tên HM]`.
    - Khối tóm tắt Đánh giá Phỏng vấn & Mức lương khuyến nghị từ Hiring Manager làm căn cứ đối sánh cho Director.
    - **2 Thẻ Lương KPI Nổi bật (`salary-kpi-card`):**
      - Thẻ Lương chính thức (VNĐ/tháng).
      - Thẻ Lương thử việc (VNĐ/tháng) kèm badge % trực quan (ví dụ: `85% lương chính thức`).
    - **3 Ô Gạch Thời gian (`date-tile`):**
      - Ngày dự kiến nhận việc (`ExpectedStartDate`).
      - Ngày lập đề xuất (`CreatedAt`).
      - Thời hạn phản hồi của ứng viên (`ResponsePeriodDays` & `ResponseDeadline`).
    - Thẻ mềm phụ: Địa điểm làm việc và Gói phúc lợi đãi ngộ.
  - **Khối 3: Quyết định của Giám đốc (Director Decision Panel):**
    - Tiêu đề sang trọng phong cách lãnh đạo: Nền màu xanh rừng đậm `#0f5132`, huy hiệu vàng `CẤP DUYỆT CUỐI`, mô tả thẩm quyền duyệt tối cao.
    - Khi ở trạng thái `Pending_Director`: Cung cấp trường nhập ý kiến chỉ đạo kèm bộ đếm ký tự trực tiếp `0/500`, 2 nút hành động phân cấp màu sắc rõ rệt (`[Phê duyệt đề xuất]` xanh lục và `[Từ chối đề xuất]` đỏ san hô).
    - Khi đã có quyết định (`Director_Approved` / `Director_Rejected` / etc.): Hiển thị thẻ thông báo quyết định trang trọng kèm tên người duyệt, thời gian và ý kiến chỉ đạo.
    - Khối Lịch sử Phê duyệt: Tự động hiển thị timeline đầy đủ khi tồn tại dữ liệu lịch sử.
- **Tối ưu CSS & JS ([offers.css](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/css/offers.css) & [offers.js](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/offers.js)):**
  - Xây dựng hệ thống class CSS chuẩn BEM: `.director-card`, `.candidate-meta-bar`, `.btn-view-cv`, `.salary-card`, `.date-tile-grid`, `.director-decision-card`.
  - Bộ đếm ký tự textarea thời gian thực không làm giật layout.

---

### 3. Chuẩn hóa Trải nghiệm Flash Notification & Custom Modal Xác nhận
- **Flash Alert Tự động Biến mất trong 2s:**
  - Định vị `.page-flash-container` ở vị trí cố định chính giữa mép trên màn hình (`position: fixed; top: 1.5rem; left: 50%; transform: translateX(-50%)`).
  - Độ rộng co giãn linh hoạt vừa khít nội dung (`width: fit-content; max-width: 90vw;`).
  - Tự động hiển thị nổi bật trong đúng 2 giây, sau đó áp dụng hiệu ứng mượt mà trượt lên mờ dần biến mất (`animation: flashFadeOut 0.4s ease-out forwards;`).
  - Đồng bộ trên toàn bộ các màn hình phân hệ Offer: [list.html](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/list.html), [detail.html](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/detail.html), [form.html](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/form.html).
- **Hiện đại hóa Hộp thoại Xác nhận (Custom SweetAlert2 Modals):**
  - **Modal gửi Offer cho ứng viên:** Thay thế `confirm()` mặc định bằng modal tùy biến chuyên nghiệp, hiển thị rõ họ tên ứng viên, vị trí và lưu ý về việc kích hoạt thời hạn phản hồi.
  - **Modal Giám đốc từ chối Offer:** Yêu cầu Giám đốc xác nhận lý do từ chối trước khi gửi form về hệ thống.
- **Đồng bộ Nhãn Điều hướng Cổng Ứng viên:**
  - Đổi tên mục menu trên sidebar của Ứng viên từ "Đề xuất nhận việc" sang **"Thư mời nhận việc"** nhằm mang lại cảm giác trang trọng và chuẩn mực.

---

## 📂 DANH SÁCH TỆP TIN THAY ĐỔI TRÊN NHÁNH (FILES CHANGED)

| STT | Tệp tin | Thao tác | Mô tả chi tiết thay đổi |
| :---: | :--- | :---: | :--- |
| 1 | `database/schema/db.sql` | Modified | Bổ sung 2 cột `ResponsePeriodDays` (CHECK > 0) và `ResponseDeadline` vào bảng `OfferProposal` |
| 2 | `database/seeds/build_seed.py` | Modified | Đồng bộ script sinh dữ liệu mẫu với thời hạn phản hồi mặc định 5 ngày |
| 3 | `database/seeds/seed_data.sql` | Modified | Thêm dữ liệu mẫu `ResponsePeriodDays` và `ResponseDeadline` cho các Offer mẫu |
| 4 | `src/main/java/com/group2/rms/offer/entity/OfferProposal.java` | Modified | Bổ sung thuộc tính `responsePeriodDays` và `responseDeadline` vào JPA Entity |
| 5 | `src/main/java/com/group2/rms/offer/dto/CreateOfferRequest.java` | Modified | Thêm trường `responsePeriodDays` kèm validation `@Min(1)`, `@Max(60)` |
| 6 | `src/main/java/com/group2/rms/offer/dto/UpdateOfferRequest.java` | Modified | Hỗ trợ cập nhật `responsePeriodDays` khi chỉnh sửa bản thảo đề xuất |
| 7 | `src/main/java/com/group2/rms/offer/dto/OfferDetailResponse.java` | Modified | Trả về thông tin thời hạn và hạn chót phản hồi cho HR và Director |
| 8 | `src/main/java/com/group2/rms/offer/dto/CandidateOfferDetailResponse.java` | Modified | Cung cấp dữ liệu thời hạn và hạn chót phản hồi cho Cổng ứng viên |
| 9 | `src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java` | Modified | Lưu thời hạn phản hồi, tự động tính `ResponseDeadline` khi phát hành Offer |
| 10 | `src/main/java/com/group2/rms/offer/service/CandidateOfferServiceImpl.java` | Modified | Kiểm tra điều kiện hết hạn phản hồi, chặn nhận/từ chối khi quá hạn |
| 11 | `src/main/java/com/group2/rms/offer/controller/OfferController.java` | Modified | Xử lý redirect và flash message thân thiện cho các kịch bản thao tác |
| 12 | `src/main/resources/templates/offers/form.html` | Modified | Thêm trường cấu hình thời hạn phản hồi (1 - 60 ngày), flash message chuẩn |
| 13 | `src/main/resources/templates/offers/detail.html` | Modified | Tái thiết kế màn hình Screen 32: header meta bar, pill "Xem CV", 2 KPI card, HM header, quyết định Director |
| 14 | `src/main/resources/templates/offers/list.html` | Modified | Căn giữa flash notification 2s auto-dismiss, đồng bộ hiển thị |
| 15 | `src/main/resources/templates/offers/candidate-detail.html` | Modified | Hiển thị hạn phản hồi, banner cảnh báo quá hạn, khóa nút khi hết hạn |
| 16 | `src/main/resources/templates/fragments/candidate-sidebar.html` | Modified | Đổi nhãn menu sang "Thư mời nhận việc" |
| 17 | `src/main/resources/static/css/offers.css` | Modified | Thêm toàn bộ style layout chi tiết Screen 32, KPI card, date tiles, flash alert 2s animation |
| 18 | `src/main/resources/static/js/offers.js` | Modified | Xử lý bộ đếm ký tự 0/500, modal custom SweetAlert2 cho gửi Offer và từ chối |
| 19 | `src/test/java/com/group2/rms/service/OfferServiceTests.java` | Modified | Cập nhật và bổ sung test cases kiểm tra hạn phản hồi và tính toán deadline |
| 20 | `src/test/java/com/group2/rms/offer/OfferExportServiceTests.java` | Modified | Sửa import JobPosting khớp gói kiến trúc mới sau khi merge main |
| 21 | `docs/members/huyenpt/pr-description.md` | Modified | Tài liệu báo cáo chi tiết Pull Request phục vụ merge |

---

## 🧪 KẾT QUẢ KIỂM THỬ & NGHIỆM THU (TEST RESULTS & VERIFICATION)

### 1. Kiểm thử Tự động (Automated Test Suite)
Đã chạy toàn bộ bộ kiểm thử đơn vị và tích hợp cho phân hệ Offer:
- **`OfferServiceTests`:** **61/61 tests PASS (100%)**, 0 failures, 0 errors.
  - Kiểm tra toàn bộ quy tắc tạo mới Offer `GBR-07` (chỉ tạo cho ứng viên Passed chưa có Offer).
  - Kiểm tra quy tắc sàn lương thử việc tối thiểu 85% lương chính thức `BR-OFF-01`.
  - Kiểm tra tính toán `ResponseDeadline` chính xác khi chuyển trạng thái sang `Sent_Candidate`.
  - Kiểm tra trạng thái khóa và các luồng chuyển đổi trạng thái hợp lệ.
- **`OfferExportServiceTests`:** **7/7 tests PASS (100%)**.
  - Kiểm tra xuất Excel với 3 scope: `FILTERED`, `SELECTED`, `ALL`.
  - Kiểm tra định dạng số tiền lương và tỷ lệ %, chuẩn hóa nhãn tiếng Việt và bảo mật phân quyền HR.
- **`OfferIntegrationTest`:** **9/9 tests PASS (100%)**.

### 2. Kịch bản Kiểm thử Thủ công (Manual Test Scenarios Verified)

| Kịch bản (Scenario) | Các bước thực hiện | Kết quả kỳ vọng | Trạng thái |
| :--- | :--- | :--- | :---: |
| **TC-01: Tạo Offer với thời hạn phản hồi tùy chỉnh** | 1. Đăng nhập HR -> Truy cập `/offers/create`<br>2. Chọn ứng viên Passed<br>3. Nhập thời hạn phản hồi = 7 ngày<br>4. Lưu bản thảo | Offer lưu thành công, trường `ResponsePeriodDays` = 7, `ResponseDeadline` = NULL (chưa phát hành). | **PASS** |
| **TC-02: Director xem màn hình chi tiết mới** | 1. Đăng nhập Giám đốc (`director@rms.com`)<br>2. Vào `/offers/{id}` | - Hiển thị thanh thông tin ứng viên gọn gàng.<br>- Nút "Xem CV" dẫn đến `/applications/{id}` mở trang ứng viên.<br>- Hiển thị "Người phụ trách: [Tên HM]" trên tiêu đề Nhóm 2.<br>- 2 thẻ Lương KPI và 3 ô gạch thời gian hiển thị đúng số liệu.<br>- Panel duyệt có nhận diện xanh đậm `#0f5132` và bộ đếm 0/500. | **PASS** |
| **TC-03: Director phê duyệt Offer** | 1. Nhập nhận xét phê duyệt<br>2. Bấm [Phê duyệt đề xuất]<br>3. Xác nhận SweetAlert2 | Trạng thái chuyển thành `Director_Approved`, flash alert xanh căn giữa hiện 2s rồi biến mất, lưu lịch sử duyệt. | **PASS** |
| **TC-04: HR phát hành Offer & tự động tính hạn chót** | 1. Đăng nhập HR -> Mở Offer đã duyệt<br>2. Bấm [Gửi thư mời cho ứng viên]<br>3. Xác nhận modal pop-up tùy biến | Offer chuyển sang `Sent_Candidate`, `ResponseDeadline` được tính chính xác bằng thời điểm gửi + số ngày cấu hình. | **PASS** |
| **TC-05: Ứng viên xem thư mời và hạn phản hồi** | 1. Đăng nhập Candidate<br>2. Vào `/portal/offers/{id}` | Hiển thị rõ thông tin hạn chót phản hồi. Nút Đồng ý và Từ chối hoạt động bình thường nếu còn trong hạn. | **PASS** |
| **TC-06: Khóa phản hồi khi quá hạn** | 1. Mở Offer có `ResponseDeadline` trước thời điểm hiện tại | Banner cảnh báo quá hạn hiển thị, các nút Đồng ý / Từ chối bị khóa, ngăn chặn gửi form. | **PASS** |
| **TC-07: Flash Notification 2 giây tự biến mất** | 1. Thực hiện bất kỳ thao tác nào tạo flash message (tạo, sửa, duyệt, từ chối) | Thông báo căn giữa màn hình, tự động biến mất sau đúng 2 giây với animation mượt mà. | **PASS** |

---

## 🔒 ĐÁNH GIÁ TÍNH TUÂN THỦ KIẾN TRÚC & RỦI RO (ARCHITECTURE & RISK ASSESSMENT)

1. **Tuân thủ Quy chuẩn Kiến trúc (`ARCHITECTURE_GUIDE.md`):**
   - **Tổ chức Package-by-Feature:** Toàn bộ code mới và sửa đổi nằm trong gói `com.group2.rms.offer` và `templates/offers/`.
   - **Mô hình Thin Controller:** Controller chỉ làm nhiệm vụ trích xuất tham số, kiểm tra quyền và gọi Service; logic tính hạn chót, kiểm tra trạng thái và lưu vết được bao gói chặt chẽ trong `OfferServiceImpl` và `CandidateOfferServiceImpl`.
   - **Không tạo file dư thừa:** Tối ưu và chỉnh sửa trực tiếp trên các file hiện có theo yêu cầu của dự án.
   - **Giao diện Mộc RMS:** Sử dụng Vanilla CSS thuần kết hợp các biến màu (CSS Variables) sẵn có, đảm bảo tốc độ tải trang nhanh và trải nghiệm đồng nhất.
2. **Đồng bộ với Nhánh Main:**
   - Đã merge đầy đủ các thay đổi mới nhất từ `main` (`821d190 Merge pull request #69`).
   - Xung đột bảng `OfferProposal` trong `database/schema/db.sql` đã được giải quyết triệt để (bảo toàn cả 2 cột `ResponsePeriodDays`, `ResponseDeadline` và cột `IsDeleted` của main).
   - Mã nguồn sạch, không còn xung đột git, sẵn sàng hợp nhất (`Ready to Merge`).
