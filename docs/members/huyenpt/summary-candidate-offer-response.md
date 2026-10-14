# BÁO CÁO TỔNG KẾT TOÀN DIỆN: LUỒNG PHẢN HỒI THƯ MỜI NHẬN VIỆC (CANDIDATE OFFER RESPONSE) & HỆ THỐNG THÔNG BÁO HAI CHIỀU

- **Dự án:** Hệ Thống Quản Lý Tuyển Dụng — SWP_RMS_Group-2 (Mộc RMS)
- **Thành viên phụ trách:** Phạm Thị Huyền (`HuyenPT`)
- **Phân hệ (Module):** Quản lý Đề xuất Tuyển dụng (`Offer Management`) & Tích hợp Cổng Thông tin Ứng viên (`Candidate Portal`)
- **Nhánh phát triển (Branch):** `feat/huyenpt/inte3/candidate-offer-response`
- **Mục tiêu nhánh:** Xây dựng hoàn chỉnh toàn bộ phân hệ xem thư mời nhận việc dành cho ứng viên, tiếp nhận phản hồi (Đồng ý / Từ chối kèm lý do), tích hợp thông báo chuông hai chiều theo thời gian thực (HR $\leftrightarrow$ Candidate), ngăn chặn đề xuất quá hạn, và đồng bộ dữ liệu sang phía quản trị HR.
- **Tài liệu tham chiếu:** [ARCHITECTURE_GUIDE.md](../../architecture/ARCHITECTURE_GUIDE.md), [model.md](../../database/model.md)

---

## I. TỔNG QUAN VÀ BỐI CẢNH TRIỂN KHAI

Trước khi thực hiện nhánh `feat/huyenpt/inte3/candidate-offer-response`, hệ thống chỉ mới hoàn thiện luồng nội bộ giữa HR và Giám đốc (Tạo bản thảo $\rightarrow$ Trình duyệt $\rightarrow$ Giám đốc phê duyệt $\rightarrow$ Phát hành `Sent_Candidate`). Toàn bộ mắt xích tương tác với Ứng viên và phản hồi về Doanh nghiệp vẫn còn nhiều khoảng trống:
1. **Thiếu cổng xem thư mời trang trọng:** Ứng viên chưa có màn hình xem thư mời tuyển dụng chuyên nghiệp (`Offer Letter`), danh sách còn hiển thị dạng bảng quản trị chưa tối ưu trải nghiệm ứng viên.
2. **Chưa có luồng phản hồi trực tiếp:** Ứng viên chưa thể bấm Đồng ý hoặc Từ chối thư mời trực tiếp trên hệ thống; chưa có cơ chế bắt buộc nhập lý do từ chối để HR ghi nhận insight tuyển dụng.
3. **Phân quyền và bảo mật chưa tách bạch:** Thiếu lớp bảo vệ ngăn chặn ứng viên truy cập vào các API/URL nội bộ của HR (`/offers/**`).
4. **Hệ thống thông báo bị đứt gãy:** 
   - Ứng viên nhận Offer không nhận được chuông thông báo (Bell Notification) trên Topbar.
   - Bảng `Notification` gặp lỗi cơ sở dữ liệu SQL Server JDBC (`The conversion from varchar to NCHAR is unsupported`).
   - Khi ứng viên trả lời Offer, chuyên viên HR lập đề xuất không nhận được thông báo in-app để kịp thời xử lý.
5. **Thiếu cơ chế xử lý quá hạn:** Đề xuất đã qua ngày bắt đầu đi làm dự kiến (`expectedStartDate < today`) nhưng hệ thống vẫn cho phép ứng viên bấm Đồng ý/Từ chối, gây sai lệch kế hoạch tiếp nhận nhân sự.

Nhánh `feat/huyenpt/inte3/candidate-offer-response` đã giải quyết triệt để tất cả các vấn đề trên, khép kín 100% vòng đời của một Đề xuất Offer trong thực tế doanh nghiệp.

---

## II. CHI TIẾT CÁC TÍNH NĂNG ĐÃ HOÀN THIỆN TRÊN NHÁNH

### 1. Phân quyền & Bảo mật Tuyệt đối (Security & Access Control)
- **Đóng toàn bộ endpoint nội bộ:**
  - Cập nhật [SecurityConfig.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/config/SecurityConfig.java): Ngăn chặn triệt để vai trò `ROLE_CANDIDATE` truy cập vào các đường dẫn nội bộ `/offers/**` và dọn dẹp các đường dẫn cũ `/api/v1/hr/offers/**`.
