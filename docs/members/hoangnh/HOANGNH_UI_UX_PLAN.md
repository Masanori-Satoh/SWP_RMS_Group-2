# KẾ HOẠCH NÂNG CẤP UI/UX CHO MODULE HOANGNH (REQUISITION & JOB POSTING)

> **Dự án:** Hệ thống Quản lý Tuyển dụng Mộc RMS (Recruitment Management System)  
> **Thành viên phụ trách:** Nguyễn Huy Hoàng (HoangNH)  
> **Tài liệu tham khảo:** UI/UX Pro Max Toolkit (`ui-ux-pro-max-skill-main`), `rule_code_nhh.md`, `checklist.md`  
> **Phạm vi áp dụng:** DUY NHẤT 6 màn hình thuộc Requisition Module và Job Posting Module.  
> **Ngày lập kế hoạch:** 08/10/2026  

---

## I. HIỆN TRẠNG VÀ CÁC VẤN ĐỀ UI/UX CẦN KHẮC PHỤC

1. **Tính nhất quán giữa 2 module của HoangNH:**
   - Màn hình Danh sách Yêu cầu (`requisitions/list.html`) và Danh sách Tin tuyển dụng (`job-postings/list.html`) còn một số khác biệt nhỏ về cấu trúc thanh lọc (Filter bar), bo góc thẻ card và bảng dữ liệu.
   - Cần đồng bộ thống nhất: Thẻ card bọc ngoài, thanh tìm kiếm + lọc + sắp xếp + phân trang + menu thao tác ba chấm (`⋮`).

2. **Quy tắc bố cục Form nhập liệu (Form Layout Rules):**
   - Biểu mẫu Requisition (`requisitions/form.html`) và Job Posting (`job-postings/create.html`) cần đảm bảo tuyệt đối:
     - **Tối đa 2 ô nhập liệu trên 1 hàng** (Không bao giờ đặt 3 ô/hàng).
     - Các trường văn bản dài (Mô tả công việc, Yêu cầu ứng viên, Quyền lợi, Lý do tuyển dụng) chiếm **100% chiều rộng** (full width).
     - Nhãn (Label) luôn hiển thị rõ ràng bên trên ô nhập liệu kèm ký hiệu bắt buộc (`*`).
     - Thông báo lỗi hiển thị ngay dưới trường dữ liệu bị sai.
     - Ô chỉ đọc (Read-only) phải có giao diện phân biệt rõ với ô được chỉnh sửa (Editable).

3. **Màn hình Tạo / Sửa Tin tuyển dụng (`job-postings/create.html`):**
   - Cần phân định rành mạch 2 phân vùng theo yêu cầu:
     - **SECTION A: THÔNG TIN YÊU CẦU TUYỂN DỤNG NGUỒN (Read-only):** Mã yêu cầu, Vị trí tuyển dụng, Phòng ban, Đợt tuyển dụng, Người tạo yêu cầu, Số lượng tuyển dụng, Hình thức làm việc, Mô hình làm việc, Ngày bắt đầu dự kiến, Địa điểm làm việc, Yêu cầu giới tính, Thời gian thử việc, Mức lương tối thiểu, Mức lương tối đa, Lý do tuyển dụng (chỉ HR xem nội bộ, không công khai).
     - **SECTION B: NỘI DUNG TIN TUYỂN DỤNG (Editable):** Tiêu đề tin, Mô tả công việc, Yêu cầu ứng viên, Quyền lợi, Mức lương hiển thị, Địa điểm làm việc, Hạn nộp hồ sơ.
     - **Nút hành động:** `Quay lại`, `Lưu nháp`, `Đăng tin tuyển dụng`.

4. **Thuật ngữ hiển thị (Terminology):**
   - 100% giao diện hiển thị bằng tiếng Việt chuẩn:
     - *Job Requisition* → **Yêu cầu tuyển dụng**
     - *Job Posting* → **Tin tuyển dụng**
     - *Recruitment Round* → **Đợt tuyển dụng** (TUYỆT ĐỐI KHÔNG DỊCH LÀ "Vòng phỏng vấn")
     - *Interview Round* → **Vòng phỏng vấn**
     - *Requester* → **Người tạo yêu cầu**
     - *Department* → **Phòng ban**
     - *Reason for Hiring* → **Lý do tuyển dụng**
     - *Screening Criteria* → **Tiêu chí sàng lọc**
     - *Application Deadline* → **Hạn nộp hồ sơ**

