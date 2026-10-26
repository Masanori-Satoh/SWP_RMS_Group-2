# Cẩm nang review: Yêu cầu tuyển dụng (Requisition)

> Ghi chú cá nhân của HoangNH để trả lời GV khi review 3 màn: **Danh sách**, **Form tạo/sửa**, **Chi tiết**.
> Mỗi nút: bấm vào thì chạy qua đâu, kiểm tra gì, ghi bảng nào. Mỗi trường: lấy từ cột nào hoặc tính ra sao.
> Code được tham chiếu theo `Class#method` (không ghi số dòng vì số dòng thay đổi khi sửa code).

---

## 0. Bản đồ nhanh

```
Trình duyệt ──► SecurityConfig (lớp 1: có được vào URL không?)
            ──► RequisitionController (nhận tham số, gọi service, chọn view)
            ──► RequisitionServiceImpl (nghiệp vụ, lớp 2: phân quyền theo dữ liệu, transaction)
                  ├─ RequisitionAccess      : ai được làm gì (role, chủ sở hữu, trạng thái)
                  ├─ RequisitionValidator   : dữ liệu nhập có hợp lệ không
                  ├─ RequisitionLabels      : dịch giá trị DB sang tiếng Việt (nơi duy nhất)
                  ├─ RequisitionSpecifications : các điều kiện WHERE của màn danh sách
                  └─ Repository ──► SQL Server
            ──► Template Thymeleaf (chỉ hiển thị, không tự tính, không tự so sánh role)
```

| URL | Controller | Service | Bảng bị ghi |
|---|---|---|---|
| `GET /requisitions` | `list` | `getListPage` | (chỉ đọc) |
| `GET /requisitions/create` | `createForm` | (không) | |
| `POST /requisitions/create` | `create` | `createRequisition` | `JobRequisition`, `ScreeningCriteria`, `AuditLog`, `Notification` (khi gửi duyệt) |
| `GET /requisitions/{id}` | `detail` | `getById` | (chỉ đọc) |
| `GET /requisitions/edit/{id}` | `edit` | `getRequestDtoById` | |
| `POST /requisitions/edit/{id}` | `update` | `updateRequisition` | `JobRequisition`, `ScreeningCriteria`, `AuditLog`, `Notification` (khi gửi duyệt) |
| `GET /requisitions/copy/{id}` | `copy` | `copy` | (không ghi gì, chỉ điền sẵn form) |
| `POST /requisitions/{id}/withdraw` | `withdraw` | `withdraw` | `JobRequisition`, `AuditLog` |
| `POST /requisitions/{id}/decision` | `decide` | `decide` | `JobRequisition`, `RequisitionApproval`, `AuditLog`, `Notification` (khi duyệt hoặc trả lại) |
| `POST /requisitions/delete/{id}` | `delete` | `deleteRequisition` | xóa `JobRequisition` (+ tiêu chí, lịch sử duyệt), ghi `AuditLog` |

Mọi `POST` đều cần CSRF token (Spring Security tự chèn `_csrf` vào form Thymeleaf). Thiếu token thì bị 403.

---

## 1. Phân quyền: hai lớp bảo vệ

**Lớp 1, theo URL** (`core/config/SecurityConfig`): `/requisitions/**` chỉ cho `HIRING_MANAGER`, `DIRECTOR`, `HR`. Candidate, Interviewer, System Admin gõ URL thì bị 403 trước khi vào controller.

**Lớp 2, theo dữ liệu** (`RequisitionServiceImpl#scope` + `RequisitionAccess`):

| Role | Thấy những yêu cầu nào (`scope`) | Xem chi tiết (`requireView`) | Sửa / xóa (`canEdit`) | Duyệt (`canDecide`) | Rút lại |
|---|---|---|---|---|---|
| Hiring Manager | Chỉ yêu cầu **mình tạo** | Của mình, hoặc thuộc phòng mình quản lý | Của mình, khi **Draft / Rejected** | Không | Của mình, khi **Pending_Director** |
| Director | Mọi yêu cầu **trừ Draft** | Mọi yêu cầu trừ Draft | Không | Khi **Pending_Director** và không phải người tạo | Không |
| HR | Chỉ **Approved** | Chỉ Approved | Không | Không | Không |

