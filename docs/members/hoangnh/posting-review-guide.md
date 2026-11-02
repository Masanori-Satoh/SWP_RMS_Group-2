# Cẩm nang review: Tin tuyển dụng (Job Posting)

> Ghi chú cá nhân của HoangNH để trả lời GV khi review 3 màn: **Danh sách**, **Tạo / Sửa tin**, **Chi tiết tin**.
> Mỗi nút: bấm vào thì chạy qua đâu, kiểm tra gì, ghi bảng nào. Mỗi trường: lấy từ cột nào hoặc tính ra sao.
> Code được tham chiếu theo `Class#method`. Quyết định nghiệp vụ D1–D11: xem `plan_posting.md` mục 2.
> Cập nhật 02/11/2026: đã bỏ "Hẹn đăng" (D1) và các số liệu AI; thẻ "Đạt ngưỡng AI" đổi thành "Qua vòng CV".

---

## 0. Bản đồ nhanh

```
Trình duyệt ──► SecurityConfig (lớp 1: vai trò có được vào URL không?)
            ──► JobPostingController (nhận tham số, @Valid + whitelist trường form, chọn view, đổi lỗi thành toast)
            ──► JobPostingListService (màn danh sách: phạm vi theo vai trò, bộ lọc, chip, phân trang)
                JobPostingServiceImpl (một tin: tạo / sửa / chuyển trạng thái / xóa / chi tiết; transaction, khóa dòng)
                  ├─ JobPostingAccess         : lớp 2 — ai được làm gì (canX / requireX), giá trị trạng thái
                  ├─ JobPostingValidator      : quy tắc form (lỗi theo từng ô), "bây giờ" theo Clock
                  ├─ JobPostingMapper         : entity → DTO, gắn cờ thao tác / dải thông báo (không truy vấn DB)
                  ├─ JobPostingAuditLogger    : ghi audit, snapshot trước / sau (FieldChanges)
                  ├─ JobPostingLabels         : nhãn tiếng Việt, màu badge, nhóm hạn nộp, tên hoạt động
                  ├─ JobPostingSpecifications : điều kiện WHERE màn danh sách
                  ├─ JobPostingSort           : các cách sắp xếp (whitelist)
                  ├─ NotificationService      : báo HM khi tin được đăng / mở lại / tạm dừng / đóng
                  └─ JobPostingRepository     : truy vấn, đếm hồ sơ gộp, @EntityGraph chống N+1
            ──► Template Thymeleaf (chỉ hiển thị; nút / dải thông báo hiện theo cờ có sẵn trong DTO)
```

| URL | Controller | Service | Bảng bị ghi |
|---|---|---|---|
| `GET /internal/job-postings` | `listJobPostings` | `JobPostingListService#getListPage` | (chỉ đọc) |
| `GET /internal/job-postings/create?requisitionId=…` | `showCreateForm` | `prepareCreateForm` | (chỉ đọc) |
| `GET /internal/job-postings/create?fromPostingId=…` | `showCreateForm` | `prepareRepostForm` ("Đăng lại") | (chỉ đọc) |
| `POST /internal/job-postings/create` | `createJobPosting` | `createJobPosting` / `createAndPublishJobPosting` | `JobPosting`, `AuditLog`, `Notification` (khi đăng) |
| `GET /internal/job-postings/{id}/edit` | `showEditForm` | `prepareEditForm` | (chỉ đọc) |
| `POST /internal/job-postings/{id}/edit` | `updateJobPosting` | `updateJobPosting` / `updateAndPublishJobPosting` | `JobPosting`, `AuditLog`, `Notification` (khi đăng / mở lại) |
| `POST /internal/job-postings/{id}/publish` | `publishJobPosting` | `publishJobPosting` (đăng nháp / mở lại tạm dừng) | `JobPosting`, `AuditLog`, `Notification` |
| `POST /internal/job-postings/{id}/pause` | `pauseJobPosting` | `pauseJobPosting` | `JobPosting`, `AuditLog`, `Notification` |
| `POST /internal/job-postings/{id}/close` | `closeJobPosting` | `closeJobPosting` → `TalentPoolService#closeJob`, rồi `RecruitmentEmailService#sendJobClosure` | `JobPosting` (Closed, ClosedAt), hồ sơ đang xử lý, lịch phỏng vấn, `Notification`; gửi email cho ứng viên |
| `POST /internal/job-postings/delete/{id}` | `deleteJobPosting` | `deleteJobPosting` | xóa `JobPosting`, ghi `AuditLog` |
| `GET /internal/job-postings/{id}` | `showJobPostingDetail` | `getInternalJobPostingDetail` | (chỉ đọc) |

