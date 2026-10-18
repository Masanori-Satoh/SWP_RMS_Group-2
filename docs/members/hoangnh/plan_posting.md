# Kế hoạch hoàn thiện Tin tuyển dụng (Job Posting) theo mockup

- **Người phụ trách:** Nguyễn Huy Hoàng (hoangnh)
- **Ngày lập:** 17/10/2026
- **Mockup đã được GV xác nhận:**
  - Danh sách: https://claude.ai/artifact/SfxQmvzDqDYRvESdzGonLh
  - Tạo / sửa tin: https://claude.ai/artifact/Xaj6FkBkPJUk83ffMDneqS
  - Chi tiết tin: https://claude.ai/artifact/GUqBCh6XW1MzK2NaL639u2
- **Quy tắc phải theo:** `CLAUDE.md` §14 (chuyển trạng thái), §16 (màn danh sách), §22 (Job Posting, gồm dùng chung nội dung với trang công khai), §23 (test), §1 (wireframe).
- **Cách làm:** giống module Requisition vừa hoàn thiện. Dùng lại mẫu `SearchRequest` / `FilterOptions` / chip / `Specifications` / `Labels` / fragment `paged` / `management-filters`. Viết code dễ hiểu cho người mới, mỗi bước có test.

> **Đã chốt 17/10/2026: mọi quyết định D1–D11 theo cột "Đề xuất".** Làm lần lượt từng bước ở **mục 3**, mỗi bước xong thì báo kết quả và ghi vào `checklist.md`.

---

## 1. Hiện trạng so với mockup

### 1.1 Màn danh sách (`/internal/job-postings`, `job-postings/list.html`)

| Mockup | Hiện tại | Việc cần làm |
|---|---|---|
| Dùng layout chung (sidebar, header) | Dùng fragment cũ `head` / `sidebar` / `workspace-header` | Chuyển sang `layout/internal` như màn requisition |
| Ô tìm "tiêu đề tin hoặc phòng ban" | Tìm theo tiêu đề hoặc **mã yêu cầu** | Đổi thành tiêu đề hoặc tên phòng ban |
| Nút "Bộ lọc" mở popover; ô lọc khác nhau theo vai trò; chip bỏ lọc; "Đặt lại mặc định" | Các ô select luôn hiện, giống nhau với mọi vai trò | Làm lại theo mẫu requisition (`mft-` + chip) |
| Lọc: Trạng thái (nhiều lựa chọn, gồm "Hẹn đăng"), Phòng ban, **Hạn nộp** (còn hạn / sắp hết ≤ 7 ngày / quá hạn), Người tạo, Ngày đăng từ–đến | Trạng thái (1 lựa chọn), Phòng ban | Thêm các ô lọc còn thiếu |
| Sắp xếp: Mới đăng nhất, Hạn nộp gần nhất, **Nhiều hồ sơ nhất**, Tiêu đề A–Z | Mới / cũ / tiêu đề A–Z / Z–A / hạn nộp | Đổi theo mockup |
| Cột: STT, Tiêu đề (+ "Lần N · hình thức"), Phòng ban, **Hồ sơ (+N mới)**, Ngày đăng (+ "sau N ngày" nếu hẹn), Hạn nộp (+ "còn N ngày" / "quá hạn"), Trạng thái, Người tạo, ⋯ | Không có cột Hồ sơ; có cột "Đợt tuyển dụng", "Ngày tạo" | Đổi cột; đếm hồ sơ bằng **1 câu truy vấn gộp** (không N+1) |
| Menu ⋯ theo trạng thái (Đăng tin / Chỉnh sửa / Tạm dừng / Mở lại / Đóng tin / Đăng lại / Xem ứng viên…) | Vài thao tác cố định | Tính cờ thao tác ở service, template chỉ đọc cờ |
| HR: dải "N yêu cầu đã duyệt chưa có tin" + nút tạo nhanh | Chỉ có ở trang dashboard | Thêm vào đầu danh sách (dùng lại `requisitionsAwaitingPosting`) |
| Phân trang giữ bộ lọc | Fragment `pager` cũ (lỗi trang bắt đầu 0/1 như requisition trước đây) | Dùng `fragments/ui/pagination :: paged` |
| Phạm vi: HR, Admin thấy tất cả; **HM thấy phòng mình, trừ nháp**; **Director thấy tất cả, trừ nháp** | HM thấy cả nháp; **Director bị 403** (SecurityConfig) | Xem quyết định D4, D5 |

