# Cẩm nang review: Tin tuyển dụng (Job Posting)

> Ghi chú cá nhân của HoangNH để trả lời GV khi review 3 màn: **Danh sách**, **Tạo / Sửa tin**, **Chi tiết tin**.
> Mỗi nút: bấm vào thì chạy qua đâu, kiểm tra gì, ghi bảng nào. Mỗi trường: lấy từ cột nào hoặc tính ra sao.
> Code được tham chiếu theo `Class#method`. Quyết định nghiệp vụ D1–D11: xem `plan_posting.md` mục 2.

---

## 0. Bản đồ nhanh

```
Trình duyệt ──► SecurityConfig (lớp 1: vai trò có được vào URL không?)
            ──► JobPostingController (nhận tham số, whitelist trường form, chọn view)
            ──► JobPostingServiceImpl (nghiệp vụ, lớp 2: phạm vi dữ liệu theo vai trò, transaction, khóa dòng)
                  ├─ JobPostingValidator      : quy tắc form (lỗi theo từng ô)
                  ├─ JobPostingLabels         : trạng thái hiển thị ("Hẹn đăng"), nhãn, màu, số ngày
                  ├─ JobPostingSpecifications : điều kiện WHERE màn danh sách
                  ├─ JobPostingSort           : các cách sắp xếp (whitelist)
                  └─ JobPostingRepository     : truy vấn, đếm hồ sơ gộp
            ──► Template Thymeleaf (chỉ hiển thị; nút hiện / ẩn theo cờ service tính sẵn)
```

| URL | Controller | Service | Bảng bị ghi |
|---|---|---|---|
| `GET /internal/job-postings` | `listJobPostings` | `getListPage` | (chỉ đọc) |
| `GET /internal/job-postings/create[?requisitionId=…]` | `showCreateForm` | `prepareCreateForm` | (chỉ đọc) |
| `GET /internal/job-postings/create?fromPostingId=…` | `showCreateForm` | `prepareRepostForm` ("Đăng lại") | (chỉ đọc) |
| `POST /internal/job-postings/create` | `createJobPosting` | `createJobPosting` / `createAndPublishJobPosting` | `JobPosting`, `AuditLog`, `Notification` (khi đăng) |
| `GET /internal/job-postings/{id}/edit` | `showEditForm` | `prepareEditForm` | (chỉ đọc) |
| `POST /internal/job-postings/{id}/edit` | `updateJobPosting` | `updateJobPosting` / `updateAndPublishJobPosting` | `JobPosting`, `AuditLog`, `Notification` (khi đổi ngày đăng) |
| `POST /internal/job-postings/{id}/publish` | `publishJobPosting` | `publishJobPosting` (đăng nháp / mở lại tạm dừng) | `JobPosting`, `AuditLog`, `Notification` |
| `POST /internal/job-postings/{id}/publish-now` | `publishNow` | `publishNow` (tin hẹn đăng) | `JobPosting`, `AuditLog`, `Notification` |
| `POST /internal/job-postings/{id}/unschedule` | `unschedule` | `unschedule` (hẹn đăng → nháp) | `JobPosting`, `AuditLog` |
| `POST /internal/job-postings/{id}/pause` | `pauseJobPosting` | `pauseJobPosting` | `JobPosting`, `AuditLog` |
| `POST /internal/job-postings/{id}/close` | `closeJobPosting` | `TalentPoolService#closeJob` | `JobPosting` (Closed, ClosedAt), hồ sơ đang xử lý, lịch phỏng vấn |
| `POST /internal/job-postings/delete/{id}` | `deleteJobPosting` | `deleteJobPosting` | xóa `JobPosting`, ghi `AuditLog` |
| `GET /internal/job-postings/{id}` | `showJobPostingDetail` | `getInternalJobPostingDetail` | (chỉ đọc) |