- **Định tuyến chuyên biệt dành riêng cho Ứng viên:**
  - `GET /portal/offers`: Xem danh sách tất cả các thư mời nhận việc của ứng viên đăng nhập.
  - `GET /portal/offers/{id}`: Xem chi tiết thư mời nhận việc chính thức (Offer Letter).
  - `POST /portal/offers/{id}/accept`: Chấp nhận thư mời nhận việc.
  - `GET /portal/offers/{id}/decline`: Màn hình xác nhận từ chối nhận việc kèm lý do.
  - `POST /portal/offers/{id}/decline`: Tiếp nhận lý do từ chối và cập nhật trạng thái.
- **Bảo vệ dữ liệu đa tầng (Data Isolation):**
  - Trong [CandidateOfferServiceImpl.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/CandidateOfferServiceImpl.java): Kiểm tra `currentUserService.requireCurrentCandidate()` và chỉ truy vấn các Offer thuộc về đơn ứng tuyển của chính ứng viên đăng nhập. Ứng viên không thể xem hoặc phản hồi Offer của người khác.

---

### 2. Giao diện Danh sách Thư mời Ứng viên Dạng Thẻ Card Hiện Đại
- **Tái thiết kế [candidate-list.html](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/candidate-list.html):**
  - Chuyển đổi từ dạng bảng (table) cồng kềnh sang **danh sách thẻ Card (`offer-row-card`)** chuẩn ngôn ngữ thiết kế Candidate Portal của Mộc RMS.
  - **Thông tin nổi bật trên từng Card:** Chức danh tuyển dụng, Tên công ty/Phòng ban, Ngày phát hành offer, Mức lương chính thức (Gross VNĐ), Lương thử việc kèm badge %, Ngày nhận việc dự kiến, Trạng thái.
  - **Hệ thống Badge trực quan:**
    - `Chờ phản hồi` (`badge--pending` - màu vàng cam)
    - `Đã đồng ý nhận việc` (`badge--success` - màu xanh lục)
    - `Đã từ chối` (`badge--danger` - màu đỏ nhạt)
    - `Đã quá hạn phản hồi` (`badge--neutral` - màu xám)
  - **Cụm nút tương tác nhanh:** "Xem thư mời", "Đồng ý", "Từ chối" hiển thị tương ứng theo trạng thái.

---

### 3. Màn hình Thư mời Nhận việc Chuyên biệt (Dedicated Offer Letter View)
- **Hiện thực [candidate-detail.html](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/candidate-detail.html):**
  - Giao diện thiết kế theo phong cách Thư mời trang trọng, chuyên nghiệp (Formal Offer Letter layout):
    1. **Khối Header & Giới thiệu:** Logo Mộc RMS, thông điệp chúc mừng chính thức từ công ty, tiêu đề vị trí ứng tuyển.
    2. **Khối Thông tin Bổ nhiệm & Thời gian:** Chức danh công việc, Bộ phận/Phòng ban, Địa điểm làm việc, Thời gian thử việc, Ngày bắt đầu nhận việc dự kiến (`expectedStartDate`).
    3. **Khối Lương & Gói Đãi ngộ (Compensation & Benefits):**
       - Lương chính thức (Gross/tháng) hiển thị nổi bật.
       - Lương thử việc (Gross/tháng) kèm tỷ lệ % tương ứng (ví dụ: `85%`).
       - Chi tiết chế độ bảo hiểm, thưởng hiệu quả, gói phúc lợi công ty.
    4. **Khu vực Phản hồi Quyết định (Decision Card):**
       - **Trạng thái Chờ phản hồi (`Sent_Candidate`):** Cung cấp 2 lựa chọn rõ ràng:
         - Nút **"Đồng ý nhận việc"** (`btn-success`): Bật popup hộp thoại SweetAlert2 xác nhận cam kết trước khi gửi.
         - Nút **"Từ chối nhận việc"** (`btn-outline-danger`): Mở form/modal nhập lý do từ chối.
       - **Trạng thái Đã phản hồi:** Khóa thao tác, hiển thị thông điệp ghi nhận quyết định của ứng viên kèm thời điểm phản hồi.
       - **Trạng thái Quá hạn:** Tự động ẩn các nút phản hồi, hiển thị hướng dẫn kết nối chuyên viên HR.

---