### 1.2 Màn tạo / sửa (`/internal/job-postings/create`, `/{id}/edit`, `job-postings/create.html`)

| Mockup | Hiện tại | Việc cần làm |
|---|---|---|
| Thẻ "Tạo từ yêu cầu đã duyệt" (mã, phòng ban, số lượng, ngày bắt đầu, "Tin lần N") + ô **Đổi yêu cầu** | Khối "Thông tin yêu cầu nguồn (chỉ đọc)" | Làm lại thẻ nguồn; thêm ô đổi yêu cầu (chỉ khi tạo mới) |
| Nhãn "Lấy từ yêu cầu" / "Đã chỉnh sửa · **Khôi phục bản gốc**" ở Tiêu đề, Địa điểm, Mô tả, Yêu cầu | Không có | JS, cần giá trị gốc của yêu cầu trong model |
| Địa điểm + **bản đồ** + "Mở Google Maps" | Chỉ ô nhập | Xem D3 |
| Không có (DB không có cột / bảng) | Xem D2 |
| Mức lương hiển thị: **Theo yêu cầu** / **Thỏa thuận** / **Tự nhập** | Ô nhập tự do | 3 lựa chọn, kết quả vẫn lưu vào `SalaryDisplay` (không đổi DB) |
| Công tắc "Hiển thị số lượng tuyển" + ô nội dung | Ô nhập | Tắt thì lưu `NumberOfPositionsDisplay = NULL` |
| Quyền lợi + **nút thêm nhanh** (Lương tháng 13, BHXH…) | Ô nhập | JS chèn dòng |
| Ngày đăng: **Đăng ngay** / **Hẹn ngày đăng** | Không có hẹn | Xem D1 |
| Hạn nộp **bắt buộc**, nút +14 / +30 / +45 ngày, dòng "Nhận hồ sơ trong N ngày", cảnh báo nếu sau ngày bắt đầu của yêu cầu | Có lựa chọn "Tuyển liên tục" (không hạn) | Xem D6 |
| Cột **Xem trước** "Ứng viên sẽ thấy" (tab Soạn / Xem trước trên điện thoại) | Không có | JS dựng bản xem trước |
| Lỗi hiện cạnh từng ô + khung tổng hợp | Service ném **một câu lỗi chung** | Đổi `JobPostingValidationException` sang map lỗi theo ô (giống `RequisitionValidationException`) |
| Nút: Hủy · Lưu nháp · Đăng tin (đổi thành "Lên lịch đăng" khi hẹn) | Lưu nháp · Đăng | Theo mockup |

### 1.3 Màn chi tiết (`/internal/job-postings/{id}`, `job-postings/detail.html`)