Mọi `POST` cần CSRF token (thiếu → 403). Thao tác ghi trên danh sách / chi tiết đều mở hộp xác nhận trước
(`job-postings/fragments :: confirmDialog` + `pages/job-posting-actions.js`).

Danh sách tin **không có nút "Tạo tin"**: tin chỉ được tạo từ một yêu cầu đã duyệt (nút "Tạo tin tuyển dụng" ở chi tiết yêu cầu, hoặc dashboard HR).

**Mã trên URL gõ tay** (`/internal/job-postings/{1689}`): `parseId` lấy phần số. Mã sai / tin không tồn tại → về danh sách tin kèm toast, không ra lỗi 500.

---

## 1. Phân quyền: hai lớp

**Lớp 1, URL** (`SecurityConfig`):

| URL | Vai trò |
|---|---|
| `GET /internal/job-postings`, `GET /internal/job-postings/{id}` | HR, Hiring Manager, Director |
| `/internal/job-postings/create` | chỉ HR |
| `/internal/job-postings/{id}/close` | chỉ HR |
| Mọi thao tác ghi khác | chỉ HR |

System Admin không có rule nào ở đây → bị chặn (Admin chỉ cấu hình hệ thống, không làm nghiệp vụ).

**Lớp 2, dữ liệu** (`JobPostingAccess#canView` / `requireView`; danh sách lọc cùng phạm vi ở `JobPostingListService#scope`):

| Vai trò | Thấy tin nào | Thao tác |
|---|---|---|
| HR | Tất cả | Tạo, sửa, đăng, tạm dừng, mở lại, **đóng**, xóa nháp, đăng lại |
| Hiring Manager | Tin thuộc phòng mình / phòng mình làm trưởng (`inManagedDepartment`), **trừ tin nháp** (D5) | Chỉ xem; xem ứng viên, ứng viên tiềm năng |
| Director | Mọi tin, **trừ tin nháp** (D4, D5) | Chỉ xem |

**Không đủ quyền** (`JobPostingController#whenAccessDenied`): về danh sách tin kèm toast lỗi, không hiện trang 403. Nếu chính danh sách bị chặn thì về `/dashboard` để tránh chuyển trang vòng lặp.

> GV hỏi: *"HM gõ thẳng `/internal/job-postings/17` của một tin nháp thì sao?"*
> Trả lời: service kiểm tra lại: tin nháp mà người xem không phải HR → `AccessDeniedException` → về danh sách kèm toast "Bạn không có quyền xem tin tuyển dụng này."
> Danh sách cũng tự ẩn tin nháp (`JobPostingSpecifications#notDraft`); sửa URL `?status=Draft` thì giá trị bị bỏ qua vì HM không có lựa chọn đó.

---

## 2. Vòng đời trạng thái (cột `JobPosting.PostingStatus`)

```
 (mới) ──Lưu nháp──► Draft ──Đăng tin──► Published ──Tạm dừng──► Paused
                                            ▲   │                  │
                                            │   └──◄──Mở lại───────┘
                                            │
                          Đóng tin (HR, từ Published hoặc Paused) ──► Closed ──Đăng lại──► tin MỚI (PostingNo + 1)
```

- DB chỉ có 4 giá trị (`CK_JobPosting_Status`): `Draft`, `Published`, `Paused`, `Closed`.
- Đăng tin (lần đầu hoặc mở lại) luôn đặt `PostingDate = now`. **Không còn "Hẹn đăng"**.
- Mỗi yêu cầu chỉ có **1 tin đang mở** (Draft / Published / Paused) cùng lúc (`requireNoOtherOpenPosting`).
- Tin đã đóng không mở lại được; muốn tuyển tiếp thì **Đăng lại** = tạo tin mới cùng yêu cầu, nội dung lấy từ tin cũ (D10).

---

## 3. Màn DANH SÁCH (`job-postings/list.html`)

### 3.1 Thanh công cụ