> GV hỏi: *"Nếu HR sửa URL thành `?status=Draft` thì sao?"*
> Trả lời: bộ lọc luôn được **nối thêm (AND)** vào `scope`. Câu SQL thành `ApprovalStatus = 'Approved' AND ApprovalStatus = 'Draft'` nên ra 0 dòng. Ngoài ra `normalizeFilter` bỏ luôn tham số `status` vì HR không có ô lọc trạng thái.

> GV hỏi: *"Ẩn nút trên giao diện có đủ bảo mật không?"*
> Trả lời: không. Nút chỉ ẩn để dễ dùng. Mỗi thao tác ở server đều kiểm tra lại (`requireEdit`, `canDecide`, `owns`…) và ném `AccessDeniedException` → trang 403.

---

## 2. Vòng đời trạng thái (cột `JobRequisition.ApprovalStatus`)

```
            Lưu nháp                Gửi duyệt
 (mới) ───────────────► Draft ─────────────────► Pending_Director
                          ▲   ◄─────────────────      │      │
                          │    Rút lại (HM)            │      │
                          │                  Phê duyệt │      │ Trả lại (bắt buộc lý do)
                          │                            ▼      ▼
                          │                       Approved   Rejected
                          └─── Lưu nháp sau khi sửa ──────────┘
                                 (Sửa & gửi lại → Pending_Director)
```

- Không có đường Draft → Approved, cũng không có Approved → Draft (rule NHH §14).
- DB còn 2 trạng thái `Closed`, `Cancelled` nhưng hiện **chưa có chức năng nào dùng**.

---

## 3. Màn DANH SÁCH (`/requisitions`, `requisitions/list.html`)

### 3.1 Thanh công cụ

| Thành phần | Bấm/nhập thì chạy như thế nào |
|---|---|
| **Ô tìm kiếm** | Form `GET`: Enter → `?q=...`. Service tìm `LIKE` trên **tên vị trí** (`JobRequisition.Title`) **hoặc tên phòng ban** (`Department.DepartmentName`), không phân biệt hoa thường. Ký tự `% _ [` được escape (`RequisitionSpecifications#escapeLike`). Cắt tối đa 120 ký tự |
| **Nút "Bộ lọc"** | Thẻ `<details>` tự mở/đóng, không cần JS. Số trên nút = `RequisitionSearchRequest#activeFilterCount` (không tính từ khóa, sắp xếp) |
| **Ô lọc theo role** | Do `RequisitionServiceImpl#buildFilterOptions` quyết định: HM (Trạng thái, Tin tuyển dụng, Ngày tạo); Director (Trạng thái trừ Bản nháp, Phòng ban, Người tạo); HR (Tin tuyển dụng, Phòng ban, Người tạo). Mọi role có Sắp xếp |
| **Trạng thái** | Checkbox nhiều lựa chọn → `?status=A&status=B` → `ApprovalStatus IN (...)` |
| **Tin tuyển dụng** | `none` → `NOT EXISTS` trong `JobPosting`; `Published/Paused/Closed` → trạng thái của **tin mới nhất** (`MAX(JobPostingId)`) bằng giá trị đó (`RequisitionSpecifications#latestPostingStatusIs`) |
| **Phòng ban / Người tạo** | `DepartmentId = ?` / `HiringManagerId = ?`. Danh sách người tạo chỉ gồm HM có yêu cầu trong phạm vi người xem được thấy (`findCreatorsWithStatusIn`) |
| **Ngày tạo từ – đến** | `CreatedAt >= từ 00:00` và `CreatedAt < (đến + 1 ngày) 00:00`, nên tính trọn cả ngày "đến" |
| **Sắp xếp** | Enum `RequisitionSort` (whitelist): Mới nhất, Cũ nhất, Ngày bắt đầu gần nhất, Tên A–Z. Giá trị lạ → Mới nhất |
| **Ô ẩn `status=`** | Luôn gửi kèm để phân biệt "lần vào đầu" (không có tham số) với "đã bỏ chọn hết" |
| **Director vào trang** | Không lọc sẵn: thấy tất cả phiếu được phép xem (mọi trạng thái trừ bản nháp của người khác). Muốn xem phiếu chờ duyệt thì chọn trạng thái "Chờ duyệt" |
| **Chip ✕** | Link do `RequisitionListLinks#without` tạo: giữ các tham số khác, bỏ tham số của chip, bỏ `page`. Không role nào bị lọc sẵn trạng thái (Director vào trang cũng thấy tất cả) |
| **"Xóa bộ lọc"** | `/requisitions?status=` → xem tất cả |
| **"Đặt lại mặc định"** | `/requisitions` → về mặc định của role (Director lại thấy Chờ duyệt) |
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
| Tin tuyển dụng | Tin **mới nhất** của yêu cầu (`JobPostingRepository#findTopByRequisition_RequisitionIdOrderByJobPostingIdDesc`) → `RequisitionLabels#postingStatus`. Chỉ thành link khi `postingViewable` (HR, HM; Director bị 403 ở trang tin nên không có link) |
| Ngày tạo | `JobRequisition.CreatedAt` (tự đặt ở `BaseEntity#onCreate`) |
| Menu ⋯ | Mỗi mục hiện theo cờ do service tính trong `RequisitionServiceImpl#response`: `editable`, `copyable`, `postable`, `postingViewable`, `deletable` |