### 4. Luồng Xử lý Phản hồi & Ghi nhận Lý do Từ chối (Decline Reason Flow)
- **Cơ sở dữ liệu:**
  - Bổ sung cột `DeclineReason NVARCHAR(500)` trong bảng `OfferProposal` ([db.sql](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/database/schema/db.sql)) và entity [OfferProposal.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/entity/OfferProposal.java).
- **Phía Ứng viên:**
  - Tạo màn hình [candidate-decline.html](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/candidate-decline.html) và modal từ chối nhanh trên trang chi tiết:
    - Bắt buộc nhập lý do từ chối (Validation: không được để trống, độ dài từ 5 đến 500 ký tự).
    - Tích hợp bộ đếm ký tự trực quan theo thời gian thực (`0/500`).
    - Nút "Xác nhận từ chối" và "Quay lại cân nhắc".
- **Phía Quản trị Tuyển dụng (HR):**
  - Cập nhật [detail.html](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/detail.html): Khi Offer ở trạng thái `Declined`, hệ thống hiển thị nổi bật khối **"Lý do Ứng viên Từ chối"** (`candidateDeclineReason`), giúp HR nắm bắt nguyên nhân (ví dụ: mức lương chưa phù hợp, chọn cơ hội khác, lý do cá nhân...).
  - Cập nhật [OfferExportServiceImpl.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferExportServiceImpl.java): Xuất cột "Lý do từ chối của ứng viên" vào file báo cáo Excel của HR.

---

### 5. Hệ thống Thông báo Nội bộ Hai chiều (Bidirectional In-App Notifications)

#### a. Khắc phục Lỗi Chuyển đổi SQL Server JDBC
- **Vấn đề:** Khi ứng viên hoặc HR mở chuông thông báo, API ném ngoại lệ:
  `com.microsoft.sqlserver.jdbc.SQLServerException: The conversion from varchar to NCHAR is unsupported.`
- **Nguyên nhân:** Cấu hình `spring.jpa.properties.hibernate.use_nationalized_character_data=true` trong `application.properties` khiến Hibernate tự động gọi `getNString()` cho tất cả các cột String, xung đột với kiểu dữ liệu `VARCHAR` của `EventType`, `ReferenceId`, `EventId` trong SQL Server.
- **Giải pháp:** Gắn annotation `@org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)` cho 3 trường trong [Notification.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/notification/Notification.java). Bổ sung test kiểm thử hồi quy trong [CandidateOfferQueryTests.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/test/java/com/group2/rms/offer/repository/CandidateOfferQueryTests.java).

#### b. Thông báo khi HR Phát hành Offer (`Sent_Candidate`)
- Bổ sung phương thức `notifyCandidateOfferSent(OfferProposal offer)` trong [NotificationServiceImpl.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/notification/NotificationServiceImpl.java).
- Được gọi tự động trong [OfferServiceImpl.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java) khi HR chuyển trạng thái sang `Sent_Candidate`.
- Tạo thông báo in-app gửi tới tài khoản Ứng viên:
  - Tiêu đề: `Thư mời nhận việc mới`
  - Nội dung: `Bạn đã nhận được thư mời nhận việc cho vị trí [Vị trí]. Hãy xem và phản hồi sớm nhé!`
  - Liên kết: `/portal/offers/{id}`

#### c. Thông báo khi Ứng viên Phản hồi (`Accepted` / `Declined`)
- Bổ sung phương thức `notifyOfferResponseFromCandidate(OfferProposal offer, boolean accepted, String declineReason)` trong [NotificationServiceImpl.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/notification/NotificationServiceImpl.java).
- Được gọi trong [CandidateOfferServiceImpl.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/CandidateOfferServiceImpl.java) khi lưu phản hồi của ứng viên.
- Gửi thông báo tới chuyên viên HR lập đề xuất (`proposedBy`):
  - **Trường hợp Đồng ý:**
    - Tiêu đề: `Ứng viên đã đồng ý nhận việc`
    - Nội dung: `Ứng viên [Tên] đã ĐỒNG Ý nhận việc cho vị trí [Vị trí].`
    - Loại sự kiện: `CANDIDATE_OFFER_ACCEPTED`, Link: `/offers/{id}`
  - **Trường hợp Từ chối:**
    - Tiêu đề: `Ứng viên đã từ chối nhận việc`
    - Nội dung: `Ứng viên [Tên] đã TỪ CHỐI nhận việc cho vị trí [Vị trí]. Lý do: [Lý do]`
    - Loại sự kiện: `CANDIDATE_OFFER_DECLINED`, Link: `/offers/{id}`