| Thành phần | Chạy như thế nào |
|---|---|
| Ô tìm kiếm | `?q=` → LIKE trên **tiêu đề tin** hoặc **tên phòng ban** (escape `% _ [`, tối đa 120 ký tự) |
| Trạng thái | Radio **một lựa chọn**: Tất cả, Bản nháp (chỉ HR), Đang đăng, Tạm dừng, Đã đóng. Server chỉ giữ 1 giá trị (`.limit(1)`) |
| Phòng ban | HR, Director (HM không có vì chỉ thấy phòng mình) |
| Hạn nộp | Còn hạn (`ApplicationDeadline >= now` hoặc chưa có hạn) / Đã quá hạn (`< now`). Nhóm "sắp hết" (≤ 7 ngày) chỉ dùng để tô màu dòng, không có trên bộ lọc |
| Sắp xếp | Mới đăng nhất, Hạn nộp gần nhất, **Nhiều hồ sơ nhất** (ORDER BY số hồ sơ bằng subquery, `JobPostingSpecifications#orderByApplicationCount`), Tiêu đề A–Z |
| Chip ✕ | Bỏ riêng một bộ lọc, giữ các tham số khác (`FilterLinks#without`, dùng chung với màn yêu cầu) |
| "Đặt lại mặc định" | Về `/internal/job-postings` (không tham số) |

Giá trị lạ trên URL (vai trò không có ô đó, giá trị ngoài danh sách) bị bỏ qua (`normalizeFilter`), không báo lỗi.

### 3.2 Các cột

| Cột | Lấy từ đâu / tính thế nào |
|---|---|
| Tiêu đề tin | `JobPosting.PostingTitle`; dòng phụ "Lần N · Hình thức" = `PostingNo` + `JobRequisition.EmploymentType` |
| Phòng ban | `Department.DepartmentName` qua yêu cầu nguồn (ẩn với HM) |
| Hồ sơ | Đếm `Application` theo tin; **"+N mới"** = hồ sơ `Applied` (chưa ai xử lý, D9). Cả trang đếm bằng **1 câu** `countApplicationsByPosting` (không N+1) |
| Ngày đăng | `PostingDate`; nháp hiện "Chưa đăng" |
| Hạn nộp | `ApplicationDeadline`; "còn N ngày" / "quá hạn N ngày"; vàng khi ≤ 7 ngày, đỏ khi quá hạn; tin đã đóng không tô màu |
| Trạng thái | `JobPostingLabels#statusLabel` / `#statusTone` |
| Người tạo | `JobPosting.CreatedBy` (chỉ HR thấy) |
| Menu ⋯ | Theo cờ do `JobPostingMapper#toListRow` tính (luật ở `JobPostingAccess`): Đăng tin, Mở lại, Xem ứng viên, Xem chi tiết, Chỉnh sửa, Tạm dừng, Đóng tin (HR), Xóa bản nháp (nháp chưa có hồ sơ) |

---

## 4. Màn TẠO / SỬA TIN (`job-postings/create.html`)

### 4.1 Thẻ yêu cầu nguồn (chỉ đọc)

Mã, phòng ban, số lượng, "Tin lần N" (= lần đăng lớn nhất + 1). "Xem thêm thông tin yêu cầu":
hình thức, mô hình, giới tính, thử việc, mức lương, HM, **Lý do tuyển dụng** (nội bộ, không đưa vào tin công khai, rule §24.2).
**Đổi yêu cầu** (chỉ khi tạo mới, khi có từ 2 lựa chọn): danh sách yêu cầu Approved chưa có tin đang mở, kèm "(đã đăng N lần)" (`requisitionChoices`); chọn xong mở lại form với yêu cầu đó.

Thông tin nguồn **luôn lấy lại từ DB** (`restoreSourceInfo` khi form lỗi). Form chỉ nhận các trường trong
whitelist (`JobPostingController#formBinder`); gửi thêm `postingStatus`, `minSalary`… bị bỏ qua.

Mở form không hợp lệ (yêu cầu chưa duyệt, đã có tin đang mở, tin đã đóng mà bấm sửa, mã không tồn tại) → về trang chi tiết tin hoặc danh sách kèm toast.

Form có 2 tab **Soạn tin / Xem trước** (xem trước nội dung như ứng viên sẽ thấy).

### 4.2 Các trường