---

## II. DESIGN TOKENS ĐỀ XUẤT (THEO UI/UX TOOLKIT & NHẬN DIỆN MỘC RMS)

Dựa trên kết quả tra cứu từ bộ công cụ `ui-ux-pro-max-skill-main`:
- **Style Archetype:** *Minimalism & Swiss Style / Flat Design Enterprise* (Sạch sẽ, hướng dữ liệu, độ tương phản cao, thẻ card trắng, viền mảnh, không gradient màu tím AI, không đổ bóng lòe loẹt).
- **Typography:** *Inter* (Google Fonts), font-family chuẩn sans-serif, weight 400 (regular), 500 (medium), 600 (semi-bold), 700 (bold).
- **Hệ màu nhận diện Mộc RMS:**
  - Nền trang chủ đạo: Nền kem ấm / off-white (`#FAF8F5` hoặc `#F8FAF8`).
  - Màu thương hiệu chính (Primary Brand): Xanh lá đậm trầm (`#1B4D3E` / `#1E4D38`).
  - Màu tương tác hover: `#153E32`.
  - Màu điểm nhấn xanh lá (Accent Green): `#16A34A` / `#15803D`.
  - Nền badge trạng thái tích cực: `#DCFCE7` (chữ `#15803D`).
  - Nền badge bản nháp / phụ trợ: `#F1F5F9` (chữ `#475569`).
  - Thẻ Card: Trắng tinh (`#FFFFFF`), bo góc `10px - 12px`, viền `1px solid #E2E8F0`.
  - Chữ chính (Text Primary): `#0F172A`.
  - Chữ phụ (Text Muted): `#64748B`.
  - Nền trường Read-only: `#F8FAFC` kèm viền `#CBD5E1`.

---

## III. CHIẾN LƯỢC PHÂN TÁCH CSS / JS AN TOÀN (MODULE-SCOPED ISOLATION)

Để đảm bảo **TUYỆT ĐỐI KHÔNG ẢNH HƯỞNG ĐẾN GIAO DIỆN CỦA THÀNH VIÊN KHÁC**:
- Tất cả các CSS selector đều được bao bọc (scoped) dưới class cha của trang:
  - Module Requisition: `.req-app`, `.req-list-page`, `.req-form-page`, `.req-detail-page`.
  - Module Job Posting: `.jp-list-page`, `.jp-create-page`, `.jp-detail-page`.
- Không sử dụng các selector thẻ HTML trần như `button`, `table`, `input`, `.card` ở cấp global.
- Toàn bộ CSS được đặt trong các file chuyên biệt:
  - `static/css/requisition-shared.css`
  - `static/css/requisition-list.css`
  - `static/css/requisition-form.css`
  - `static/css/requisition-detail.css`
  - `static/css/job-posting-list.css`
  - `static/css/job-posting-create.css`
  - `static/css/job-posting-detail.css`
- Toàn bộ JS được đặt trong file chuyên biệt:
  - `static/js/requisitions.js`
  - `static/js/job-posting-create.js`
  - `static/js/job-posting-detail.js`

---

## IV. PHẠM VI TỆP TIN RÕ RÀNG

### 1. FILES TO MODIFY (Các file trong phạm vi HoangNH được phép tinh chỉnh UI/UX):
- `src/main/resources/templates/requisitions/list.html` (Màn hình Danh sách Yêu cầu tuyển dụng)
- `src/main/resources/templates/requisitions/form.html` (Màn hình Tạo / Sửa Yêu cầu tuyển dụng)
- `src/main/resources/templates/requisitions/detail.html` (Màn hình Chi tiết Yêu cầu tuyển dụng)
- `src/main/resources/templates/job-postings/list.html` (Màn hình Danh sách Tin tuyển dụng)
- `src/main/resources/templates/job-postings/create.html` (Màn hình Tạo / Sửa Tin tuyển dụng)
- `src/main/resources/templates/job-postings/detail.html` (Màn hình Chi tiết Tin tuyển dụng nội bộ)
- `src/main/java/com/group2/rms/requisition/dto/JobPostingCreateRequest.java` (Thêm trường read-only `workModel`, `reasonForHiring` để phục vụ hiển thị đầy đủ Section A)
- `src/main/java/com/group2/rms/requisition/service/JobPostingServiceImpl.java` (Map dữ liệu `workModel`, `reasonForHiring` vào form tạo tin)
- `src/main/resources/static/css/requisition-shared.css`
- `src/main/resources/static/css/requisition-list.css`
- `src/main/resources/static/css/requisition-form.css`
- `src/main/resources/static/css/requisition-detail.css`
- `src/main/resources/static/css/job-posting-list.css`
- `src/main/resources/static/css/job-posting-create.css`
- `src/main/resources/static/css/job-posting-detail.css`
- `src/main/resources/static/js/requisitions.js`
- `src/main/resources/static/js/job-posting-create.js`
- `src/main/resources/static/js/job-posting-detail.js`

