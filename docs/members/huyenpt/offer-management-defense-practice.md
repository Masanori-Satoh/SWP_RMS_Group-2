# CẨM NANG THỰC HÀNH LIVE CODING, VẤN ĐÁP BẢO VỆ & KỊCH BẢN DEMO MODULE OFFER MANAGEMENT
**Project:** SWP391 - Recruitment Management System (Mộc RMS - Group 2)  
**Tài liệu ôn thi tốt nghiệp & Bảo vệ đồ án:** `docs/members/huyenpt/offer-management-defense-practice.md`  
**Chuyên môn:** Senior Spring Boot Developer & Technical Reviewer  
**Bộ tài liệu nền tảng:** `offer-management-source-audit.md`, `offer-management-flow-analysis.md`, `offer-management-knowledge-guide.md`  
**Ngày phát hành:** 02/11/2026  

---

## MỤC LỤC

- [PHẦN 7. LIVE CODING THỰC CHIẾN (20 BÀI TẬP)](#phần-7-live-coding-thực-chiến-20-bài-tập)
  - [7.1. Nhóm Dễ (5 bài: Thay đổi cấu hình, nhãn, validation cơ bản)](#71-nhóm-dễ-5-bài)
  - [7.2. Nhóm Trung bình (10 bài: Bổ sung trường, query, logic nghiệp vụ, UI component)](#72-nhóm-trung-bình-10-bài)
  - [7.3. Nhóm Khó (5 bài: Tái cấu trúc, vá lỗi bảo mật, bất đồng bộ, tối ưu giao dịch)](#73-nhóm-khó-5-bài)
- [PHẦN 8. BỘ CÂU HỎI VẤN ĐÁP BẢO VỆ ĐỒ ÁN (60 CÂU Q&A THEO 6 NHÓM CHUYÊN ĐỀ)](#phần-8-bộ-câu-hỏi-vấn-đáp-bảo-vệ-đồ-án-60-câu-qa)
  - [Nhóm 1: Kiến trúc Hệ thống & Spring Boot Core (Câu 1–10)](#nhóm-1-kiến-trúc-hệ-thống--spring-boot-core)
  - [Nhóm 2: Cơ sở Dữ liệu, JPA & Hibernate (Câu 11–20)](#nhóm-2-cơ-sở-dữ-liệu-jpa--hibernate)
  - [Nhóm 3: Bảo mật, Phân quyền RBAC & Phòng thủ Lỗ hổng (Câu 21–30)](#nhóm-3-bảo-mật-phân-quyền-rbac--phòng-thủ-lỗ-hổng)
  - [Nhóm 4: Quy tắc Nghiệp vụ & Vòng đời Thư mời (Câu 31–40)](#nhóm-4-quy-tắc-nghiệp-vụ--vòng-đời-thư-mời)
  - [Nhóm 5: Frontend, JavaScript & Tương tác UX (Câu 41–50)](#nhóm-5-frontend-javascript--tương-tác-ux)
  - [Nhóm 6: Kiểm thử Phần mềm, Debug & Xử lý Sự cố (Câu 51–60)](#nhóm-6-kiểm-thử-phần-mềm-debug--xử-lý-sự-cố)
- [PHẦN 9. ĐIỂM YẾU KỸ THUẬT, HẠN CHẾ & GIẢI PHÁP NÂNG CẤP](#phần-9-điểm-yếu-kỹ-thuật-hạn-chế--giải-pháp-nâng-cấp)
- [PHẦN 10. KỊCH BẢN THUYẾT TRÌNH & DEMO TỐT NGHIỆP](#phần-10-kịch-bản-thuyết-trình--demo-tốt-nghiệp)
  - [10.1. Kịch bản Thuyết trình 5 phút (Tập trung Trọng tâm)](#101-kịch-bản-thuyết-trình-5-phút)
  - [10.2. Kịch bản Thuyết trình 10 phút (Kỹ thuật Chuyên sâu & Điểm sáng)](#102-kịch-bản-thuyết-trình-10-phút)
  - [10.3. Checklist Chuẩn bị Trước khi Lên bảng](#103-checklist-chuẩn-bị)
  - [10.4. Xử lý Sự cố Kỹ thuật Khi Demo](#104-xử-lý-sự-cố-kỹ-thuật)
- [PHỤ LỤC TRA CỨU NHANH](#phụ-lục-tra-cứu-nhanh)
  - [Phụ lục 1: Toàn bộ Endpoints của Module Offer](#phụ-lục-1-toàn-bộ-endpoints)
  - [Phụ lục 2: Bảng Ma trận Chuyển đổi Trạng thái (State Machine)](#phụ-lục-2-bảng-ma-trận-chuyển-đổi-trạng-thái)
  - [Phụ lục 3: Bản đồ Tệp tin Module Offer Management](#phụ-lục-3-bản-đồ-tệp-tin)
  - [Phụ lục 4: Spring & JPA Annotation Cheat Sheet](#phụ-lục-4-annotation-cheat-sheet)

---

# PHẦN 7. LIVE CODING THỰC CHIẾN (20 BÀI TẬP)

---

### 7.1. Nhóm Dễ (5 bài)

#### Bài 1: Đổi thời hạn phản hồi mặc định từ 5 ngày thành 7 ngày
* **Yêu cầu:** Ban Giám đốc muốn tăng thời gian suy nghĩ cho ứng viên từ 5 ngày lên 7 ngày theo mặc định khi tạo mới Offer.
* **File cần sửa:** [`src/main/java/com/group2/rms/offer/dto/CreateOfferRequest.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/dto/CreateOfferRequest.java)
* **Code thay đổi (Dòng 69):**
  ```java
  // TRƯỚC:
  @Builder.Default
  private Integer responsePeriodDays = 5;

  // SAU:
  @Builder.Default
  private Integer responsePeriodDays = 7;
  ```
* **Kiểm tra/Test:** Mở form `/offers/create`, ô "Thời hạn phản hồi (ngày)" hiển thị giá trị mặc định là `7`.

---

#### Bài 2: Bổ sung cột Số điện thoại ứng viên vào bảng danh sách Offer của HR
* **Yêu cầu:** Giúp HR liên hệ nhanh với ứng viên ngay từ bảng danh sách mà không cần bấm vào trang chi tiết.
* **File cần sửa:** [`src/main/resources/templates/offers/list.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/list.html)
* **Code thay đổi:**
  Tại phần tiêu đề bảng `<thead>`:
  ```html
  <th>Số điện thoại</th>
  ```
  Tại phần thân bảng `<tbody>`:
  ```html
  <td th:text="${offer.candidatePhone != null ? offer.candidatePhone : '---'}">0912345678</td>
  ```
* **Kiểm tra/Test:** Truy cập `/offers`, cột Số điện thoại hiển thị đúng số điện thoại của ứng viên từ `OfferResponse`.

---

#### Bài 3: Giới hạn độ dài địa điểm làm việc tối đa 100 ký tự (thay vì 255)
* **Yêu cầu:** Rút gọn độ dài trường `workLocation` trên DTO để tránh người dùng paste văn bản quá dài.
* **File cần sửa:** [`src/main/java/com/group2/rms/offer/dto/CreateOfferRequest.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/dto/CreateOfferRequest.java) và [`UpdateOfferRequest.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/dto/UpdateOfferRequest.java)
* **Code thay đổi:**
  ```java
  // Sửa annotation @Size:
  @NotBlank(message = "Địa điểm làm việc không được để trống.")
  @Size(max = 100, message = "Địa điểm làm việc không được vượt quá 100 ký tự.")
  private String workLocation;
  ```
* **Kiểm tra/Test:** Nhập địa điểm làm việc 105 ký tự $\rightarrow$ Submit $\rightarrow$ Nhận thông báo lỗi validate vi phạm độ dài.

---

#### Bài 4: Tùy biến tiêu đề email thư mời có gắn tiền tố thương hiệu
* **Yêu cầu:** Thêm tiền tố `[Mộc RMS]` vào tiêu đề email gửi ứng viên để tránh bị rơi vào hòm thư Spam.
* **File cần sửa:** [`src/main/java/com/group2/rms/offer/service/OfferMailSender.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferMailSender.java)
* **Code thay đổi (Dòng 58):**
  ```java
  // TRƯỚC:
  message.setSubject("Thư mời nhận việc - " + offer.getOfferedPositionTitle());

  // SAU:
  message.setSubject("[Mộc RMS] Thư mời nhận việc - " + offer.getOfferedPositionTitle());
  ```
* **Kiểm tra/Test:** HR bấm Phát hành Thư mời, kiểm tra dòng log server: `[MOCK EMAIL]` hoặc tiêu đề email nhận được có chứa `[Mộc RMS]`.

---

#### Bài 5: Hiển thị định dạng tiền tệ VNĐ cho lương thử việc trên modal của Candidate Portal
* **Yêu cầu:** Giao diện modal chấp thuận nhận việc cần format số tiền rõ ràng dạng `17,000,000 VNĐ` thay vì số thô.
* **File cần sửa:** [`src/main/resources/templates/offers/candidate-detail.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/candidate-detail.html)
* **Code thay đổi:**
  ```html
  <!-- Dùng cú pháp định dạng số của Thymeleaf #numbers.formatDecimal -->
  <span th:text="${#numbers.formatDecimal(offer.probationSalary, 0, 'COMMA', 0, 'POINT') + ' VNĐ/tháng'}">17,000,000 VNĐ/tháng</span>
  ```
* **Kiểm tra/Test:** Vào trang `/portal/offers/{id}`, số tiền lương thử việc hiển thị phân cách hàng nghìn bằng dấu phẩy đẹp mắt.

---

### 7.2. Nhóm Trung bình (10 bài)

#### Bài 6: Thêm trường Ghi chú nội bộ (`internalNotes`) cho HR vào Offer
* **Yêu cầu:** Cho phép HR ghi chú nội bộ khi tạo bản nháp (ví dụ: "Ứng viên yêu cầu hỗ trợ máy tính Mac"). Trường này ứng viên không nhìn thấy.
* **Các bước thực hiện:**
  1. Thêm cột vào Entity [`OfferProposal.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/entity/OfferProposal.java):
     ```java
     @Column(name = "InternalNotes", length = 1000)
     private String internalNotes;
     ```
  2. Bổ sung trường vào `CreateOfferRequest.java` & `UpdateOfferRequest.java`:
     ```java
     @Size(max = 1000, message = "Ghi chú nội bộ không được vượt quá 1000 ký tự.")
     private String internalNotes;
     ```
  3. Map dữ liệu trong [`OfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java):
     ```java
     offer.setInternalNotes(dto.getInternalNotes());
     ```
  4. Cập nhật DDL trong SQL Server: `ALTER TABLE OfferProposal ADD InternalNotes NVARCHAR(1000) NULL;`.

---

#### Bài 7: Validation không cho phép ngày bắt đầu làm việc rơi vào Thứ Bảy hoặc Chủ Nhật
* **Yêu cầu:** Công ty làm việc từ Thứ Hai đến Thứ Sáu, ngày bắt đầu đi làm phải là ngày làm việc chính thức.
* **File cần sửa:** [`CreateOfferRequest.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/dto/CreateOfferRequest.java)
* **Code bổ sung:**
  ```java
  @AssertTrue(message = "Ngày bắt đầu làm việc không được rơi vào Thứ Bảy hoặc Chủ Nhật.")
  public boolean isNotWeekend() {
      if (expectedStartDate == null) return true;
      java.time.DayOfWeek day = expectedStartDate.getDayOfWeek();
      return day != java.time.DayOfWeek.SATURDAY && day != java.time.DayOfWeek.SUNDAY;
  }
  ```
* **Kiểm tra/Test:** Chọn ngày bắt đầu là Chủ Nhật tuần tới $\rightarrow$ Submit $\rightarrow$ Form báo lỗi vi phạm ngày cuối tuần.

---

#### Bài 8: Bổ sung thống kê Thư mời "Đã quá hạn phản hồi" trên Dashboard Giám đốc
* **Yêu cầu:** Thêm thẻ KPI hiển thị số lượng thư mời đã gửi đi nhưng quá hạn phản hồi mà ứng viên chưa trả lời.
* **File cần sửa:** [`OfferProposalRepository.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/repository/OfferProposalRepository.java) và [`OfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java)
* **Code bổ sung Repository:**
  ```java
  @Query("SELECT COUNT(o) FROM OfferProposal o WHERE (o.isDeleted IS NULL OR o.isDeleted = false) " +
         "AND o.offerStatus = 'Sent_Candidate' AND o.responseDeadline < CURRENT_TIMESTAMP")
  long countExpiredOffers();
  ```
* **Code Service:** Đưa giá trị `countExpiredOffers()` vào `Map<String, Long> getDirectorOfferStats()`.

---

#### Bài 9: Viết Custom JPA Query tìm kiếm Offer theo từ khóa không phân biệt hoa thường
* **Yêu cầu:** Tìm kiếm thư mời theo Tên ứng viên, Email hoặc Vị trí đề xuất.
* **File cần sửa:** [`OfferProposalRepository.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/repository/OfferProposalRepository.java)
* **Code bổ sung:**
  ```java
  @Query("SELECT o FROM OfferProposal o JOIN o.application a JOIN a.candidate c JOIN c.account u " +
         "WHERE (o.isDeleted IS NULL OR o.isDeleted = false) " +
         "AND (LOWER(o.offeredPositionTitle) LIKE LOWER(CONCAT('%', :kw, '%')) " +
         "     OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :kw, '%')) " +
         "     OR LOWER(u.email) LIKE LOWER(CONCAT('%', :kw, '%')))")
  Page<OfferProposal> searchOffers(@Param("kw") String keyword, Pageable pageable);
  ```

---

#### Bài 10: Thêm tính năng "Sao chép liên kết Thư mời" (Copy Link) trên Portal Ứng viên
* **Yêu cầu:** Cho phép ứng viên bấm nút để copy link thư mời vào Clipboard gửi cho người thân.
* **File cần sửa:** [`src/main/resources/templates/offers/candidate-detail.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/candidate-detail.html) và [`candidate-offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/pages/candidate-offers.js)
* **Code JavaScript:**
  ```javascript
  function copyOfferLink() {
      navigator.clipboard.writeText(window.location.href).then(() => {
          showCopy('Đã sao chép liên kết thư mời vào bộ nhớ tạm!');
      }).catch(err => {
          console.error('Không thể sao chép: ', err);
      });
  }
  ```

---

#### Bài 11: Validation chặn mức lương đề xuất vượt khung trần 500 triệu VNĐ
* **Yêu cầu:** Ngăn ngừa sai sót nhập thừa số 0 (ví dụ gõ 2 tỷ thay vì 20 triệu).
* **File cần sửa:** [`CreateOfferRequest.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/dto/CreateOfferRequest.java)
* **Code thay đổi:**
  ```java
  @NotNull(message = "Mức lương chính thức không được để trống.")
  @Positive(message = "Mức lương chính thức phải lớn hơn 0.")
  @DecimalMax(value = "500000000", message = "Mức lương đề xuất không được vượt quá 500,000,000 VNĐ.")
  private BigDecimal proposedSalary;
  ```

---

#### Bài 12: Bổ sung cột "Thời điểm phản hồi" vào Báo cáo Xuất Excel
* **Yêu cầu:** Khi HR xuất Excel, cần có thêm cột ghi nhận ngày giờ ứng viên bấm Accept/Decline (lấy từ `updatedAt`).
* **File cần sửa:** [`src/main/java/com/group2/rms/offer/service/OfferExportServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferExportServiceImpl.java)
* **Code thực hiện:**
  Định nghĩa thêm cột `RESPONDED_AT` trong danh sách cột hỗ trợ:
  ```java
  if ("Accepted".equals(offer.getOfferStatus()) || "Declined".equals(offer.getOfferStatus())) {
      cell.setCellValue(offer.getUpdatedAt() != null ? offer.getUpdatedAt().format(DATE_TIME_FORMATTER) : "---");
  } else {
      cell.setCellValue("Chưa phản hồi");
  }
  ```

---

#### Bài 13: Gửi email tự động thông báo cho HR khi Ứng viên bấm Từ chối Thư mời
* **Yêu cầu:** Báo ngay cho HR phụ trách để kịp thời gọi điện khảo sát lại ứng viên hoặc chuẩn bị ứng viên thay thế.
* **File cần sửa:** [`CandidateOfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/CandidateOfferServiceImpl.java)
* **Code bổ sung:**
  Trong method `answer()` (nhánh `!accepted`):
  ```java
  User hrUser = offer.getProposedBy();
  if (hrUser != null && hrUser.getEmail() != null) {
      offerMailSender.sendDeclineNotificationToHr(offer, hrUser.getEmail(), cleanReason);
  }
  ```

---

#### Bài 14: Siết chặt thời hạn phản hồi tối thiểu là 3 ngày (thay vì 1 ngày)
* **Yêu cầu:** Tránh trường hợp HR ép tiến độ ứng viên bằng cách đặt hạn 1 ngày.
* **File cần sửa:** [`CreateOfferRequest.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/dto/CreateOfferRequest.java)
* **Code thay đổi:**
  ```java
  @NotNull(message = "Thời gian phản hồi không được để trống.")
  @Min(value = 3, message = "Thời gian phản hồi cho ứng viên tối thiểu phải từ 3 ngày trở lên.")
  @Max(value = 60, message = "Thời gian phản hồi không được vượt quá 60 ngày.")
  private Integer responsePeriodDays = 5;
  ```

---

#### Bài 15: Ghi Audit Log Client IP và User-Agent khi Ứng viên Accept/Decline
* **Yêu cầu:** Lưu lại chứng cứ điện tử về IP và trình duyệt của ứng viên khi ký nhận hoặc từ chối thư mời.
* **File cần sửa:** [`CandidateOfferController.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/controller/CandidateOfferController.java)
* **Code bổ sung:**
  Tiêm `HttpServletRequest request` vào phương thức:
  ```java
  String clientIp = request.getHeader("X-Forwarded-For");
  if (clientIp == null) clientIp = request.getRemoteAddr();
  String userAgent = request.getHeader("User-Agent");
  log.info("Ứng viên [ID: {}] phản hồi Offer [ID: {}] từ IP: {}, Thiết bị: {}", userId, id, clientIp, userAgent);
  ```

---

### 7.3. Nhóm Khó (5 bài)

#### Bài 16: Khắc phục triệt để BUG-OFF-10 trong Spring Security RequestCache
* **Yêu cầu:** Loại bỏ hoàn toàn nguy cơ bị chuyển hướng về URL JSON `/api/notifications/unread-count` sau khi hết hạn session đăng nhập lại.
* **File cần sửa:** [`src/main/java/com/group2/rms/core/config/SecurityConfig.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/config/SecurityConfig.java)
* **Code thay đổi (Dòng 60–62):**
  ```java
  // TRƯỚC:
  HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
  requestCache.setMatchingRequestParameterName(null);

  // SAU (FIX CHUẨN XÁC):
  HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
  requestCache.setMatchingRequestParameterName(null);
  // Loại trừ toàn bộ các request REST API / AJAX ngầm khỏi Request Cache
  requestCache.setRequestMatcher(new org.springframework.security.web.util.matcher.NegatedRequestMatcher(
          new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/api/**")
  ));
  ```
* **Kiểm tra/Test:** Mở một tab web để yên 35 phút (quá timeout), kiểm tra polling `/api/notifications/unread-count` gửi lên bị chặn $\rightarrow$ Trình duyệt redirect về `/login`. Sau khi đăng nhập lại, hệ thống đưa về trang `/dashboard` hoặc trang nghiệp vụ trước đó, không bao giờ bị rơi vào trang JSON thô nữa!

---

#### Bài 17: Chuyển đổi trạng thái Offer sang Java Enum kèm JPA AttributeConverter
* **Yêu cầu:** Chuẩn hóa việc quản lý trạng thái Offer từ kiểu String lỏng lẻo sang Enum có Type-Safe mà vẫn bảo toàn 100% dữ liệu chuỗi trong SQL Server.
* **Các bước thực hiện:**
  1. Tạo Enum `OfferStatus.java`:
     ```java
     public enum OfferStatus {
         Draft, Pending_Director, Director_Approved, Director_Rejected, Sent_Candidate, Accepted, Declined
     }
     ```
  2. Tạo bộ chuyển đổi `@Converter`:
     ```java
     @Converter(autoApply = true)
     public class OfferStatusConverter implements AttributeConverter<OfferStatus, String> {
         @Override
         public String convertToDatabaseColumn(OfferStatus status) {
             return status != null ? status.name() : null;
         }
         @Override
         public OfferStatus convertToEntityAttribute(String dbData) {
             return dbData != null ? OfferStatus.valueOf(dbData) : null;
         }
     }
     ```
  3. Đổi thuộc tính trong `OfferProposal.java`:
     ```java
     @Column(name = "OfferStatus", nullable = false, length = 40)
     private OfferStatus offerStatus;
     ```

---

#### Bài 18: Cho phép Thương lượng lại Lương (Tạo nhiều Đợt Offer - Multiple Revisions)
* **Yêu cầu:** Khắc phục nhược điểm "1 Application chỉ có 1 Offer duy nhất". Cho phép tạo Offer lần 2 khi ứng viên từ chối lần 1 và thương lượng lại.
* **Các bước thực hiện:**
  1. Thêm cột `RevisionNumber INT NOT NULL DEFAULT 1` và `IsActive BIT NOT NULL DEFAULT 1` vào bảng `OfferProposal`.
  2. Đổi ràng buộc Unique trong SQL Server:
     ```sql
     ALTER TABLE OfferProposal DROP CONSTRAINT UQ_OfferProposal_Application;
     -- Tạo Unique Constraint có điều kiện (Filtered Unique Index)
     CREATE UNIQUE NONCLUSTERED INDEX UQ_ActiveOffer_Application 
     ON OfferProposal (ApplicationId) WHERE IsActive = 1;
     ```
  3. Trong logic tạo đợt mới: Set `IsActive = 0` cho bản ghi Offer cũ và `RevisionNumber = old.getRevisionNumber() + 1` cho bản ghi mới.

---

#### Bài 19: Tách Gửi Email Thư Mời Chạy Bất Đồng Bộ (`@Async` & Transactional Event Listener)
* **Yêu cầu:** Ngăn chặn việc gửi mail qua SMTP làm nghẽn kết nối database và làm chậm thời gian phản hồi của nút "Phát hành Thư mời".
* **Các bước thực hiện:**
  1. Bật `@EnableAsync` trong cấu hình Spring Boot.
  2. Tạo Event `OfferSentEvent(Integer offerId)`.
  3. Bắn event trong `OfferServiceImpl`:
     ```java
     eventPublisher.publishEvent(new OfferSentEvent(saved.getOfferId()));
     ```
  4. Lắng nghe và chỉ gửi email sau khi Database đã COMMIT thành công:
     ```java
     @Async
     @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
     public void handleOfferSentEvent(OfferSentEvent event) {
         OfferProposal offer = repository.findById(event.offerId()).orElse(null);
         if (offer != null) {
             offerMailSender.sendOfferLetterToCandidate(offer);
         }
     }
     ```

---

#### Bài 20: Cài đặt Bộ Giới Hạn Tần Suất (Rate Limiting) Chống Spam Endpoint Phản Hồi
* **Yêu cầu:** Chặn các cuộc tấn công Brute-force hoặc Tool auto click gửi dồn dập vào `/portal/offers/{id}/accept` và `/decline` (giới hạn tối đa 5 request/phút trên mỗi session).
* **Các bước thực hiện:**
  1. Tạo `OfferResponseRateLimitFilter` kế thừa `OncePerRequestFilter`.
  2. Sử dụng thư viện `Bucket4j` hoặc `ConcurrentHashMap` lưu trữ số lần request theo Session ID.
  3. Nếu số request vượt quá 5 lần/phút: Trả về ngay mã lỗi **HTTP 429 Too Many Requests** kèm thông điệp: *"Bạn đang thao tác quá nhanh. Vui lòng chờ 1 phút trước khi thử lại."*.

---

# PHẦN 8. BỘ CÂU HỎI VẤN ĐÁP BẢO VỆ ĐỒ ÁN (60 CÂU Q&A)

---

### Nhóm 1: Kiến trúc Hệ thống & Spring Boot Core

#### Câu 1: Em hãy giải thích mô hình kiến trúc phân tầng (Layered Architecture) trong module Offer?
* **Đáp án ngắn:** Module được phân tách thành 4 tầng rõ rệt: Web (Controller), Business Logic (Service), Data Access (Repository) và Database Entity.
* **Giải thích chuyên sâu:** Controller tiếp nhận HTTP request, validate dữ liệu đầu vào qua DTO và điều hướng. Service đảm nhiệm toàn bộ quy tắc nghiệp vụ (tính 85% lương, kiểm tra deadline, nộp duyệt, soft delete) và quản lý giao dịch `@Transactional`. Repository sử dụng Spring Data JPA để giao tiếp CSDL. Việc phân tầng giúp hệ thống đạt tính Loose Coupling (kết nối lỏng) và High Cohesion (gắn kết cao), dễ viết Unit Test độc lập.
* **Dẫn chứng code:** [`OfferController.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/controller/OfferController.java) tiêm `OfferService`, `OfferService` tiêm `OfferProposalRepository`.

#### Câu 2: Sự khác biệt giữa `@Controller` và `@RestController` trong Spring Boot là gì, và project của em áp dụng như thế nào?
* **Đáp án ngắn:** `@Controller` trả về View template (Thymeleaf), còn `@RestController` trả về dữ liệu thô (JSON/XML). Project dùng `@Controller` kết hợp `@ResponseBody` (Hybrid Controller).
* **Giải thích chuyên sâu:** Nếu gắn `@RestController`, method trả về chuỗi String `"offers/list"` sẽ bị in thẳng ra trình duyệt dưới dạng chữ thô thay vì load trang HTML. Để tối ưu, nhóm dùng `@Controller` trên toàn class, và chỉ gắn thêm `@ResponseBody` trên các hàm trả về JSON cho modal Ajax duyệt nhanh của Giám đốc.
* **Dẫn chứng code:** [`OfferController.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/controller/OfferController.java) dòng 41 (`@Controller`) và dòng 463 (`@ResponseBody`).

#### Câu 3: Annotation `@RequiredArgsConstructor` của Lombok hoạt động như thế nào trong tầng Service?
* **Đáp án ngắn:** Tự động sinh constructor chứa tất cả các trường được khai báo `private final`, phục vụ Constructor Injection chuẩn mực.
* **Giải thích chuyên sâu:** Spring khuyến nghị tiêm phụ thuộc qua Constructor thay vì `@Autowired` trên thuộc tính (Field Injection). Constructor Injection đảm bảo các phụ thuộc không thể null (bất biến sau khi khởi tạo) và dễ dàng mock khi viết Unit Test.
* **Dẫn chứng code:** [`OfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java) dòng 50 có `@RequiredArgsConstructor`.

#### Câu 4: `@Transactional` hoạt động dựa trên cơ chế nào trong Spring Framework?
* **Đáp án ngắn:** Dựa trên cơ chế Spring AOP Proxy bọc quanh phương thức để quản lý `begin`, `commit`, và `rollback` kết nối CSDL.
* **Giải thích chuyên sâu:** Khi một phương thức có `@Transactional` được gọi từ bên ngoài, Spring Proxy sẽ chặn lại, mở một Transaction trên CSDL. Nếu method chạy hoàn tất không có lỗi, Proxy thực hiện `commit`. Nếu ném ra `RuntimeException` hoặc `Error`, Proxy tự động thực hiện `rollback`.
* **Dẫn chứng code:** [`OfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java) dòng 48 `@Transactional`.

#### Câu 5: Tại sao trong `OfferExportServiceImpl` lại sử dụng `@Transactional(readOnly = true)`?
* **Đáp án ngắn:** Để tối ưu hóa hiệu năng bộ nhớ và tốc độ đọc của Hibernate khi xuất Excel khối lượng lớn.
* **Giải thích chuyên sâu:** Khi đặt `readOnly = true`, Hibernate sẽ tắt cơ chế Dirty Checking (không lưu snapshot trạng thái ban đầu của thực thể vào bộ nhớ để so sánh thay đổi). Điều này giúp giảm đáng kể lượng tiêu thụ RAM và báo cho JDBC driver tối ưu khóa đọc.
* **Dẫn chứng code:** [`OfferExportServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferExportServiceImpl.java) dòng 45.

#### Câu 6: DTO (Data Transfer Object) có vai trò gì và tại sao không dùng trực tiếp Entity ở Controller?
* **Đáp án ngắn:** DTO đóng gói dữ liệu truyền tải giữa client và server, tách biệt mô hình CSDL khỏi mô hình biểu diễn giao diện.
* **Giải thích chuyên sâu:** Dùng Entity ở Controller dễ gây lộ thông tin nhạy cảm (như mật khẩu, cờ xóa mềm), bị lỗi vòng lặp tuần hoàn JSON (Circular Dependency) và dễ dính lỗ hổng Mass Assignment. DTO cho phép validate dữ liệu đầu vào độc lập và định dạng dữ liệu đầu ra phù hợp với từng màn hình.
* **Dẫn chứng code:** [`CreateOfferRequest.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/dto/CreateOfferRequest.java) và [`OfferDetailResponse.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/dto/OfferDetailResponse.java).

#### Câu 7: Phương thức `GlobalExceptionHandler` bắt lỗi và chuẩn hóa phản hồi như thế nào?
* **Đáp án ngắn:** Sử dụng `@ControllerAdvice` và `@ExceptionHandler` để bắt tập trung mọi ngoại lệ từ tầng Controller ném ra.
* **Giải thích chuyên sâu:** Thay vì đặt `try-catch` ở khắp mọi nơi, `GlobalExceptionHandler` đóng vai trò chốt chặn cuối cùng. Khi gặp `BaseBusinessException`, nó chuyển thành HTTP 400 và gắn thông điệp lỗi vào Flash message hoặc JSON `ApiResponse` thân thiện cho người dùng.
* **Dẫn chứng code:** [`GlobalExceptionHandler.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/exception/GlobalExceptionHandler.java) dòng 178 bắt `BaseBusinessException`.

#### Câu 8: Java Record được áp dụng ở đâu trong module Offer và ưu điểm của nó là gì?
* **Đáp án ngắn:** Áp dụng cho [`CandidateOfferDeclineRequest.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/dto/CandidateOfferDeclineRequest.java) để làm DTO bất biến, cú pháp cực kỳ ngắn gọn.
* **Giải thích chuyên sâu:** Java Record (từ Java 14+) tự động sinh constructor, getters, `equals()`, `hashCode()` và `toString()` mà không cần viết boilerplate code hoặc dùng Lombok. Dữ liệu trong Record là bất biến (immutable), đảm bảo tính an toàn dữ liệu cao.
* **Dẫn chứng code:** [`CandidateOfferDeclineRequest.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/dto/CandidateOfferDeclineRequest.java) dòng 9.

#### Câu 9: Lợi ích của việc cấu hình `ObjectProvider<JavaMailSender>` trong `OfferMailSender` là gì?
* **Đáp án ngắn:** Giúp ứng dụng không bị sập (crash) khi khởi động trong môi trường thiếu cấu hình SMTP server.
* **Giải thích chuyên sâu:** Nếu tiêm trực tiếp `JavaMailSender`, khi không khai báo thông số mail trong `application.properties`, Spring Context sẽ văng lỗi `NoSuchBeanDefinitionException`. Dùng `ObjectProvider`, nếu có cấu hình thì lấy bean gửi mail thật, nếu không có thì fallback sang ghi log `[MOCK EMAIL]` an toàn.
* **Dẫn chứng code:** [`OfferMailSender.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferMailSender.java) dòng 26 và dòng 51.

#### Câu 10: Spring Boot tự động map dữ liệu phân trang như thế nào qua `Pageable`?
* **Đáp án ngắn:** Tự động đọc các query param `page`, `size`, `sort` trên URL và đóng gói thành đối tượng `PageRequest`.
* **Giải thích chuyên sâu:** Khi Controller khai báo `Pageable pageable`, Spring MVC `PageableHandlerMethodArgumentResolver` sẽ tự động phân tích tham số HTTP. Tại repository, Spring Data JPA tự động sinh câu lệnh `OFFSET ... ROWS FETCH NEXT ... ROWS ONLY` trong SQL Server.
* **Dẫn chứng code:** [`OfferController.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/controller/OfferController.java) dòng 74–75.

---

### Nhóm 2: Cơ sở Dữ liệu, JPA & Hibernate

#### Câu 11: Phân biệt `GenerationType.IDENTITY` và `GenerationType.SEQUENCE`? Bảng `OfferProposal` dùng loại nào?
* **Đáp án ngắn:** IDENTITY dựa vào cột tự tăng của bảng CSDL, còn SEQUENCE dùng đối tượng Sequence độc lập của CSDL. `OfferProposal` dùng IDENTITY.
* **Giải thích chuyên sâu:** Trong SQL Server, cột `OfferId INT IDENTITY(1,1)` tự động sinh số tăng dần khi có lệnh INSERT. Khi dùng IDENTITY, Hibernate phải thực thi lệnh INSERT ngay lập tức để lấy lại khóa chính được sinh ra, thay vì trì hoãn tới lúc commit transaction.
* **Dẫn chứng code:** [`OfferProposal.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/entity/OfferProposal.java) dòng 29 `@GeneratedValue(strategy = GenerationType.IDENTITY)`.

#### Câu 12: Ràng buộc `UQ_OfferProposal_Application` giải quyết quy tắc nghiệp vụ gì?
* **Đáp án ngắn:** Đảm bảo quy tắc `GBR-07`: Mỗi hồ sơ ứng tuyển (`ApplicationId`) chỉ có tối đa một Thư mời nhận việc tồn tại.
* **Giải thích chuyên sâu:** Ngăn chặn việc 2 HR khác nhau cùng tạo 2 đề xuất lương cho cùng một ứng viên trong cùng một đợt tuyển dụng. Ràng buộc UNIQUE ở tầng database là chốt chặn tuyệt đối chống trùng lặp dữ liệu.
* **Dẫn chứng code:** [`database/schema/db.sql`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/database/schema/db.sql) dòng 508.

#### Câu 13: Trình bày cơ chế hoạt động của `CHECK CONSTRAINT` cho lương thử việc 85% trong CSDL?
* **Đáp án ngắn:** Ngăn chặn mọi hành vi lưu dữ liệu có mức lương thử việc thấp hơn 85% lương chính thức ngay tại tầng SQL Server.
* **Giải thích chuyên sâu:** Câu lệnh `CHECK (ProposedSalary >= 0 AND ProbationSalary >= 0 AND ProbationSalary >= (ProposedSalary * 0.85))` được SQL Server đánh giá trước khi ghi dòng dữ liệu. Nếu điều kiện trả về FALSE, hệ quản trị CSDL lập tức hủy giao dịch và ném lỗi vi phạm ràng buộc.
* **Dẫn chứng code:** [`database/schema/db.sql`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/database/schema/db.sql) dòng 543 `CK_OfferProposal_Salaries`.

#### Câu 14: Tại sao quan hệ giữa `OfferProposal` và `OfferApproval` là 1 - N mà không phải 1 - 1?
* **Đáp án ngắn:** Để lưu vết lịch sử phê duyệt qua nhiều đợt (Audit Trail) khi Giám đốc từ chối rồi HR nộp duyệt lại.
* **Giải thích chuyên sâu:** Nếu dùng quan hệ 1-1, mỗi lần Giám đốc duyệt lại sẽ phải ghi đè lên quyết định cũ, làm mất đi bằng chứng lịch sử (ai từ chối, lý do từ chối ban đầu là gì). Quan hệ 1-N cho phép vẽ sơ đồ Timeline phê duyệt chi tiết trên trang xem Offer.
* **Dẫn chứng code:** [`OfferApprovalRepository.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/repository/OfferApprovalRepository.java) dòng 15: `findByOfferProposal_OfferIdOrderByApprovedAtDesc`.

#### Câu 15: Lỗi N+1 Query là gì và `JOIN FETCH` giải quyết nó như thế nào trong repository ứng viên?
* **Đáp án ngắn:** N+1 Query là lỗi sinh ra 1 câu query cha và N câu query con để lấy dữ liệu liên quan. `JOIN FETCH` gom tất cả vào 1 câu SQL duy nhất.
* **Giải thích chuyên sâu:** Khi duyệt danh sách Offer, nếu không có `JOIN FETCH`, mỗi lần gọi `offer.getApplication().getJobPosting()` Hibernate sẽ gửi thêm 1 câu SELECT. `JOIN FETCH` ép Hibernate sinh câu lệnh `INNER JOIN` ngay từ đầu, nạp đầy đủ dữ liệu vào bộ nhớ chỉ trong 1 chuyến gửi nhận dữ liệu mạng.
* **Dẫn chứng code:** [`CandidateOfferRepository.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/repository/CandidateOfferRepository.java) dòng 21–29.

#### Câu 16: Tại sao lại chọn kiểu dữ liệu `DECIMAL(18,2)` cho mức lương thay vì `FLOAT` hay `DOUBLE`?
* **Đáp án ngắn:** Để đảm bảo độ chính xác tuyệt đối trong tính toán tài chính, không bị sai số dấu chấm động (Floating-point error).
* **Giải thích chuyên sâu:** Kiểu `FLOAT` và `DOUBLE` lưu trữ xấp xỉ nhị phân, dẫn tới hiện tượng phép tính `0.1 + 0.2 = 0.30000000000000004`. Với tiền lương doanh nghiệp, `DECIMAL(18,2)` lưu trữ chính xác từng chữ số thập phân tương ứng với lớp `BigDecimal` trong Java.
* **Dẫn chứng code:** [`OfferProposal.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/entity/OfferProposal.java) dòng 40 và 46.

#### Câu 17: Bản chất kỹ thuật của câu lệnh Atomic Update `answerIfSent` là gì?
* **Đáp án ngắn:** Là câu lệnh `UPDATE ... WHERE offerStatus = 'Sent_Candidate'` dựa vào khóa cấp dòng của CSDL để ngăn chặn Race Condition.
* **Giải thích chuyên sâu:** SQL Server tự động đặt Exclusive Lock trên dòng dữ liệu khi thực thi UPDATE. Nếu 2 request cùng lao vào, request thứ nhất đổi trạng thái thành `Accepted` và commit; request thứ hai kiểm tra điều kiện `WHERE offerStatus = 'Sent_Candidate'` thấy không còn khớp nữa nên trả về 0 dòng bị tác động, giúp ứng dụng phát hiện xung đột tức thì.
* **Dẫn chứng code:** [`CandidateOfferRepository.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/repository/CandidateOfferRepository.java) dòng 49–57.

#### Câu 18: Lỗi Data Truncation xảy ra ở trường nào trong quá khứ và cách khắc phục?
* **Đáp án ngắn:** Xảy ra ở trường `DeclineReason` khi ứng viên nhập quá 500 ký tự. Đã khắc phục bằng cách tăng cột lên `NVARCHAR(1000)`.
* **Giải thích chuyên sâu:** Ban đầu cột `DeclineReason` chỉ có kích thước 500 ký tự. Khi ứng viên giải thích lý do từ chối quá dài, SQL Server ném lỗi `String or binary data would be truncated`. Nhóm đã tạo script migration `008` nâng kích thước lên `NVARCHAR(1000)` và đặt `@Size(max = 1000)` ở DTO.
* **Dẫn chứng code:** [`OfferProposal.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/entity/OfferProposal.java) dòng 68 và `CandidateOfferDeclineRequest.java` dòng 10.

#### Câu 19: Sự khác biệt giữa `save()` và `saveAndFlush()` trong Spring Data JPA?
* **Đáp án ngắn:** `save()` lưu đối tượng vào Persistence Context và chờ kết thúc transaction mới đồng bộ CSDL, còn `saveAndFlush()` ép đồng bộ xuống CSDL ngay lập tức.
* **Giải thích chuyên sâu:** `save()` tối ưu hiệu năng hơn vì gom các câu lệnh SQL lại thực thi một lượt (Batching). `saveAndFlush()` chỉ dùng khi cần thấy ngay kết quả trên CSDL trong cùng transaction để phục vụ câu truy vấn tiếp theo. Trong module Offer, hầu hết đều dùng `save()`.
* **Dẫn chứng code:** [`OfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java) dòng 315: `offerProposalRepository.save(offer)`.

#### Câu 20: Cơ chế `@PrePersist` trong thực thể `OfferApproval` có tác dụng gì?
* **Đáp án ngắn:** Tự động gán thời gian hiện tại (`approvedAt = LocalDateTime.now()`) trước khi bản ghi được lưu xuống CSDL nếu trường này đang null.
* **Giải thích chuyên sâu:** Đây là cơ chế Lifecycle Callback của JPA. Nó đảm bảo dữ liệu audit thời gian phê duyệt luôn luôn tồn tại mà không phụ thuộc vào việc lập trình viên có nhớ gọi setter trong code Java hay không.
* **Dẫn chứng code:** [`OfferApproval.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/entity/OfferApproval.java) dòng 107–112.

---

### Nhóm 3: Bảo mật, Phân quyền RBAC & Phòng thủ Lỗ hổng

#### Câu 21: Thứ tự cấu hình URL trong `SecurityConfig` có ý nghĩa như thế nào đối với phân hệ Offer?
* **Đáp án ngắn:** URL nào cụ thể hơn và có quyền hẹp hơn bắt buộc phải đặt trước URL tổng quát.
* **Giải thích chuyên sâu:** Spring Security so khớp URL theo cơ chế First-Match-Wins (khớp luật nào đầu tiên thì áp dụng luật đó). Vì vậy, route ứng viên `/portal/offers/**` (`ROLE_CANDIDATE`) và route duyệt `/offers/*/approve` (`ROLE_DIRECTOR`) bắt buộc phải đặt trước route nội bộ tổng quát `/offers/**` (`ROLE_HR`, `ROLE_DIRECTOR`). Nếu đảo ngược, quyền rộng sẽ nuốt mất quyền hẹp.
* **Dẫn chứng code:** [`SecurityConfig.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/config/SecurityConfig.java) dòng 118 đặt trước dòng 132 và dòng 134.

#### Câu 22: Lỗ hổng IDOR là gì và hệ thống phòng chống nó tại Candidate Portal như thế nào?
* **Đáp án ngắn:** IDOR là lỗ hổng cho phép người dùng xem dữ liệu người khác bằng cách đổi tham số ID trên URL. Hệ thống chống bằng cách ép buộc so khớp `userId` trong câu query.
* **Giải thích chuyên sâu:** Khi ứng viên truy cập `/portal/offers/21`, hệ thống không dùng `findById(21)` thuần túy. Phương thức `findVisibleById(21, currentUserId, ...)` yêu cầu hồ sơ phải thuộc về đúng `userId` đang đăng nhập. Nếu không khớp, repository trả về rỗng và ném lỗi HTTP 404, kẻ tấn công không thể đọc trộm thư mời của người khác.
* **Dẫn chứng code:** [`CandidateOfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/CandidateOfferServiceImpl.java) dòng 109–111.

#### Câu 23: CSRF là loại tấn công gì và token bảo vệ hoạt động ra sao trong các form của Offer?
* **Đáp án ngắn:** CSRF lừa người dùng thực thi các hành động ngoài ý muốn trên ứng dụng mà họ đã đăng nhập. Hệ thống bảo vệ bằng chuỗi CSRF Token bí mật duy nhất trong mỗi session.
* **Giải thích chuyên sâu:** Mỗi khi người dùng mở form, Spring Security sinh một mã token ngẫu nhiên nhúng vào trường ẩn `_csrf`. Khi gửi request POST, token gửi lên được so sánh với token trong session. Website giả mạo của hacker dù có lừa người dùng gửi request cũng không thể biết được token bí mật này nên sẽ bị chặn lại (HTTP 403).
* **Dẫn chứng code:** [`SecurityConfig.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/config/SecurityConfig.java) dòng 65–66 và JavaScript đọc meta `_csrf` trong [`offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/offers.js).

#### Câu 24: Tại sao tài khoản `ROLE_CANDIDATE` khi truy cập `/offers` lại nhận mã lỗi HTTP 403 chứ không phải 404?
* **Đáp án ngắn:** Vì ứng viên đã đăng nhập thành công (Authentication OK) nhưng không có thẩm quyền truy cập tài nguyên nội bộ (Authorization FAILED).
* **Giải thích chuyên sâu:** Mã HTTP 401 dùng cho người chưa đăng nhập. Khi ứng viên đã đăng nhập và mang quyền `ROLE_CANDIDATE`, bộ lọc `FilterSecurityInterceptor` kiểm tra thấy URL `/offers` chỉ cho phép `ROLE_HR` và `ROLE_DIRECTOR`. Vì thiếu quyền, hệ thống trả về HTTP 403 Forbidden theo đúng chuẩn giao thức web.
* **Dẫn chứng code:** [`SecurityConfig.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/config/SecurityConfig.java) dòng 134–135.

#### Câu 25: Phân tích nguyên nhân sâu xa của lỗi BUG-OFF-10 liên quan đến `HttpSessionRequestCache`?
* **Đáp án ngắn:** Do Request Cache lưu nhầm request polling ngầm `/api/notifications/unread-count` khi hết hạn session, khiến user bị redirect vào trang JSON thô sau khi login lại.
* **Giải thích chuyên sâu:** Khi session timeout, JavaScript ở topbar vẫn gửi polling ngầm. Spring Security bắt gặp request chưa xác thực và lưu request này vào session để sau khi login sẽ đưa người dùng quay lại trang họ đang xem dở. Do không loại trừ endpoint API, trang được "nhớ" lại chính là URL JSON thô thay vì trang HTML nghiệp vụ.
* **Dẫn chứng code:** [`SecurityConfig.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/config/SecurityConfig.java) dòng 60–61 và dòng 165.

#### Câu 26: Tại sao phương thức `populateCommonAttributes` lại có thể ném `AccessDeniedException` ngay trong `@ModelAttribute`?
* **Đáp án ngắn:** Để phòng thủ chiều sâu ngay tại Controller, chặn đứng người dùng lạ trước khi kịp gọi vào bất kỳ phương thức xử lý nghiệp vụ nào.
* **Giải thích chuyên sâu:** `@ModelAttribute` method chạy trước mọi `@GetMapping`/`@PostMapping`. Nếu tài khoản có vai trò lạ lọt qua được lớp filter mạng, Controller sẽ kiểm tra lại `Principal`. Nếu không thuộc HR hoặc Giám đốc, ngoại lệ `AccessDeniedException` được ném ra ngay lập tức, đảm bảo an toàn tối đa.
* **Dẫn chứng code:** [`OfferController.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/controller/OfferController.java) dòng 57–59.

#### Câu 27: Làm sao ngăn chặn việc HR tự phê duyệt thư mời thay cho Giám đốc?
* **Đáp án ngắn:** Khóa chặt quyền ở cả tầng Spring Security Route (`ROLE_DIRECTOR`), tầng Controller method và tầng giao diện (ẩn nút Duyệt đối với HR).
* **Giải thích chuyên sâu:** Tại `SecurityConfig.java`, method `POST /offers/*/approve` chỉ cấp quyền duy nhất cho `ROLE_DIRECTOR`. Nếu HR cố tình dùng Postman hoặc sửa code giao diện để gửi request, Spring Security sẽ chặn đứng ở tầng Filter với mã HTTP 403.
* **Dẫn chứng code:** [`SecurityConfig.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/config/SecurityConfig.java) dòng 132–133.

#### Câu 28: Thuộc tính `isLockedCandidate` trong form tạo Offer giải quyết vấn đề bảo mật/nghiệp vụ gì?
* **Đáp án ngắn:** Khóa cố định ứng viên khi HR đi từ màn hình chi tiết hồ sơ ứng tuyển sang tạo Offer, ngăn chọn nhầm ứng viên khác.
* **Giải thích chuyên sâu:** Khi HR đang thẩm định hồ sơ của Ứng viên A tại `/applications/10` và bấm nút "Lập Offer", URL sẽ mang tham số `/offers/create?applicationId=10`. Controller gán `isLockedCandidate = true`, giao diện sẽ disable dropdown chọn ứng viên để HR không vô tình chọn nhầm Ứng viên B.
* **Dẫn chứng code:** [`OfferController.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/controller/OfferController.java) dòng 185.

#### Câu 29: Làm thế nào để bảo vệ file tải về `/offers/export` không bị truy cập trái phép?
* **Đáp án ngắn:** Kiểm tra quyền `ROLE_HR` hoặc `ROLE_DIRECTOR` ngay trong method Controller trước khi sinh stream dữ liệu Excel.
* **Giải thích chuyên sâu:** Endpoint `POST /offers/export` kiểm tra quyền của `Principal`. Nếu không thuộc HR hoặc Director, hệ thống trả về `ResponseEntity.status(HttpStatus.FORBIDDEN).build()`, ngăn chặn việc rò rỉ dữ liệu bảng lương doanh nghiệp ra bên ngoài.
* **Dẫn chứng code:** [`OfferController.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/controller/OfferController.java) dòng 513–520.

#### Câu 30: Tại sao các trạng thái nội bộ của Offer (`Draft`, `Pending_Director`) lại vô hình hoàn toàn với Ứng viên?
* **Đáp án ngắn:** Để bảo mật thông tin nội bộ; ứng viên chỉ được biết khi mức đãi ngộ đã được phê duyệt chính thức và gửi đi (`Sent_Candidate`).
* **Giải thích chuyên sâu:** Trong `CandidateOfferServiceImpl`, biến `CANDIDATE_VISIBLE_STATUSES` chỉ chứa 3 trạng thái: `Sent_Candidate`, `Accepted`, `Declined`. Các câu truy vấn tại `CandidateOfferRepository` luôn có điều kiện `AND offerStatus IN :statuses`, loại bỏ hoàn toàn các bản nháp hoặc các đề xuất đang chờ Giám đốc duyệt khỏi tầm nhìn của ứng viên.
* **Dẫn chứng code:** [`CandidateOfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/CandidateOfferServiceImpl.java) dòng 48.

---

### Nhóm 4: Quy tắc Nghiệp vụ & Vòng đời Thư mời

#### Câu 31: Hãy trình bày vòng đời hoàn chỉnh của một Thư mời nhận việc từ khi tạo đến kết thúc?
* **Đáp án ngắn:** `Draft` $\rightarrow$ `Pending_Director` $\rightarrow$ `Director_Approved` $\rightarrow$ `Sent_Candidate` $\rightarrow$ `Accepted` hoặc `Declined`.
* **Giải thích chuyên sâu:** HR lập bản nháp (`Draft`). Khi hoàn thiện, nộp trình Giám đốc (`Pending_Director`). Giám đốc duyệt (`Director_Approved`) hoặc từ chối (`Director_Rejected`). Khi đã duyệt, HR phát hành thư mời (`Sent_Candidate`), kích hoạt hạn phản hồi và gửi email. Cuối cùng ứng viên đăng nhập Portal để Chấp thuận (`Accepted`) hoặc Từ chối (`Declined`).
* **Dẫn chứng code:** State machine matrix trong [`offer-management-flow-analysis.md`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/docs/members/huyenpt/offer-management-flow-analysis.md).

#### Câu 32: Quy tắc 85% lương thử việc được quy định như thế nào trong hệ thống?
* **Đáp án ngắn:** Lương thử việc (`ProbationSalary`) phải $\ge 85\%$ lương chính thức (`ProposedSalary`) theo Bộ luật Lao động Việt Nam.
* **Giải thích chuyên sâu:** Được cài đặt qua quy tắc mã hiệu `BR-OFF-01`. Hệ thống kiểm tra: `probationSalary >= proposedSalary * 0.85` và đồng thời lương thử việc không được vượt quá lương chính thức. Được bảo vệ đa tầng: JS gợi ý, Bean Validation chặn, Service kiểm tra và SQL Check Constraint bảo vệ.
* **Dẫn chứng code:** [`CreateOfferRequest.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/dto/CreateOfferRequest.java) dòng 98–108.

#### Câu 33: Điều gì xảy ra với trạng thái Hồ sơ ứng tuyển (`Application`) khi Thư mời được phát hành tới Ứng viên?
* **Đáp án ngắn:** `ApplicationStatus` tự động chuyển từ `Interviewing` (hoặc `HM_Passed`) sang `Offered`.
* **Giải thích chuyên sâu:** Khi HR bấm "Phát hành Thư mời", phương thức `sendOfferToCandidate()` trong `OfferServiceImpl` sẽ lấy thực thể `Application` liên kết, gọi `application.setApplicationStatus("Offered")` và lưu lại trong cùng transaction, giúp đồng bộ tiến trình tuyển dụng.
* **Dẫn chứng code:** [`OfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java) dòng 663–667.

#### Câu 34: Khi Ứng viên bấm "Đồng ý nhận việc", những bảng dữ liệu nào được cập nhật và trạng thái chuyển sang là gì?
* **Đáp án ngắn:** Bảng `OfferProposal` chuyển sang `Accepted`; Bảng `Application` chuyển sang `Hired`.
* **Giải thích chuyên sâu:** Thao tác này là giao dịch nguyên tử. `CandidateOfferRepository.answerIfSent` đổi trạng thái Offer sang `Accepted`. Ngay sau đó, `ApplicationStatusService.onOfferAnswered` cập nhật Application sang `Hired` (Đã tuyển dụng thành công), kết thúc thắng lợi quy trình tuyển dụng cho ứng viên đó.
* **Dẫn chứng code:** [`CandidateOfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/CandidateOfferServiceImpl.java) dòng 120–125.

#### Câu 35: Khi Ứng viên "Từ chối nhận việc", hệ thống lưu vết những thông tin gì?
* **Đáp án ngắn:** Lưu trạng thái `Declined`, lưu lý do từ chối vào `DeclineReason`, chuyển Application sang `Rejected` và bắn thông báo cho HR.
* **Giải thích chuyên sâu:** Ứng viên được yêu cầu chọn hoặc nhập lý do từ chối (tối đa 1000 ký tự). Dữ liệu này được ghi vào cột `DeclineReason` của bảng `OfferProposal` phục vụ công tác báo cáo phân tích lý do trượt ứng viên sau này của phòng nhân sự.
* **Dẫn chứng code:** [`CandidateOfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/CandidateOfferServiceImpl.java) dòng 104–106.

#### Câu 36: Tại sao Giám đốc khi từ chối đề xuất nhận việc thì bắt buộc phải nhập lý do?
* **Đáp án ngắn:** Để HR hiểu rõ nguyên nhân không được duyệt (ví dụ: lương quá cao, thiếu ngân sách) và có cơ sở điều chỉnh lại đề xuất.
* **Giải thích chuyên sâu:** Đây là yêu cầu nghiệp vụ bắt buộc được chuẩn hóa tại commit `339b25c8`. Nếu Giám đốc để trống ô nhận xét, cả tầng JavaScript lẫn tầng Controller Backend đều từ chối xử lý và yêu cầu nhập lý do trước khi tiếp tục.
* **Dẫn chứng code:** [`OfferController.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/controller/OfferController.java) dòng 489–492.

#### Câu 37: Hạn phản hồi `ResponseDeadline` được tính toán như thế nào và có ràng buộc gì với Ngày bắt đầu làm việc?
* **Đáp án ngắn:** Tính bằng: Thời điểm phát hành + `ResponsePeriodDays`. Ràng buộc: Deadline phải trước ngày bắt đầu làm việc dự kiến.
* **Giải thích chuyên sâu:** Khi HR bấm gửi thư mời, hệ thống tính `deadline = now.plusDays(responsePeriodDays)`. Nếu `deadline` lớn hơn hoặc bằng `expectedStartDate`, hệ thống ném `OfferValidationException` vì không thể để hạn trả lời sau khi công việc đã bắt đầu.
* **Dẫn chứng code:** [`OfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java) dòng 636–641.

#### Câu 38: Cơ chế Hồi sinh (Reactivation) giải quyết triệt để lỗi BUG-OFF-11 như thế nào?
* **Đáp án ngắn:** Kiểm tra nếu đã có bản ghi cũ bị xóa mềm (`isDeleted = true`) thì cập nhật lại thông tin mới và set `isDeleted = false` thay vì chạy INSERT.
* **Giải thích chuyên sâu:** Do có ràng buộc `UQ_OfferProposal_Application`, việc chạy INSERT mới cho ứng viên từng có Offer bị xóa mềm sẽ văng lỗi HTTP 500 vi phạm khóa Unique. Cơ chế Reactivation tận dụng chính bản ghi cũ đó, ghi đè dữ liệu mới và hủy cờ xóa mềm, vượt qua ràng buộc CSDL hoàn toàn hợp lệ.
* **Dẫn chứng code:** [`OfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java) dòng 249–265.

#### Câu 39: HR có thể xóa một Offer khi nó đang ở trạng thái `Pending_Director` hoặc `Director_Approved` không?
* **Đáp án ngắn:** Không thể. Hệ thống chỉ cho phép xóa khi Offer ở trạng thái `Draft`.
* **Giải thích chuyên sâu:** Một khi đã nộp trình duyệt hoặc đã được phê duyệt, dữ liệu đã trở thành tài sản nghiệp vụ chính thức có sự tham gia của Giám đốc. Nếu cho phép xóa tùy tiện sẽ làm sai lệch báo cáo và mất dấu vết phê duyệt. Service sẽ ném `OfferValidationException` nếu trạng thái khác `Draft`.
* **Dẫn chứng code:** [`OfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java) dòng 604–606.

#### Câu 40: Điều gì xảy ra khi thời gian hiện tại vượt quá `ResponseDeadline` trên Candidate Portal?
* **Đáp án ngắn:** Giao diện tự động khóa nút Chấp thuận/Từ chối và hiển thị huy hiệu "Đã quá hạn phản hồi".
* **Giải thích chuyên sâu:** Cờ `canRespond` trả về `false`. Nếu ứng viên cố tình gửi request POST lên server, phương thức `requireAnswerable()` trong Service sẽ phát hiện `now.isAfter(responseDeadline)` và ném `OfferResponseException`, ngăn chặn mọi tương tác muộn.
* **Dẫn chứng code:** [`CandidateOfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/CandidateOfferServiceImpl.java) dòng 146–150.

---

### Nhóm 5: Frontend, JavaScript & Tương tác UX

#### Câu 41: Tại sao nhóm lại chuyển toàn bộ Modal từ SweetAlert2 sang HTML5 `<dialog>` native?
* **Đáp án ngắn:** Để loại bỏ hoàn toàn sự phụ thuộc vào mạng CDN bên ngoài (Zero Dependency), khắc phục lỗi `Swal is not defined` (BUG-OFF-03).
* **Giải thích chuyên sâu:** SweetAlert2 tải từ CDN internet. Khi chạy trong mạng nội bộ công ty bị chặn firewall hoặc khi mất mạng, thư viện không tải được làm tê liệt các nút bấm trên giao diện. Thẻ `<dialog>` có sẵn trong trình duyệt, tải trang siêu nhanh, không tốn thêm byte dữ liệu mạng nào.
* **Dẫn chứng code:** Commit `da95239a` và [`offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/offers.js).

#### Câu 42: Phương thức `showModal()` của HTML5 `<dialog>` khác gì so với phương thức `show()`?
* **Đáp án ngắn:** `showModal()` mở hộp thoại chặn tương tác nền (backdrop) và bẫy tiêu điểm phím, còn `show()` chỉ mở như một popup thông thường.
* **Giải thích chuyên sâu:** Khi dùng `showModal()`, trình duyệt tự động tạo lớp giả `::backdrop` làm tối màn hình phía sau, người dùng không thể click vào các nút bên dưới. Trình duyệt cũng tự động gán phím `Escape` để đóng dialog, tối ưu khả năng tiếp cận (Accessibility).
* **Dẫn chứng code:** [`offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/offers.js) dòng 444: `modal.showModal()`.

#### Câu 43: JavaScript phòng chống lỗi Double Click (bấm 2 lần liên tiếp) khi submit như thế nào?
* **Đáp án ngắn:** Ngay khi nhận sự kiện submit, JavaScript vô hiệu hóa nút bấm (`disabled = true`) và đổi nhãn thành "Đang xử lý...".
* **Giải thích chuyên sâu:** Tránh việc người dùng sốt ruột bấm nút nhiều lần gửi liên tiếp các HTTP request khi mạng chậm. Nếu server phản hồi xong hoặc gặp lỗi, nút bấm mới được kích hoạt lại trong khối xử lý kết quả.
* **Dẫn chứng code:** [`offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/offers.js) dòng 482–485.

#### Câu 44: Đồng hồ đếm ngược (Countdown Timer) trong `candidate-offers.js` hoạt động ra sao?
* **Đáp án ngắn:** Sử dụng `setInterval()` chạy mỗi 1 giây, lấy `Deadline - Now` rồi tính ra số ngày, giờ, phút, giây còn lại render lên DOM.
* **Giải thích chuyên sâu:** Đọc mốc thời gian từ thuộc tính `data-deadline`. Mỗi giây tính toán chênh lệch mili-giây. Khi khoảng cách $\le 0$, script gọi `clearInterval()` để giải phóng tài nguyên CPU và thay đổi giao diện thành thông báo hết hạn.
* **Dẫn chứng code:** [`candidate-offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/pages/candidate-offers.js) dòng 50–95.

#### Câu 45: Bộ đếm ký tự thời gian thực cho ô lý do từ chối hoạt động như thế nào?
* **Đáp án ngắn:** Bắt sự kiện `input` trên thẻ `<textarea>`, lấy độ dài chuỗi cập nhật vào thẻ `<span>` đếm ký tự (ví dụ: `45/2000`).
* **Giải thích chuyên sâu:** Giúp người dùng biết mình còn được gõ bao nhiêu ký tự, ngăn ngừa việc gõ vượt quá giới hạn gây lỗi cắt cụt dữ liệu (Data Truncation) ở phía server.
* **Dẫn chứng code:** [`offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/offers.js) hàm `handleDirectorDecisionCommentInput`.

#### Câu 46: Cơ chế tương tác của các thẻ KPI Cards trên bàn làm việc của HR và Giám đốc?
* **Đáp án ngắn:** Click vào thẻ KPI sẽ tự động lọc bảng dữ liệu theo trạng thái tương ứng mà không cần chọn dropdown.
* **Giải thích chuyên sâu:** Mỗi thẻ KPI card có thuộc tính `data-status`. Khi người dùng click, JavaScript đọc giá trị này, gán vào URL query param `?status=...` và submit lọc lại bảng dữ liệu, mang lại trải nghiệm tương tác trực quan cao.
* **Dẫn chứng code:** [`offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/offers.js) hàm `filterDirectorOffersByStatus`.

#### Câu 47: Hệ thống Flash Toast thông báo được thiết kế như thế nào để không dùng thư viện ngoài?
* **Đáp án ngắn:** Sử dụng thẻ `<div>` cố định góc màn hình kết hợp CSS Animation và hàm `setTimeout()` tự động xóa sau 4 giây.
* **Giải thích chuyên sâu:** Script tạo động phần tử DOM `<div class="toast toast--success">`, chèn thông điệp và thêm class `show` để kích hoạt hiệu ứng trượt vào. Sau 4 giây, hàm tự gỡ bỏ class và xóa phần tử khỏi DOM, hoàn toàn thuần CSS và Vanilla JS.
* **Dẫn chứng code:** [`offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/offers.js) hàm `showFlashToast`.

#### Câu 48: Cách xử lý tự động tính gợi ý 85% lương thử việc phía client?
* **Đáp án ngắn:** Bắt sự kiện `input` trên ô Lương chính thức, lấy giá trị nhân `0.85` rồi gán tự động vào ô Lương thử việc.
* **Giải thích chuyên sâu:** Khi HR gõ số tiền (ví dụ: 20 triệu), script làm sạch định dạng dấu phẩy, nhân với 0.85 (ra 17 triệu) và gán vào trường `probationSalary`. HR vẫn có quyền chỉnh sửa nâng mức lương thử việc lên cao hơn nếu muốn, nhưng không thể hạ xuống dưới 85%.
* **Dẫn chứng code:** Form logic trong `offers/form.html` và [`offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/offers.js).

#### Câu 49: Tại sao form từ chối của ứng viên lại có các chip gợi ý lý do (Reason Chips)?
* **Đáp án ngắn:** Giúp ứng viên chọn nhanh lý do phổ biến (đã nhận việc khác, lương chưa phù hợp) chỉ bằng 1 cú click.
* **Giải thích chuyên sâu:** Nâng cao trải nghiệm khảo sát (Survey UX). Khi click vào chip, JavaScript đọc thuộc tính `data-reason` và tự động điền chuỗi văn bản mẫu vào `<textarea>` để ứng viên chỉnh sửa bổ sung nếu cần.
* **Dẫn chứng code:** [`candidate-offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/pages/candidate-offers.js) hàm `initReasonChips`.

#### Câu 50: Làm sao xử lý hiện tượng "Giật giao diện" (Flash of Unstyled Content - FOUC) khi tải Countdown Timer?
* **Đáp án ngắn:** Gọi hàm cập nhật thời gian `update()` ngay lập tức 1 lần trước khi thiết lập vòng lặp `setInterval(update, 1000)`.
* **Giải thích chuyên sâu:** Nếu chỉ gọi `setInterval`, trình duyệt sẽ đợi hết 1000ms đầu tiên mới tính toán thời gian, khiến người dùng nhìn thấy chuỗi thô trong giây đầu tiên. Gọi `update()` ngay lập tức giúp hiển thị thời gian chính xác ngay khi DOM vừa render.
* **Dẫn chứng code:** [`candidate-offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/pages/candidate-offers.js) dòng 84–85.

---

### Nhóm 6: Kiểm thử Phần mềm, Debug & Xử lý Sự cố

#### Câu 51: Sự khác biệt giữa Unit Test và Integration Test trong module Offer?
* **Đáp án ngắn:** Unit Test kiểm thử logic nghiệp vụ cô lập có Mockito; Integration Test kiểm thử tích hợp toàn diện qua MockMvc xuyên suốt từ Filter, Controller tới Database.
* **Giải thích chuyên sâu:** `OfferServiceTests` (Unit Test) mock toàn bộ repository để kiểm tra các nhánh logic tính lương 85% và deadline. `OfferIntegrationTests` (Integration Test) khởi động Spring Web Context với MockMvc để kiểm tra phân quyền URL, CSRF token và view Thymeleaf trả về thực tế.
* **Dẫn chứng code:** [`OfferServiceTests.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/test/java/com/group2/rms/offer/service/OfferServiceTests.java) và [`OfferIntegrationTests.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/test/java/com/group2/rms/offer/OfferIntegrationTests.java).

#### Câu 52: Phương thức kiểm thử `withMockUser` trong Spring Security Test hoạt động như thế nào?
* **Đáp án ngắn:** Giả lập sẵn một đối tượng `Authentication` có tên đăng nhập và vai trò chỉ định nạp vào `SecurityContext`.
* **Giải thích chuyên sâu:** Cho phép test phân quyền mà không cần phải gọi API đăng nhập thực tế. Ví dụ `@WithMockUser(roles = "HR")` sẽ cho phép test các endpoint của HR, và kiểm tra xem tài khoản này có bị chặn HTTP 403 khi gọi vào endpoint của Giám đốc hay không.
* **Dẫn chứng code:** [`OfferIntegrationTests.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/test/java/com/group2/rms/offer/OfferIntegrationTests.java) dòng 118 `@WithMockUser(authorities = "ROLE_HR")`.

#### Câu 53: Em đã thiết kế ca kiểm thử nào để chứng minh hệ thống chống được lỗ hổng IDOR?
* **Đáp án ngắn:** Giả lập đăng nhập bằng Ứng viên A (`candidateA@test.com`) nhưng gọi API xem thư mời của Ứng viên B (`offerOfCandidateB`).
* **Giải thích chuyên sâu:** Trong `CandidateOfferWebTests`, test case `candidateCannotViewOthersOffer` thực hiện request `GET /portal/offers/{idOfCandidateB}`. Kết quả mong đợi là hệ thống trả về mã lỗi HTTP 404 Not Found (hoặc 403), chứng minh Ứng viên A không thể xem trộm thư mời của người khác.
* **Dẫn chứng code:** [`CandidateOfferWebTests.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/test/java/com/group2/rms/offer/CandidateOfferWebTests.java).

#### Câu 54: Làm thế nào để tái hiện và kiểm chứng lỗi BUG-OFF-01 (SpEL `isLockedCandidate`) bằng kiểm thử tự động?
* **Đáp án ngắn:** Viết test gọi `GET /offers/{id}/edit` và `GET /offers/create` không truyền param, kiểm tra model có chứa thuộc tính `isLockedCandidate`.
* **Giải thích chuyên sâu:** Trong `OfferControllerBugTests`, test case gọi vào form và assert kết quả `model().attributeExists("isLockedCandidate")`. Nếu Controller quên đưa thuộc tính này vào Model, Thymeleaf parsing engine sẽ ném lỗi SpEL `Property or field cannot be found` và test case sẽ FAIL ngay.
* **Dẫn chứng code:** [`OfferControllerBugTests.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/test/java/com/group2/rms/offer/controller/OfferControllerBugTests.java) dòng 69–85.

#### Câu 55: Khi server gặp lỗi HTTP 500, em sử dụng quy trình debug nào để tìm ra nguyên nhân gốc rễ?
* **Đáp án ngắn:** Đọc Stack Trace trong console log, tìm dòng `Caused by:` đầu tiên để xác định chính xác file và dòng code gây lỗi.
* **Giải thích chuyên sâu:** Bước 1: Xem file log server (`offer-server.log`). Bước 2: Bỏ qua các frame của Spring framework, tìm đến package `com.group2.rms`. Bước 3: Xác định loại ngoại lệ (ví dụ: `NullPointerException`, `DataIntegrityViolationException`). Bước 4: Tái hiện lại bằng Unit Test để viết code sửa lỗi và xác nhận lại.

#### Câu 56: Làm thế nào để kiểm thử tính năng gửi Email mà không làm phiền hòm thư thật của khách hàng?
* **Đáp án ngắn:** Sử dụng `MockMailSender` hoặc lớp `OfferMailSender` có cơ chế fallback in log `[MOCK EMAIL]` trong môi trường test.
* **Giải thích chuyên sâu:** Trong profile test, không cấu hình thông số SMTP. Khi gọi hàm gửi mail, hệ thống kiểm tra `mailSender.getIfAvailable() == null` và ghi ra dòng log kiểm thử thay vì gửi kết nối TCP ra ngoài internet, giúp test suite chạy nhanh và không tốn chi phí gửi mail.
* **Dẫn chứng code:** [`OfferMailSender.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferMailSender.java) dòng 84–86.

#### Câu 57: Test case nào kiểm chứng tính năng bắt buộc nhập lý do khi Giám đốc từ chối?
* **Đáp án ngắn:** Test case `it17b_rejectOfferJson_emptyComments_returnsBadRequest` trong `OfferIntegrationTests`.
* **Giải thích chuyên sâu:** Test case gửi một request `POST /offers/{id}/reject` với payload JSON rỗng `{"comments": "   "}`. Kết quả mong đợi là server kiên quyết từ chối với HTTP Status `400 Bad Request` và thông điệp: *"Vui lòng nhập lý do từ chối vào ô nhận xét trước khi tiếp tục."*.
* **Dẫn chứng code:** [`OfferIntegrationTests.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/test/java/com/group2/rms/offer/OfferIntegrationTests.java) dòng 453–465.

#### Câu 58: Tại sao khi test xuất Excel, test case lại kiểm tra Magic Number của mảng byte?
* **Đáp án ngắn:** Để đảm bảo file sinh ra là file Excel chuẩn định dạng (.xlsx - OOXML Zip) chứ không phải file văn bản lỗi trá hình.
* **Giải thích chuyên sâu:** File Excel `.xlsx` thực chất là một gói nén Zip. Hai byte đầu tiên của file Zip luôn luôn là `0x50, 0x4B` (ký tự 'PK'). Trong `OfferExportServiceTests`, việc assert 2 byte đầu tiên bằng `PK` chứng minh thư viện Apache POI đã đóng gói đúng cấu trúc binary của bảng tính Excel.
* **Dẫn chứng code:** [`OfferExportServiceTests.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/test/java/com/group2/rms/offer/OfferExportServiceTests.java).

#### Câu 59: Làm sao kiểm thử được logic hết hạn Deadline khi đồng hồ máy tính luôn chạy về phía trước?
* **Đáp án ngắn:** Tiêm đối tượng `java.time.Clock` vào Service và mock thời gian của Clock trong Unit Test.
* **Giải thích chuyên sâu:** Thay vì gọi `LocalDateTime.now()` cứng, `CandidateOfferServiceImpl` nhận `Clock` qua constructor. Trong test case, ta có thể dùng `Clock.fixed(futureInstant, ZoneId.of("UTC"))` để "du hành thời gian" tới tương lai sau hạn deadline, giúp kiểm tra nhánh logic hết hạn một cách hoàn toàn tất định (Deterministic).
* **Dẫn chứng code:** [`CandidateOfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/CandidateOfferServiceImpl.java) dòng 54 và dòng 145.

#### Câu 60: Quy trình kiểm thử hồi quy (Regression Testing) sau khi fix lỗi BUG-OFF-11 được thực hiện thế nào?
* **Đáp án ngắn:** Tạo Offer $\rightarrow$ Xóa mềm bản nháp $\rightarrow$ Tạo lại Offer mới cho cùng ứng viên $\rightarrow$ Kiểm tra HTTP Status 200/302 và CSDL có `IsDeleted = 0`.
* **Giải thích chuyên sâu:** Được tự động hóa trong `OfferControllerBugTests`. Kịch bản tái hiện lại chính xác chuỗi thao tác từng gây lỗi HTTP 500 trước đây. Nếu test case hoàn thành với mã trạng thái thành công và bản ghi được tái kích hoạt đúng, ta khẳng định bug đã được khắc phục triệt để và không làm ảnh hưởng các luồng khác.
* **Dẫn chứng code:** [`OfferControllerBugTests.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/test/java/com/group2/rms/offer/controller/OfferControllerBugTests.java).

---

# PHẦN 9. ĐIỂM YẾU KỸ THUẬT, HẠN CHẾ & GIẢI PHÁP NÂNG CẤP

Trong buổi bảo vệ đồ án tốt nghiệp, sinh viên chủ động chỉ ra các điểm yếu kỹ thuật hiện tại và đưa ra giải pháp nâng cấp rõ ràng sẽ được hội đồng đánh giá rất cao về tư duy phản biện và năng lực kiến trúc phần mềm thực tế.

---

### Điểm yếu 1: Lỗ hổng Trôi URL khi Session Timeout (`BUG-OFF-10`)
* **Hiện trạng trong code:** Tại [`SecurityConfig.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/config/SecurityConfig.java) (dòng 60–61 và 165), cấu hình `HttpSessionRequestCache` chưa có bộ lọc loại trừ các endpoint `/api/**`.
* **Rủi ro:** Khi người dùng mở trang web và để máy đó quá 30 phút, script polling ngầm `/api/notifications/unread-count` sẽ bị lưu làm mục tiêu chuyển hướng. Khi người dùng đăng nhập lại, trình duyệt bị đẩy vào trang trắng in chuỗi JSON thô `{"unreadCount": 0}`.
* **Giải pháp khắc phục đề xuất:**
  Bổ sung `NegatedRequestMatcher` loại trừ toàn bộ request API trong `SecurityConfig.java`:
  ```java
  HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
  requestCache.setRequestMatcher(new NegatedRequestMatcher(
      new AntPathRequestMatcher("/api/**")
  ));
  http.requestCache(cache -> cache.requestCache(requestCache));
  ```

---

### Điểm yếu 2: Quản lý Trạng thái Bằng Chuỗi Ký Tự (`String`) Thay Vì Java Enum
* **Hiện trạng trong code:** Cột `OfferStatus` trong thực thể `OfferProposal` được khai báo là kiểu `String`. Các trạng thái được so sánh bằng chuỗi (`"Draft"`, `"Pending_Director"`, `"Sent_Candidate"`).
* **Rủi ro:** Không có tính Type-safe trong lúc biên dịch. Lập trình viên có thể viết nhầm chính tả (ví dụ `"draft"` chữ thường hoặc `"Approved"` thay vì `"Director_Approved"`), dẫn tới lỗi logic tiềm ẩn chỉ phát hiện được khi chạy ứng dụng.
* **Giải pháp khắc phục đề xuất:**
  Định nghĩa Enum `OfferStatus` chuẩn và sử dụng annotation `@Enumerated(EnumType.STRING)` hoặc JPA `AttributeConverter` để đảm bảo an toàn tuyệt đối ở tầng mã nguồn Java.

---

### Điểm yếu 3: Ràng buộc Cứng 1 Thư mời trên 1 Hồ sơ Ứng tuyển (`UQ_OfferProposal_Application`)
* **Hiện trạng trong code:** Bảng `OfferProposal` áp dụng ràng buộc Unique trên cột `ApplicationId`.
* **Rủi ro:** Hệ thống không hỗ trợ quy trình thương lượng lương nhiều vòng (Re-negotiation). Nếu ứng viên từ chối Offer đợt 1 và HR đàm phán lại mức lương mới, hệ thống buộc phải ghi đè lên bản ghi cũ, làm mất đi dữ liệu lịch sử của đợt chào lương đầu tiên.
* **Giải pháp khắc phục đề xuất:**
  Chuyển Unique Constraint từ `(ApplicationId)` sang cặp khóa phức hợp `(ApplicationId, RevisionNumber)` hoặc sử dụng Filtered Unique Index trên SQL Server:
  ```sql
  CREATE UNIQUE NONCLUSTERED INDEX UQ_ActiveOffer_Application 
  ON OfferProposal (ApplicationId) WHERE IsActive = 1;
  ```

---

### Điểm yếu 4: Gửi Email Đồng Bộ (Synchronous) Trong Giao Dịch
* **Hiện trạng trong code:** Phương thức `sendOfferToCandidate()` trong `OfferServiceImpl` gọi trực tiếp `offerMailSender.sendOfferLetterToCandidate()` bên trong khối `@Transactional`.
* **Rủi ro:** Việc kết nối tới máy chủ SMTP qua giao thức mạng bên ngoài có độ trễ lớn (thường mất từ 1 đến 3 giây). Trong thời gian đó, kết nối cơ sở dữ liệu (Database Connection) bị giữ chặt trong Connection Pool, làm giảm khả năng chịu tải đồng thời của hệ thống.
* **Giải pháp khắc phục đề xuất:**
  Áp dụng mô hình hướng sự kiện (Event-driven Architecture): Bắn sự kiện `OfferSentEvent` và sử dụng `@Async` kết hợp `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` để gửi email trong một Thread riêng biệt sau khi database đã commit thành công.

---

### Điểm yếu 5: Thiếu Bộ Kiểm Thử Tự Động Giao Diện End-to-End (E2E Testing)
* **Hiện trạng trong code:** Hệ thống hiện có 27 ca MockMvc và 41 ca Unit Test rất tốt, nhưng chưa có kịch bản kiểm thử tự động trên trình duyệt thật (như Selenium WebDriver hoặc Playwright).
* **Rủi ro:** Các thao tác đặc thù của giao diện phía client như: hiển thị modal native HTML5 `<dialog>`, hiệu ứng đếm ngược Countdown Timer, sự kiện phím `Escape` đóng modal chưa được kiểm tra tự động trong quy trình CI/CD.
* **Giải pháp khắc phục đề xuất:**
  Tích hợp bộ kiểm thử Playwright Java để tự động mở trình duyệt Chromium không đầu (Headless Browser) giả lập hành vi click của HR và Ứng viên trên môi trường staging.

---

# PHẦN 10. KỊCH BẢN THUYẾT TRÌNH & DEMO TỐT NGHIỆP

---

### 10.1. Kịch bản Thuyết trình 5 phút (Tập trung Trọng tâm)

* **00:00 – 01:00 | Giới thiệu Bài toán:**
  > *"Kính thưa Hội đồng, em là HuyenPT, đại diện nhóm phát triển phân hệ **Quản lý Đề xuất và Thư mời nhận việc (Offer Management)** trong hệ thống Mộc RMS. Bài toán trọng tâm của phân hệ này là chuẩn hóa quy trình phê duyệt lương 2 cấp giữa HR và Ban Giám đốc, đảm bảo tuân thủ nghiêm ngặt Luật Lao động Việt Nam về mức sàn lương thử việc 85%, đồng thời cung cấp cổng Portal trang trọng để Ứng viên xem và phản hồi thư mời trực tuyến."*

* **01:00 – 02:30 | Demo Luồng HR Lập Offer & Giám đốc Duyệt nhanh:**
  > *"Em xin phép demo luồng khởi tạo: HR vào danh sách ứng viên đỗ tuyển dụng, bấm 'Lập Offer'. Hệ thống tự động khóa ứng viên qua cơ chế Contextual Locking. Khi em nhập mức lương chính thức 20 triệu, JavaScript tự động gợi ý mức lương thử việc 17 triệu đúng 85%. Em bấm 'Trình Giám đốc phê duyệt'. Ngay lập tức, thẻ KPI chuyển sang 'Chờ Giám đốc duyệt'. Đăng nhập tài khoản Giám đốc, một modal native HTML5 `<dialog>` mở ra, Giám đốc nhập nhận xét và bấm 'Phê duyệt nhanh' qua Ajax mà không phải reload trang."*

* **02:30 – 04:00 | Demo Phát hành Thư mời & Ứng viên Chấp thuận:**
  > *"Sau khi được duyệt, HR bấm 'Phát hành Thư mời'. Hệ thống tự động kích hoạt thời hạn phản hồi 5 ngày, gửi email cho ứng viên và chuyển trạng thái hồ sơ ứng tuyển sang `Offered`. Đăng nhập tài khoản Ứng viên trên Candidate Portal, ứng viên xem bức thư mời trang trọng có đồng hồ đếm ngược thời gian thực. Khi ứng viên bấm 'Đồng ý nhận việc', cơ chế Atomic Update SQL Server được kích hoạt chống Race Condition, đồng thời hồ sơ ứng tuyển tự động nhảy sang trạng thái `Hired` (Đã tuyển dụng thành công)."*

* **04:00 – 05:00 | Điểm sáng Kỹ thuật & Kết luận:**
  > *"Điểm sáng kỹ thuật của phân hệ là kiến trúc phòng thủ đa tầng cho quy tắc 85% lương, cơ chế Reactivation xử lý triệt để xung đột giữa Soft Delete và Unique Constraint, và khả năng phòng chống IDOR tuyệt đối tại cổng Portal. Em xin kết thúc phần trình bày và sẵn sàng nhận câu hỏi từ Hội đồng."*

---

### 10.2. Kịch bản Thuyết trình 10 phút (Kỹ thuật Chuyên sâu & Điểm sáng)

* **00:00 – 02:00 | Giới thiệu Kiến trúc & State Machine:**
  * Trình chiếu sơ đồ State Machine 7 trạng thái của Offer.
  * Giới thiệu mô hình Hybrid Controller (Thymeleaf MVC + JSON Ajax).
  * Giải thích cơ chế phân tầng và nguyên lý Single Responsibility.

* **02:00 – 04:00 | Demo Phòng thủ 4 Tầng của Quy tắc 85% Lương:**
  * Thử cố tình nhập lương thử việc 15 triệu cho lương chính thức 20 triệu (75%).
  * Chỉ ra: Tầng JS cảnh báo đỏ; Tầng DTO `@AssertTrue` chặn đứng; Tầng Service ném `OfferValidationException`; Tầng Database có Check Constraint `CK_OfferProposal_Salaries`.
  * Sửa lại đúng 85% và nộp trình duyệt.

* **04:00 – 06:00 | Demo Phê duyệt 2 Cấp & Bắt buộc Lý do khi Từ chối:**
  * Đăng nhập Giám đốc, mở modal Từ chối.
  * Cố tình để trống lý do $\rightarrow$ Hệ thống chặn submit (chứng minh tính đồng nhất của commit `339b25c8`).
  * Nhập lý do: *"Vượt ngân sách phòng ban"*, bấm Từ chối.
  * Đăng nhập lại HR: Xem timeline lịch sử phê duyệt trong `OfferApproval`, sửa lại lương và nộp duyệt lại lần 2 $\rightarrow$ Giám đốc Phê duyệt thành công.

* **06:00 – 08:00 | Demo Cổng Ứng viên & Chống Lỗ hổng IDOR:**
  * Phát hành thư mời, chỉ ra email log `[MOCK EMAIL]` và thời hạn phản hồi `ResponseDeadline`.
  * Đăng nhập Ứng viên A, mở `/portal/offers/{idA}`.
  * Thử đổi URL sang `{idB}` của người khác $\rightarrow$ Hệ thống trả về HTTP 404 (chứng minh tính năng chống IDOR thành công).
  * Ứng viên A bấm "Đồng ý nhận việc" $\rightarrow$ Thư mời chuyển `Accepted`, hồ sơ ứng tuyển nhảy sang `Hired`.

* **08:00 – 10:00 | Demo Xuất Excel & Tóm tắt Điểm yếu/Khuyến nghị:**
  * Bấm nút Xuất Excel, mở file `.xlsx` được format tiền tệ, border và căn lề chuyên nghiệp bằng Apache POI.
  * Chủ động trình bày 3 điểm yếu kỹ thuật (RequestCache timeout, String status, Async email) và giải pháp nâng cấp trong tương lai.
  * Cảm ơn Hội đồng và chuyển sang phần Hỏi - Đáp.

---

### 10.3. Checklist Chuẩn bị Trước khi Lên bảng

| STT | Hạng mục kiểm tra | Trạng thái đạt yêu cầu | Ghi chú |
| :---: | :--- | :---: | :--- |
| 1 | Dịch vụ SQL Server đang chạy | ĐÃ SẴN SÀNG | Service `MSSQLSERVER` hoặc `SQLEXPRESS` ở trạng thái Running. |
| 2 | Chạy script seed data mẫu | ĐÃ SẴN SÀNG | Có sẵn ít nhất 1 hồ sơ đỗ phỏng vấn (`HM_Passed`) để tạo Offer mới. |
| 3 | Khởi động server Spring Boot | ĐÃ SẴN SÀNG | Chạy lệnh `mvn spring-boot:run` trên cổng `8080`, log in dòng `Started RmsApplication`. |
| 4 | Chuẩn bị sẵn 3 trình duyệt / Profile | ĐÃ SẴN SÀNG | • Chrome: Đăng nhập HR (`lan.hr@rms-tech.vn`)<br>• Edge: Đăng nhập Giám đốc (`hoang.director@rms-tech.vn`)<br>• Cửa sổ ẩn danh: Đăng nhập Ứng viên (`tuan.candidate@gmail.com`). |
| 5 | Kiểm tra mạng và firewall local | ĐÃ SẴN SÀNG | Truy cập `localhost:8080/offers` không bị phần mềm diệt virus chặn port. |
| 6 | Xóa sạch cache trình duyệt (Ctrl+F5) | ĐÃ SẴN SÀNG | Đảm bảo file JavaScript và CSS mới nhất được tải đầy đủ. |

---

### 10.4. Xử lý Sự cố Kỹ thuật Khi Demo

1. **Sự cố 1: Bấm nút trên modal không phản hồi**
   * *Nguyên nhân:* Có thể do lỗi cú pháp JavaScript hoặc token CSRF bị hết hạn.
   * *Xử lý nhanh:* Bấm `F12` mở Console. Nếu lỗi CSRF, nhấn `F5` reload lại trang để lấy token mới. Bình tĩnh giải thích với hội đồng: *"Dạ do session vừa được làm mới, em reload lại để cập nhật CSRF Token bảo mật."*
2. **Sự cố 2: Server báo lỗi HTTP 500 khi tạo Offer**
   * *Nguyên nhân:* Hồ sơ ứng viên đó đã có một Offer đang hoạt động trong database (vi phạm GBR-07).
   * *Xử lý nhanh:* Quay lại danh sách, chọn một ứng viên khác chưa từng có Offer, hoặc vào trang danh sách Offer xóa bản nháp cũ đi để kích hoạt cơ chế Reactivation.
3. **Sự cố 3: Đăng nhập xong bị văng ra trang JSON thô (BUG-OFF-10)**
   * *Nguyên nhân:* Do session bị timeout trong lúc chờ đến lượt báo cáo.
   * *Xử lý nhanh:* Không bấm Back. Gõ trực tiếp trên thanh địa chỉ URL: `localhost:8080/offers` và nhấn Enter. Giải thích tự tin với hội đồng: *"Dạ đây chính là lỗi liên quan đến cơ chế RequestCache khi polling ngầm mà nhóm đã phân tích sâu trong phần điểm yếu kỹ thuật của báo cáo."*

---

# PHỤ LỤC TRA CỨU NHANH

---

### Phụ lục 1: Toàn bộ Endpoints của Module Offer

| HTTP Method | Endpoint URL | Vai trò (Role) | Controller & Method | Kiểu phản hồi |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/offers` | `ROLE_HR`, `ROLE_DIRECTOR` | `OfferController.listOffers` | HTML View (`offers/list` hoặc `offers/director-list`) |
| `GET` | `/offers/create` | `ROLE_HR` | `OfferController.createOfferForm` | HTML View (`offers/form`) |
| `POST` | `/offers/create` | `ROLE_HR` | `OfferController.createOffer` | Redirect `/offers` hoặc HTML View có lỗi |
| `GET` | `/offers/{id}` | `ROLE_HR`, `ROLE_DIRECTOR` | `OfferController.viewOfferDetail` | HTML View (`offers/detail`) |
| `GET` | `/offers/{id}/edit` | `ROLE_HR` | `OfferController.editOfferForm` | HTML View (`offers/form`) |
| `POST` | `/offers/{id}/edit` | `ROLE_HR` | `OfferController.updateOffer` | Redirect `/offers/{id}` |
| `POST` | `/offers/{id}/delete` | `ROLE_HR` | `OfferController.deleteOffer` | Redirect `/offers` |
| `POST` | `/offers/{id}/send` | `ROLE_HR` | `OfferController.sendOfferToCandidate` | Redirect `/offers/{id}` |
| `POST` | `/offers/{id}/approve` | `ROLE_DIRECTOR` | `OfferController.approveOfferJson` | JSON `ApiResponse<OfferResponse>` |
| `POST` | `/offers/{id}/reject` | `ROLE_DIRECTOR` | `OfferController.rejectOfferJson` | JSON `ApiResponse<OfferResponse>` |
| `POST` | `/offers/export` | `ROLE_HR`, `ROLE_DIRECTOR` | `OfferController.exportOffers` | Binary Stream `byte[]` (.xlsx) |
| `GET` | `/portal/offers` | `ROLE_CANDIDATE` | `CandidateOfferController.list` | HTML View (`offers/candidate-list`) |
| `GET` | `/portal/offers/{id}` | `ROLE_CANDIDATE` | `CandidateOfferController.detail` | HTML View (`offers/candidate-detail`) |
| `GET` | `/portal/offers/{id}/decline`| `ROLE_CANDIDATE` | `CandidateOfferController.declinePage` | Redirect `/portal/offers/{id}?openDecline=true` |
| `POST` | `/portal/offers/{id}/accept` | `ROLE_CANDIDATE` | `CandidateOfferController.accept` | Redirect `/portal/offers/{id}` |
| `POST` | `/portal/offers/{id}/decline`| `ROLE_CANDIDATE` | `CandidateOfferController.decline` | Redirect `/portal/offers/{id}` |

---

### Phụ lục 2: Bảng Ma trận Chuyển đổi Trạng thái (State Machine)

| Trạng thái Nguồn | Sự kiện Kích hoạt (Event) | Trạng thái Đích | Tác nhân | Điều kiện ràng buộc |
| :--- | :--- | :--- | :---: | :--- |
| *(None)* | Tạo mới (Lưu bản nháp) | `Draft` | HR | Hồ sơ chưa có Offer hoạt động |
| *(None)* / `Draft` | Nộp trình duyệt | `Pending_Director` | HR | Lương thử việc $\ge 85\%$, ngày đi làm hợp lệ |
| `Pending_Director` | Phê duyệt đề xuất | `Director_Approved` | Director | Ghi log vào `OfferApproval` |
| `Pending_Director` | Từ chối đề xuất | `Director_Rejected` | Director | **Bắt buộc nhập lý do từ chối** |
| `Director_Rejected`| HR chỉnh sửa & nộp lại | `Pending_Director` | HR | Đã cập nhật lại lương/chế độ |
| `Director_Approved`| Phát hành thư mời | `Sent_Candidate` | HR | Chốt `ResponseDeadline`, Sync App $\rightarrow$ `Offered` |
| `Sent_Candidate` | Ứng viên chấp thuận | `Accepted` | Candidate | Chưa quá Deadline, Sync App $\rightarrow$ `Hired` |
| `Sent_Candidate` | Ứng viên từ chối | `Declined` | Candidate | Chưa quá Deadline, Sync App $\rightarrow$ `Rejected` |
| `Draft` | Xóa bản thảo | `IsDeleted = 1` | HR | Chỉ xóa mềm khi đang ở Draft |

---

### Phụ lục 3: Bản đồ Tệp tin Module Offer Management

```
SWP_RMS_Group-2/
├── src/main/java/com/group2/rms/offer/
│   ├── controller/
│   │   ├── OfferController.java                   (532 lines - Điều phối nội bộ HR & Director)
│   │   └── CandidateOfferController.java          (125 lines - Điều phối Candidate Portal)
│   ├── service/
│   │   ├── OfferService.java / OfferServiceImpl.java                 (1006 lines - Nghiệp vụ cốt lõi)
│   │   ├── CandidateOfferService.java / CandidateOfferServiceImpl.java (294 lines - Cổng ứng viên)
│   │   ├── OfferExportService.java / OfferExportServiceImpl.java     (753 lines - Xuất báo cáo Excel)
│   │   └── OfferMailSender.java                   (92 lines - Gửi email thông báo thư mời)
│   ├── repository/
│   │   ├── OfferProposalRepository.java           (67 lines - JPA Repo cho OfferProposal)
│   │   ├── OfferApprovalRepository.java           (20 lines - JPA Repo cho lịch sử phê duyệt)
│   │   └── CandidateOfferRepository.java          (58 lines - Atomic update chống race condition)
│   ├── entity/
│   │   ├── OfferProposal.java                     (80 lines - Entity Thư mời nhận việc)
│   │   └── OfferApproval.java                     (113 lines - Entity Lưu vết phê duyệt Giám đốc)
│   ├── dto/                                       (13 files DTO, Record & Enum OfferExportScope)
│   └── exception/                                 (2 files Custom Business Exceptions)
├── src/main/resources/
│   ├── templates/offers/                          (6 Thymeleaf templates HTML)
│   └── static/
│       ├── js/offers.js                           (2005 lines - Script modal, validation, Ajax)
│       ├── js/pages/candidate-offers.js           (285 lines - Countdown timer, chip lý do)
│       └── css/                                   (3 files CSS tạo kiểu trang trọng)
├── database/
│   ├── schema/db.sql                              (DDL bảng OfferProposal, OfferApproval)
│   └── migrations/                                (006_is_deleted.sql, 008_response_deadline.sql)
└── src/test/java/com/group2/rms/offer/            (7 files kiểm thử tự động với hơn 100 ca test)
```

---

### Phụ lục 4: Spring & JPA Annotation Cheat Sheet

| Annotation | Tầng áp dụng | Mục đích sử dụng trong module Offer |
| :--- | :---: | :--- |
| `@Controller` | Web | Khai báo Controller MVC phục vụ render Thymeleaf template. |
| `@ResponseBody` | Web | Đánh dấu dữ liệu trả về được serialize thành JSON (dùng cho modal Ajax Giám đốc). |
| `@ModelAttribute` | Web | Gắn sẵn dữ liệu dùng chung (`currentUser`, `userRole`) trước mọi request handler. |
| `@Valid` | Web / DTO | Kích hoạt bộ kiểm tra hợp lệ Bean Validation cho đối tượng DTO gửi lên. |
| `@AssertTrue` | DTO | Định nghĩa logic validation tùy biến cấp hàm (kiểm tra luật 85% lương thử việc). |
| `@Service` | Service | Đánh dấu Spring Bean chứa logic nghiệp vụ và giao dịch. |
| `@Transactional`| Service | Quản lý ranh giới giao dịch CSDL (ACID), tự động rollback khi gặp RuntimeException. |
| `@Repository` | Persistence | Đánh dấu interface truy cập dữ liệu và kích hoạt cơ chế DataAccessException translation. |
| `@Query` | Persistence | Định nghĩa câu truy vấn JPQL tùy biến hoặc câu lệnh SQL Native. |
| `@Modifying` | Persistence | Báo hiệu cho JPA biết câu truy vấn là lệnh DML (UPDATE / DELETE nguyên tử). |
| `@Entity` / `@Table`| Entity | Ánh xạ lớp Java thành một bảng tương ứng trong CSDL SQL Server. |
| `@Id` / `@GeneratedValue` | Entity | Định nghĩa khóa chính tự tăng `IDENTITY(1,1)` cho thực thể. |
| `@PrePersist` | Entity | Tự động gán thời gian hiện tại (`approvedAt`) ngay trước khi lưu bản ghi audit. |
| `@RequiredArgsConstructor`| Class | Lombok tự động sinh Constructor tiêm phụ thuộc cho toàn bộ các trường `final`. |
| `@Builder` | DTO / Entity | Cung cấp Builder Pattern giúp khởi tạo đối tượng sạch sẽ, linh hoạt trong mã nguồn. |