| Mockup | Hiện tại | Việc cần làm |
|---|---|---|
| Đầu trang: tiêu đề, badge trạng thái, "Lần N", phòng ban, hình thức + nút theo trạng thái; "Xem trên trang tuyển dụng ↗" khi đang đăng | Nút nằm ở cột phải, điều kiện viết trong template | Cờ thao tác tính ở service; nút ở đầu trang |
| Dải thông báo theo trạng thái (nháp / hẹn đăng / tạm dừng / đã đóng / **còn ≤ 7 ngày hết hạn**) | Không có | Thêm |
| **5 thẻ số liệu**: Tổng hồ sơ, Chờ xử lý, Đạt ngưỡng AI, Đang phỏng vấn, Đã tuyển x / số lượng | Không có | Đếm từ `Application` + `AIScreeningResult` (ngưỡng lấy `SystemConfig.AI_SCORE_THRESHOLD`) |
| **Thanh tiến trình hồ sơ** theo trạng thái (Mới nộp → Đã tuyển, Loại) | Không có | 1 câu `GROUP BY ApplicationStatus` |
| Tab **Nội dung tin** / **Ứng viên (N)** (hồ sơ gần đây + điểm AI + "Xem tất cả") | Chỉ nội dung | Thêm tab ứng viên, link sang màn hồ sơ của nhóm |
| Cột phải: Thông tin tin · Yêu cầu nguồn (+ link) · **Các lần đăng** · Lịch sử | Có thông tin, yêu cầu gốc, lịch sử | Thêm "Các lần đăng" (các tin khác cùng yêu cầu) |
| Không có bảng tiêu chí sàng lọc | Có bảng tiêu chí (lấy từ yêu cầu) | Đề xuất bỏ (đã có ở trang yêu cầu), xem D8 |
| Thao tác mới: **Đăng ngay**, **Hủy lịch đăng**, **Đăng lại (tạo tin lần mới)** | Chưa có | Thêm hàm service + URL |

---

## 2. Quyết định cần chốt trước khi code

| # | Câu hỏi | Lựa chọn | Đề xuất |
|---|---|---|---|
| **D1** | **Hẹn ngày đăng** ("Hẹn đăng") | **A.** Làm: HR chọn thời điểm đăng trong tương lai; trạng thái vẫn là `Published`, "Hẹn đăng" = Published nhưng `PostingDate` > hiện tại (không thêm trạng thái mới, không đổi CHECK của DB). Phải sửa truy vấn trang công khai thêm điều kiện `PostingDate <= now` (phần của DungLT). Phải sửa rule NHH §24.3 / §24.5 (đang ghi "không cho nhập PostingDate", "PostingDate = giờ server"). **B.** Bỏ, chỉ "Đăng ngay". | **A** vì GV đã duyệt wireframe có hẹn đăng. Nếu không kịp thì B, ghi vào hạn chế |
| **D2** | | **A.** Làm: thêm bảng `JobPostingPhoto` (migration 009, bạn tự chạy), lưu file ở thư mục upload, kiểm tra loại / dung lượng ở server. **B.** Hoãn sang sau khi xong phần chính | **B**: làm ở bước cuối nếu còn thời gian |
| **D3** | **Bản đồ** | **A.** Chỉ nút "Mở Google Maps" (link, không cần API key). **B.** Nhúng `<iframe src="https://www.google.com/maps?q=...&output=embed">` + nút link; cần mạng khi demo | **B**, kèm A làm dự phòng khi không có mạng |
| **D4** | **Director xem danh sách / chi tiết tin** | Mockup cho Director xem (trừ nháp). Hiện `SecurityConfig` chặn 403. Sửa `SecurityConfig` là file chung của nhóm | Cho xem (chỉ GET), báo nhóm trước khi sửa `SecurityConfig` |
| **D5** | **HM thấy tin nháp?** | Mockup: không. Code hiện tại: có | Theo mockup: HM chỉ thấy tin của phòng mình, **trừ nháp** |
| **D6** | **Tuyển liên tục (không hạn nộp)** | Mockup bắt buộc hạn nộp. Code đang cho phép bỏ trống | Theo mockup: bắt buộc, bỏ lựa chọn "Tuyển liên tục" |
| **D7** | **Admin đóng tin?** | Hiện chỉ HR được đóng (`closeJob` kiểm tra còn Offer chưa xong). Mockup cho Admin thấy "Đóng tin" | Giữ **chỉ HR** (đóng tin ảnh hưởng Offer / talent pool), ẩn nút với Admin |
| **D8** | Bảng tiêu chí sàng lọc trên trang chi tiết tin | Mockup không có | Bỏ khỏi trang tin (đã có ở trang yêu cầu) |
| **D9** | "+N mới" ở cột Hồ sơ nghĩa là gì | Hồ sơ trạng thái `Applied` (chưa ai xử lý) hoặc nộp trong 7 ngày | `Applied` (đúng nghĩa "chờ xử lý") |
| **D10** | "Đăng lại" tin đã đóng lấy nội dung từ đâu | Từ tin vừa đóng hoặc từ yêu cầu gốc | Từ **tin vừa đóng** (HR đã chỉnh sửa), `PostingNo` + 1 |
| **D11** | Trang `/internal/job-postings/dashboard` (không có trong mockup) | Giữ / bỏ | Giữ nguyên, không đụng |