Đã bỏ cột "Người tạo yêu cầu" để bảng không phải cuộn ngang trên laptop (vẫn lọc được, vẫn xem được ở trang chi tiết).

### 3.3 Phân trang

Fragment catalog `fragments/ui/pagination :: paged`, link do `PageLinks#to` tạo: **giữ mọi tham số đang có**, chỉ đổi `page` (đếm từ 0). Gõ `?page=99` → tự lùi về trang cuối. Kích thước trang 1–50 (mặc định 10).

---

## 4. Màn FORM (`/requisitions/create`, `/edit/{id}`, `/copy/{id}`; `requisitions/form.html`)

### 4.1 Chi tiết các trường và quy tắc kiểm tra (Validation Rules)

| Trường | Cột DB & Kiểu | Lưu nháp | Gửi duyệt | Quy tắc kiểm tra & Hành vi giao diện |
|---|---|---|---|---|
| **Tên vị trí** | `Title` NVARCHAR(200) NOT NULL | **Bắt buộc** | **Bắt buộc** | Không được để trống. Cắt khoảng trắng 2 đầu; tối đa 200 ký tự. Khi gửi duyệt không được để tiêu đề mặc định `"Untitled requisition"` |
| **Phòng ban** | `DepartmentId` INT NOT NULL | (tự động) | (tự động) | **Không lấy từ form** (không có trong `@InitBinder`). Tạo mới: phòng ban của tài khoản đang đăng nhập, phải đang `Active`, không có thì báo "Tài khoản của bạn chưa được gán phòng ban đang hoạt động...". Sửa: giữ phòng ban đã lưu |
| **Đợt tuyển dụng** | `RecruitmentRound` INT NOT NULL | (tự động) | (tự động) | Tạo mới = 1; sao chép = đợt lớn nhất trong chuỗi + 1 (`assignRecruitmentRound`, khóa dòng phiếu gốc tránh trùng đợt) |
| **Số lượng cần tuyển** | `NumberOfPositions` INT NOT NULL | **Bắt buộc** | **Bắt buộc** | Số nguyên **1–30** (mặc định 1, hằng số `POSITIONS_MIN`/`POSITIONS_MAX`). Nút −/+ dừng ở 1 và 30 |
| **Loại tuyển dụng** | `HiringType` NVARCHAR(20) | Không | Không | Tuyển mới (`New`) hoặc Tuyển thay thế (`Replacement`). Không chọn gì tương đương "Chưa xác định" (`NULL`) |
| **Hình thức làm việc** | `EmploymentType` NVARCHAR(50) NOT NULL | **Bắt buộc** | **Bắt buộc** | Chọn 1 trong 4 loại: Toàn thời gian (`Full-time`), Bán thời gian (`Part-time`), Thực tập (`Internship`), Hợp đồng (`Contract`) |
| **Mô hình làm việc** | `WorkModel` NVARCHAR(20) NOT NULL | **Bắt buộc** | **Bắt buộc** | Chọn 1 trong 3 loại: Tại văn phòng (`On-site`), Kết hợp (`Hybrid`), Từ xa (`Remote`) |
| **Yêu cầu giới tính** | `RequiredGender` NVARCHAR(20) NOT NULL | Không | Không | Không yêu cầu (`Any` - mặc định), Nam (`Male`), Nữ (`Female`). Để trống tự quy về `Any` |
| **Địa điểm làm việc** | `WorkLocation` NVARCHAR(255) | Không | **Bắt buộc** | **Không được để trống khi gửi duyệt**. Tối đa 255 ký tự |
| **Ngày bắt đầu dự kiến** | `ExpectedStartDate` DATE | Không | **Bắt buộc** | Gửi duyệt: **≥ hôm nay**. Luôn luôn (cả nháp): **≤ hôm nay + 1 năm** (`START_DATE_MAX_YEARS`), chặn cả năm gõ nhầm như `20266` vượt kiểu `DATE` của SQL. Ô ngày có `min`/`max` |
| **Thời gian thử việc** | `ProbationDays` INT | Không | Không | Có thể để trống (không thử việc) hoặc nhập theo **số ngày từ 1 đến 180 ngày** (Điều 25 Bộ luật Lao động 2019). Giao diện: nút −, ô số, chữ `"ngày"`, nút + |
| **Lương tối thiểu / tối đa** | `MinSalary` / `MaxSalary` DECIMAL(18,2) | Không | Không | Có thể để trống (lương thỏa thuận). Được nhập một ô (hiển thị "Từ X" / "Đến Y"). Nếu nhập: **> 0**, **≤ 1.000.000.000** (`SALARY_MAX`) và **Lương tối đa >= Lương tối thiểu**. Khi nhập tự động định dạng kiểu `20.000.000` (dấu chấm hàng nghìn, giữ vị trí con trỏ). Controller có `PropertyEditorSupport` chuyển `20.000.000` ↔ `BigDecimal` |
| **Lý do tuyển dụng** | `ReasonForHiring` NVARCHAR(2000) | Không | **Bắt buộc** | Tối đa 2000 ký tự (xuống dòng `

` được đổi thành `
` trước khi đếm, `cleanMultiline`). Có bộ đếm ký tự cập nhật thời gian thực (ví dụ: `234/2000`) |
| **Mô tả công việc** | `JobDescription` NVARCHAR(MAX) NOT NULL | Không | **Bắt buộc** | Tối đa 2000 ký tự. Có bộ đếm ký tự cập nhật thời gian thực (ví dụ: `234/2000`) |
| **Yêu cầu ứng viên** | `RequirementDetails` NVARCHAR(MAX) NOT NULL | Không | **Bắt buộc** | Tối đa 2000 ký tự. Có bộ đếm ký tự cập nhật thời gian thực (ví dụ: `234/2000`) |
| **Tiêu chí sàng lọc hồ sơ** | Bảng `ScreeningCriteria` | Điền đủ nếu đã nhập | **Bắt buộc** | Gửi duyệt cần **>= 1 tiêu chí**, tên tiêu chí không trùng lặp, mỗi tiêu chí điền đủ 4 trường (Tên, Loại, Giá trị, Trọng số), và **tổng trọng số đúng bằng 100%**. Mỗi trọng số 0.01–100 (cả khi lưu nháp). Tối đa 50 tiêu chí |