| Trường | Cột DB | Quy tắc (`JobPostingValidator#normalizeAndValidate`) |
|---|---|---|
| Tiêu đề tin * | `PostingTitle` NVARCHAR(200) NOT NULL | Bắt buộc cả khi nháp, ≤ 200. Điền sẵn tên vị trí của yêu cầu |
| Địa điểm | `WorkLocation` NVARCHAR(255) | ≤ 255. Có "Xem bản đồ" (Google Maps nhúng) (D3) |
| Mức lương hiển thị | `SalaryDisplay` NVARCHAR(100) | 3 lựa chọn: **Theo yêu cầu** (`range`: server tính lại từ `MinSalary/MaxSalary` của yêu cầu, không tin số gửi lên), **Thỏa thuận** (`nego`), **Tự nhập** (`custom`: bắt buộc nội dung, ≤ 100) |
| Hiển thị số lượng | `NumberOfPositionsDisplay` NVARCHAR(50) | Bật thì bắt buộc nội dung; tắt thì lưu **NULL** (trang công khai không hiện số lượng) |
| Mô tả công việc * / Yêu cầu ứng viên * | NVARCHAR(MAX) NOT NULL | Bắt buộc cả khi nháp, ≤ 5000 (giới hạn ở ứng dụng). Điền sẵn từ `JobDescription` / `RequirementDetails` của yêu cầu |
| Quyền lợi | `Benefits` NVARCHAR(MAX) | ≤ 5000. Nút thêm nhanh (+ Lương tháng 13, ...) |
| Hạn nộp * | `ApplicationDeadline` (lưu 23:59:59 của ngày chọn) | **Bắt buộc khi đăng** và khi sửa tin đã đăng (D6); nếu có thì phải **sau thời điểm hiện tại**. Mặc định hôm nay + 30 ngày |

Mọi chuỗi trim; ô nhiều dòng đổi `\r\n` → `\n` trước khi đếm. Lỗi trả về **theo từng ô** (`JobPostingValidationException#getFieldErrors`),
form giữ nguyên dữ liệu đã nhập. JS (`pages/job-posting-form.js`) kiểm tra giống server để báo sớm; nút bị khóa khi đang gửi.

### 4.3 Các nút

| Tin đang là | Nút | Kết quả |
|---|---|---|
| Mới / Nháp | **Lưu nháp** (`action=draft`) | `Draft`, `PostingDate = NULL` |
| Mới / Nháp | **Đăng tin** (`action=publish`) | `Published`, `PostingDate = now`, báo HM (`notifyJobPostingPublished`) |
| Đang đăng | **Lưu thay đổi** | Giữ ngày đăng, chỉ đổi nội dung, không báo lại |
| Tạm dừng | **Lưu thay đổi** | Giữ tạm dừng, chỉ đổi nội dung |
| Tạm dừng | **Lưu và mở lại** | `Published`, `PostingDate = now`, báo HM (`notifyJobPostingReopened`) |

Khi tạo, service **khóa dòng yêu cầu nguồn** (`findForUpdate`) rồi mới kiểm tra "đã có tin đang mở" và tính `PostingNo`.
Khi sửa, không đổi gì thì không lưu, không ghi audit. Lưu xong chuyển sang trang chi tiết tin, có thông báo. Mỗi lần lưu có thay đổi ghi `AuditLog`.

---

## 5. Màn CHI TIẾT (`job-postings/detail.html`)

### 5.1 Dữ liệu

| Phần | Lấy từ đâu / tính thế nào |
|---|---|
| Tiêu đề, "Lần N" | `PostingTitle`, `PostingNo` |
| Dải thông báo | Theo trạng thái: nháp / tạm dừng / đã đóng (`ClosedAt`, "Đã tuyển X/N vị trí") / còn ≤ 7 ngày / quá hạn |
| Tổng hồ sơ | `COUNT(Application)` của tin; "+N trong 7 ngày" theo `SubmissionDate` |
| Chờ xử lý | Hồ sơ `Applied` |
| Qua vòng CV | `ApplicationRepository#countPastCvRoundByJobPosting`: hồ sơ HR từng cho Đạt (kể cả bị loại ở bước sau), hoặc đang ở bước sau vòng CV (`HR_Passed`, `HM_Passed`, `Interviewing`, `Offered`, `Hired`) |
| Đang phỏng vấn / Đã tuyển | Hồ sơ `Interviewing` / `Hired`; "/ N" = `JobRequisition.NumberOfPositions` |
| Tab Ứng viên | Hồ sơ mới nhất (tên ứng viên, trạng thái, ngày nộp) + "Xem tất cả" → `/applications?jobPostingId=` |
| Các lần đăng | Mọi `JobPosting` cùng `RequisitionId`, kèm số hồ sơ và số `Hired` (2 câu đếm gộp) |
| Lịch sử | `AuditLog` có `EntityName = 'JobPosting'` |

