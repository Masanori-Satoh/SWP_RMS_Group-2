# TÀI LIỆU HỌC TẬP & HƯỚNG DẪN KỸ THUẬT CHUYÊN SÂU MODULE OFFER MANAGEMENT
**Project:** SWP391 - Recruitment Management System (Mộc RMS - Group 2)  
**Tài liệu ôn tập & Bảo vệ đồ án:** `docs/members/huyenpt/offer-management-knowledge-guide.md`  
**Đối tượng sử dụng:** Sinh viên ngành Kỹ thuật Phần mềm (SE), Lập trình viên Java Spring Boot, QA/Tester  
**Dựa trên tài liệu thẩm định:** `offer-management-source-audit.md` & `offer-management-flow-analysis.md`  
**Ngày phát hành:** 02/11/2026  

---

## LỜI NÓI ĐẦU & BẢN XÁC MINH MÂU THUẪN TÀI LIỆU

Trước khi đi sâu vào chi tiết các khái niệm kỹ thuật trong 4 phần trọng tâm (**Spring Boot**, **JavaScript**, **Database**, **Business Rules**), nhóm kỹ thuật đã thực hiện đối chiếu chéo giữa các tài liệu đặc tả cũ và mã nguồn thực tế đang chạy trên nhánh `fix/huyenpt/offer-management-flow-fixes`. Dưới đây là biên bản xác minh các mâu thuẫn đã được chuẩn hóa:

1. **Về file giao diện khảo sát từ chối (`candidate-decline.html`):**
   * *Mâu thuẫn cũ:* Một số tài liệu ghi nhận hệ thống có trang HTML riêng `candidate-decline.html`.
   * *Xác minh thực tế:* **Không tồn tại** file này. Mã nguồn thực tế xử lý từ chối thông qua modal native `<dialog id="declineModal">` nhúng trực tiếp trong [`candidate-detail.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/candidate-detail.html) và [`candidate-list.html`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/candidate-list.html). Route `GET /portal/offers/{id}/decline` chuyển hướng 302 về `/portal/offers/{id}?openDecline=true`.
2. **Về `Enum OfferStatus`:**
   * *Mâu thuẫn cũ:* Báo cáo inventory ghi có Java Enum `OfferStatus.java`.
   * *Xác minh thực tế:* **Không tồn tại** Enum này. Trạng thái Offer được lưu dạng chuỗi ký tự (`String`) và quản lý bằng hằng số chuỗi trong Service, đồng thời được kiểm soát tính toàn vẹn ở tầng CSDL bằng `CHECK CONSTRAINT`. Enum duy nhất trong phân hệ Offer là [`OfferExportScope.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/dto/OfferExportScope.java).
3. **Về Bản chất lỗi `BUG-OFF-10` (Session Timeout Polling Redirect):**
   * *Mâu thuẫn cũ:* Cho rằng lỗi do code JavaScript polling ở topbar.
   * *Xác minh thực tế:* Lỗi xuất phát từ cấu hình `HttpSessionRequestCache` trong [`SecurityConfig.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/core/config/SecurityConfig.java). Khi session hết hạn, request polling ngầm `/api/notifications/unread-count` bị lưu làm redirect target sau đăng nhập, khiến người dùng bị đưa vào trang JSON thô.

---

# PHẦN 3. SPRING BOOT

---

### Khái niệm 3.1: Kiến trúc Hybrid Controller (MVC View song hành REST API JSON)

#### 1. Giải thích từ cơ bản
Trong kiến trúc Spring Web, một `@Controller` truyền thống thường trả về tên của View (chuỗi String tương ứng với file template Thymeleaf như `"offers/list"`). Ngược lại, một `@RestController` trả về dữ liệu thô (JSON/XML) trực tiếp vào HTTP Response Body.  
**Hybrid Controller** là kỹ thuật sử dụng `@Controller` kết hợp có chọn lọc annotation `@ResponseBody` (hoặc trả về `ResponseEntity<ApiResponse<T>>`) trên một số method cụ thể. Điều này cho phép một Controller vừa render giao diện HTML hoàn chỉnh cho trình duyệt, vừa đóng vai trò API Backend phục vụ các thao tác Ajax/Fetch bất đồng bộ (như modal duyệt nhanh, xuất file binary).

#### 2. Liên hệ code Offer
Trong [`OfferController.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/controller/OfferController.java):
* Các hàm xem danh sách, form tạo mới, xem chi tiết trả về chuỗi String trỏ tới view Thymeleaf: `return "offers/list";`, `return "offers/form";`.
* Các hàm phê duyệt nhanh của Giám đốc (`approveOfferJson`, `rejectOfferJson`) được gắn `@ResponseBody` và trả về `ResponseEntity<ApiResponse<OfferResponse>>`.
* Hàm xuất Excel (`exportOffers`) trả về `ResponseEntity<byte[]>`.