> **Vì sao bắt buộc một số trường ngay từ lúc Lưu nháp?**
> Các cột `Title`, `NumberOfPositions`, `EmploymentType`, `WorkModel` là `NOT NULL` trong cơ sở dữ liệu. Nếu không validate ngay thì câu lệnh INSERT của DB sẽ ném lỗi `DataIntegrityViolationException`, vi phạm quy chuẩn thiết kế nghiệp vụ (không bao giờ để lỗi database lộ ra cho người dùng).

---

### 4.2 Chống vỡ giao diện (Anti-breakage Layout) khi đổ dữ liệu lớn

Form được thiết kế với cơ chế phòng ngừa vỡ layout toàn diện:
1. **Chống tràn ngang ô nhập & văn bản dài**:
   - Tất cả các thẻ `.form-input`, `.form-textarea`, `.form-select`, `.rqf-readonly` đều được định cấu hình `max-width: 100%`, `box-sizing: border-box`, và `min-width: 0`.
   - Các trường textarea và thông báo lỗi `.field-error` áp dụng `overflow-wrap: anywhere; word-break: break-word;` đảm bảo người dùng nhập hoặc paste các chuỗi ký tự cực dài không có khoảng trắng thì nội dung vẫn tự động ngắt dòng, không đẩy bè khung form.