Mọi `POST` cần CSRF token (thiếu → 403). Thao tác ghi trên danh sách / chi tiết đều mở hộp xác nhận trước
(`job-postings/fragments :: confirmDialog` + `pages/job-posting-actions.js`).

---

## 1. Phân quyền: hai lớp

**Lớp 1, URL** (`SecurityConfig`):

| URL | Vai trò |
|---|---|
| `GET /internal/job-postings`, `GET /internal/job-postings/{id}` | HR, Hiring Manager, Director, System Admin |
| `/internal/job-postings/create`, `/dashboard` | HR, System Admin |
| `/internal/job-postings/{id}/close` | chỉ HR |
| Mọi thao tác ghi khác | HR, System Admin |

**Lớp 2, dữ liệu** (`JobPostingServiceImpl#scope`, `#getInternalJobPostingDetail`):

| Vai trò | Thấy tin nào | Thao tác |
|---|---|---|
| HR | Tất cả | Tạo, sửa, đăng, hẹn đăng, tạm dừng, mở lại, **đóng**, xóa nháp, đăng lại |
| System Admin | Tất cả | Như HR, **trừ đóng tin** (D7: đóng tin còn xử lý Offer / đề xuất ứng viên) |
| Hiring Manager | Tin của phòng mình, **trừ tin nháp** (D5) | Chỉ xem; xem ứng viên, ứng viên tiềm năng |
| Director | Mọi tin, **trừ tin nháp** (D4, D5) | Chỉ xem |

> GV hỏi: *"HM gõ thẳng `/internal/job-postings/17` của một tin nháp thì sao?"*
> Trả lời: service kiểm tra lại, tin nháp mà người xem không phải HR / Admin → `AccessDeniedException` → 403.
> Danh sách cũng tự ẩn tin nháp (`JobPostingSpecifications#notDraft`), sửa URL `?status=Draft` cũng bị bỏ qua.

---

## 2. Vòng đời trạng thái (cột `JobPosting.PostingStatus`)

```
 (mới) ──Lưu nháp──► Draft ──Đăng tin──────────────────────────► Published ──Tạm dừng──► Paused
                       │                                            ▲   │                  │
                       └──Lên lịch đăng──► "Hẹn đăng" ──Đăng ngay───┘   │   ◄──Mở lại──────┘
                                           (Published,  ──Hủy lịch──► Draft
                                            PostingDate tương lai)      │
                                                                        └──Đóng tin (HR)──► Closed ──Đăng lại──► tin MỚI (PostingNo + 1)
```

- **"Hẹn đăng" không phải trạng thái trong DB**: là `Published` có `PostingDate` ở tương lai (`JobPostingLabels#viewStatus`).
  Không cần đổi CHECK của DB; tới giờ đăng tin tự hiện vì mọi truy vấn trang công khai có điều kiện `PostingDate <= hiện tại`.
- Mỗi yêu cầu chỉ có **1 tin đang mở** (Draft / Published / Paused) cùng lúc (`requireNoOtherOpenPosting`).
- Tin đã đóng không mở lại được; muốn tuyển tiếp thì **Đăng lại** = tạo tin mới cùng yêu cầu, nội dung lấy từ tin cũ (D10).

---

## 3. Màn DANH SÁCH (`job-postings/list.html`)

### 3.1 Thanh công cụ