#### 3. Ví dụ code và giải thích từng dòng
Trích từ [`OfferController.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/controller/OfferController.java) (Dòng 462–478):

```java
// Dòng 462: Đón nhận request POST tới /{id}/approve và yêu cầu Header Content-Type là application/json
@PostMapping(value = "/{id}/approve", consumes = MediaType.APPLICATION_JSON_VALUE)
// Dòng 463: Đánh dấu dữ liệu trả về sẽ được chuyển đổi thẳng thành JSON (không tìm Thymeleaf template)
@ResponseBody
// Dòng 464: Định nghĩa method nhận tham số id từ URL và payload JSON map vào DirectorDecisionRequest
public ResponseEntity<ApiResponse<OfferResponse>> approveOfferJson(
        @PathVariable Integer id,
        @Valid @RequestBody DirectorDecisionRequest request) {
    try {
        // Dòng 468: Gọi Service xử lý nghiệp vụ phê duyệt và cập nhật database
        OfferResponse approved = offerService.approveOfferByDirector(id, request.getComments());
        // Dòng 469: Trả về HTTP 200 OK kèm payload chuẩn ApiResponse chứa data đã duyệt
        return ResponseEntity.ok(new ApiResponse<>(true, "Đã phê duyệt đề xuất nhận việc thành công!", approved));
    } catch (BaseBusinessException e) {
        // Dòng 471: Phân loại mã lỗi HTTP phù hợp (404 Not Found hoặc 400 Bad Request)
        HttpStatus status = (e instanceof ResourceNotFoundException) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(new ApiResponse<>(false, e.getMessage()));
    } catch (Exception e) {
        // Dòng 474: Ghi log lỗi hệ thống và trả về mã lỗi HTTP 500 an toàn cho client
        log.error("Lỗi không mong muốn khi phê duyệt đề xuất nhận việc ID {}: {}", id, e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Đã xảy ra lỗi hệ thống khi phê duyệt đề xuất nhận việc."));
    }
}
```

#### 4. File / Method / Dòng code
* **File:** [`src/main/java/com/group2/rms/offer/controller/OfferController.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/controller/OfferController.java)
* **Method:** `approveOfferJson` (L462–478), `rejectOfferJson` (L483–503), `listOffers` (L70–147).

#### 5. Vì sao dùng?
* **Tối ưu trải nghiệm người dùng (UX):** Giám đốc duyệt hàng loạt đề xuất trực tiếp trên modal mà không phải reload toàn bộ trang web.
* **Gọn nhẹ kiến trúc:** Không cần tạo thêm một lớp `OfferRestController` riêng biệt gây phân mảnh cấu trúc thư mục khi số lượng API Ajax nội bộ còn nhỏ.

#### 6. Lỗi thường gặp
* **Quên `@ResponseBody`:** Controller sẽ hiểu nhầm chuỗi trả về là tên view Thymeleaf, dẫn tới lỗi `TemplateInputException: Error resolving template [com.group2.rms.core.dto.ApiResponse]`.
* **Quên khai báo `consumes = MediaType.APPLICATION_JSON_VALUE`:** Dẫn tới lỗi `HttpMediaTypeNotSupportedException` (HTTP 415) khi client gửi request JSON.

#### 7. Câu hỏi bảo vệ và đáp án
* **Hội đồng hỏi:** *"Tại sao trong `OfferController` vừa có method trả về String, vừa có method trả về `ResponseEntity`?"*
* **Đáp án bảo vệ:** *"Thưa thầy cô, `OfferController` được thiết kế theo mô hình Hybrid Controller. Các method trả về String phục vụ Server-Side Rendering với Thymeleaf cho các màn hình tĩnh/form lớn. Các method trả về `ResponseEntity` kèm `@ResponseBody` phục vụ các tương tác Ajax nhanh của modal (như Giám đốc duyệt/từ chối nhanh) giúp người dùng không bị load lại trang."*

#### 8. Bài thực hành
1. Mở DevTools Network trên màn hình Giám đốc (`/offers`).
2. Mở modal Duyệt nhanh một đề xuất và bấm "Xác nhận Phê duyệt".
3. Kiểm tra request `/offers/{id}/approve`: Header gửi lên `Content-Type: application/json`, Response trả về JSON `{"success": true, ...}` với HTTP Status 200.

---

### Khái niệm 3.2: `@ModelAttribute` ở cấp độ Method (Controller Advice Scope)

#### 1. Giải thích từ cơ bản
Trong Spring MVC, khi `@ModelAttribute` được đặt trên một method trong Controller, method đó sẽ **luôn luôn được thực thi trước** bất kỳ `@GetMapping` hay `@PostMapping` nào trong Controller đó. Dữ liệu mà method này gắn vào `Model` sẽ tự động có mặt trong tất cả các View trả về.

#### 2. Liên hệ code Offer
Trong [`OfferController.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/controller/OfferController.java), method `populateCommonAttributes` chạy trước mọi request để:
1. Trích xuất thông tin người dùng từ `Principal`.
2. Kiểm tra vai trò người dùng (`ROLE_HR` vs `ROLE_DIRECTOR`).
3. Gắn các thuộc tính dùng chung (`currentUser`, `userRole`, `activeMenu = "offers"`) vào Model để menu navigation và sidebar tự động active.
4. Chặn ngay `AccessDeniedException` nếu người dùng không thuộc hai vai trò này.

#### 3. Ví dụ code và giải thích từng dòng
Trích từ [`OfferController.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/controller/OfferController.java) (Dòng 48–64):

```java
// Dòng 48: Đánh dấu method này chạy tự động trước mọi request handler trong Controller
@ModelAttribute
public void populateCommonAttributes(Model model, Principal principal) {
    String userRole = "HR";
    // Dòng 51: Kiểm tra xem Principal có phải là Authentication hợp lệ của Spring Security không
    if (principal instanceof org.springframework.security.core.Authentication auth) {
        // Dòng 52: Kiểm tra nếu có authority ROLE_DIRECTOR thì gán userRole là Director
        if (auth.getAuthorities().stream().anyMatch(a -> "ROLE_DIRECTOR".equals(a.getAuthority()))) {
            userRole = "Director";
        // Dòng 54: Nếu có authority ROLE_HR thì gán userRole là HR
        } else if (auth.getAuthorities().stream().anyMatch(a -> "ROLE_HR".equals(a.getAuthority()))) {
            userRole = "HR";
        } else {
            // Dòng 57: Nếu là Candidate hoặc tài khoản lạ cố tình vào thì ném lỗi 403 ngay lập tức
            throw new org.springframework.security.access.AccessDeniedException(
                    "Bạn không có quyền truy cập vào phân hệ Quản lý Đề xuất nhận việc.");
        }
    }
    // Dòng 61: Gắn tên người dùng vào model để hiển thị trên topbar
    model.addAttribute("currentUser", principal != null ? principal.getName() : "HR");
    // Dòng 62: Gắn vai trò để Thymeleaf hiển thị đúng giao diện (thẻ KPI, nút bấm phù hợp)
    model.addAttribute("userRole", userRole);
    // Dòng 63: Đánh dấu menu sidebar 'offers' đang active (highlight giao diện)
    model.addAttribute("activeMenu", "offers");
}
```

#### 4. File / Method / Dòng code
* **File:** [`src/main/java/com/group2/rms/offer/controller/OfferController.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/controller/OfferController.java)
* **Method:** `populateCommonAttributes(Model model, Principal principal)` (L48–64).

#### 5. Vì sao dùng?
* **Tuân thủ nguyên lý DRY (Don't Repeat Yourself):** Không phải viết lặp lại việc kiểm tra role và set `activeMenu` ở 10 method khác nhau trong controller.
* **Bảo vệ phòng thủ chiều sâu (Defense-in-depth):** Bổ sung thêm một lớp kiểm soát phân quyền ngay tại Controller bên cạnh lớp bảo vệ URL của `SecurityConfig`.

#### 6. Lỗi thường gặp
* Ném exception bên trong method `@ModelAttribute` mà không có `@ControllerAdvice` bắt lỗi sẽ khiến request crash trước khi vào method chính.
* Gán dữ liệu quá nặng (truy vấn DB lớn) vào `@ModelAttribute` method sẽ làm chậm toàn bộ các endpoint của controller đó.

#### 7. Câu hỏi bảo vệ và đáp án
* **Hội đồng hỏi:** *"Làm sao Thymeleaf ở trang `list.html` biết được người đang đăng nhập là HR hay Giám đốc để hiển thị bảng phù hợp?"*
* **Đáp án bảo vệ:** *"Dạ, Controller sử dụng method `@ModelAttribute populateCommonAttributes()` chạy trước mọi request. Method này trích xuất authorities từ `SecurityContext`, phân loại thành `userRole` và truyền vào `Model`. Template Thymeleaf chỉ cần kiểm tra điều kiện `${userRole == 'Director'}` để hiển thị component tương ứng."*

#### 8. Bài thực hành
1. Đăng nhập bằng tài khoản HR `lan.hr@rms-tech.vn` $\rightarrow$ Truy cập `/offers` $\rightarrow$ View `offers/list.html` được render.
2. Đăng xuất, đăng nhập bằng Giám đốc `hoang.director@rms-tech.vn` $\rightarrow$ Truy cập `/offers` $\rightarrow$ View `offers/director-list.html` được render tự động.

---

### Khái niệm 3.3: Bean Validation Đa Tầng & Custom `@AssertTrue` Business Logic

#### 1. Giải thích từ cơ bản
Trong ứng dụng doanh nghiệp, xác thực dữ liệu (Validation) cần được chia thành 2 mức:
1. **Field-level Validation:** Kiểm tra tính hợp lệ của từng trường đơn lẻ (không rỗng `@NotNull`, chuỗi không trắng `@NotBlank`, số dương `@Positive`, ngày tương lai `@Future`).
2. **Cross-field / Business-level Validation:** Kiểm tra mối tương quan logic giữa nhiều trường với nhau (ví dụ: Trường A phải lớn hơn hoặc bằng 85% trường B). Spring hỗ trợ việc này thông qua annotation `@AssertTrue` đặt trên method dạng `isXxxValid()` bên trong DTO.

#### 2. Liên hệ code Offer
Trong [`CreateOfferRequest.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/dto/CreateOfferRequest.java):
* Các annotation chuẩn: `@Positive` cho lương, `@Future` cho ngày bắt đầu, `@Min(1) @Max(60)` cho hạn phản hồi.
* Annotation nghiệp vụ: `@AssertTrue isProbationSalaryValid()` kiểm tra luật lương thử việc tối thiểu 85% so với lương chính thức (`BR-OFF-01`).
* `@AssertTrue isProbationSalaryNotExceedProposed()` kiểm tra lương thử việc không được vượt quá lương chính thức.

#### 3. Ví dụ code và giải thích từng dòng
Trích từ [`CreateOfferRequest.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/dto/CreateOfferRequest.java) (Dòng 98–108):

```java
// Dòng 98: Khai báo thông điệp lỗi sẽ hiển thị nếu method này trả về giá trị false
@AssertTrue(message = "Lương thử việc phải đạt tối thiểu 85% lương chính thức.")
public boolean isProbationSalaryValid() {
    // Dòng 100: Nếu một trong hai trường null, trả về true để nhường việc bắt lỗi cho @NotNull
    if (proposedSalary == null || probationSalary == null) {
        return true;
    }
    // Dòng 103: Nếu số âm hoặc bằng 0, trả về true để nhường việc bắt lỗi cho @Positive
    if (proposedSalary.compareTo(BigDecimal.ZERO) <= 0 || probationSalary.compareTo(BigDecimal.ZERO) <= 0) {
        return true;
    }
    // Dòng 106: Tính toán mức sàn 85% của lương chính thức bằng BigDecimal chính xác tuyệt đối
    BigDecimal minProbation = proposedSalary.multiply(new BigDecimal("0.85"));
    // Dòng 107: So sánh: probationSalary >= minProbation trả về true (hợp lệ), ngược lại false (lỗi)
    return probationSalary.compareTo(minProbation) >= 0;
}
```

#### 4. File / Method / Dòng code
* **File:** [`src/main/java/com/group2/rms/offer/dto/CreateOfferRequest.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/dto/CreateOfferRequest.java)
* **Method:** `isProbationSalaryValid()` (L98–108), `isProbationSalaryNotExceedProposed()` (L113–119).

#### 5. Vì sao dùng?
* **Chặn lỗi từ sớm (Fail-fast):** Dữ liệu không hợp lệ bị chặn ngay tại tầng Web (`BindingResult.hasErrors()`) trước khi kịp gọi xuống Service hoặc Database.
* **Gắn trực tiếp vào Model Validation:** Thông điệp lỗi từ `@AssertTrue` tự động map vào thẻ `<span th:errors="*{probationSalaryValid}">` trên giao diện HTML.

#### 6. Lỗi thường gặp
* Dùng kiểu dữ liệu `double` hoặc `float` thay vì `BigDecimal` khi tính 85%, dẫn tới sai số dấu chấm động (ví dụ: `20000000 * 0.85 = 16999999.999999996` khiến lương `17,000,000` bị báo lỗi vi phạm).

#### 7. Câu hỏi bảo vệ và đáp án
* **Hội đồng hỏi:** *"Tại sao trong method `@AssertTrue` khi `proposedSalary == null` lại trả về `true` thay vì `false`?"*
* **Đáp án bảo vệ:** *"Dạ, đây là nguyên lý phân tách trách nhiệm trong Bean Validation. Khi trường bị null, nhiệm vụ bắt lỗi thuộc về annotation `@NotNull`. Nếu `@AssertTrue` cũng trả về `false`, giao diện người dùng sẽ bị xuất hiện 2 thông báo lỗi trùng lặp cùng lúc cho một ô nhập liệu, gây rối cho người dùng."*

#### 8. Bài thực hành
1. Mở form tạo Offer `/offers/create`.
2. Nhập Lương chính thức: `10,000,000 VNĐ`. Nhập Lương thử việc: `8,000,000 VNĐ` (80%).
3. Bấm Submit $\rightarrow$ Trang web không chuyển hướng mà hiển thị thông báo lỗi màu đỏ ngay dưới ô nhập liệu: *"Lương thử việc phải đạt tối thiểu 85% lương chính thức."*

---

### Khái niệm 3.4: Atomic Conditional Query với `@Modifying` trong Spring Data JPA

#### 1. Giải thích từ cơ bản
Thông thường trong Spring Data JPA, thao tác cập nhật dữ liệu được thực hiện qua chu trình: `findById()` $\rightarrow$ gọi setter $\rightarrow$ `save()`. Tuy nhiên, chu trình này tiềm ẩn lỗ hổng nghiêm trọng khi có 2 request gửi đồng thời (Race Condition).  
`@Modifying` kết hợp `@Query` cho phép lập trình viên viết trực tiếp câu lệnh `UPDATE` hoặc `DELETE` của JPQL/SQL. Khi câu lệnh UPDATE chứa mệnh đề điều kiện logic tại thời điểm thực thi (`WHERE status = 'Sent_Candidate'`), nó sẽ thực thi như một thao tác nguyên tử (Atomic Operation) được đảm bảo bởi khóa cấp dòng (Row-level lock) của hệ quản trị CSDL.

#### 2. Liên hệ code Offer
Trong [`CandidateOfferRepository.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/repository/CandidateOfferRepository.java):
* Phương thức `answerIfSent(offerId, to)` và `answerIfSent(offerId, to, reason)` được định nghĩa để cập nhật Offer khi ứng viên Accept hoặc Decline.
* Trả về kiểu số nguyên `int` đại diện cho số dòng được cập nhật thực tế trên database.

#### 3. Ví dụ code và giải thích từng dòng
Trích từ [`CandidateOfferRepository.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/repository/CandidateOfferRepository.java) (Dòng 49–57):

```java
// Dòng 49: Báo cho Spring Data JPA biết đây là câu lệnh DML (UPDATE/DELETE), không phải câu lệnh SELECT
@Modifying
// Dòng 50: Câu lệnh JPQL cập nhật trạng thái và thời gian updatedAt
@Query("UPDATE OfferProposal o SET o.offerStatus = :to, o.updatedAt = CURRENT_TIMESTAMP " +
       // Dòng 51: ĐIỀU KIỆN THEN CHỐT: Chỉ cập nhật nếu trạng thái hiện tại đúng là 'Sent_Candidate'
       "WHERE o.offerId = :id AND o.offerStatus = 'Sent_Candidate'")
// Dòng 52: Trả về số lượng bản ghi bị tác động (1 nếu thành công, 0 nếu bản ghi đã bị sửa trước đó)
int answerIfSent(@Param("id") Integer offerId, @Param("to") String to);
```

#### 4. File / Method / Dòng code
* **File:** [`src/main/java/com/group2/rms/offer/repository/CandidateOfferRepository.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/repository/CandidateOfferRepository.java)
* **Method:** `answerIfSent` (L49–57).
* **Nơi gọi:** [`CandidateOfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/CandidateOfferServiceImpl.java) (L120–124).

#### 5. Vì sao dùng?
* **Triệt tiêu hoàn toàn Race Condition và Double Submit:** Nếu ứng viên click đúp 2 lần trong 100ms hoặc mở 2 tab duyệt cùng lúc (1 tab Accept, 1 tab Decline), request đầu tiên sẽ thực hiện thành công (trả về `1`). Request thứ hai điều kiện `offerStatus = 'Sent_Candidate'` không còn đúng nữa nên trả về `0`. Tầng Service bắt được giá trị `0` này và ném lỗi ngay:
  ```java
  if (updated == 0) {
      throw new OfferResponseException("Thư mời vừa được cập nhật. Vui lòng tải lại trang.");
  }
  ```

#### 6. Lỗi thường gặp
* Quên thêm chú thích `@Transactional` trên Service khi gọi method `@Modifying` sẽ gây ngoại lệ `TransactionRequiredException: Executing an update/delete query`.
* Quên kiểm tra giá trị trả về (`int updated`), dẫn tới việc câu lệnh update không tác động bản ghi nào nhưng code vẫn chạy tiếp các logic sau (như chuyển trạng thái Application thành công dù Offer chưa cập nhật).

#### 7. Câu hỏi bảo vệ và đáp án
* **Hội đồng hỏi:** *"Phương thức `@Modifying answerIfSent` trong `CandidateOfferRepository` giải quyết bài toán gì và hoạt động ra sao?"*
* **Đáp án bảo vệ:** *"Thưa thầy cô, phương thức này dùng để chống lỗi Race Condition khi ứng viên phản hồi thư mời. Bằng cách thực thi trực tiếp lệnh `UPDATE ... WHERE offerStatus = 'Sent_Candidate'`, hệ thống tận dụng khóa cấp dòng của SQL Server. Nếu có 2 thao tác đồng thời, chỉ 1 thao tác đổi được trạng thái và trả về kết quả 1, thao tác còn lại nhận kết quả 0 và bị Service ném `OfferResponseException` chặn đứng."*

#### 8. Bài thực hành
1. Mở trang thư mời của ứng viên trên 2 tab trình duyệt cạnh nhau.
2. Tại Tab 1: Bấm nút "Đồng ý nhận việc".
3. Tại Tab 2: Bấm nút "Từ chối thư mời".
4. Quan sát kết quả: Tab 1 thành công chuyển trạng thái thành "Đã nhận việc", Tab 2 báo lỗi: *"Thư mời này đã được trả lời nên không thể thay đổi."*

---

# PHẦN 4. JAVASCRIPT

---

### Khái niệm 4.1: Quản lý Modal bằng HTML5 Native `<dialog>` (Zero External CDN Dependency)

#### 1. Giải thích từ cơ bản
Trước đây, các ứng dụng web thường phụ thuộc vào các thư viện bên thứ ba như SweetAlert2, Bootstrap Modal, jQuery UI để hiển thị hộp thoại xác nhận (Modal Popup). Tuy nhiên, HTML5 đã chuẩn hóa thẻ `<dialog>` với API native tích hợp sẵn trên trình duyệt:
* `dialogElement.showModal()`: Mở modal ở chế độ chặn tương tác nền (backdrop), tự động bẫy tiêu điểm bàn phím (focus trap) và hỗ trợ phím Escape để đóng.
* `dialogElement.close()`: Đóng modal và trả lại tiêu điểm cho phần tử trước đó.

#### 2. Liên hệ code Offer
Trong dự án Mộc RMS, commit `da95239a` đã loại bỏ hoàn toàn SweetAlert2 (sau khi xảy ra sự cố lỗi mạng CDN `Swal is not defined` - `BUG-OFF-03`). Toàn bộ các hộp thoại xác nhận được chuyển sang HTML5 `<dialog>`:
* `#directorDecisionModal`: Modal Giám đốc phê duyệt / từ chối nhanh trong `director-list.html`.
* `#sendOfferModal`: Modal HR xác nhận phát hành thư mời trong `detail.html`.
* `#acceptModal` & `#declineModal`: Modal ứng viên chấp thuận / từ chối trong `candidate-detail.html`.

#### 3. Ví dụ code và giải thích từng dòng
Trích từ [`src/main/resources/static/js/offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/offers.js) (Dòng 420–445):

```javascript
// Dòng 420: Hàm mở modal quyết định của Giám đốc (nhận vào id của Offer và hành động 'approve' hoặc 'reject')
function openDirectorDecisionModal(offerId, action) {
    // Dòng 422: Tìm thẻ <dialog> trong DOM bằng ID
    const modal = document.getElementById('directorDecisionModal');
    if (!modal) return;

    // Dòng 426: Gán dữ liệu ngữ cảnh vào hidden input của form modal
    document.getElementById('directorDecisionOfferId').value = offerId;
    document.getElementById('directorDecisionAction').value = action;

    // Dòng 430: Cập nhật tiêu đề và giao diện phù hợp với hành động
    const titleEl = document.getElementById('directorDecisionTitle');
    const submitBtn = document.getElementById('directorDecisionSubmitBtn');
    if (action === 'approve') {
        titleEl.textContent = 'Phê Duyệt Đề Xuất Nhận Việc';
        submitBtn.className = 'btn btn-primary';
        submitBtn.textContent = 'Xác nhận Phê duyệt';
    } else {
        titleEl.textContent = 'Từ Chối Đề Xuất Nhận Việc';
        submitBtn.className = 'btn btn-danger';
        submitBtn.textContent = 'Xác nhận Từ chối';
    }

    // Dòng 444: GỌI NATIVE API CỦA TRÌNH DUYỆT: Mở hộp thoại modal có lớp phủ mờ (backdrop)
    modal.showModal();
}
```

#### 4. File / Method / Dòng code
* **File:** [`src/main/resources/static/js/offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/offers.js)
* **Method:** `openDirectorDecisionModal` (L420–445), `closeDirectorDecisionModal` (L450–456).
* **CSS liên quan:** [`offers.css`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/css/offers.css) định nghĩa `dialog::backdrop { background: rgba(0, 0, 0, 0.5); }`.

#### 5. Vì sao dùng?
* **Zero Dependency:** Không phụ thuộc thư viện ngoài, chạy hoàn hảo trong môi trường mạng nội bộ (Intranet) hoặc khi rớt mạng Internet.
* **Hiệu năng & Khả năng tiếp cận (Accessibility):** Trình duyệt tự động quản lý focus, phím ESC tự động đóng hộp thoại, không bị xung đột z-index với các thành phần CSS khác.

#### 6. Lỗi thường gặp
* Gọi nhầm `modal.show()` thay vì `modal.showModal()`: `show()` chỉ làm hiện thẻ dialog thông thường mà không có lớp phủ backdrop và không chặn tương tác với trang phía sau.
* Không chặn sự kiện click vào phần nội dung bên trong dialog khi viết tính năng click ra ngoài backdrop để đóng.

#### 7. Câu hỏi bảo vệ và đáp án
* **Hội đồng hỏi:** *"Tại sao nhóm lại tự viết modal bằng thẻ HTML5 `<dialog>` thay vì dùng thư viện SweetAlert2 hay Bootstrap Modal?"*
* **Đáp án bảo vệ:** *"Dạ thưa thầy cô, việc dùng thư viện SweetAlert2 phụ thuộc vào mạng CDN bên ngoài, khi chạy trong mạng nội bộ công ty bị chặn firewall sẽ phát sinh lỗi `Swal is not defined` làm tê liệt tính năng (như lỗi BUG-OFF-03 nhóm từng gặp). Chuyển sang thẻ chuẩn HTML5 `<dialog>` giúp ứng dụng tự chủ 100% tài nguyên, tải trang nhanh hơn và không có rủi ro phụ thuộc bên ngoài."*

#### 8. Bài thực hành
1. Mở DevTools Console trên trang `/offers`.
2. Gõ lệnh: `document.getElementById('directorDecisionModal').showModal()`.
3. Modal Giám đốc lập tức hiện lên giữa màn hình kèm lớp nền làm tối. Nhấn phím `Escape` trên bàn phím $\rightarrow$ Modal tự động đóng lại.

---

### Khái niệm 4.2: Tích hợp CSRF Token trong Fetch API & Phòng chống Double Click

#### 1. Giải thích từ cơ bản
Khi Spring Security kích hoạt bảo vệ CSRF (mặc định), mọi HTTP request có tính chất thay đổi dữ liệu (`POST`, `PUT`, `DELETE`) gửi qua JavaScript `fetch()` đều bắt buộc phải đính kèm CSRF Token trong Request Header (thường là `X-CSRF-TOKEN`). Nếu thiếu header này, Spring Security sẽ chặn đứng request và trả về mã lỗi **HTTP 403 Forbidden**.  
Đồng thời, khi người dùng bấm nút xác nhận, JavaScript cần ngay lập tức vô hiệu hóa nút bấm (`disabled = true`) để ngăn chặn việc gửi request 2 lần liên tiếp trước khi server kịp phản hồi.

#### 2. Liên hệ code Offer
Trong [`src/main/resources/static/js/offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/offers.js):
* Hàm `submitDirectorDecision(event)` đọc CSRF Token từ thẻ `<meta name="_csrf">` trên phần `<head>` của trang HTML.
* Vô hiệu hóa nút bấm xác nhận và gắn trạng thái loading.
* Gửi `fetch()` kèm `headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': csrfToken }`.

#### 3. Ví dụ code và giải thích từng dòng
Trích từ [`src/main/resources/static/js/offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/offers.js) (Dòng 460–510):

```javascript
async function submitDirectorDecision(event) {
    event.preventDefault(); // Ngăn chặn hành vi submit form mặc định làm reload trang
    
    const offerId = document.getElementById('directorDecisionOfferId').value;
    const action = document.getElementById('directorDecisionAction').value;
    const comments = document.getElementById('directorDecisionComments').value.trim();
    const submitBtn = document.getElementById('directorDecisionSubmitBtn');

    // Dòng 470: Validate tầng client: Bắt buộc nhập lý do nếu action là 'reject'
    if (action === 'reject' && !comments) {
        showDirectorDecisionError('Vui lòng nhập lý do từ chối vào ô nhận xét.');
        return;
    }

    // Dòng 478: Trích xuất CSRF Token từ thẻ meta của trang
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');

    // Dòng 482: PHÒNG CHỐNG DOUBLE CLICK: Disable nút bấm và hiển thị loading
    submitBtn.disabled = true;
    const originalText = submitBtn.textContent;
    submitBtn.textContent = 'Đang xử lý...';

    try {
        // Dòng 488: Thực hiện gọi Fetch API bất đồng bộ
        const response = await fetch(`/offers/${offerId}/${action}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': csrfToken // Bắt buộc để Spring Security CsrfFilter chấp thuận
            },
            body: JSON.stringify({ comments: comments })
        });

        const data = await response.json();
        if (response.ok && data.success) {
            closeDirectorDecisionModal();
            showFlashToast(data.message, 'success');
            // Cập nhật lại giao diện sau 1 giây
            setTimeout(() => window.location.reload(), 1000);
        } else {
            showDirectorDecisionError(data.message || 'Thao tác không thành công.');
            submitBtn.disabled = false;
            submitBtn.textContent = originalText;
        }
    } catch (err) {
        showDirectorDecisionError('Lỗi kết nối mạng, vui lòng thử lại.');
        submitBtn.disabled = false;
        submitBtn.textContent = originalText;
    }
}
```

#### 4. File / Method / Dòng code
* **File:** [`src/main/resources/static/js/offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/offers.js)
* **Method:** `submitDirectorDecision(event)` (L460–510).

#### 5. Vì sao dùng?
* Đảm bảo mọi giao tiếp bất đồng bộ tuân thủ tiêu chuẩn bảo mật phòng chống tấn công Cross-Site Request Forgery.
* Tránh trường hợp người dùng click chuột liên tục tạo ra nhiều request trùng lặp làm quá tải server.

#### 6. Lỗi thường gặp
* Không lấy được CSRF token do quên khai báo cặp thẻ meta `<meta name="_csrf" th:content="${_csrf.token}"/>` trong file HTML layout.
* Không đưa việc enable lại nút bấm vào khối `finally` hoặc nhánh `catch`, khiến nút bấm bị treo vĩnh viễn ở trạng thái "Đang xử lý..." nếu request gặp lỗi mạng.

#### 7. Câu hỏi bảo vệ và đáp án
* **Hội đồng hỏi:** *"Làm thế nào request JavaScript Fetch của bạn vượt qua được bộ lọc bảo vệ CSRF của Spring Security?"*
* **Đáp án bảo vệ:** *"Thưa thầy cô, trong thẻ `<head>` của layout Thymeleaf, nhóm đã nhúng sẵn token bảo mật qua thẻ meta. Khi JavaScript gửi Fetch POST, hàm sẽ đọc giá trị này và gắn vào header với tên `X-CSRF-TOKEN`. Khi request đi qua `CsrfFilter` của Spring Security, token trong header sẽ được so sánh khớp với token trong session của người dùng để cấp phép thực thi."*

#### 8. Bài thực hành
1. Mở DevTools Console, gõ thử lệnh `fetch('/offers/21/approve', {method: 'POST'})` (không truyền header `X-CSRF-TOKEN`).
2. Quan sát server trả về mã lỗi **HTTP 403 Forbidden**.
3. Thêm header CSRF token và chạy lại $\rightarrow$ Request được xử lý thành công.

---

### Khái niệm 4.3: Countdown Timer & Tự Động Khóa Tương Tác Phía Client

#### 1. Giải thích từ cơ bản
Countdown Timer là cơ chế sử dụng hàm `setInterval()` của JavaScript để định kỳ tính toán chênh lệch thời gian giữa thời điểm hiện tại (`new Date()`) và một mốc thời gian trong tương lai (`deadline`). Khi thời gian đếm ngược kết thúc (`distance <= 0`), script sẽ xóa bỏ bộ đếm (`clearInterval`) và ngay lập tức cập nhật DOM để vô hiệu hóa các nút bấm tương tác.

#### 2. Liên hệ code Offer
Trong [`src/main/resources/static/js/pages/candidate-offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/pages/candidate-offers.js):
* Khởi tạo bộ đếm ngược thời hạn phản hồi thư mời nhận việc thông qua hàm `initCountdowns()`.
* Đọc mốc thời gian hết hạn từ thuộc tính `data-deadline="2026-11-07T18:00:00"`.
* Cập nhật số Ngày, Giờ, Phút, Giây còn lại theo thời gian thực.
* Khi hết hạn: Tự động thay đổi giao diện, ẩn nút Accept/Decline và hiển thị thông báo "Đã quá hạn phản hồi".

#### 3. Ví dụ code và giải thích từng dòng
Trích từ [`src/main/resources/static/js/pages/candidate-offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/pages/candidate-offers.js) (Dòng 50–95):

```javascript
function initCountdowns() {
    // Dòng 51: Tìm tất cả các phần tử có thuộc tính data-deadline trên trang
    document.querySelectorAll('[data-deadline]').forEach(el => {
        const deadlineStr = el.getAttribute('data-deadline');
        if (!deadlineStr) return;
        const target = new Date(deadlineStr).getTime();

        function update() {
            const now = Date.now();
            const diff = target - now; // Khoảng cách mili-giây còn lại

            // Dòng 60: Nếu đã quá thời hạn phản hồi
            if (diff <= 0) {
                el.textContent = 'Đã hết hạn phản hồi';
                el.classList.add('countdown-expired');
                // Vô hiệu hóa nút bấm Đồng ý và Từ chối trên giao diện ngay lập tức
                const actionGroup = document.querySelector('.offer-action-buttons');
                if (actionGroup) {
                    actionGroup.innerHTML = '<div class="alert alert-warning">Thư mời này đã quá thời hạn phản hồi.</div>';
                }
                clearInterval(timerId); // Dừng vòng lặp timer để tiết kiệm CPU
                return;
            }

            // Dòng 74: Tính toán số ngày, giờ, phút, giây còn lại
            const days = Math.floor(diff / (1000 * 60 * 60 * 24));
            const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
            const mins = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
            const secs = Math.floor((diff % (1000 * 60)) / 1000);

            // Dòng 80: Format dạng chuỗi 2 chữ số (pad) và render ra DOM
            el.textContent = `Còn lại: ${days} ngày ${pad(hours)}:${pad(mins)}:${pad(secs)}`;
        }

        update(); // Chạy ngay lần đầu tiên để tránh bị giật giao diện
        const timerId = setInterval(update, 1000); // Lặp lại mỗi 1000ms (1 giây)
    });
}
```

#### 4. File / Method / Dòng code
* **File:** [`src/main/resources/static/js/pages/candidate-offers.js`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/pages/candidate-offers.js)
* **Method:** `initCountdowns()` (L50–95).

#### 5. Vì sao dùng?
* Tạo tính trực quan và cảm giác cấp thiết chuyên nghiệp cho ứng viên khi xem thư mời nhận việc.
* Khóa giao diện chủ động phía client giúp người dùng không mất công bấm submit khi thư mời vừa hết hạn.

#### 6. Lỗi thường gặp
* Sử dụng thời gian cục bộ của máy khách (`new Date()`) mà không có sự kiểm tra chéo ở tầng backend. Người dùng có thể chỉnh lùi giờ hệ điều hành để nút bấm sáng lại. **(Do đó, tầng Backend bắt buộc phải kiểm tra lại `LocalDateTime.now()` tại Service như `requireAnswerable`)**.
* Quên gọi `clearInterval(timerId)` khi element bị xóa khỏi DOM hoặc khi đếm ngược về 0, gây rò rỉ bộ nhớ (Memory Leak).

#### 7. Câu hỏi bảo vệ và đáp án
* **Hội đồng hỏi:** *"Nếu ứng viên cố tình chỉnh lùi giờ máy tính để nút 'Đồng ý nhận việc' sáng lên và bấm gửi thì hệ thống có bị đánh lừa không?"*
* **Đáp án bảo vệ:** *"Dạ hoàn toàn không thể đánh lừa được hệ thống ạ. JavaScript đếm ngược ở client chỉ phục vụ trải nghiệm hiển thị (UX). Tại tầng backend trong phương thức `CandidateOfferServiceImpl.requireAnswerable()`, server kiểm tra mốc thời gian thực tế `LocalDateTime.now(clock).isAfter(offer.getResponseDeadline())`. Nếu đã quá hạn, server sẽ ném ngay `OfferResponseException` và từ chối xử lý."*

#### 8. Bài thực hành
1. Mở trang chi tiết thư mời ứng viên `/portal/offers/{id}`.
2. Quan sát đồng hồ đếm ngược đang chạy giảm từng giây.
3. Mở Console gõ: `document.querySelector('[data-deadline]').setAttribute('data-deadline', '2020-01-01')` $\rightarrow$ Quan sát giao diện lập tức chuyển sang dòng chữ màu xám "Đã hết hạn phản hồi" và nhóm nút bấm hành động bị ẩn đi.

---

# PHẦN 5. DATABASE (SQL SERVER & JPA)

---

### Khái niệm 5.1: Ràng buộc Toàn vẹn CSDL (Unique Constraint vs Check Constraint)

#### 1. Giải thích từ cơ bản
Trong hệ quản trị CSDL quan hệ (RDBMS), tính toàn vẹn dữ liệu được bảo vệ bằng các ràng buộc (Constraints):
* **UNIQUE Constraint:** Đảm bảo giá trị trong một hoặc nhiều cột không bao giờ bị trùng lặp giữa các dòng dữ liệu khác nhau trong bảng.
* **CHECK Constraint:** Đảm bảo giá trị của một cột phải thỏa mãn một biểu thức logic nhất định (ví dụ: số dương, nằm trong danh sách cho phép, hoặc so sánh tương quan giữa 2 cột).

#### 2. Liên hệ code Offer
Trong tệp [`database/schema/db.sql`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/database/schema/db.sql) (L506–550):
* Ràng buộc `UQ_OfferProposal_Application`: Đảm bảo một hồ sơ ứng tuyển chỉ có tối đa 1 bản ghi đề xuất thư mời (luật `GBR-07`).
* Ràng buộc `CK_OfferProposal_Salaries`: Đảm bảo lương thử việc phải $\ge 85\%$ lương chính thức (`ProbationSalary >= (ProposedSalary * 0.85)`).
* Ràng buộc `CK_OfferProposal_Status`: Đảm bảo trạng thái Offer chỉ nằm trong 7 giá trị hợp lệ.

#### 3. Ví dụ code và giải thích từng dòng
Trích từ [`database/schema/db.sql`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/database/schema/db.sql):

```sql
-- Dòng 508: Ràng buộc UNIQUE bảo đảm mỗi ApplicationId chỉ xuất hiện tối đa 1 lần trong bảng
ApplicationId INT NOT NULL CONSTRAINT UQ_OfferProposal_Application UNIQUE,

-- Dòng 531: Ràng buộc CHECK kiểm tra trạng thái của Offer chỉ được phép rơi vào 7 trạng thái chuẩn
CONSTRAINT CK_OfferProposal_Status
    CHECK (OfferStatus IN (
        N'Draft',
        N'Pending_Director',
        N'Director_Approved',
        N'Director_Rejected',
        N'Sent_Candidate',
        N'Accepted',
        N'Declined'
    )),

-- Dòng 543: Ràng buộc CHECK nghiệp vụ kiểm tra mức lương tại tầng CSDL
CONSTRAINT CK_OfferProposal_Salaries
    CHECK (
        ProposedSalary >= 0 
        AND ProbationSalary >= 0 
        AND ProbationSalary >= (ProposedSalary * 0.85) -- Luật 85% lương thử việc
    )
```

#### 4. File / Method / Dòng code
* **File:** [`database/schema/db.sql`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/database/schema/db.sql) (L506–550).
* **Migration scripts:** [`006_offer_is_deleted.sql`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/database/migrations/006_offer_is_deleted.sql), [`008_offer_response_deadline.sql`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/database/migrations/008_offer_response_deadline.sql).

#### 5. Vì sao dùng?
* **Tuyến phòng thủ cuối cùng (Last Line of Defense):** Ngay cả khi code Java bị lỗi bypass validation hoặc lập trình viên can thiệp trực tiếp bằng câu lệnh SQL thô, CSDL vẫn kiên quyết từ chối lưu dữ liệu sai quy tắc, bảo vệ tuyệt đối tính toàn vẹn hệ thống.

#### 6. Lỗi thường gặp
* Insert trực tiếp bằng lệnh SQL mà không tính đúng 85% lương sẽ gặp lỗi SQL Server: `The INSERT statement conflicted with the CHECK constraint "CK_OfferProposal_Salaries"`.
* Khi đã xóa mềm bản ghi (`IsDeleted = 1`), ràng buộc `UQ_OfferProposal_Application` vẫn tính bản ghi đó, gây lỗi xung đột khi tạo mới (chính là nguyên nhân của `BUG-OFF-11`).

#### 7. Câu hỏi bảo vệ và đáp án
* **Hội đồng hỏi:** *"Tại sao đã validate ở Java code rồi mà còn phải tạo CHECK constraint trong database?"*
* **Đáp án bảo vệ:** *"Thưa thầy cô, đó là kiến trúc phòng thủ đa tầng (Defense-in-depth). Validation ở code Java giúp phản hồi thông báo thân thiện cho người dùng, còn CHECK constraint ở database là rào chắn bảo vệ dữ liệu tối hậu. Nếu có ai đó truy cập database bằng công cụ quản trị (như SSMS) hoặc có tiến trình migration chạy ngầm thì constraint sẽ đảm bảo dữ liệu không bao giờ bị sai lệch với quy định pháp luật."*

#### 8. Bài thực hành
1. Mở SQL Server Management Studio (SSMS).
2. Chạy thử câu lệnh:
   ```sql
   INSERT INTO OfferProposal (ApplicationId, OfferedPositionTitle, ProposedSalary, ProbationSalary, OfferStatus, ProposedBy)
   VALUES (999, 'Test', 10000000, 7000000, 'Draft', 1);
   ```
3. Quan sát SQL Server báo lỗi xung đột ràng buộc `CK_OfferProposal_Salaries` và hủy thao tác.

---

### Khái niệm 5.2: Triệt Tiêu Vấn Đề N+1 Query Bằng `JOIN FETCH` trong JPQL

#### 1. Giải thích từ cơ bản
Vấn đề **N+1 Query** xảy ra khi truy vấn một danh sách gồm $N$ thực thể (1 câu query), sau đó vòng lặp trong mã nguồn lại tiếp tục truy cập vào các thuộc tính lười (`FetchType.LAZY`) của từng phần tử, khiến Hibernate phải sinh thêm $N$ câu truy vấn con riêng lẻ để lấy dữ liệu liên quan. Tổng cộng hệ thống phải thực hiện $N + 1$ câu lệnh xuống CSDL, gây tắc nghẽn I/O và sụt giảm nghiêm trọng hiệu năng.  
`JOIN FETCH` trong JPQL hướng dẫn Hibernate thực hiện phép kết hợp bảng (SQL INNER/LEFT JOIN) và nạp toàn bộ các thực thể liên quan vào bộ nhớ ngay trong **1 câu truy vấn duy nhất**.

#### 2. Liên hệ code Offer
Trong [`CandidateOfferRepository.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/repository/CandidateOfferRepository.java):
* Một Thư mời nhận việc (`OfferProposal`) liên kết với `Application`, `JobPosting`, `Candidate`, `Account (User)`, `Department`.
* Để hiển thị danh sách thư mời cho ứng viên, phương thức `findVisibleByUserId` sử dụng `JOIN FETCH` để gom toàn bộ dữ liệu chỉ trong 1 câu SQL duy nhất.

#### 3. Ví dụ code và giải thích từng dòng
Trích từ [`CandidateOfferRepository.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/repository/CandidateOfferRepository.java) (Dòng 21–29):

```java
// Dòng 21: Định nghĩa câu lệnh truy vấn JPQL tùy biến
@Query("""
        select o from OfferProposal o
            -- Dòng 23: Nạp ngay thực thể Application trong cùng 1 câu lệnh JOIN (không Lazy load)
            join fetch o.application a
            -- Dòng 24: Nạp tiếp thực thể JobPosting thuộc Application
            join fetch a.jobPosting
        -- Dòng 25: Điều kiện lọc theo UserId của ứng viên đăng nhập và danh sách trạng thái hợp lệ
        where a.candidate.account.userId = :userId and o.offerStatus in :statuses
        order by o.offerId desc
        """)
List<OfferProposal> findVisibleByUserId(@Param("userId") Integer userId,
                                        @Param("statuses") Collection<String> statuses);
```

#### 4. File / Method / Dòng code
* **File:** [`src/main/java/com/group2/rms/offer/repository/CandidateOfferRepository.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/repository/CandidateOfferRepository.java)
* **Method:** `findVisibleByUserId` (L21–29), `findVisibleById` (L31–43).

#### 5. Vì sao dùng?
* Giảm số lượng round-trip mạng giữa máy chủ Spring Boot và máy chủ SQL Server từ $N+1$ xuống còn đúng $1$.
* Tránh được ngoại lệ phổ biến `LazyInitializationException: could not initialize proxy - no Session` khi dữ liệu được chuyển ra ngoài tầng Service tới View Thymeleaf.

#### 6. Lỗi thường gặp
* Áp dụng `JOIN FETCH` trên nhiều quan hệ tập hợp `@OneToMany` (Collection) cùng lúc sẽ gây ra lỗi `MultipleBagFetchException` do tạo ra tích đề-các (Cartesian Product) làm bùng nổ dữ liệu trùng lặp trong bộ nhớ.

#### 7. Câu hỏi bảo vệ và đáp án
* **Hội đồng hỏi:** *"Làm sao nhóm kiểm soát được hiệu năng khi ứng viên mở danh sách thư mời nhận việc?"*
* **Đáp án bảo vệ:** *"Dạ thưa thầy cô, trong `CandidateOfferRepository`, nhóm đã chủ động phòng chống lỗi N+1 Query bằng cách sử dụng từ khóa `JOIN FETCH` trong JPQL. Thay vì để Hibernate nạp lười từng hồ sơ và tin tuyển dụng, một câu lệnh SQL JOIN duy nhất sẽ lấy trọn vẹn thông tin thư mời, vị trí và công ty, giúp thời gian phản hồi trang dưới 50ms."*

#### 8. Bài thực hành
1. Bật cấu hình `spring.jpa.show-sql=true` trong `application.properties`.
2. Truy cập vào trang danh sách thư mời `/portal/offers`.
3. Quan sát console log: Chỉ có duy nhất 1 câu truy vấn SQL `SELECT ... FROM OfferProposal INNER JOIN Application ... INNER JOIN JobPosting ...` được sinh ra.

---

### Khái niệm 5.3: Cơ Chế Xóa Mềm (Soft Delete) & Khắc Phục Lỗi Xung Đột Bằng Reactivation

#### 1. Giải thích từ cơ bản
* **Hard Delete (Xóa cứng):** Xóa hoàn toàn dòng dữ liệu khỏi đĩa cứng bằng lệnh `DELETE FROM ...`. Nhược điểm: Mất lịch sử kiểm toán (Audit Trail) và dễ làm vỡ các ràng buộc khóa ngoại (Foreign Key) với các bảng khác.
* **Soft Delete (Xóa mềm):** Giữ nguyên dòng dữ liệu trong CSDL, chỉ cập nhật một cờ đánh dấu (ví dụ: `IsDeleted = 1`). Mọi câu truy vấn nghiệp vụ thông thường sẽ tự động thêm điều kiện `WHERE IsDeleted = 0`.
* **Vấn đề nảy sinh:** Khi bảng có ràng buộc `UNIQUE(ApplicationId)`, bản ghi đã xóa mềm vẫn chiếm giữ giá trị `ApplicationId` đó. Khi người dùng cố gắng tạo lại dữ liệu mới cho cùng một ApplicationId, lệnh `INSERT` sẽ đâm sầm vào ràng buộc Unique và báo lỗi HTTP 500 (`BUG-OFF-11`).

#### 2. Liên hệ code Offer
* Thêm cột `IsDeleted BIT NOT NULL DEFAULT 0` trong migration [`006_offer_is_deleted.sql`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/database/migrations/006_offer_is_deleted.sql).
* **Cơ chế Reactivation (Hồi sinh bản ghi):** Trong [`OfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java), khi HR tạo mới Offer cho một hồ sơ, service sẽ tìm kiếm xem hồ sơ đó đã từng có Offer chưa. Nếu có và đang bị xóa mềm (`isDeleted == true`), service sẽ **tái kích hoạt** bản ghi cũ: gán dữ liệu mới, đặt lại `isDeleted = false` và lưu lại bằng lệnh `UPDATE` thay vì chạy `INSERT`.

#### 3. Ví dụ code và giải thích từng dòng
Trích từ [`OfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java) (Dòng 245–268):

```java
// Dòng 245: Tìm kiếm đề xuất Offer theo ApplicationId trong cơ sở dữ liệu
Optional<OfferProposal> existingOfferOpt = offerProposalRepository.findByApplication_ApplicationId(dto.getApplicationId());

if (existingOfferOpt.isPresent()) {
    OfferProposal existingOffer = existingOfferOpt.get();
    // Dòng 249: KIỂM TRA ĐẶC BIỆT: Bản ghi cũ đang ở trạng thái đã xóa mềm (IsDeleted = true)
    if (Boolean.TRUE.equals(existingOffer.getIsDeleted())) {
        log.info("Phát hiện Offer cũ đã bị xóa mềm cho Application #{}. Tiến hành tái kích hoạt (Reactivation).", dto.getApplicationId());
        
        // Dòng 252: Cập nhật đè toàn bộ thông tin mới do HR nhập vào
        existingOffer.setOfferedPositionTitle(dto.getOfferedPositionTitle());
        existingOffer.setProposedSalary(dto.getProposedSalary());
        existingOffer.setProbationSalary(dto.getProbationSalary());
        existingOffer.setExpectedStartDate(dto.getExpectedStartDate());
        existingOffer.setWorkLocation(dto.getWorkLocation());
        existingOffer.setBenefitsPackage(dto.getBenefitsPackage());
        existingOffer.setResponsePeriodDays(dto.getResponsePeriodDays());
        
        // Dòng 260: HỦY CỜ XÓA MỀM (Reactivate)
        existingOffer.setIsDeleted(false);
        existingOffer.setOfferStatus(Boolean.TRUE.equals(dto.getIsDraft()) ? "Draft" : "Pending_Director");
        existingOffer.setUpdatedAt(LocalDateTime.now());
        
        // Dòng 264: Lưu lại bằng câu lệnh UPDATE -> Không vi phạm UQ_OfferProposal_Application!
        OfferProposal reactivated = offerProposalRepository.save(existingOffer);
        return mapToResponse(reactivated);
    }
    // Dòng 267: Nếu bản ghi cũ đang hoạt động (IsDeleted = false) thì ném lỗi nghiệp vụ
    throw new OfferValidationException("Ứng viên này đã có một đề xuất Offer đang hoạt động.");
}
```

#### 4. File / Method / Dòng code
* **File:** [`src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java)
* **Method:** `createOfferByHr` (L231–318), `deleteDraftOfferByHr` (L600–625).

#### 5. Vì sao dùng?
* Giải quyết triệt để lỗi xung đột giữa Soft Delete và Unique Constraint (`BUG-OFF-11`).
* Tiết kiệm dung lượng và bảo toàn khóa chính ID, giữ cho quan hệ hồ sơ luôn sạch sẽ.

#### 6. Lỗi thường gặp
* Chỉ gán `isDeleted = false` mà quên cập nhật lại `UpdatedAt` hoặc quên xóa lý do từ chối cũ (`declineReason`), khiến dữ liệu cũ còn sót lại trong đề xuất mới.

#### 7. Câu hỏi bảo vệ và đáp án
* **Hội đồng hỏi:** *"Khi HR xóa một bản nháp Offer rồi tạo lại cho cùng ứng viên đó, làm thế nào hệ thống không bị lỗi trùng khóa Unique trên database?"*
* **Đáp án bảo vệ:** *"Thưa thầy cô, nhóm đã áp dụng cơ chế Hồi sinh bản ghi (Reactivation) trong `OfferServiceImpl`. Trước khi tạo mới, service sẽ kiểm tra `findByApplication_ApplicationId()`. Nếu thấy bản ghi đã tồn tại nhưng có cờ `IsDeleted = 1`, hệ thống sẽ cập nhật thông tin mới lên bản ghi đó và đổi `IsDeleted = 0` (sinh câu lệnh UPDATE) thay vì cố INSERT bản ghi mới, nhờ đó vượt qua được ràng buộc Unique một cách hoàn toàn hợp lệ."*

#### 8. Bài thực hành
1. Tạo một Offer bản nháp cho Ứng viên A $\rightarrow$ Xem trên DB thấy `OfferId = 50, IsDeleted = 0`.
2. Bấm nút "Xóa bản nháp" $\rightarrow$ Xem trên DB thấy `OfferId = 50, IsDeleted = 1`.
3. Bấm Tạo mới Offer cho chính Ứng viên A với mức lương mới $\rightarrow$ Thao tác thành công. Kiểm tra DB: Vẫn là `OfferId = 50`, nhưng lương đã đổi và `IsDeleted = 0`.

---

# PHẦN 6. BUSINESS RULES (QUY TẮC NGHIỆP VỤ)

---

### Khái niệm 6.1: Luật Lương Thử Việc 85% (Bộ Luật Lao Động Việt Nam & BR-OFF-01)

#### 1. Giải thích từ cơ bản
Theo Điều 26 Bộ luật Lao động 2019 của Việt Nam: *"Tiền lương của người lao động trong thời gian thử việc do hai bên thỏa thuận nhưng ít nhất phải bằng 85% mức lương của công việc đó."*  
Trong hệ thống phần mềm Mộc RMS, quy tắc này được định danh là **`BR-OFF-01`**. Để đảm bảo tính pháp lý và không cho phép bất kỳ sai sót nào xảy ra, quy tắc này được cài đặt bảo vệ qua **4 lớp độc lập**:
1. **Lớp 1 (UI Client):** JavaScript tự động tính toán lương thử việc gợi ý bằng đúng 85% ngay khi HR vừa nhập mức lương chính thức.
2. **Lớp 2 (Bean Validation DTO):** Annotation `@AssertTrue isProbationSalaryValid()` trên `CreateOfferRequest` và `UpdateOfferRequest`.
3. **Lớp 3 (Service Layer):** Phương thức `validateProbationSalaryRule()` trong `OfferServiceImpl` kiểm tra và ném `OfferValidationException`.
4. **Lớp 4 (Database Constraint):** Ràng buộc `CK_OfferProposal_Salaries` trong CSDL SQL Server.

#### 2. Liên hệ code Offer
Phương thức kiểm tra chuyên biệt tại tầng Service [`OfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java) (L672–690).

#### 3. Ví dụ code và giải thích từng dòng
Trích từ [`OfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java):

```java
private void validateProbationSalaryRule(BigDecimal proposedSalary, BigDecimal probationSalary) {
    // Dòng 673: Kiểm tra hai trường không được phép null
    if (proposedSalary == null || probationSalary == null) {
        throw new OfferValidationException("Mức lương đề xuất và lương thử việc không được để trống.");
    }

    // Dòng 677: Kiểm tra giá trị phải dương
    if (proposedSalary.compareTo(BigDecimal.ZERO) <= 0 || probationSalary.compareTo(BigDecimal.ZERO) <= 0) {
        throw new OfferValidationException("Mức lương đề xuất và lương thử việc phải lớn hơn 0.");
    }

    // Dòng 681: Tính toán mức tối thiểu 85%
    BigDecimal minProbationSalary = proposedSalary.multiply(new BigDecimal("0.85"));

    // Dòng 683: Bắt buộc lương thử việc phải >= 85%
    if (probationSalary.compareTo(minProbationSalary) < 0) {
        throw new OfferValidationException(
                "Lương thử việc (" + probationSalary + " VND) phải đạt tối thiểu 85% lương chính thức ("
                        + minProbationSalary + " VND) theo quy định Luật Lao động.");
    }

    // Dòng 689: Ràng buộc bổ sung: Lương thử việc không được phép lớn hơn lương chính thức
    if (probationSalary.compareTo(proposedSalary) > 0) {
        throw new OfferValidationException("Lương thử việc không được vượt quá lương chính thức.");
    }
}
```

#### 4. File / Method / Dòng code
* **File:** [`src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java) (L672–690).
* **Test kiểm chứng:** [`OfferServiceTests.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/test/java/com/group2/rms/offer/service/OfferServiceTests.java) (`testProbationSalaryLessThan85Percent_throwsException`).

#### 5. Vì sao dùng?
* Đảm bảo tính tuân thủ pháp luật lao động, hạn chế rủi ro pháp lý cho doanh nghiệp khi ký kết hợp đồng thử việc với ứng viên.

#### 6. Lỗi thường gặp
* Làm tròn số không cẩn thận khi nhân `0.85`, ví dụ: lương chính thức `15,000,000`, 85% là `12,750,000`. Nếu nhập `12,749,999` sẽ bị hệ thống chặn.

#### 7. Câu hỏi bảo vệ và đáp án
* **Hội đồng hỏi:** *"Quy tắc 85% lương thử việc được kiểm tra ở những tầng nào trong hệ thống?"*
* **Đáp án bảo vệ:** *"Dạ thưa thầy cô, quy tắc BR-OFF-01 được nhóm bảo vệ qua 4 tầng: (1) Tầng giao diện JS tự tính gợi ý, (2) Tầng DTO dùng `@AssertTrue`, (3) Tầng Service dùng hàm `validateProbationSalaryRule()` ném `OfferValidationException`, và (4) Tầng Database có Check Constraint `CK_OfferProposal_Salaries`. Nhờ đó dữ liệu luôn đảm bảo tuân thủ tuyệt đối quy định pháp luật."*

#### 8. Bài thực hành
1. Mở form tạo Offer, nhập lương chính thức: `20,000,000`.
2. Quan sát ô Lương thử việc: JavaScript tự động điền gợi ý `17,000,000` (đúng 85%).
3. Thử sửa thành `16,000,000` rồi submit $\rightarrow$ Bị chặn lại ở cả client và server.

---

### Khái niệm 6.2: Quy Tắc Deadline & Tự Động Khóa Phản Hồi (Response Deadline Lockout)

#### 1. Giải thích từ cơ bản
Khi phát hành một Thư mời nhận việc, doanh nghiệp không thể để ứng viên suy nghĩ vô thời hạn. Hệ thống quy định một khoảng thời hạn phản hồi (`ResponsePeriodDays`, mặc định từ 1 đến 60 ngày, tiêu chuẩn là 5 ngày).  
Thời điểm hết hạn phản hồi chính thức (`ResponseDeadline`) được tính tại thời điểm HR bấm phát hành thư mời:
$$\text{ResponseDeadline} = \text{Thời điểm gửi (now)} + \text{ResponsePeriodDays (ngày)}$$
**Quy tắc nghiệp vụ bất biến:**
1. Hạn phản hồi `ResponseDeadline` bắt buộc phải diễn ra **trước** Ngày bắt đầu làm việc dự kiến (`ExpectedStartDate`).
2. Khi thời điểm hiện tại `now` vượt quá `ResponseDeadline`, quyền Chấp thuận (`Accept`) và Từ chối (`Decline`) của ứng viên lập tức bị khóa. Trạng thái Offer trên DB vẫn duy trì là `Sent_Candidate` nhưng không ai có thể can thiệp được nữa.

#### 2. Liên hệ code Offer
* Tại thời điểm phát hành ([`OfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java) L636–644): Tính và kiểm tra deadline với ngày bắt đầu.
* Tại thời điểm ứng viên phản hồi ([`CandidateOfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/CandidateOfferServiceImpl.java) L141–155): Chặn phản hồi nếu đã quá hạn.

#### 3. Ví dụ code và giải thích từng dòng
Trích từ [`CandidateOfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/CandidateOfferServiceImpl.java) (Dòng 141–155):

```java
private void requireAnswerable(OfferProposal offer) {
    // Dòng 142: Kiểm tra trạng thái: Phải đang ở trạng thái Sent_Candidate
    if (!SENT.equals(offer.getOfferStatus())) {
        throw new OfferResponseException("Thư mời này đã được trả lời nên không thể thay đổi.");
    }
    
    // Dòng 145: Lấy thời gian hiện tại từ Clock (hỗ trợ giả lập thời gian khi chạy Unit Test)
    LocalDateTime now = LocalDateTime.now(clock);
    
    // Dòng 146: KIỂM TRA QUÁ HẠN PHẢN HỒI (ResponseDeadline)
    if (offer.getResponseDeadline() != null && now.isAfter(offer.getResponseDeadline())) {
        String deadlineStr = java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
                .format(offer.getResponseDeadline());
        throw new OfferResponseException("Thư mời này đã quá thời hạn phản hồi (" + deadlineStr + "). Vui lòng liên hệ bộ phận Tuyển dụng để được hỗ trợ.");
    }
    
    // Dòng 151: KIỂM TRA QUÁ NGÀY ĐI LÀM DỰ KIẾN (ExpectedStartDate)
    if (offer.getExpectedStartDate() != null && offer.getExpectedStartDate().isBefore(now.toLocalDate())) {
        String dateStr = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy").format(offer.getExpectedStartDate());
        throw new OfferResponseException("Thư mời này đã quá ngày bắt đầu nhận việc dự kiến (" + dateStr + "). Vui lòng liên hệ bộ phận Tuyển dụng để được hỗ trợ.");
    }
}
```

#### 4. File / Method / Dòng code
* **File:** [`src/main/java/com/group2/rms/offer/service/CandidateOfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/CandidateOfferServiceImpl.java)
* **Method:** `requireAnswerable` (L141–155), `isExpired` (L157–167).

#### 5. Vì sao dùng?
* Giúp doanh nghiệp chủ động kế hoạch nhân sự; nếu ứng viên không trả lời kịp thời, HR có quyền liên hệ ứng viên dự phòng khác mà không bị ràng buộc pháp lý treo.

#### 6. Lỗi thường gặp
* Để hạn phản hồi rơi vào sau ngày dự kiến đi làm (ví dụ: ngày đi làm là ngày mai, nhưng hạn phản hồi là 5 ngày nữa). Service đã có lệnh chặn tình huống này tại dòng 639 [`OfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java).

#### 7. Câu hỏi bảo vệ và đáp án
* **Hội đồng hỏi:** *"Khi thư mời hết hạn phản hồi thì trạng thái của nó trong database đổi thành gì?"*
* **Đáp án bảo vệ:** *"Dạ thưa thầy cô, trạng thái trong database vẫn được giữ nguyên là `Sent_Candidate` để bảo toàn vết nghiệp vụ (đã từng gửi đi). Tuy nhiên, cột `ResponseDeadline` sẽ chỉ ra rằng thời hạn đã trôi qua. Khi hiển thị ra ngoài hoặc khi ứng viên bấm nút, phương thức `isExpired()` và `requireAnswerable()` sẽ phát hiện ra và tự động gắn nhãn 'Đã quá hạn phản hồi' đồng thời vô hiệu hóa mọi thao tác."*

#### 8. Bài thực hành
1. Dùng tài khoản Candidate mở một thư mời có `ResponseDeadline` trong quá khứ.
2. Thấy giao diện hiển thị badge màu xám `badge--neutral` "Đã quá hạn phản hồi".
3. Thử gửi POST request `/portal/offers/{id}/accept` bằng Postman $\rightarrow$ Server trả về lỗi: *"Thư mời này đã quá thời hạn phản hồi..."*.

---

### Khái niệm 6.3: Quy Tắc Đồng Bộ Trạng Thái Hồ Sơ Ứng Tuyển Một Chiều (GBR-01 & Cross-Module Sync)

#### 1. Giải thích từ cơ bản
Trong hệ thống tuyển dụng Mộc RMS, một ứng viên có hồ sơ `Application`. Vòng đời của `Application` trải qua nhiều module: Tiếp nhận hồ sơ $\rightarrow$ Phỏng vấn (`Interview`) $\rightarrow$ Thư mời nhận việc (`Offer`).  
Để đảm bảo các module hoạt động độc lập (Decoupled) nhưng vẫn đồng bộ dữ liệu:
* Module `Offer` được phép gọi sang `ApplicationStatusService` để thông báo sự kiện đã diễn ra.
* **Quy tắc GBR-01 (Tiến trình một chiều):** Trạng thái hồ sơ chỉ có tiến, không có lùi. Không có thao tác nào trong phân hệ Offer được phép đảo ngược trạng thái của `Application` về các vòng tuyển dụng trước.

#### 2. Liên hệ code Offer
* Khi phát hành thư mời: `application.setApplicationStatus("Offered")`.
* Khi ứng viên chấp thuận nhận việc: gọi `applicationStatusService.onOfferAnswered(applicationId, true)` $\rightarrow$ `ApplicationStatus` chuyển thành `"Hired"`.
* Khi ứng viên từ chối nhận việc: gọi `applicationStatusService.onOfferAnswered(applicationId, false)` $\rightarrow$ `ApplicationStatus` chuyển thành `"Rejected"`.

#### 3. Ví dụ code và giải thích từng dòng
Trích từ [`ApplicationStatusService.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/domain/rule/ApplicationStatusService.java) (Dòng 80–88):

```java
// Dòng 80: Nhận sự kiện từ module Offer: ứng viên đã trả lời thư mời
public void onOfferAnswered(Integer applicationId, boolean accepted) {
    // Dòng 81: Chỉ cho phép chuyển từ trạng thái 'Offered' sang 'Hired' (nếu nhận) hoặc 'Rejected' (nếu từ chối)
    move(applicationId, OFFERED, accepted ? "Hired" : REJECTED);
}

// Dòng 84: Hàm thực hiện chuyển trạng thái nguyên tử
private void move(Integer applicationId, Set<String> from, String to) {
    // Dòng 85: Gọi câu lệnh SQL transition: UPDATE Application SET Status = :to WHERE Id = :id AND Status IN (:from)
    if (applications.transition(applicationId, from, to) == 0) {
        // Dòng 86: Nếu hồ sơ không ở trạng thái 'Offered', giữ nguyên trạng thái, không ném exception làm crash bên gọi
        log.debug("Đơn #{} không ở {} nên giữ nguyên trạng thái (không chuyển sang {}).", applicationId, from, to);
    }
}
```

#### 4. File / Method / Dòng code
* **File:** [`src/main/java/com/group2/rms/candidate/domain/rule/ApplicationStatusService.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/candidate/domain/rule/ApplicationStatusService.java)
* **Method:** `onOfferAnswered` (L80–82), `move` (L84–88).
* **Nơi gọi:** [`CandidateOfferServiceImpl.java`](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/CandidateOfferServiceImpl.java) (L125).

#### 5. Vì sao dùng?
* Đảm bảo tính nhất quán dữ liệu xuyên suốt các phòng ban: Khi ứng viên đồng ý nhận việc trên Portal thì trên màn hình quản lý hồ sơ của HR và Hiring Manager, ứng viên đó ngay lập tức hiển thị nhãn xanh lá **"Hired" (Đã tuyển dụng)**.

#### 6. Lỗi thường gặp
* Viết code cập nhật trạng thái `Application` bằng setter mà không kiểm tra trạng thái trước đó, vô tình ghi đè hồ sơ đã bị Hủy/Rút lui (`Withdrawn`).

#### 7. Câu hỏi bảo vệ và đáp án
* **Hội đồng hỏi:** *"Mối quan hệ giữa bảng `OfferProposal` và bảng `Application` được đồng bộ như thế nào khi ứng viên đồng ý hoặc từ chối nhận việc?"*
* **Đáp án bảo vệ:** *"Thưa thầy cô, hai bảng này có mối quan hệ 1-1. Trong cùng transaction của phương thức `CandidateOfferServiceImpl.answer()`, sau khi bảng `OfferProposal` đổi trạng thái thành `Accepted`, service lập tức gọi `ApplicationStatusService.onOfferAnswered()`. Lệnh SQL `transition` sẽ kiểm tra nếu `Application` đang ở `Offered` thì chuyển thành `Hired` (hoặc `Rejected` nếu từ chối). Cả 2 thao tác cùng commit hoặc rollback đồng thời."*

#### 8. Bài thực hành
1. Mở trang quản trị hồ sơ ứng viên `/applications` $\rightarrow$ Thấy ứng viên đang có trạng thái `Offered`.
2. Đăng nhập tài khoản của ứng viên đó vào `/portal/offers`, bấm "Đồng ý nhận việc".
3. F5 lại trang `/applications` của HR $\rightarrow$ Thấy trạng thái của hồ sơ đó tự động chuyển sang `Hired` mà HR không cần thao tác thủ công.

---

# TỔNG KẾT KỸ THUẬT DÀNH CHO HUYENPT

| Tầng công nghệ | Điểm nhấn kỹ thuật cần nhấn mạnh khi bảo vệ |
| :--- | :--- |
| **Spring Boot** | • Mô hình Hybrid Controller linh hoạt giữa Thymeleaf HTML và JSON Ajax.<br>• `@ModelAttribute` kiểm soát vai trò và phân luồng giao diện tự động.<br>• Bean Validation 2 tầng: `@Valid` cơ bản kết hợp `@AssertTrue` cho quy tắc 85% lương.<br>• Quản lý giao dịch `@Transactional` đồng bộ giữa Offer và Application. |
| **JavaScript** | • HTML5 native `<dialog>` thay thế hoàn toàn SweetAlert2 / CDN bên ngoài, loại bỏ rủi ro rớt mạng.<br>• Tích hợp Fetch API bất đồng bộ với Header `X-CSRF-TOKEN` bảo mật.<br>• Bộ đếm ngược thời gian thực `setInterval` đếm lùi thời hạn phản hồi và khóa DOM tự động.<br>• Phòng thủ Double Click bằng cơ chế disable nút bấm và hiển thị trạng thái loading. |
| **Database** | • Bảo vệ đa tầng bằng `CHECK CONSTRAINT` cho lương 85% và danh sách trạng thái hợp lệ.<br>• Atomic Conditional Query `answerIfSent()` triệt tiêu hoàn toàn Race Condition.<br>• Cơ chế `JOIN FETCH` ngăn chặn triệt để lỗi N+1 Query khi load chi tiết thư mời.<br>• Cơ chế Hồi sinh dữ liệu (Reactivation) giải quyết triệt để xung đột giữa Soft Delete và Unique Constraint. |
| **Business Rules** | • Tuân thủ chuẩn mực Bộ luật Lao động Việt Nam về mức lương thử việc $\ge 85\%$.<br>• Quy tắc Single Active Offer: Mỗi hồ sơ chỉ có tối đa 1 thư mời hoạt động.<br>• Quy tắc khóa phản hồi khi quá hạn `ResponseDeadline`.<br>• Tiến trình trạng thái Application một chiều (GBR-01) chuyển dịch tự động theo phản hồi của ứng viên. |