Số liệu chỉ có khi tin đã lên trang tuyển dụng (đang đăng / tạm dừng / đã đóng, `JobPostingAccess#hasBeenPublished`).

### 5.2 Nút ở đầu trang

| Trạng thái | Nút (HR) | Bấm thì |
|---|---|---|
| Nháp | Đăng tin · Chỉnh sửa · Xóa bản nháp (chưa có hồ sơ) | Đăng: `POST /{id}/publish` → kiểm lại yêu cầu vẫn Approved, tin đủ tiêu đề / mô tả / yêu cầu, có hạn nộp và hạn nộp chưa qua (`requirePublishableContent`) |
| Đang đăng | Xem trên trang tuyển dụng ↗ · Chỉnh sửa · Tạm dừng nhận hồ sơ · Đóng tin | Tạm dừng: `POST /{id}/pause` → `Paused`, báo HM |
| Tạm dừng | Mở lại · Chỉnh sửa · Đóng tin | Mở lại: `POST /{id}/publish` (giống Đăng tin), báo HM "đã mở lại" |
| Đã đóng | Đăng lại (tạo tin lần N+1), khi yêu cầu còn Approved và chưa có tin đang mở | `GET /create?fromPostingId=` → form tạo tin, điền sẵn nội dung tin cũ |

**Đóng tin** (`POST /{id}/close`, chỉ HR): `TalentPoolService#closeJob` đóng tin, xử lý các hồ sơ còn dở và lịch phỏng vấn → báo HM → gửi email kết quả cho ứng viên. Gửi email lỗi thì **tin vẫn đóng**, chỉ hiện toast lỗi email.

HM, Director: chỉ có "Xem trên trang tuyển dụng" (nếu đang đăng). HR, HM có thêm khối "Ứng viên tiềm năng" (module talent pool).

---

## 6. Câu hỏi GV hay hỏi

**Đếm hồ sơ cho cả bảng có chậm không?**
Một câu `GROUP BY JobPostingId` cho cả trang (10 dòng), không phải mỗi dòng một câu.

**Hai HR cùng tạo tin cho một yêu cầu?**
Service khóa dòng `JobRequisition` (`findForUpdate`) trước khi kiểm tra "đã có tin đang mở" và tính `PostingNo`,
nên người sau phải chờ và sẽ bị báo "Yêu cầu này đã có tin nháp hoặc tin đang hoạt động."

**Sao "Theo yêu cầu" không dùng số lương trên form?**
Số lương nằm ở yêu cầu đã duyệt; form không gửi số này (whitelist), service đọc từ DB và định dạng bằng `RequisitionLabels#salary`.

**Xóa tin được không?**
Chỉ tin nháp chưa có hồ sơ. Tin đã đăng thì tạm dừng hoặc đóng, để giữ lịch sử hồ sơ ứng tuyển.

**Ứng viên thấy tin khi nào?**
Chỉ khi tin `Published` (trang công khai `/jobs` lọc trạng thái). Tạm dừng hoặc đóng thì tin biến khỏi trang công khai, không nộp hồ sơ được nữa.

**Yêu cầu nguồn bị đổi trạng thái sau khi đã có tin thì sao?**
Sửa, đăng, mở lại đều gọi lại `validator.requireApproved(requisition)`; yêu cầu không còn Approved thì báo "Chỉ có thể đăng tin từ yêu cầu đã được phê duyệt."

---

## 7. Hạn chế đã biết

- Chưa có **ảnh nơi làm việc** (mockup có; cần bảng mới + migration, để sau theo D2).
- Bản đồ nhúng cần mạng.
- Phần nội dung tin chưa tách fragment dùng chung với trang chi tiết công khai (rule §25), cần phối hợp DungLT.
- Lịch sử chỉ có từ khi tin được tạo qua hệ thống; tin trong seed không có `AuditLog` sẽ chỉ hiện "Tạo mới".
