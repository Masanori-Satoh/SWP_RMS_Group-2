# Cẩm nang review: Yêu cầu tuyển dụng (Requisition)

> Ghi chú cá nhân của HoangNH để trả lời GV khi review 3 màn: **Danh sách**, **Form tạo/sửa**, **Chi tiết**.
> Mỗi nút: bấm vào thì chạy qua đâu, kiểm tra gì, ghi bảng nào. Mỗi trường: lấy từ cột nào hoặc tính ra sao.
> Code được tham chiếu theo `Class#method` (không ghi số dòng vì số dòng thay đổi khi sửa code).
> Cập nhật 02/11/2026: đã bỏ tiêu chí sàng lọc và ngày bắt đầu dự kiến (#135); Giám đốc tạo phiếu thì phê duyệt trực tiếp.

---

## 0. Bản đồ nhanh

```
Trình duyệt ──► SecurityConfig (lớp 1: có được vào URL không?)
            ──► RequisitionController (nhận tham số, gọi service, chọn view, đổi lỗi quyền thành toast)
            ──► RequisitionListService (màn danh sách: phạm vi theo role, bộ lọc, chip, phân trang)
                RequisitionServiceImpl (một phiếu: tạo / sửa / xóa / duyệt / sao chép; transaction)
                  ├─ RequisitionAccess      : ai được làm gì (role, chủ sở hữu, trạng thái), canX / requireX
                  ├─ RequisitionValidator   : dữ liệu nhập có hợp lệ không
                  ├─ RequisitionMapper      : entity → DTO, gắn cờ nút (editable, decidable...)
                  ├─ RequisitionAuditLogger : ghi AuditLog, ảnh chụp trước / sau
                  ├─ RequisitionLabels      : dịch giá trị DB sang tiếng Việt (nơi duy nhất)
                  ├─ RequisitionSpecifications : các điều kiện WHERE của màn danh sách
                  ├─ NotificationService    : tạo thông báo cho Director / HM / HR
                  └─ Repository ──► SQL Server
            ──► Template Thymeleaf (chỉ hiển thị, không tự tính, không tự so sánh role)
```

| URL | Controller | Service | Bảng bị ghi |
|---|---|---|---|
| `GET /requisitions` | `list` | `RequisitionListService#getListPage` | (chỉ đọc) |
| `GET /requisitions/create` | `createForm` | (không) | |
| `POST /requisitions/create` | `create` | `createRequisition` | `JobRequisition`, `AuditLog`, `Notification` (khi gửi duyệt); Director gửi thì thêm `RequisitionApproval` |
| `GET /requisitions/{id}` | `detail` | `getById` | (chỉ đọc) |
| `GET /requisitions/edit/{id}` | `edit` | `prepareEditForm` | |
| `POST /requisitions/edit/{id}` | `update` | `updateRequisition` | `JobRequisition`, `AuditLog`, `Notification` (khi gửi duyệt); Director gửi thì thêm `RequisitionApproval` |
| `GET /requisitions/copy/{id}` | `copy` | `copy` | (không ghi gì, chỉ điền sẵn form) |
| `POST /requisitions/{id}/decision` | `decide` | `decide` | `JobRequisition`, `RequisitionApproval`, `AuditLog`, `Notification` |
| `POST /requisitions/delete/{id}` | `delete` | `deleteRequisition` | xóa `JobRequisition` (+ lịch sử duyệt), ghi `AuditLog` |

Mọi `POST` đều cần CSRF token (Spring Security tự chèn `_csrf` vào form Thymeleaf). Thiếu token thì bị 403.

**Mã trên URL gõ tay** (`/requisitions/{2796}`, `/requisitions/abc`): `parseId` lấy phần số; không có số thì báo "Mã yêu cầu tuyển dụng không hợp lệ." và quay về trang vừa xem, không ra lỗi 500.

---

## 1. Phân quyền: hai lớp bảo vệ

**Lớp 1, theo URL** (`core/config/SecurityConfig`): `/requisitions/**` chỉ cho `HIRING_MANAGER`, `DIRECTOR`, `HR`. Candidate, Interviewer, System Admin gõ URL thì bị chặn trước khi vào controller.

**Lớp 2, theo dữ liệu** (`RequisitionListService#scope` cho danh sách, `RequisitionAccess` cho từng phiếu):

| Role | Thấy những yêu cầu nào (`scope` / `requireView`) | Sửa / xóa (`canEdit`) | Duyệt (`canDecide`) | Sao chép (`canCopy`) |
|---|---|---|---|---|
| Hiring Manager | Phiếu **mình tạo**, hoặc thuộc **phòng mình / phòng mình làm trưởng** | Của mình, khi **Draft / Rejected** | Không | Phiếu **Approved** |
| Director | Mọi phiếu **trừ bản nháp của người khác** (thấy nháp do mình tạo) | Của mình, khi Draft / Rejected | Khi **Pending_Director** và **không phải người tạo** | Phiếu Approved |
| HR | Chỉ **Approved** | Không | Không | Không |

**Khi không đủ quyền** (`RequisitionController#whenAccessDenied`): không hiện trang 403 nữa, mà quay về trang danh sách / chi tiết vừa xem kèm toast lỗi (trang vừa xem lưu trong session theo đúng tài khoản, `rememberPage`). Nếu chính trang danh sách bị chặn thì về `/dashboard` để tránh chuyển trang vòng lặp.

**Đúng người nhưng sai trạng thái** (ví dụ mở `/edit/{id}` của phiếu đã Approved, hoặc bấm Duyệt trên tab cũ khi phiếu đã được quyết định): `requireEdit` / `requireDecide` / `requireCopy` ném `RequisitionStateException`, controller đưa về trang vừa xem kèm toast (`backToPreviousPage`).

> GV hỏi: *"Nếu HR sửa URL thành `?status=Draft` thì sao?"*
> Trả lời: HR không có ô lọc trạng thái nên `normalizeFilter` bỏ luôn tham số `status`. Kể cả nếu còn, bộ lọc luôn được **nối AND** vào `scope` (`ApprovalStatus = 'Approved'`) nên ra 0 dòng.

> GV hỏi: *"Ẩn nút trên giao diện có đủ bảo mật không?"*
> Trả lời: không. Nút chỉ ẩn để dễ dùng. Mỗi thao tác ở server đều kiểm tra lại bằng `requireX` trong service.

---

## 2. Vòng đời trạng thái (cột `JobRequisition.ApprovalStatus`)

```
            Lưu nháp                Gửi duyệt (HM)
 (mới) ───────────────► Draft ─────────────────► Pending_Director
                           ▲                           │      │
                           │                  Phê duyệt │      │ Trả lại (bắt buộc lý do)
                          │                            ▼      ▼
                          │    Director tự gửi ──►  Approved   Rejected
                          └─── Lưu nháp sau khi sửa ──────────┘
                                 (Sửa & gửi lại → Pending_Director)
```

- **Director tạo / sửa phiếu của mình và bấm "Phê duyệt trực tiếp"** → `Approved` luôn (`applySavedStatus`), service tự ghi một bản `RequisitionApproval` với ý kiến "Giám đốc tạo và phê duyệt trực tiếp." và báo HR (`recordDirectApprovalIfNeeded`). Lý do: không có cấp nào trên Giám đốc để duyệt, và Giám đốc không được tự duyệt qua nút Duyệt (`canDecide` loại người tạo).
- Không có đường Approved → Draft (rule NHH §14). Approved muốn tuyển tiếp thì **Sao chép thành đợt mới**.
- DB còn 2 trạng thái `Closed`, `Cancelled` nhưng hiện **chưa có chức năng nào dùng**.

---

## 3. Màn DANH SÁCH (`/requisitions`, `requisitions/list.html`)

### 3.1 Thanh công cụ

| Thành phần | Bấm/nhập thì chạy như thế nào |
|---|---|
| **Ô tìm kiếm** | Form `GET`: Enter → `?q=...`. Tìm `LIKE` trên **tên vị trí** (`JobRequisition.Title`) **hoặc tên phòng ban** (`Department.DepartmentName`), không phân biệt hoa thường. Ký tự `% _ [` được escape. Cắt tối đa 120 ký tự |
| **Nút "Bộ lọc"** | Thẻ `<details>` tự mở/đóng, không cần JS. Số trên nút = số bộ lọc đang bật (không tính từ khóa, sắp xếp) |
| **Ô lọc theo role** | Do `RequisitionListService#buildFilterOptions` quyết định: HM (Trạng thái, Tin tuyển dụng, Ngày tạo); Director (Trạng thái có cả Bản nháp của mình, Phòng ban, Người tạo); HR (Tin tuyển dụng, Phòng ban, Người tạo). Mọi role có Sắp xếp |
| **Trạng thái** | Radio **một lựa chọn** ("Tất cả" hoặc 1 trạng thái) → `?status=X` → `ApprovalStatus = X`. Server cũng chỉ giữ 1 giá trị (`.limit(1)`) |
| **Tin tuyển dụng** | `none` → `NOT EXISTS` trong `JobPosting`; `Published/Paused/Closed` → trạng thái của **tin mới nhất** (`MAX(JobPostingId)`) bằng giá trị đó (`RequisitionSpecifications#latestPostingStatusIs`) |
| **Phòng ban / Người tạo** | `DepartmentId = ?` / `HiringManagerId = ?`. Danh sách người tạo chỉ gồm người có phiếu trong phạm vi được xem (Director: bỏ người chỉ có bản nháp) |
| **Ngày tạo từ – đến** | `CreatedAt >= từ 00:00` và `CreatedAt < (đến + 1 ngày) 00:00`, nên tính trọn cả ngày "đến" |
| **Sắp xếp** | Enum `RequisitionSort` (whitelist): Mới nhất, Cũ nhất, Tên A–Z... Giá trị lạ → Mới nhất |
| **Giá trị lạ trên URL** | Role không có ô đó, hoặc giá trị không nằm trong danh sách lựa chọn → `normalizeFilter` bỏ qua, không báo lỗi |
| **Mặc định khi vào trang** | Không role nào bị lọc sẵn trạng thái: thấy tất cả phiếu trong phạm vi. Director muốn xem phiếu chờ duyệt thì chọn "Chờ duyệt" |
| **Chip ✕** | Link do `FilterLinks#without` tạo: giữ các tham số khác, bỏ tham số của chip, bỏ `page` |
| **"Xóa bộ lọc" / "Đặt lại mặc định"** | Cùng về `/requisitions` (không tham số) |
| **"+ Tạo yêu cầu mới"** | Chỉ hiện khi `canCreate` (HM, Director) → `GET /requisitions/create` |

### 3.2 Các cột của bảng

| Cột | Lấy từ đâu / tính thế nào |
|---|---|
| STT | `page.number * page.size + thứ tự dòng` (trang 2 bắt đầu từ 11) |
| Vị trí tuyển dụng | `JobRequisition.Title` (link sang chi tiết) |
| Phòng ban | `Department.DepartmentName` qua `JobRequisition.DepartmentId` |
| Đợt tuyển dụng | `JobRequisition.RecruitmentRound` |
| Số lượng | `JobRequisition.NumberOfPositions` |
| Hình thức | `EmploymentType` dịch bằng `RequisitionLabels#employmentType` |
| Trạng thái | `ApprovalStatus` → nhãn `RequisitionLabels#approvalStatus`, màu `#approvalTone` (Đã duyệt xanh, Chờ duyệt vàng, Từ chối đỏ, Nháp xám) |
| Tin tuyển dụng | Tin **mới nhất** của mỗi phiếu, lấy **gộp cho cả trang** (`RequisitionListService#latestPostingsOf`) → `RequisitionLabels#postingStatus`. Thành link khi `postingViewable` (tin nháp chỉ HR mở được) |
| Ngày tạo | `JobRequisition.CreatedAt` (tự đặt ở `BaseEntity#onCreate`) |
| Menu ⋯ | Mỗi mục hiện theo cờ do `RequisitionMapper` tính từ `RequisitionAccess`: `editable`, `copyable`, `postable`, `postingViewable`, `deletable` |

### 3.3 Phân trang

Fragment catalog `fragments/ui/pagination :: paged`: **giữ mọi tham số đang có**, chỉ đổi `page` (đếm từ 0). Gõ `?page=99` → tự lùi về trang cuối. Kích thước trang 1–50 (mặc định 10).

---

## 4. Màn FORM (`/requisitions/create`, `/edit/{id}`, `/copy/{id}`; `requisitions/form.html`)

### 4.1 Các trường và quy tắc kiểm tra (`RequisitionValidator#validate`)

| Trường | Cột DB & Kiểu | Lưu nháp | Gửi duyệt | Quy tắc & hành vi giao diện |
|---|---|---|---|---|
| **Tên vị trí** | `Title` NVARCHAR(200) NOT NULL | **Bắt buộc** | **Bắt buộc** | Cắt khoảng trắng 2 đầu; tối đa 200. Gửi duyệt không được để tiêu đề mặc định `"Untitled requisition"` |
| **Phòng ban** | `DepartmentId` INT NOT NULL | (xem bên phải) | | **HM**: không lấy từ form, dùng phòng ban của tài khoản (phải `Active`, không có thì báo "Tài khoản của bạn chưa được gán phòng ban đang hoạt động..."); sửa thì giữ phòng ban đã lưu. **Director**: chọn trên form (ô select chỉ hiện khi `directApprove`), service kiểm tra phòng ban tồn tại và `Active` (`requireActiveDepartment`) |
| **Đợt tuyển dụng** | `RecruitmentRound` INT NOT NULL | (tự động) | (tự động) | Tạo mới = 1; sao chép = đợt lớn nhất trong chuỗi + 1 (`assignRecruitmentRound`, khóa dòng phiếu gốc tránh trùng đợt). Bản sao đổi tên vị trí hoặc phòng ban thì thành phiếu mới, đợt 1 |
| **Số lượng cần tuyển** | `NumberOfPositions` INT NOT NULL | **Bắt buộc** | **Bắt buộc** | Số nguyên **1–30** (`POSITIONS_MIN`/`POSITIONS_MAX`). Nút −/+ dừng ở 1 và 30 |
| **Loại tuyển dụng** | `HiringType` NVARCHAR(20) | Không | Không | Tuyển mới (`New`) / Tuyển thay thế (`Replacement`); không chọn = `NULL` |
| **Hình thức làm việc** | `EmploymentType` NVARCHAR(50) NOT NULL | **Bắt buộc** | **Bắt buộc** | `Full-time`, `Part-time`, `Internship`, `Contract` |
| **Mô hình làm việc** | `WorkModel` NVARCHAR(20) NOT NULL | **Bắt buộc** | **Bắt buộc** | `On-site`, `Hybrid`, `Remote` |
| **Yêu cầu giới tính** | `RequiredGender` NVARCHAR(20) NOT NULL | Không | Không | `Any` (mặc định), `Male`, `Female`. Để trống tự quy về `Any` |
| **Địa điểm làm việc** | `WorkLocation` NVARCHAR(255) | Không | **Bắt buộc** | Tối đa 255 |
| **Thời gian thử việc** | `ProbationDays` INT | Không | Không | Để trống (không thử việc) hoặc **1–180 ngày** (Điều 25 BLLĐ 2019). Ô số + chữ "ngày" + nút −/+ |
| **Lương tối thiểu / tối đa** | `MinSalary` / `MaxSalary` DECIMAL(18,2) | Không | Không | Để trống = thỏa thuận. Nếu nhập: **> 0**, tối đa 2 chữ số thập phân, **≤ 1.000.000.000** (`SALARY_MAX`), **tối đa ≥ tối thiểu**. Tự định dạng `20.000.000`; controller có `PropertyEditorSupport` đổi `20.000.000` ↔ `BigDecimal` |
| **Lý do tuyển dụng** | `ReasonForHiring` NVARCHAR(2000) | Không | **Bắt buộc** | Tối đa 2000 (đổi `\r\n` → `\n` trước khi đếm, `cleanMultiline`). Có bộ đếm `234/2000` |
| **Mô tả công việc** | `JobDescription` NVARCHAR(MAX) NOT NULL | Không | **Bắt buộc** | Tối đa 2000. Có bộ đếm |
| **Yêu cầu ứng viên** | `RequirementDetails` NVARCHAR(MAX) NOT NULL | Không | **Bắt buộc** | Tối đa 2000. Có bộ đếm |

> Đã bỏ: **Tiêu chí sàng lọc** (bảng `ScreeningCriteria`) và **Ngày bắt đầu dự kiến** (PR #135, gỡ AI CV screening).

> **Vì sao bắt buộc một số trường ngay từ lúc Lưu nháp?**
> Các cột `Title`, `NumberOfPositions`, `EmploymentType`, `WorkModel` là `NOT NULL` trong DB. Không kiểm tra trước thì INSERT ném `DataIntegrityViolationException`, lỗi DB lộ ra cho người dùng.

### 4.2 Chống vỡ giao diện khi dữ liệu dài

- Ô nhập, textarea, select có `max-width: 100%`, `box-sizing: border-box`, `min-width: 0`; textarea và `.field-error` có `overflow-wrap: anywhere` nên chuỗi dài không khoảng trắng vẫn tự xuống dòng.
- Bố cục 2 cột: trái `.rqf-main` (các nhóm trường), phải `.rqf-rail` (sticky: "Sẵn sàng gửi duyệt" + nút). Màn hẹp xếp 1 cột, thanh nút ghim đáy.

### 4.3 Nút "Lưu bản nháp"

1. JS (`pages/requisition-form.js#validateAll('draft')`) kiểm tra các trường bắt buộc khi nháp + định dạng số (số lượng 1–30, thử việc 1–180 nếu có, lương 1–1 tỷ và max ≥ min). Sai → lỗi đỏ cạnh ô, không gửi.
2. Form `POST` với `action=draft` → `RequisitionController#create` (hoặc `#update`).
3. `@InitBinder("form")` chỉ cho bind các trường trong whitelist (`approvalStatus`, `hiringManager`, `createdBy`... không có trong danh sách nên gửi lên cũng bị bỏ qua).
4. `RequisitionServiceImpl#createRequisition`:
   - `access.requireCreate`: chỉ HM, Director.
   - Xác định phòng ban (HM: theo tài khoản; Director: theo ô đã chọn).
   - `RequisitionValidator#validate` (kiểm tra lại toàn bộ, không tin JS).
   - Chặn trùng: đã có **bản nháp cùng tên vị trí trong cùng phòng ban** thì báo lỗi (`requireNoOtherDraft`).
   - Tạo `JobRequisition` (`HiringManagerId` = `CreatedBy` = người đang đăng nhập), gán đợt, ghi các trường.
   - `ApprovalStatus = 'Draft'` → `saveAndFlush` → sinh **mã `REQ-0025`** từ id → ghi `AuditLog` (`CREATE`).
5. Thành công → redirect `/requisitions/{id}` + "Đã lưu bản nháp thành công.". Lỗi → render lại form, dữ liệu còn nguyên, lỗi hiện cạnh ô.

### 4.4 Nút "Gửi Giám đốc duyệt" (HM) / "Phê duyệt trực tiếp" (Director)

1. Bấm nút **chưa gửi ngay**: JS kiểm tra đủ mọi trường dấu `*`. Thiếu → lỗi cạnh ô, cuộn tới lỗi đầu tiên.
2. Đủ → hiện **hộp xác nhận** trong khung bên phải. Bấm "Gửi duyệt" / "Phê duyệt" mới thật sự `POST action=submit`. Sau khi gửi, các nút bị khóa để bấm 2 lần không tạo 2 phiếu.
3. Service giống lưu nháp, khác ở: validator áp quy tắc gửi duyệt; `SubmittedAt = now`; trạng thái:
   - **HM** → `Pending_Director`, tạo `Notification` cho **mọi Director** (`notifyRequisitionPendingDirector`). Thông báo: "Yêu cầu đã được gửi lên Giám đốc phê duyệt."
   - **Director** → `Approved`, thêm `RequisitionApproval`, báo **HR** (`notifyRequisitionApproved`). Thông báo: "Yêu cầu đã được phê duyệt. HR có thể tạo tin tuyển dụng ngay."

### 4.5 Các thành phần khác của form

| Thành phần | Hoạt động |
|---|---|
| **Bộ đếm ký tự** | Lý do, Mô tả, Yêu cầu: số ban đầu do Thymeleaf tính, JS cập nhật khi gõ / dán |
| **Định dạng lương** | Tự chèn dấu chấm hàng nghìn khi gõ, giữ vị trí con trỏ; nạp lại đúng định dạng khi sửa / sao chép |
| **Ô thử việc** | Ô số + "ngày" + nút −/+ (dùng chung code stepper với ô Số lượng, giới hạn từ `min`/`max`). Ô trống bấm nút thì điền sẵn 60 ngày |
| **Cột "Sẵn sàng gửi duyệt"** | 8 mục bắt buộc khi gửi (Director thêm mục Phòng ban), tự đánh dấu khi ô hợp lệ; bấm mục → cuộn tới ô đó |
| **"Hủy"** | Tạo mới → về danh sách; sửa → về trang chi tiết. Không lưu gì |
| **Sao chép (`/copy/{id}`)** | Chỉ phiếu **Approved** (`canCopy`). Form điền sẵn, có thông báo "Biểu mẫu chưa được lưu...", **chưa ghi DB** cho tới khi bấm Lưu / Gửi. Bỏ mã, trạng thái, lịch sử duyệt (NHH §23.4) |

---

## 5. Màn CHI TIẾT (`/requisitions/{id}`, `requisitions/detail.html`)

### 5.1 Dữ liệu hiển thị

| Phần | Lấy từ đâu / tính thế nào |
|---|---|
| Mã yêu cầu | `RequisitionCode` (sinh lúc tạo: `"REQ-" + id` đệm 4 chữ số) |
| Banner "đã chờ N ngày" | `RequisitionMapper`: số ngày từ `SubmittedAt` tới hôm nay (chỉ khi Chờ duyệt) |
| Banner Từ chối | `rejectionReason` = `Comments` của bản ghi `RequisitionApproval` mới nhất |
| Thanh tiến trình 4 bước | Tạo nháp = `CreatedAt`; Gửi duyệt = `SubmittedAt`; Giám đốc duyệt / Bị trả lại = `ApprovalDate` mới nhất; Đăng tin = `PostingDate` của tin mới nhất |
| Thông tin cơ bản | Số lượng, loại tuyển, người tạo, hình thức, mô hình, giới tính, thử việc, mức lương, ngày tạo, địa điểm (trống → "Chưa xác định") |
| Mức lương | `RequisitionLabels#salary`: "20 – 30 triệu VND/tháng" hoặc "Thỏa thuận" |
| Thử việc | `ProbationDays` + " ngày" hoặc "Không thử việc" |
| Lý do / Mô tả / Yêu cầu | Service **escape HTML** rồi đổi xuống dòng thành `<br/>` (`RequisitionLabels#multilineHtml`), template dùng `th:utext`. Escape để chống XSS |
| Khối Tin tuyển dụng | Tin mới nhất: `PostingStatus`, `PostingNo`, `PostingDate`, `ApplicationDeadline` |
| Ý kiến của Giám đốc | Bảng `RequisitionApproval`, mới nhất trước |
| Lịch sử | Bảng `AuditLog` với `EntityName = 'JobRequisition'` và `EntityId = id`, mới nhất trước |

### 5.2 Các nút

| Nút | Hiện khi (cờ) | Bấm thì |
|---|---|---|
| **← Quay về danh sách** | Luôn hiện | `GET /requisitions` |
| **Chỉnh sửa / Sửa & gửi lại** | `editable`: người tạo, Draft hoặc Rejected | `GET /edit/{id}` → form |
| **Xóa** | `deletable`: sửa được, **chưa có tin tuyển dụng, chưa có đợt sau sao chép từ nó** | Hộp xác nhận → `POST /delete/{id}` → `requireEdit`, chặn nếu đã có `JobPosting` hoặc đã là phiếu gốc của đợt sau, ghi `AuditLog` (`DELETE`), xóa `RequisitionApproval`, xóa phiếu |
| **Sao chép thành đợt mới** | `copyable`: HM / Director, phiếu Approved | `GET /copy/{id}` (mục 4.5) |
| **Tạo tin tuyển dụng** | `postable`: HR, Approved, chưa có tin | `GET /internal/job-postings/create?requisitionId=...` |
| **Xem tin tuyển dụng** | `postingViewable`: đã có tin (tin nháp chỉ HR) | `GET /internal/job-postings/{id}` |
| **Phê duyệt / Trả lại** | `decidable`: Director, không phải người tạo, Pending_Director | `POST /{id}/decision` (`decision=approve|reject`) → khóa dòng, `requireDecide`; **Trả lại bắt buộc ý kiến** (JS + server), tối đa 1000 ký tự; đổi trạng thái; thêm `RequisitionApproval`; ghi `AuditLog`; **Phê duyệt báo HM + mọi HR, Trả lại báo HM**. Lỗi ý kiến thì giữ lại nội dung đã gõ (`decisionComment`) |

---

## 6. Câu hỏi GV hay hỏi

**Vì sao service dùng `findForUpdate` (khóa dòng)?**
Hai Giám đốc cùng quyết định trên một phiếu. `PESSIMISTIC_WRITE` khóa dòng trong transaction, người sau phải chờ. Sau đó người sau kiểm tra lại trạng thái, bị `RequisitionStateException` → về trang vừa xem kèm toast.

**Dữ liệu ngày tạo, người tạo lấy từ client à?**
Không. `CreatedAt` do `BaseEntity#onCreate` đặt; `HiringManagerId`, `CreatedBy` lấy từ người đang đăng nhập (`RequisitionAccess#actor`); phòng ban của HM lấy từ tài khoản. `@InitBinder` không cho bind `approvalStatus`.

**Giám đốc tạo phiếu thì ai duyệt?**
Không ai. Bấm "Phê duyệt trực tiếp" là `Approved` luôn, có bản ghi `RequisitionApproval` để vẫn thấy ai duyệt, lúc nào. Giám đốc không thể dùng nút Duyệt cho phiếu của chính mình (`canDecide` yêu cầu không phải người tạo).

**JS kiểm tra rồi sao server còn kiểm tra?**
JS chỉ để báo lỗi sớm; người dùng có thể tắt JS hoặc gửi bằng Postman. `RequisitionValidator` mới quyết định. Quy tắc JS viết giống server.

**Tại sao lưu nháp lại bắt buộc vài trường?**
Xem cuối mục 4.1: các cột đó `NOT NULL` trong DB.

**Mã yêu cầu sinh thế nào, có trùng không?**
Sinh từ `RequisitionId` (khóa chính tự tăng, không trùng) sau lần lưu đầu: `REQ-` + id đệm 4 chữ số.

**Lịch sử thay đổi lưu ở đâu?**
Bảng `AuditLog`: mỗi lần tạo / sửa / rút / duyệt / xóa ghi một dòng; khi sửa chỉ ghi các trường **thực sự thay đổi** (so ảnh chụp trước / sau, `FieldChanges#changedKeys`); không đổi gì thì không lưu, không ghi audit. Ý kiến duyệt lưu riêng ở `RequisitionApproval`.

**Gõ URL bậy (`/requisitions/abc`, phiếu không tồn tại, không đủ quyền) thì sao?**
Không ra trang lỗi: controller bắt lỗi, quay về trang danh sách / chi tiết vừa xem kèm toast (`backWhenNotFound`, `whenAccessDenied`).

---

## 7. Hạn chế đã biết (nói trước nếu GV hỏi)

- Yêu cầu đang chờ duyệt không còn thao tác đưa về nháp; người tạo chỉ sửa được sau khi Giám đốc trả lại.
- Trạng thái `Closed` / `Cancelled` có trong DB nhưng chưa có chức năng.
- Một số phiếu seed thiếu `AuditLog` nên phần Lịch sử ít dòng.