2. **Khung bảng tiêu chí thích ứng (Responsive Table)**:
   - Bảng tiêu chí sử dụng `table-layout: fixed` với độ rộng các cột cố định; bọc trong vùng `.table-wrapper` có `role="region"` hỗ trợ cuộn ngang độc lập khi ở màn hình nhỏ mà không làm hỏng lưới 2 cột chính.
3. **Bố cục 2 cột linh hoạt**:
   - Khung trái (`.rqf-main`) chứa các nhóm thông tin nhập liệu, khung phải (`.rqf-rail`) gắn cố định (`sticky`) chứa bảng kiểm tra tiến độ "Sẵn sàng gửi duyệt" và các nút hành động. Trên màn hình hẹp (tablet/mobile), layout tự động xếp chồng 1 cột và thanh nút hành động ghim đáy màn hình.

---

### 4.3 Nút "Lưu bản nháp" chạy như thế nào

1. JS (`pages/requisition-form.js#validateAll('draft')`) kiểm tra 4 trường tối thiểu + định dạng số (số lượng 1–30, thử việc 1..180 nếu có, lương 1–1 tỷ và max >= min nếu có, ngày ≤ hôm nay + 1 năm). Sai → hiện lỗi đỏ cạnh ô, không gửi.
2. Form `POST` với `action=draft` → `RequisitionController#create` (hoặc `#update`).
3. `@InitBinder` đăng ký whitelist các trường được phép bind (chống can thiệp các trường nhạy cảm như `approvalStatus`) và đăng ký custom `PropertyEditor` cho `minSalary`, `maxSalary` (đọc `20.000.000` thành 20000000 khi parse sang `BigDecimal`).
4. `RequisitionServiceImpl#createRequisition`:
   - `access.requireCreate`: chỉ HM, Director.
   - Lấy phòng ban của người tạo (phải có và đang `Active`), ghi vào `departmentId`.
   - `RequisitionValidator#validate` (kiểm tra lại toàn bộ, không tin JS).
   - Chặn trùng: đã có **bản nháp cùng tên trong cùng phòng ban** thì báo lỗi.
   - Tạo `JobRequisition` (`HiringManagerId` = `CreatedBy` = người đang đăng nhập), gán đợt, ghi các trường, thêm tiêu chí (bỏ dòng trống).
   - `ApprovalStatus = 'Draft'` → `saveAndFlush` → sinh **mã `REQ-0025`** từ id → ghi `AuditLog` (`CREATE`).