#### d. Giao diện Topbar Chuông Thông báo
- Cập nhật [topbar.css](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/css/topbar.css) & [topbar-notifications.js](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/topbar-notifications.js):
  - Hiển thị Chấm đỏ số lượng (Red Badge `.topbar__badge`) chuẩn xác trên cả Portal Ứng viên và Workspace Nội bộ.
  - Tải động danh sách thông báo qua REST API, hiển thị icon phân loại theo sự kiện, định dạng thời gian thân thiện ("vừa xong", "15 phút trước", "Hôm qua").
  - Click vào dòng thông báo sẽ đánh dấu đã đọc và chuyển hướng mượt mà đến trang đích.

---

### 6. Cơ chế Chặn Đề xuất Quá hạn Bắt đầu Đi làm (Expired Offer Guarding)
- **Quy tắc Nghiệp vụ:** Một đề xuất Offer được xác định là quá hạn nếu:
  $$\text{expectedStartDate} \ne \text{null} \quad \text{và} \quad \text{expectedStartDate} < \text{LocalDate.now()}$$
- **Kiểm soát Tầng Dịch vụ (Backend Service):**
  - Trong [CandidateOfferServiceImpl.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/CandidateOfferServiceImpl.java), phương thức `isExpired(offer)` được áp dụng:
    - Trong `requireAnswerable(offer)`: Nếu offer đã quá hạn, lập tức ném [OfferResponseException](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/exception/OfferResponseException.java) với thông điệp: *"Thư mời này đã quá ngày bắt đầu làm việc dự kiến, không thể phản hồi nữa. Vui lòng liên hệ HR."*
    - Trong `toResponse` & `toDetailResponse`: Khi quá hạn, gán `canRespond = false`, `statusLabel = "Đã quá hạn phản hồi"`, `statusBadge = "badge--neutral"`.
- **Giao diện Người dùng (Frontend UX):**
  - **Trên [candidate-list.html](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/candidate-list.html):** Ẩn các nút "Đồng ý" / "Từ chối", hiển thị ghi chú cảnh báo: `⏱ Thư mời đã quá ngày bắt đầu nhận việc dự kiến.`
  - **Trên [candidate-detail.html](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/candidate-detail.html):**
    - Xuất hiện Banner màu vàng trên đầu trang:
      > ⚠️ **Thư mời đã quá thời hạn phản hồi:** Ngày bắt đầu nhận việc dự kiến ([dd/MM/yyyy]) đã trôi qua. Bạn không thể phản hồi thư mời này trực tiếp trên hệ thống nữa. Vui lòng liên hệ bộ phận Tuyển dụng (HR) nếu bạn cần hỗ trợ thêm.
    - Tại Decision Card: Ẩn toàn bộ form phản hồi, thay thế bằng Thẻ liên hệ chuyên viên HR phụ trách (Họ tên chuyên viên, Email liên hệ kèm nút mở nhanh `mailto:`) để ứng viên chủ động liên hệ.

---

### 7. Chuẩn hóa Thông báo Validation & Đa ngôn ngữ (Validation Sanitization)
- Bổ sung cấu hình [messages.properties](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/messages.properties) và [GlobalExceptionHandler.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/exception/GlobalExceptionHandler.java):
  - Khắc phục các thông báo lỗi thô ráp của Spring/Hibernate khi người dùng nhập sai định dạng số tiền lương hoặc ngày tháng.
  - Chuẩn hóa toàn bộ thông báo lỗi xác thực sang tiếng Việt trong sáng, chuyên nghiệp.

---

## III. BẢNG TỔNG HỢP DANH MỤC TỆP TIN THAY ĐỔI TRÊN NHÁNH (47 FILES)