---

## 3. Các bước thực hiện

Thứ tự: **Bước 1 → 2 → 3 → 4 → 5** (bắt buộc), **6** nếu chọn D2 = A, **7** làm xuyên suốt và chốt cuối.

### Bước 1. Nền chung (service, DTO, phân quyền)

| Việc | File (dự kiến) |
|---|---|
| Record bộ lọc `JobPostingSearchRequest` (q, status[], departmentId, creatorId, deadline, postedFrom/To, sort, page, size); `status == null` = lần đầu vào | `jobposting/dto/` |
| `JobPostingFilterOptions`, `JobPostingListResponse` (page + bộ lọc đã làm sạch + ô lọc theo vai trò + chip), dùng lại `FilterOption`, `RequisitionFilterChip` hoặc tách bản chung | `jobposting/dto/` |
| `JobPostingSort` (enum, whitelist): NEWEST, DEADLINE, MOST_APPLICATIONS, TITLE | `jobposting/service/` |
| `JobPostingSpecifications`: tìm tiêu đề / phòng ban (escape LIKE), trạng thái (gồm "Hẹn đăng" suy ra), phòng ban, người tạo, nhóm hạn nộp, khoảng ngày đăng | `jobposting/service/` |
| `JobPostingLabels`: nhãn + màu badge (Bản nháp / Hẹn đăng / Đang đăng / Tạm dừng / Đã đóng), "còn N ngày" / "quá hạn N ngày" | `jobposting/service/` |
| Phạm vi theo vai trò (`scope`): HR, Admin = tất cả; HM = phòng mình trừ nháp; Director = tất cả trừ nháp (D4, D5) | `JobPostingServiceImpl` |
| Đếm hồ sơ theo tin: 1 truy vấn `GROUP BY JobPostingId` trả `(tổng, mới)` cho cả trang | `ApplicationRepository` (thêm query, báo nhóm vì repository chung) |
| Cờ thao tác trên `InternalJobPostingResponse`: `publishable`, `schedulable`, `unschedulable`, `editable`, `pausable`, `reopenable`, `closable`, `repostable`, `deletable`, `applicationsViewable` | `jobposting/dto/`, service |
| `JobPostingValidationException` mang **map lỗi theo ô** | `jobposting/exception/` |

**Xong khi:** unit test cho bộ lọc theo vai trò (giống `RequisitionListFilterTests`), sort, nhãn, cờ thao tác theo từng trạng thái × vai trò.

### Bước 2. Màn danh sách

- Viết lại `list.html` theo `layout/internal` + `management-filters` (toolbar: ô tìm kiếm + nút Bộ lọc + chip), giống `requisitions/list.html`.
- Ô lọc theo vai trò:

  | Vai trò | Trạng thái | Phòng ban | Hạn nộp | Người tạo | Ngày đăng | Sắp xếp |
  |---|:---:|:---:|:---:|:---:|:---:|:---:|
  | HR | ✓ (5) | ✓ | ✓ | | | ✓ |
  | HM | ✓ (4, không nháp) | | ✓ | | | ✓ |
  | Director | ✓ (4, không nháp) | ✓ | ✓ | | | ✓ |
  | Admin | ✓ (5) | ✓ | | ✓ | ✓ | ✓ |

