# TỔNG KẾT NỘI DUNG HOÀN THIỆN LUỒNG PHẢN HỒI OFFER & HỆ THỐNG THÔNG BÁO (PHASE 3 - CANDIDATE OFFER RESPONSE & IN-APP NOTIFICATIONS)

- **Dự án:** Hệ Thống Quản Lý Tuyển Dụng — SWP_RMS_Group-2 (Mộc RMS)
- **Thành viên phụ trách:** Phạm Thị Huyền (`HuyenPT`)
- **Phân hệ (Module):** Quản lý Đề xuất Tuyển dụng (`Offer Management`) — Phase 3: Luồng Phản hồi Thư mời Nhận việc của Ứng viên (`Candidate Offer Response`) & Tích hợp Thông báo Nội bộ (`In-App Notification System`).
- **Nhánh phát triển (Branch):** `feat/huyenpt/inte3/candidate-offer-response`
- **Tài liệu tham chiếu:** [ARCHITECTURE_GUIDE.md](../../architecture/ARCHITECTURE_GUIDE.md), [model.md](../../database/model.md)

---

## I. BỐI CẢNH VÀ MỤC TIÊU TRIỂN KHAI

Tiếp nối Phase 1 (Cải thiện UI/UX và Xuất Excel) và Phase 2 (Luồng Phê duyệt Đề xuất của Giám đốc), Phase 3 tập trung giải quyết và khép kín **trải nghiệm người dùng 2 chiều giữa Ứng viên và Doanh nghiệp**:

1. **Chuông thông báo tức thời (Real-time In-App Notification):**
   - Đảm bảo ngay khi HR phát hành thư mời nhận việc (`Sent_Candidate`), ứng viên đăng nhập vào Portal sẽ nhận ngay chấm đỏ thông báo (Red Badge) trên biểu tượng chuông topbar, bấm vào hiển thị nội dung thông báo và có thể điều hướng trực tiếp đến thư mời `/portal/offers/{id}`.
   - Khắc phục triệt để lỗi cơ sở dữ liệu SQL Server JDBC (`The conversion from varchar to NCHAR is unsupported`) khiến chuông thông báo không tải được dữ liệu.
2. **Thông báo ngược cho HR khi Ứng viên Phản hồi (Enhancement 1):**
   - Khi ứng viên bấm "Đồng ý nhận việc" hoặc "Từ chối nhận việc" (kèm lý do chi tiết), hệ thống lập tức phát sinh thông báo in-app gửi thẳng tới chuyên viên HR lập đề xuất (`proposedBy`), kèm liên kết dẫn đến chi tiết Offer `/offers/{id}` để HR kịp thời thực hiện các thủ tục onboard hoặc bổ sung nhân sự.
3. **Cơ chế Chặn Đề xuất Quá hạn Bắt đầu Đi làm (Enhancement 2):**
   - Trường hợp ứng viên nhận được Offer nhưng ngày bắt đầu làm việc dự kiến (`expectedStartDate`) đã nhỏ hơn ngày hiện tại (`LocalDate.now()`), hệ thống sẽ tự động chuyển trạng thái hiển thị sang **"Đã quá hạn phản hồi"**.
   - Khóa toàn bộ các nút thao tác "Đồng ý" / "Từ chối" ở cả tầng Backend lẫn Frontend.
   - Hiển thị banner cảnh báo giải thích rõ lý do quá hạn và cung cấp thông tin liên hệ chuyên viên HR phụ trách (Họ tên, Email) để ứng viên có thể liên hệ làm rõ.
4. **Kiểm thử tự động & Tính toàn vẹn kiến trúc:**
   - Duy trì 100% tỷ lệ vượt qua tất cả các bộ kiểm thử tự động (Unit Test, Web Controller Test, Repository Query Test), không làm gãy vỡ các luồng nghiệp vụ hiện hữu.

---

## II. CHI TIẾT CÁC BƯỚC ĐÃ THỰC HIỆN VÀ CAM KẾT MÃ NGUỒN

### 1. Nền tảng Thông báo & Sửa lỗi SQL Server JDBC