5. Thành công → redirect `/requisitions/{id}` + thông báo "Đã lưu bản nháp thành công.". Lỗi nghiệp vụ → render lại form, dữ liệu đã nhập còn nguyên, lỗi hiện ở đầu form và cạnh ô.

---

### 4.4 Nút "Gửi Giám đốc duyệt"

1. Bấm nút **chưa gửi ngay**: JS kiểm tra đủ điều kiện gửi duyệt (tất cả các trường dấu `*`, ngày bắt đầu >= hôm nay, tiêu chí đủ 100%). Thiếu → lỗi cạnh ô, cuộn tới lỗi đầu tiên.
2. Đủ → hiện **hộp xác nhận** trong khung bên phải. Bấm "Gửi duyệt" mới thật sự `POST action=submit`. Sau khi gửi, các nút bị khóa để bấm 2 lần không tạo 2 yêu cầu.
3. Service giống lưu nháp, khác ở: validator áp quy tắc gửi duyệt (mọi trường *, tiêu chí 100%), `ApprovalStatus = 'Pending_Director'`, `SubmittedAt = now`.

---

### 4.5 Các thành phần tương tác khác của form

| Thành phần | Hoạt động & Xử lý |
|---|---|
| **Bộ đếm ký tự textarea** | Cả 3 trường Lý do, Mô tả, Yêu cầu đều có bộ đếm trực quan phía dưới góc phải (VD: `234/2000`). Được khởi tạo số lượng ngay từ phía máy chủ qua Thymeleaf `th:text="|${#strings.length(...)}/2000|"`, và cập nhật tức thì qua JS khi người dùng gõ phím (`input`, `keyup`), đổi giá trị (`change`) hoặc dán văn bản (`paste`) |
| **Định dạng lương tự động** | Lương tối thiểu & tối đa tự động phân tách hàng nghìn bằng dấu chấm kiểu `20.000.000` khi người dùng nhập dữ liệu, giữ nguyên vị trí con trỏ soạn thảo; tự động hiển thị lại đúng định dạng khi nạp trang sửa/sao chép |
| **Kiểm tra ngày trực tiếp** | Ô ngày có `min` = hôm nay, `max` = hôm nay + 1 năm (controller truyền `startDateMin`/`startDateMax`). Rời ô thì JS kiểm tra theo luật gửi duyệt: ngày đã qua báo "Ngày bắt đầu dự kiến phải từ hôm nay trở đi."; quá 1 năm báo ngay cả khi nháp. "Hôm nay" ở JS tính theo giờ máy người dùng (không dùng `toISOString()` vì trả ngày UTC) |
| **Ô thử việc** | Ô số kèm chữ `"ngày"` và nút −/+ (dùng chung code stepper với ô Số lượng, giới hạn lấy từ `min`/`max` = 1..180). Ô trống bấm nút thì điền sẵn 60 ngày (`data-step-start`). Ẩn nút spin mặc định của trình duyệt |
| **"+ Thêm tiêu chí"** | JS sao chép `<template id="criteria-template">`, đánh lại chỉ số `screeningCriteria[0], [1]...` (Spring cần chỉ số liên tục để bind vào `List`) |
| **Nút × trên dòng tiêu chí** | Xóa dòng ở giao diện. Khi **sửa**, dòng đã lưu bị xóa thì service xóa bản ghi (`synchronizeCriteria`, `orphanRemoval`) |
| **Thanh tổng trọng số** | Chỉ cộng dòng đã nhập. Vàng = thiếu, xanh = đủ 100%, đỏ = thừa |
| **"Chia đều trọng số"** | 100 chia đều cho các dòng đã nhập, dòng cuối nhận phần dư (ví dụ 3 dòng → 33.33 / 33.33 / 33.34) |
| **Cột "Sẵn sàng gửi duyệt"** | 10 mục bắt buộc khi gửi duyệt, tự động đánh dấu hoàn thành khi các trường thỏa mãn điều kiện hợp lệ; bấm mục → cuộn mượt tới ô đó |
| **"Hủy"** | Tạo mới → về danh sách; sửa → về trang chi tiết. Không lưu gì |
| **Sao chép (`/copy/{id}`)** | Chỉ yêu cầu **đã Approved / Rejected** mới sao chép được. Form điền sẵn, **chưa ghi DB** cho tới khi bấm Lưu/Gửi. Bỏ id tiêu chí, mã, trạng thái, lịch sử duyệt (NHH §23.4) |