- Cột theo mục 1.1; HM ẩn cột Phòng ban, Người tạo; Director ẩn cột Người tạo.
- Menu ⋯ theo cờ (Bước 1). Thao tác POST có xác nhận (Đóng tin, Xóa nháp, Hủy lịch).
- HR: dải "N yêu cầu đã duyệt chưa có tin" + link tạo nhanh.
- Trạng thái rỗng "Không có tin nào khớp bộ lọc · Đặt lại mặc định".
- Bỏ CSS / JS cũ không dùng nữa (`job-posting-list.css` phần pager, select cũ).

**Xong khi:** test controller (view, model, 403 theo vai trò), test bộ lọc; xem trên trình duyệt với 4 tài khoản seed.

### Bước 3. Màn tạo / sửa tin

- Thẻ nguồn + "Đổi yêu cầu" (danh sách yêu cầu Approved, kèm "đã đăng N lần"); khi sửa thì không đổi được (§24.3).
- Các ô theo mục 1.2. Giá trị gốc của yêu cầu đưa vào `data-*` để JS hiện "Khôi phục bản gốc".
- Mức lương 3 lựa chọn → service ghép thành chuỗi `SalaryDisplay` (theo yêu cầu: "20 – 30 triệu/tháng", dùng `RequisitionLabels.salary`).
- Hạn nộp bắt buộc (D6), phải sau ngày đăng (`CK_JobPosting_Deadline`); cảnh báo (không chặn) khi sau ngày bắt đầu dự kiến của yêu cầu.
- Hẹn ngày đăng (nếu D1 = A): thêm ô thời điểm đăng; server kiểm tra > hiện tại và < hạn nộp.
- Cột xem trước + tab Soạn / Xem trước (JS, không gọi server).
- JS riêng `static/js/pages/job-posting-form.js` (quy tắc giống server, như `requisition-form.js`), CSS dùng token.
- Lỗi theo ô + khung tổng hợp; giữ dữ liệu khi lỗi; khóa nút khi đang gửi.
- Không hiển thị "Lý do tuyển dụng" ra nội dung công khai (§24.2).

**Xong khi:** validator / service test cho lưu nháp, đăng ngay, hẹn đăng, sửa tin đang đăng, sai hạn nộp; controller test lỗi theo ô.

### Bước 4. Màn chi tiết tin

- Đầu trang + nút theo trạng thái:

  | Trạng thái | Nút (HR / Admin) |
  |---|---|
  | Bản nháp | **Đăng tin** · Chỉnh sửa · Xóa bản nháp |
  | Hẹn đăng | **Đăng ngay** · Chỉnh sửa · Hủy lịch đăng (→ Bản nháp) |
  | Đang đăng | Xem trên trang tuyển dụng ↗ · Chỉnh sửa · Tạm dừng nhận hồ sơ · Đóng tin (HR) |
  | Tạm dừng | **Mở lại** · Chỉnh sửa · Đóng tin (HR) |
  | Đã đóng | **Đăng lại (tạo tin lần N+1)** |

  HM, Director: không có nút thao tác.
- Dải thông báo, 5 thẻ số liệu, thanh tiến trình hồ sơ (chỉ khi tin đã đăng).
- Tab Nội dung: tách fragment `job-postings/fragments/job-content.html` dùng chung với trang công khai (§25), phối hợp với DungLT.
- Tab Ứng viên: 7 hồ sơ mới nhất (tên, điểm AI, trạng thái, ngày nộp) + "Xem tất cả" → màn hồ sơ hiện có.
- Cột phải: Thông tin tin, Yêu cầu nguồn (+ link `/requisitions/{id}`), Các lần đăng, Lịch sử (`AuditLog`).
- Service mới: `publishNow`, `unschedule`, `repost` (D10); giữ khóa dòng (`findForUpdate`) và ghi `AuditLog` cho mọi thao tác.