| Thành phần | Chạy như thế nào |
|---|---|
| Ô tìm kiếm | `?q=` → LIKE trên **tiêu đề tin** hoặc **tên phòng ban** (escape `% _ [`, tối đa 120 ký tự) |
| Trạng thái (nhiều lựa chọn) | `?status=` gồm Bản nháp, **Hẹn đăng**, Đang đăng, Tạm dừng, Đã đóng (HM / Director không có Bản nháp). Hẹn đăng = `Published AND PostingDate > now`; Đang đăng = `Published AND PostingDate <= now` |
| Phòng ban | HR, Director, Admin |
| Hạn nộp | HR, HM, Director: còn hạn (> 7 ngày) / sắp hết (0–7 ngày) / đã quá hạn |
| Người tạo, Ngày đăng (từ–đến) | Chỉ Admin |
| Sắp xếp | Mới đăng nhất, Hạn nộp gần nhất, **Nhiều hồ sơ nhất** (ORDER BY số hồ sơ bằng subquery, `JobPostingSpecifications#orderByApplicationCount`), Tiêu đề A–Z |
| Chip ✕ | Bỏ riêng một bộ lọc, giữ các tham số khác (`RequisitionListLinks#without`) |
| Dải "N yêu cầu đã duyệt chưa có tin" | HR / Admin: yêu cầu Approved chưa có tin đang mở; bấm để mở form tạo tin |

Giá trị lạ trên URL (vai trò không có ô đó, giá trị ngoài danh sách) bị bỏ qua (`normalizeFilter`), không báo lỗi.

### 3.2 Các cột

| Cột | Lấy từ đâu / tính thế nào |
|---|---|
| Tiêu đề tin | `JobPosting.PostingTitle`; dòng phụ "Lần N · Hình thức" = `PostingNo` + `JobRequisition.EmploymentType` |
| Phòng ban | `Department.DepartmentName` qua yêu cầu nguồn (ẩn với HM) |
| Hồ sơ | Đếm `Application` theo tin; **"+N mới"** = hồ sơ `Applied` (chưa ai xử lý, D9). Cả trang đếm bằng **1 câu** `countApplicationsByPosting` (không N+1) |
| Ngày đăng | `PostingDate`; tin hẹn đăng có "sau N ngày"; nháp hiện "Chưa đăng" |
| Hạn nộp | `ApplicationDeadline`; "còn N ngày" / "quá hạn N ngày"; vàng khi ≤ 7 ngày, đỏ khi quá hạn; tin đã đóng không tô màu |
| Trạng thái | `JobPostingLabels#statusLabel` / `#statusTone` |
| Người tạo | `JobPosting.CreatedBy` (chỉ HR, Admin thấy) |
| Menu ⋯ | Theo cờ do service tính (`toListRow`): Đăng tin, Mở lại, Xem ứng viên, Xem chi tiết, Chỉnh sửa, Tạm dừng, Đóng tin (HR), Xóa bản nháp (nháp chưa có hồ sơ) |

---

## 4. Màn TẠO / SỬA TIN (`job-postings/create.html`)

### 4.1 Thẻ yêu cầu nguồn (chỉ đọc)

Mã, phòng ban, số lượng, ngày bắt đầu, "Tin lần N" (= lần đăng lớn nhất + 1). "Xem thêm thông tin yêu cầu":
hình thức, mô hình, giới tính, thử việc, mức lương, HM, **Lý do tuyển dụng** (nội bộ, không đưa vào tin công khai, rule §24.2).
**Đổi yêu cầu** (chỉ khi tạo mới): danh sách yêu cầu Approved chưa có tin đang mở, chọn xong mở lại form với yêu cầu đó.

Thông tin nguồn **luôn lấy lại từ DB** (`restoreSourceInfo` khi form lỗi). Form chỉ nhận các trường trong
whitelist (`JobPostingController#formBinder`); gửi thêm `postingStatus`, `minSalary`… bị bỏ qua.

### 4.2 Các trường