### 2. FILES TO CREATE:
- `docs/HOANGNH_UI_UX_PLAN.md` (Tài liệu này)

### 3. FILES NOT TO TOUCH (TUYỆT ĐỐI KHÔNG ĐƯỢC CHẠM VÀO):
- `src/main/resources/static/css/global.css`
- `src/main/resources/static/css/app-layout.css`
- `src/main/resources/static/css/design-tokens.css`
- `src/main/resources/static/css/main.css`
- `src/main/resources/static/css/workspace.css`
- `src/main/resources/static/css/dashboard.css`
- `src/main/resources/static/css/offers.css`
- `src/main/resources/static/css/interview-list.css`
- `src/main/resources/static/css/interview-form.css`
- `src/main/resources/static/css/career-pages.css`
- `src/main/resources/static/css/job-board.css`
- `src/main/resources/static/css/profile.css`
- `src/main/resources/static/css/account-list.css`
- `src/main/resources/static/css/account-form.css`
- `src/main/resources/templates/fragments/head.html`
- `src/main/resources/templates/fragments/sidebar.html`
- `src/main/resources/templates/fragments/workspace-header.html`
- Toàn bộ templates thuộc: `candidate/`, `interview/`, `offers/`, `admin/`, `auth/`, `dashboard/`, `careers/`
- Cơ sở dữ liệu, SQL Server schema, migration scripts, repository interfaces, security configurations.

---

## V. THỨ TỰ TRIỂN KHAI THỰC HIỆN (EXECUTION SEQUENCE)

1. **Bước 1 — DTO & Service Data Support:**
   Cập nhật `JobPostingCreateRequest` và `JobPostingServiceImpl` bổ sung `workModel` và `reasonForHiring` từ Requisition nguồn phục vụ hiển thị Section A.
2. **Bước 2 — Requisition List (`requisitions/list.html` & `requisition-list.css`):**
   Tinh chỉnh thanh lọc tìm kiếm, bộ đếm kết quả, bảng dữ liệu, nút dropdown ba chấm `⋮` và phân trang.
3. **Bước 3 — Requisition Form (`requisitions/form.html` & `requisition-form.css`):**
   Kiểm soát nghiêm ngặt tối đa 2 trường/hàng, full width cho text dài, nhãn rõ ràng, hiển thị lỗi ngay dưới trường, style bảng tiêu chí sàng lọc động.
4. **Bước 4 — Requisition Detail (`requisitions/detail.html` & `requisition-detail.css`):**
   Đồng bộ giao diện chi tiết với thẻ card Mộc RMS, timeline, tiêu chí và lịch sử phê duyệt.
5. **Bước 5 — Job Posting List (`job-postings/list.html` & `job-posting-list.css`):**
   Đồng nhất layout thẻ card, thanh filter, bảng dữ liệu, badge trạng thái và phân trang với Requisition List.
6. **Bước 6 — Job Posting Form (`job-postings/create.html` & `job-posting-create.css`):**
   Tổ chức rõ ràng 2 phân vùng (Section A: Yêu cầu nguồn read-only, Section B: Nội dung tin tuyển dụng editable), tối đa 2 ô/hàng, 3 nút hành động chính.
7. **Bước 7 — Job Posting Detail (`job-postings/detail.html` & `job-posting-detail.css`):**
   Đảm bảo toàn bộ thuật ngữ tiếng Việt chuẩn hóa, thanh điểm nhấn xanh lá dọc, bảng Activity History và 3 thẻ tóm tắt.
8. **Bước 8 — Xác minh toàn diện:**
   Chạy build `mvnw test-compile`, chạy unit tests, kiểm tra rendered HTML, xác minh không ảnh hưởng tới bất kỳ màn hình nào của các thành viên khác.