**Xong khi:** test service cho từng chuyển trạng thái (hợp lệ / không hợp lệ / sai vai trò), test số liệu (đếm theo trạng thái), controller test các URL POST mới (CSRF, 403).

### Bước 5. Trang công khai và thông báo (phối hợp DungLT)

- Nếu D1 = A: `findPublishedJobs`, `findOpenPostings`, kiểm tra nộp hồ sơ (`ApplicationSubmissionService`) thêm `PostingDate <= now`, để tin hẹn đăng chưa lộ ra ngoài và chưa nộp được hồ sơ.
- Thông báo cho HM khi đăng tin (đã có): với tin hẹn đăng thì ghi rõ "sẽ đăng lúc …".
- Đường dẫn "Xem trên trang tuyển dụng" trỏ đúng trang chi tiết công khai.

**Xong khi:** integration test: tin hẹn đăng không xuất hiện ở trang công khai trước giờ đăng, xuất hiện sau giờ đăng.

### Bước 6. Ảnh nơi làm việc (chỉ khi D2 = A)

- Migration `009_job_posting_photo.sql` (bảng `JobPostingPhoto`: Id, JobPostingId, FilePath, Caption, SortOrder, CreatedAt) và cập nhật `db.sql`. **Bạn tự chạy migration.**
- Upload tối đa 3 ảnh, JPG / PNG / WebP, ≤ 5 MB, kiểm tra cả đuôi file lẫn nội dung ở server; lưu ngoài thư mục `static`.
- Hiển thị ở form, chi tiết nội bộ, trang công khai.

### Bước 7. Test, tài liệu, nghiệm thu

- Unit: bộ lọc theo vai trò, sort, nhãn, cờ thao tác, validator.
- Service: mọi chuyển trạng thái theo bảng ở Bước 4 (+ sai trạng thái, sai vai trò).
- Controller: GET 4 vai trò (200 / 403), POST thiếu CSRF (403), lỗi theo ô.
- Integration: đếm hồ sơ gộp, phạm vi HM / Director, hẹn đăng (nếu có).
- Postman: thêm thư mục "Job Posting" vào collection (HR tạo → đăng → tạm dừng → mở lại → (đóng hoặc xóa) → tự dọn).
- Tài liệu: `posting-review-guide.md` (nút → luồng chạy, trường → cột DB, giống cẩm nang requisition), cập nhật `checklist.md`, sửa `CLAUDE.md` §22 nếu chọn D1 = A.

---

## 4. Ngoài phạm vi / cần phối hợp

- **Trang việc làm công khai `/jobs`** thuộc DungLT: chỉ sửa truy vấn lọc (Bước 5) và tách fragment dùng chung (Bước 4) sau khi thống nhất.
- **`SecurityConfig`, `ApplicationRepository`** là file chung: báo nhóm trước khi sửa (D4, Bước 1).
- **Màn danh sách hồ sơ** (link "Xem ứng viên" / "Xem tất cả") dùng màn hiện có của nhóm, không làm mới.
- Tự động đóng tin khi hết hạn: không làm (danh sách chỉ hiện "quá hạn"; HR tự đóng).

---

## 5. Nghiệm thu chung

- [ ] 3 màn khớp mockup trên 4 vai trò (HR, HM, Director, Admin) với dữ liệu seed.
- [ ] Mọi thao tác kiểm tra quyền và trạng thái ở **server**, nút chỉ ẩn / hiện theo cờ do service tính.
- [ ] Không có truy vấn N+1 ở danh sách (đếm hồ sơ 1 câu cho cả trang).
- [ ] Không lộ tin nháp / hẹn đăng / tạm dừng / đã đóng ra trang công khai.
- [ ] Toàn bộ test job posting + requisition pass; collection Postman pass.
- [ ] `checklist.md` ghi lại từng bước đã xong.