| Trường | Cột DB | Quy tắc (`JobPostingValidator#validateForm`) |
|---|---|---|
| Tiêu đề tin * | `PostingTitle` NVARCHAR(200) NOT NULL | Bắt buộc cả khi nháp, ≤ 200. Có "Lấy từ yêu cầu / Khôi phục bản gốc" |
| Địa điểm | `WorkLocation` NVARCHAR(255) | ≤ 255. Bản đồ Google Maps nhúng + link "Mở Google Maps" (D3) |
| Mức lương hiển thị | `SalaryDisplay` NVARCHAR(100) | 3 lựa chọn: **Theo yêu cầu** (server tính lại từ `MinSalary/MaxSalary` của yêu cầu, không tin số gửi lên), **Thỏa thuận**, **Tự nhập** (bắt buộc nội dung, ≤ 100) |
| Hiển thị số lượng | `NumberOfPositionsDisplay` NVARCHAR(50) | Bật thì bắt buộc nội dung; tắt thì lưu **NULL** (trang công khai không hiện số lượng) |
| Mô tả công việc * / Yêu cầu ứng viên * | NVARCHAR(MAX) NOT NULL | Bắt buộc cả khi nháp, ≤ 5000 (giới hạn ở ứng dụng). Điền sẵn từ yêu cầu (Yêu cầu = chi tiết + tiêu chí bắt buộc) |
| Quyền lợi | `Benefits` NVARCHAR(MAX) | ≤ 5000. Nút thêm nhanh (Lương tháng 13, BHXH…) |
| Ngày đăng | `PostingDate` | **Đăng ngay** hoặc **Hẹn ngày đăng** (D1): > hiện tại, ≤ 90 ngày, trước hạn nộp. Chỉ khi tin chưa lên trang tuyển dụng. Lưu nháp không giữ lịch |
| Hạn nộp * | `ApplicationDeadline` (lưu 23:59:59) | **Bắt buộc khi đăng** (D6), ≥ hôm nay, sau ngày đăng (`CK_JobPosting_Deadline`). Nút +14/+30/+45 ngày; cảnh báo (không chặn) nếu sau ngày bắt đầu của yêu cầu |

Mọi chuỗi trim; ô nhiều dòng đổi `\r\n` → `\n` trước khi đếm. Lỗi trả về **theo từng ô** (`JobPostingValidationException#getFieldErrors`),
form giữ nguyên dữ liệu đã nhập. JS (`pages/job-posting-form.js`) kiểm tra giống server để báo sớm; nút bị khóa khi đang gửi.

### 4.3 Các nút

| Tin đang là | Nút | Kết quả |
|---|---|---|
| Mới / Nháp | **Lưu nháp** | `Draft`, `PostingDate = NULL` |
| Mới / Nháp | **Đăng tin** | `Published`, `PostingDate = now`, báo HM |
| Mới / Nháp | **Lên lịch đăng** (khi chọn hẹn ngày) | `Published`, `PostingDate = thời điểm hẹn`, báo HM "hẹn đăng lúc …" |
| Hẹn đăng | **Lưu thay đổi** | Áp lại lịch (chọn "Đăng ngay" thì đăng luôn) |
| Đang đăng | **Lưu thay đổi** | Giữ ngày đăng, chỉ đổi nội dung, không báo lại |
| Tạm dừng | **Lưu thay đổi** / **Lưu và mở lại** | Giữ tạm dừng / `Published`, `PostingDate = now` |

Lưu xong chuyển sang trang chi tiết tin, có thông báo. Mọi lần lưu ghi `AuditLog`.

---

## 5. Màn CHI TIẾT (`job-postings/detail.html`)

### 5.1 Dữ liệu