---

## 5. Màn CHI TIẾT (`/requisitions/{id}`, `requisitions/detail.html`)

### 5.1 Dữ liệu hiển thị

| Phần | Lấy từ đâu / tính thế nào |
|---|---|
| Mã yêu cầu | `RequisitionCode` (sinh lúc tạo: `"REQ-" + id` đệm 4 chữ số) |
| Banner "đã chờ N ngày" | `RequisitionServiceImpl#response`: số ngày từ `SubmittedAt` tới hôm nay (chỉ khi Chờ duyệt) |
| Banner Từ chối | `rejectionReason` = `Comments` của bản ghi `RequisitionApproval` mới nhất |
| Thanh tiến trình | Tạo nháp = `CreatedAt`; Gửi duyệt = `SubmittedAt`; Giám đốc duyệt = `ApprovalDate` mới nhất; Đăng tin = `JobPosting.PostingDate` của tin mới nhất |
| Ngày bắt đầu "còn X ngày" | `ExpectedStartDate - hôm nay` (≤ 30 ngày tô vàng) |
| Mức lương | `RequisitionLabels#salary`: đổi sang triệu, "20 – 30 triệu VND/tháng" hoặc "Thỏa thuận" |
| Thử việc | `ProbationDays` + " ngày" hoặc "Không thử việc" |
| Lý do / Mô tả / Yêu cầu | Service **escape HTML** rồi đổi xuống dòng thành `<br/>` (`RequisitionLabels#multilineHtml`), template dùng `th:utext`. Escape để chống XSS |
| Tiêu chí | bảng `ScreeningCriteria`; tổng trọng số cộng trong service |
| Khối Tin tuyển dụng | Tin mới nhất: `PostingStatus`, `PostingNo` (lần đăng), `PostingDate`, `ApplicationDeadline` |
| Ý kiến của Giám đốc | bảng `RequisitionApproval`, mới nhất trước |
| Lịch sử | bảng `AuditLog` với `EntityName = 'JobRequisition'` và `EntityId = id`, mới nhất trước |

### 5.2 Các nút

| Nút | Hiện khi | Bấm thì |
|---|---|---|
| **← Quay về danh sách** | Luôn hiện | `GET /requisitions` (về màn danh sách, bộ lọc mặc định của vai trò) |
| **Chỉnh sửa / Sửa & gửi lại** | `editable`: người tạo (HM / Director), trạng thái Draft hoặc Rejected | `GET /edit/{id}` → form |
| **Rút lại để chỉnh sửa** | `withdrawable`: người tạo, trạng thái Pending_Director | `POST /{id}/withdraw` → `withdraw`: khóa dòng, kiểm tra lại quyền, `Pending_Director → Draft`, ghi `AuditLog` |
| **Xóa** | `deletable`: được sửa, **chưa có tin tuyển dụng và chưa có đợt tuyển sau sao chép từ nó** | Hộp thoại xác nhận → `POST /delete/{id}` → `deleteRequisition`: kiểm tra quyền, chặn nếu đã có `JobPosting` hoặc đã là phiếu gốc của đợt sau (`existsByOriginalRequisition_RequisitionId`), ghi `AuditLog` (`DELETE`), xóa `RequisitionApproval`, xóa yêu cầu (tiêu chí xóa theo cascade) |
| **Sao chép thành đợt mới** | `copyable`: có quyền tạo, trạng thái Approved / Rejected | `GET /copy/{id}` (xem mục 4.4) |
| **Tạo tin tuyển dụng** | `postable`: HR, Approved, chưa có tin | `GET /internal/job-postings/create?requisitionId=...` (module Tin tuyển dụng) |
| **Xem tin tuyển dụng** | `postingViewable`: đã có tin, role HR / HM | `GET /internal/job-postings/{id}` |
| **Phê duyệt / Trả lại** | `decidable`: Director, không phải người tạo, Pending_Director | `POST /{id}/decision` → `decide`: khóa dòng; **Trả lại bắt buộc có ý kiến** (cả JS lẫn server), tối đa 1000 ký tự; đổi trạng thái; thêm bản ghi `RequisitionApproval` (Director, kết quả, ý kiến, thời điểm); ghi `AuditLog`; **Phê duyệt hoặc Trả lại đều tạo `Notification` cho HM** |