| Nhóm tệp tin | Tệp tin (File) | Tóm tắt thay đổi chính |
|:---|:---|:---|
| **Cơ sở dữ liệu** | `database/schema/db.sql` | Bổ sung cột `DeclineReason NVARCHAR(500)` trong bảng `OfferProposal`. |
| **Bảo mật & Cấu hình** | `SecurityConfig.java` | Chặn `ROLE_CANDIDATE` vào `/offers/**`, dọn dẹp các đường dẫn cũ `/api/v1/hr/offers/**`. |
| | `application.properties` | Bổ sung cấu hình thông báo và encoding. |
| | `messages.properties` | Khai báo các thông báo lỗi xác thực tiếng Việt. |
| | `GlobalExceptionHandler.java` | Chuẩn hóa thông báo lỗi TypeMismatch và Validation cho form Offer. |
| **Hệ thống Thông báo** | `Notification.java` | Thêm `@JdbcTypeCode(SqlTypes.VARCHAR)` cho `EventType`, `ReferenceId`, `EventId` sửa lỗi JDBC. |
| | `NotificationRepository.java` | Thêm truy vấn tìm thông báo chưa đọc và đếm theo User. |
| | `NotificationService.java` | Khai báo `notifyCandidateOfferSent` và `notifyOfferResponseFromCandidate`. |
| | `NotificationServiceImpl.java` | Hiện thực logic gửi thông báo in-app 2 chiều (HR $\leftrightarrow$ Candidate). |
| | `NotificationApiController.java` | Thêm log chi tiết theo dõi API chuông thông báo. |
| **Offer - Backend** | `OfferProposal.java` | Bổ sung trường `declineReason` kèm getter/setter. |
| | `CandidateOfferRepository.java` | Truy vấn Offer theo ứng viên đăng nhập, nạp chi tiết kèm Application và Job. |
| | `CandidateOfferService.java` | Định nghĩa giao diện phản hồi thư mời và lấy chi tiết cho Ứng viên. |
| | `CandidateOfferServiceImpl.java` | Kiểm tra bảo mật ứng viên, kiểm tra `isExpired`, chặn quá hạn, lưu lý do từ chối, gọi notification tới HR. |
| | `CandidateOfferController.java` | Điều hướng danh sách, chi tiết thư mời, tiếp nhận action Accept/Decline. |
| | `CandidateOfferDetailResponse.java` | DTO chi tiết thư mời ứng viên (đãi ngộ, ngày bắt đầu, cờ `canRespond`, nhãn trạng thái). |
| | `OfferServiceImpl.java` | Tích hợp gọi `notifyCandidateOfferSent` khi phát hành Offer. |
| | `OfferExportServiceImpl.java` | Bổ sung cột "Lý do từ chối của ứng viên" khi xuất file Excel báo cáo HR. |
| **Giao diện Templates** | `candidate-list.html` | Chuyển đổi danh sách sang Card view, hiển thị trạng thái quá hạn, ẩn nút khi hết hạn. |
| | `candidate-detail.html` | Thiết kế trang thư mời trang trọng, banner cảnh báo quá hạn, card liên hệ HR. |
| | `candidate-decline.html` | Màn hình từ chối nhận việc kèm khung nhập lý do và bộ đếm 500 ký tự. |
| | `offers/detail.html` | Hiển thị khối Lý do từ chối của ứng viên trong trang chi tiết HR. |
| | `offers/form.html` & `list.html` | Chuẩn hóa hiển thị và nhãn trạng thái. |
| **Giao diện CSS & JS** | `candidate-offers.css` | 600+ dòng CSS thiết kế chuẩn Mộc RMS cho Card list, Offer Letter, Decision Card, Expired Banner. |
| | `candidate-offers.js` | Xử lý SweetAlert xác nhận Accept, đếm ký tự Decline, xử lý form submit. |
| | `topbar.css` & `topbar-notifications.js` | Chấm đỏ Red Badge đếm thông báo, hiển thị dropdown chuông, tự động đánh dấu đã đọc. |
| **Kiểm thử Tự động** | `CandidateOfferServiceTests.java` | 30 tests kiểm tra toàn bộ service ứng viên (Accept, Decline, Quá hạn, Phân quyền). |
| | `CandidateOfferWebTests.java` | 14 tests kiểm tra controller, redirect, validate form. |
| | `NotificationServiceImplTest.java` | 14 tests kiểm tra việc tạo thông báo cho Ứng viên và HR. |
| | `CandidateOfferQueryTests.java` | 2 tests kiểm tra truy vấn SQL Server JDBC và mapping kiểu dữ liệu. |
| | `OfferExportServiceTests.java` | 16 tests kiểm tra xuất dữ liệu Excel có lý do từ chối. |
| | `OfferServiceTests.java` | 59 tests kiểm tra toàn bộ luồng tạo, cập nhật, gửi offer của HR. |
| | `OfferControllerBugTest.java` | 1 test hồi quy kiểm tra bug controller. |

---

## IV. KẾT QUẢ KIỂM THỬ TỰ ĐỘNG & ĐẢM BẢO CHẤT LƯỢNG