| Phần | Lấy từ đâu / tính thế nào |
|---|---|
| Tiêu đề, "Lần N" | `PostingTitle`, `PostingNo` |
| Dải thông báo | Theo trạng thái hiển thị: nháp / hẹn đăng (giờ đăng) / tạm dừng / đã đóng (`ClosedAt`) / còn ≤ 7 ngày / quá hạn |
| Tổng hồ sơ | `COUNT(Application)` của tin; "+N trong 7 ngày" theo `SubmissionDate` |
| Chờ xử lý | Hồ sơ `Applied` + `AI_Screened` |
| Đạt ngưỡng AI | Hồ sơ có điểm `AIScreeningResult.AIMatchScore` (lần chấm mới nhất) ≥ `SystemConfig.AI_SCORE_THRESHOLD` (mặc định 70) |
| Đang phỏng vấn / Đã tuyển | Hồ sơ `Interviewing` / `Hired`; "/ N" = `JobRequisition.NumberOfPositions` |
| Tab Ứng viên | 7 hồ sơ mới nhất (tên ứng viên, điểm AI mới nhất, trạng thái, ngày nộp) + "Xem tất cả" → `/applications?jobPostingId=` |
| Các lần đăng | Mọi `JobPosting` cùng `RequisitionId`, kèm số hồ sơ và số `Hired` (2 câu gộp) |
| Lịch sử | `AuditLog` có `EntityName = 'JobPosting'` |

Số liệu chỉ có khi tin đã lên trang tuyển dụng (đang đăng / tạm dừng / đã đóng).

### 5.2 Nút ở đầu trang

| Trạng thái | Nút (HR / Admin) |
|---|---|
| Nháp | Đăng tin · Chỉnh sửa · Xóa bản nháp (chưa có hồ sơ) |
| Hẹn đăng | Đăng ngay · Chỉnh sửa · Hủy lịch đăng |
| Đang đăng | Xem trên trang tuyển dụng ↗ · Chỉnh sửa · Tạm dừng nhận hồ sơ · Đóng tin (chỉ HR) |
| Tạm dừng | Mở lại · Chỉnh sửa · Đóng tin (chỉ HR) |
| Đã đóng | Đăng lại (tạo tin lần N+1) — khi yêu cầu còn Approved và chưa có tin đang mở |

HM, Director: chỉ "Xem trên trang tuyển dụng" (nếu đang đăng). HR, HM có thêm mục "Ứng viên tiềm năng" (module talent pool).

---

## 6. Câu hỏi GV hay hỏi

**Hẹn đăng hoạt động thế nào, có job chạy nền không?**
Không cần. Tin hẹn đăng đã là `Published` với `PostingDate` ở tương lai. Trang công khai (`findPublishedJobs`, `findOpenPostings`,
`CareerPortalService`, `ApplicationSubmissionService`) chỉ lấy tin có `PostingDate <= hiện tại` (`JobPosting#isVisibleToCandidates`),
nên đúng giờ là tin tự hiện; trước đó ứng viên mở link sẽ gặp 404 và không nộp được hồ sơ.

**Đếm hồ sơ cho cả bảng có chậm không?**
Một câu `GROUP BY JobPostingId` cho cả trang (10 dòng), không phải mỗi dòng một câu.

**Hai HR cùng tạo tin cho một yêu cầu?**
Service khóa dòng `JobRequisition` (`findForUpdate`) trước khi kiểm tra "đã có tin đang mở" và tính `PostingNo`,
nên người sau phải chờ và sẽ bị báo "Yêu cầu này đã có tin nháp hoặc tin đang hoạt động."

**Sao "Theo yêu cầu" không dùng số lương trên form?**
Số lương nằm ở yêu cầu đã duyệt; form không gửi số này (whitelist), service đọc từ DB và định dạng bằng `RequisitionLabels#salary`.

**Xóa tin được không?**
Chỉ tin nháp chưa có hồ sơ. Tin đã đăng thì tạm dừng hoặc đóng, để giữ lịch sử hồ sơ ứng tuyển.

---

## 7. Hạn chế đã biết

- Chưa có **ảnh nơi làm việc** (mockup có; cần bảng mới + migration, để sau theo D2).
- Bản đồ nhúng cần mạng; không có mạng thì dùng link "Mở Google Maps".
- Phần nội dung tin chưa tách fragment dùng chung với trang chi tiết công khai (rule §25), cần phối hợp DungLT.
- Lịch sử chỉ có từ khi tin được tạo qua hệ thống; tin trong seed không có `AuditLog` sẽ chỉ hiện "Tạo mới".