- **Commit `0e3f5c2`:** `feat(notification): notify candidate on job offer sent with red badge and direct link`
  - Bổ sung `notifyCandidateOfferSent(OfferProposal offer)` trong `NotificationService` và `NotificationServiceImpl`.
  - Tự động liên kết tài khoản ứng viên thông qua `offer.getApplication().getCandidate().getUser()`.
  - Thiết lập định tuyến `/portal/offers/{id}` khi ứng viên click vào dòng thông báo trên chuông.
  - Cập nhật CSS/JS Topbar (`topbar.css`, `topbar-notifications.js`) hiển thị chấm đỏ badge đếm số lượng thông báo chưa đọc trên cả Portal Ứng viên và Workspace Nội bộ.
- **Commit `bbef1b1`:** `fix(notification): map varchar columns with JdbcTypeCode to prevent SQL Server conversion error`
  - **Nguyên nhân lỗi:** File `application.properties` cấu hình `spring.jpa.properties.hibernate.use_nationalized_character_data=true`. Khi Hibernate đọc các cột kiểu `VARCHAR` (`EventType`, `ReferenceId`, `EventId`) trong bảng `Notification` của SQL Server, driver ném ngoại lệ `The conversion from varchar to NCHAR is unsupported`.
  - **Giải pháp:** Bổ sung annotation `@org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)` cho 3 trường trong `Notification.java`.
  - Thêm log chi tiết trong `NotificationApiController.java` và bổ sung test hồi quy trong `CandidateOfferQueryTests.java`.

---

### 2. Bước 1: Thông báo In-App cho HR khi Ứng viên Phản hồi (Commit `7803ffb`)

- **Commit `7803ffb`:** `feat(notification): add notifyOfferResponseFromCandidate to alert HR on offer accept or decline`
- **Tầng Service (`NotificationService` & `NotificationServiceImpl`):**
  - Khai báo và hiện thực phương thức:
    ```java
    void notifyOfferResponseFromCandidate(OfferProposal offer, boolean accepted, String declineReason);
    ```
  - **Logic gửi thông báo:**
    - Lấy thông tin tài khoản người lập đề xuất: `User recipient = offer.getProposedBy()`.
    - Trích xuất họ tên ứng viên (`candidateName`) và vị trí tuyển dụng (`jobTitle`).
    - Nếu Ứng viên **Đồng ý**:
      - Tiêu đề: `Ứng viên đã đồng ý nhận việc`
      - Nội dung: `Ứng viên [candidateName] đã ĐỒNG Ý nhận việc cho vị trí [jobTitle].`
      - Loại sự kiện: `CANDIDATE_OFFER_ACCEPTED`, Link: `/offers/[id]`
    - Nếu Ứng viên **Từ chối**:
      - Tiêu đề: `Ứng viên đã từ chối nhận việc`
      - Nội dung: `Ứng viên [candidateName] đã TỪ CHỐI nhận việc cho vị trí [jobTitle]. Lý do: [declineReason]`
      - Loại sự kiện: `CANDIDATE_OFFER_DECLINED`, Link: `/offers/[id]`
- **Kiểm thử Unit Test:**
  - Viết 2 unit test mới trong `NotificationServiceImplTest.java` kiểm tra chặt chẽ cả 2 kịch bản Accept và Decline: 14/14 tests PASS.

---

### 3. Bước 2: Xử lý Nghiệp vụ Quá hạn & Tích hợp Service (Commit `df49f85`)