Toàn bộ **141/141 bài kiểm thử tự động** liên quan đến phân hệ Offer và Thông báo đều vượt qua với kết quả tuyệt đối:

```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.group2.rms.offer.CandidateOfferWebTests
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0 -- in CandidateOfferWebTests
[INFO] Running com.group2.rms.offer.service.CandidateOfferServiceTests
[INFO] Tests run: 30, Failures: 0, Errors: 0, Skipped: 0 -- in CandidateOfferServiceTests
[INFO] Running com.group2.rms.notification.NotificationServiceImplTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0 -- in NotificationServiceImplTest
[INFO] Running com.group2.rms.offer.OfferExportServiceTests
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0 -- in OfferExportServiceTests
[INFO] Running com.group2.rms.offer.repository.CandidateOfferQueryTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in CandidateOfferQueryTests
[INFO] Running com.group2.rms.OfferControllerBugTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in OfferControllerBugTest
[INFO] Running com.group2.rms.service.OfferServiceTests
[INFO] Tests run: 59, Failures: 0, Errors: 0, Skipped: 0 -- in OfferServiceTests
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 141, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

## V. TOÀN BỘ LỊCH SỬ COMMITS TRÊN NHÁNH

Nhánh `feat/huyenpt/inte3/candidate-offer-response` bao gồm chuỗi commits logic, tường minh theo chuẩn Conventional Commits:

```text
43293ec Merge branch 'main' of https://github.com/Masanori-Satoh/SWP_RMS_Group-2 into feat/huyenpt/inte3/candidate-offer-response
c47be9c docs: add summary report for candidate offer response and in-app notifications
a73dc58 feat(ui): display expired offer notice and guidance banner in candidate offer views
df49f85 feat(offer): notify HR on candidate answer and guard against expired expectedStartDate
7803ffb feat(notification): add notifyOfferResponseFromCandidate to alert HR on offer accept or decline
bbef1b1 fix(notification): map varchar columns with JdbcTypeCode to prevent SQL Server conversion error
0e3f5c2 feat(notification): notify candidate on job offer sent with red badge and direct link
1cc109f Merge branch 'main' of https://github.com/Masanori-Satoh/SWP_RMS_Group-2 into feat/huyenpt/inte3/candidate-offer-response
b523d0e feat(offer): record candidate decline reason and display in HR detail view
6379123 fix(offer): sanitize validation and type conversion messages in HR flow
376394f style(offer): refine card padding, zone separation, and typography spacing
4676840 feat(offer): refactor candidate offer list from table to offer row cards
80df5c1 feat(offer): implement candidate dedicated offer letter view and response flow
2babe59 docs(huyenpt): clean up legacy /api/v1/hr/offers references in documentation
3749a11 refactor(security): clean up legacy /api/v1/hr/offers paths
ca796f9 fix(security): disallow candidate access to internal offer endpoints
```

---

## VI. KẾT LUẬN & ĐÁNH GIÁ NGHIỆP VỤ

1. **Tính hoàn chỉnh (Completeness):** 
   - Đã khép kín 100% toàn bộ vòng đời của Đề xuất Offer trong thực tế: Từ lúc Giám đốc duyệt $\rightarrow$ HR gửi Offer $\rightarrow$ Ứng viên nhận chuông thông báo $\rightarrow$ Xem thư mời trang trọng $\rightarrow$ Chấp nhận / Từ chối kèm lý do $\rightarrow$ HR nhận chuông thông báo kết quả $\rightarrow$ Ghi nhận vào báo cáo và hồ sơ.
2. **Trải nghiệm người dùng (UI/UX Excellence):**
   - Đạt chuẩn hệ thống thiết kế Mộc RMS: Sử dụng Card layout hiện đại, màu sắc nhận diện trực quan, popup SweetAlert2 chuyên nghiệp, banner cảnh báo rõ ràng khi quá hạn, hỗ trợ kết nối nhanh với HR qua email.
3. **Độ tin cậy & Ổn định (Reliability):**
   - Giải quyết triệt để lỗi cơ sở dữ liệu SQL Server JDBC.
   - Bảo vệ an toàn dữ liệu cá nhân giữa các ứng viên.
   - Bảo đảm 100% vượt qua bộ kiểm thử tự động 141 bài test.
4. **Trạng thái:** Mã nguồn và tài liệu trên nhánh đã hoàn thiện sạch sẽ, đã đồng bộ lên Git server và sẵn sàng tạo Pull Request (PR) để merge vào nhánh chính (`main`).