---

## 6. Câu hỏi GV hay hỏi

**Vì sao service dùng `findForUpdate` (khóa dòng)?**
Hai người cùng thao tác một yêu cầu (ví dụ HM rút lại đúng lúc Director bấm duyệt). `PESSIMISTIC_WRITE` khóa dòng trong transaction, người sau phải chờ người trước xong. Sau đó người sau kiểm tra lại trạng thái và bị từ chối vì trạng thái đã đổi.

**Dữ liệu ngày tạo, người tạo lấy từ client à?**
Không. `CreatedAt` do `BaseEntity#onCreate` đặt; `HiringManagerId`, `CreatedBy`, `DepartmentId` lấy từ người đang đăng nhập (`RequisitionAccess#actor`). Form không có ô cho các giá trị này, và `@InitBinder` không cho bind `approvalStatus`.

**JS kiểm tra rồi sao server còn kiểm tra?**
JS chỉ để người dùng thấy lỗi sớm; người dùng có thể tắt JS hoặc gửi request bằng Postman. `RequisitionValidator` mới là nơi quyết định. Quy tắc ở JS được viết **giống hệt** server (có chú thích "khớp RequisitionValidator").

**Tổng trọng số 100% kiểm tra ở đâu?**
`RequisitionValidator#validate`, hằng số `REQUIRED_TOTAL_WEIGHT`, chỉ khi gửi duyệt (NHH §23.3).

**Tại sao lưu nháp lại bắt buộc vài trường?**
Xem cuối mục 4.1: các cột đó `NOT NULL` trong DB.

**Mã yêu cầu sinh thế nào, có trùng không?**
Sinh từ `RequisitionId` (khóa chính tự tăng, không bao giờ trùng) sau lần lưu đầu: `REQ-` + id đệm 4 chữ số. Migration `008` đổi các mã cũ dạng `REQ-2026-0001` về dạng mới.

**Lịch sử thay đổi lưu ở đâu?**
Bảng `AuditLog`: mỗi lần tạo/sửa/rút/duyệt/xóa ghi một dòng; khi sửa chỉ ghi các trường **thực sự thay đổi** (so sánh ảnh chụp trước/sau trong `updateRequisition`). Ý kiến duyệt lưu riêng ở `RequisitionApproval`.

---

## 7. Hạn chế đã biết (nói trước nếu GV hỏi)

- Duyệt (Approve) **không gửi thông báo** cho HM/HR; chỉ Trả lại mới gửi.
- Rút lại không xóa `SubmittedAt` (lần gửi lại sẽ ghi đè).
- Màn danh sách chạy thêm 2 câu SQL cho mỗi dòng để lấy tin tuyển dụng (N+1), chấp nhận được với dữ liệu nhỏ.
- Trạng thái `Closed` / `Cancelled` có trong DB nhưng chưa có chức năng.
- Một số yêu cầu seed sai nghiệp vụ (ví dụ Chờ duyệt nhưng không có tiêu chí) và thiếu `AuditLog`.