- **Commit `df49f85`:** `feat(offer): notify HR on candidate answer and guard against expired expectedStartDate`
- **Tầng Service (`CandidateOfferServiceImpl`):**
  - **Tích hợp Thông báo:** Gọi `systemNotificationService.notifyOfferResponseFromCandidate(offer, accepted, declineReason)` ngay sau khi lưu trạng thái mới `Accepted` hoặc `Declined`.
  - **Kiểm tra Quá hạn (`isExpired`):**
    ```java
    private boolean isExpired(OfferProposal offer) {
        return offer.getExpectedStartDate() != null && offer.getExpectedStartDate().isBefore(LocalDate.now());
    }
    ```
  - **Bảo vệ Nghiệp vụ (`requireAnswerable`):**
    - Kiểm tra `isExpired(offer)`. Nếu đã quá ngày dự kiến đi làm, lập tức chặn thao tác và ném ngoại lệ:
      ```java
      throw new OfferResponseException("Thư mời này đã quá ngày bắt đầu làm việc dự kiến, không thể phản hồi nữa. Vui lòng liên hệ HR.");
      ```
  - **Ánh xạ DTO (`toResponse` và `toDetailResponse`):**
    - Nếu `isExpired(offer)` là `true`:
      - Gán `canRespond = false` (ngăn cản việc render nút bấm).
      - Hiển thị nhãn trạng thái: `statusLabel = "Đã quá hạn phản hồi"`.
      - Gán badge trạng thái: `statusBadge = "badge--neutral"`.
  - **Đảm bảo Tương thích Ngược:**
    - Cung cấp overload constructor 3 tham số để giữ an toàn tuyệt đối cho các bài kiểm thử hoặc mã nguồn cũ chưa tiêm `systemNotificationService`.
- **Kiểm thử Unit Test:**
  - Bổ sung 5 kịch bản kiểm thử trong `CandidateOfferServiceTests.java`:
    - `answerOffer_whenAccepted_notifiesHr`
    - `answerOffer_whenDeclined_notifiesHrWithReason`
    - `answerOffer_whenExpired_throwsException`
    - `getOfferDetail_whenExpired_marksCannotRespondAndExpiredStatus`
    - `listOffers_whenExpired_marksExpiredStatus`
  - Toàn bộ 30/30 unit tests trong `CandidateOfferServiceTests` PASS (100%).

---

### 4. Bước 3: Hoàn thiện Giao diện Người dùng UI/UX (Commit `a73dc58`)

- **Commit `a73dc58`:** `feat(ui): display expired offer notice and guidance banner in candidate offer views`
- **Màn hình Danh sách Thư mời Ứng viên (`offers/candidate-list.html`):**
  - Khi Offer đã quá hạn (`!offer.canRespond && offer.statusLabel == 'Đã quá hạn phản hồi'`):
    - Hiển thị dòng ghi chú biểu tượng đồng hồ: `⏱ Thư mời đã quá ngày bắt đầu nhận việc dự kiến.`
    - Tự động ẩn cụm nút thao tác "Đồng ý" / "Từ chối", chỉ giữ nút "Xem chi tiết".
- **Màn hình Chi tiết Thư mời Ứng viên (`offers/candidate-detail.html`):**
  - **Banner cảnh báo trên đầu trang:**
    - Đặt ngay dưới tiêu đề thư mời khi quá hạn:
      > ⚠️ **Thư mời đã quá thời hạn phản hồi:** Ngày bắt đầu nhận việc dự kiến ([dd/MM/yyyy]) đã trôi qua. Bạn không thể phản hồi thư mời này trực tiếp trên hệ thống nữa. Vui lòng liên hệ bộ phận Tuyển dụng (HR) nếu bạn cần hỗ trợ thêm.
  - **Khu vực Quyết định Nhận việc (Decision Card):**
    - Ẩn hoàn toàn cụm nút "Đồng ý nhận việc" và "Từ chối nhận việc".
    - Hiển thị khối thông báo xám trang nhã (`alert--neutral`) giải thích lý do quá hạn.
    - Tích hợp Card thông tin liên hệ HR: Hiển thị họ tên chuyên viên HR phụ trách và email liên hệ kèm nút mở nhanh `mailto:` để ứng viên gửi email phản hồi trực tiếp.
- **Kiểm thử Web Controller:**
  - Xác minh toàn bộ 14/14 tests trong `CandidateOfferWebTests.java` PASS (100%).

---

### 5. Bước 4: Kiểm thử Hồi quy Toàn diện (Regression Testing)

- **Kết quả thực thi Maven Test Suite:**
  - `CandidateOfferQueryTests`: **2/2 PASS**
  - `OfferExportServiceTests`: **16/16 PASS**
  - `CandidateOfferWebTests`: **14/14 PASS**
  - `CandidateOfferServiceTests`: **30/30 PASS**
  - `NotificationServiceImplTest`: **14/14 PASS**
  - `OfferControllerBugTest`: **1/1 PASS**
  - `OfferServiceTests`: **59/59 PASS**
  - **Tổng số test cases kiểm thử:** **141/141 PASS (100%)**, `BUILD SUCCESS`, 0 failure, 0 error.

---

## III. BẢNG TỔNG HỢP DANH MỤC THAY ĐỔI THEO TỪNG FILE

| STT | Tệp tin (File) | Tầng kiến trúc | Tóm tắt thay đổi |
|:---:|:---|:---|:---|
| 1 | `Notification.java` | Entity | Thêm `@JdbcTypeCode(SqlTypes.VARCHAR)` cho `EventType`, `ReferenceId`, `EventId` sửa lỗi JDBC SQL Server. |
| 2 | `NotificationRepository.java` | Repository | Thêm query tìm kiếm và đếm thông báo theo User. |
| 3 | `NotificationService.java` | Service Interface | Bổ sung `notifyCandidateOfferSent` và `notifyOfferResponseFromCandidate`. |
| 4 | `NotificationServiceImpl.java` | Service Impl | Hiện thực hóa việc tạo thông báo in-app cho Ứng viên khi nhận Offer và cho HR khi Ứng viên phản hồi. |
| 5 | `CandidateOfferServiceImpl.java` | Service Impl | Thêm kiểm tra `isExpired`, chặn trả lời khi quá ngày đi làm, cập nhật DTO response, và gọi notification tới HR. |
| 6 | `OfferServiceImpl.java` | Service Impl | Gọi `notifyCandidateOfferSent` khi chuyển trạng thái sang `Sent_Candidate`. |
| 7 | `candidate-list.html` | UI Template | Hiển thị thông báo quá hạn, ẩn nút Đồng ý/Từ chối cho offer quá hạn. |
| 8 | `candidate-detail.html` | UI Template | Hiển thị Banner cảnh báo quá hạn, card liên hệ chuyên viên HR, ẩn form phản hồi. |
| 9 | `topbar.css` & `topbar-notifications.js` | UI Asset | Hiển thị chấm đỏ thông báo (Red Badge), điều hướng link khi click chuông. |
| 10 | `NotificationServiceImplTest.java` | Test | Thêm 2 test cases kiểm tra thông báo phản hồi Offer cho HR. |
| 11 | `CandidateOfferServiceTests.java` | Test | Thêm 5 test cases kiểm tra thông báo HR và logic quá hạn. |
| 12 | `CandidateOfferQueryTests.java` | Test | Bổ sung test kiểm tra tương thích kiểu dữ liệu SQL Server JDBC. |

---

## IV. LỊCH SỬ COMMITS TRÊN NHÁNH

```text
a73dc58 feat(ui): display expired offer notice and guidance banner in candidate offer views
df49f85 feat(offer): notify HR on candidate answer and guard against expired expectedStartDate
7803ffb feat(notification): add notifyOfferResponseFromCandidate to alert HR on offer accept or decline
bbef1b1 fix(notification): map varchar columns with JdbcTypeCode to prevent SQL Server conversion error
0e3f5c2 feat(notification): notify candidate on job offer sent with red badge and direct link
```

---

## V. KẾT LUẬN VÀ TRẠNG THÁI HIỆN TẠI

- **Trạng thái nhánh:** Đã hoàn thành 100% các yêu cầu nghiệp vụ, đã commit và push đầy đủ lên nhánh `origin/feat/huyenpt/inte3/candidate-offer-response`.
- **Chất lượng mã nguồn:**
  - Tuân thủ chặt chẽ nguyên tắc Clean Architecture: Thin Controller, Rich Service, An toàn Null-Safety.
  - Phù hợp hệ thống chuẩn màu, badge và layout của Mộc RMS Design System.
  - Không phát sinh nợ kỹ thuật (Technical Debt), toàn bộ 141 bài kiểm thử tự động đều đạt `BUILD SUCCESS`.
- **Sẵn sàng:** Toàn bộ mã nguồn đã sẵn sàng để tạo Pull Request (PR) merge vào nhánh chính (`main`).
