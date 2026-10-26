# BỘ DỮ LIỆU ĐẦU VÀO MẪU HỢP LỆ (TEST INPUT DATA)

> **Phụ trách:** Nguyễn Huy Hoàng (`hoangnh`)  
> **Phân hệ:** Job Requisition (Iter 1) & Job Posting (Iter 2)  
> **Mục đích:** Cung cấp sẵn các bộ dữ liệu đầu vào chuẩn chỉ, hợp lệ 100% theo các quy tắc Validation của hệ thống. Dễ dàng sao chép (copy-paste) trực tiếp vào giao diện web hoặc công cụ API (Postman/Curl) khi kiểm thử, demo với Giảng viên.

---

## 0. THÔNG TIN TÀI KHOẢN ĐĂNG NHẬP SẴN TRONG HỆ THỐNG

| Vai trò (Role) | Username | Password | Họ và tên | Phòng ban | Mục đích sử dụng |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Hiring Manager** | `hm_tuan` | `12345678` | Vũ Anh Tuấn | Engineering (ID: 1) | Tạo / Sửa / Rút Yêu cầu tuyển dụng kỹ thuật |
| **Hiring Manager** | `hm_huong` | `12345678` | Đỗ Thu Hương | Sales & Marketing (ID: 2) | Tạo / Sửa Yêu cầu tuyển dụng kinh doanh |
| **Director** | `director` | `12345678` | Nguyễn Thế Hoàng | Ban Giám đốc (HR) | Duyệt / Từ chối Yêu cầu tuyển dụng; tự tạo yêu cầu và Phê duyệt trực tiếp |
| **HR Specialist** | `hr_lan` | `12345678` | Lê Thị Mai Lan | Human Resources (ID: 3) | Soạn tin / Đăng tin / Quản lý Tin tuyển dụng |
| **System Admin** | `admin` | `12345678` | Trần Minh Đức | Ban Quản trị | Cấu hình hệ thống (tài khoản, phòng ban, giám sát API); không vào nghiệp vụ tuyển dụng |

---

## 1. FORM TẠO & CẬP NHẬT YÊU CẦU TUYỂN DỤNG (JOB REQUISITION)

* **URL:** `GET /requisitions/create` (Tạo mới) hoặc `GET /requisitions/{id}/edit` (Chỉnh sửa)
* **Quyền:** `Hiring Manager` (chỉ tạo cho phòng ban mình quản lý), `Director` (tự chọn phòng ban)
* **Quy tắc cốt lõi:**
  - **Lưu nháp (`action = "draft"`):** Chỉ bắt buộc Tên vị trí, Số lượng (1–30), Hình thức, Mô hình. Tiêu chí AI có thể bỏ trống hoặc nhập dở dang.
  - **Gửi duyệt (`action = "submit"`):** Bắt buộc điền đầy đủ các mục dấu `*`; Ngày bắt đầu ≥ Hôm nay; Tổng trọng số tiêu chí AI bắt buộc **tròn 100%**.
  - **Giám đốc tạo:** chọn Phòng ban (bắt buộc, cả khi lưu nháp); nút gửi là **"Phê duyệt trực tiếp"** → phiếu thành `Approved` ngay, HR tạo tin được luôn. Ví dụ: đăng nhập `director`, nhập Bộ dữ liệu 1, chọn phòng ban `Engineering`, bấm "Phê duyệt trực tiếp".

---

### BỘ DỮ LIỆU REQUISITION 1: Senior Java Backend Engineer (Gửi Giám đốc duyệt)

#### Thông tin chung:
* **Tên vị trí (`title`):** `Senior Java Backend Engineer (Spring Boot / Microservices)`
* **Số lượng cần tuyển (`numberOfPositions`):** `2`
* **Loại tuyển dụng (`hiringType`):** `New` (Mới)
* **Hình thức làm việc (`employmentType`):** `Full-time` (Toàn thời gian)
* **Mô hình làm việc (`workModel`):** `Hybrid` (Kết hợp văn phòng & từ xa)
* **Địa điểm làm việc (`workLocation`):** `Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội`
* **Yêu cầu giới tính (`gender`):** `Any` (Không yêu cầu)
* **Thời gian thử việc (`probationDays`):** `60` (ngày)
* **Ngày bắt đầu dự kiến (`expectedStartDate`):** *(Chọn ngày cách hôm nay ít nhất 15 - 30 ngày, ví dụ: `2026-11-15`)*
* **Mức lương tối thiểu (`minSalary`):** `30000000` *(giao diện hiển thị: `30.000.000`)*
* **Mức lương tối đa (`maxSalary`):** `45000000` *(giao diện hiển thị: `45.000.000`)*

#### Nội dung văn bản (Copy-paste nguyên đoạn):
* **Lý do tuyển dụng (`reasonForHiring`):**
```text
Dự án trọng điểm ERP Microservices cho khách hàng Nhật Bản chuẩn bị bước vào giai đoạn 2. Team Backend hiện tại đang bị quá tải tiến độ do khối lượng module tích hợp cổng thanh toán và xử lý luồng dữ liệu lớn gia tăng. Cần bổ sung gấp 2 kỹ sư cấp Senior dày dạn kinh nghiệm để dẫn dắt kỹ thuật và review kiến trúc.
```

* **Mô tả công việc (`jobDescription`):**
```text
- Thiết kế, phát triển và tối ưu hóa các module dịch vụ backend sử dụng Spring Boot 3 và Java 21.
- Xây dựng kiến trúc RESTful APIs, Message Queue (RabbitMQ/Kafka) đảm bảo hiệu năng cao, độ trễ thấp và khả năng mở rộng.
- Thiết kế và chuẩn hóa lược đồ cơ sở dữ liệu trên MS SQL Server / PostgreSQL; viết và tối ưu hóa các truy vấn phức tạp.
- Phối hợp chặt chẽ với Tech Lead, Product Owner và đội ngũ Frontend để phân tích yêu cầu nghiệp vụ và tích hợp hệ thống.
- Tham gia code review, hướng dẫn và cố vấn kỹ thuật cho các thành viên Junior trong nhóm.
```

* **Yêu cầu ứng viên (`requirementDetails`):**
```text
- Tối thiểu 4 năm kinh nghiệm phát triển ứng dụng web doanh nghiệp với ngôn ngữ Java và Spring Framework (Spring Boot, Spring Security, Spring Data JPA).
- Nắm vững kiến trúc Microservices, RESTful API, Docker và quy trình CI/CD cơ bản.
- Thành thạo thiết kế cơ sở dữ liệu quan hệ (SQL Server/MySQL/PostgreSQL), có kinh nghiệm giải quyết nghẽn hiệu năng (query optimization, indexing).
- Có kiến thức tốt về OOP, Design Patterns, Clean Architecture và nguyên lý SOLID.
- Kỹ năng giao tiếp và làm việc nhóm tốt; có tinh thần trách nhiệm cao và khả năng đọc hiểu tài liệu tiếng Anh chuyên ngành tốt.
```

#### Bảng tiêu chí sàng lọc AI (Screening Criteria) — Tổng trọng số: 100%:
| # | Tên tiêu chí (`criteriaName`) | Loại tiêu chí (`criteriaType`) | Giá trị yêu cầu (`requiredValue`) | Trọng số (`weight`) | Bắt buộc (`isMandatory`) |
| :---: | :--- | :--- | :--- | :---: | :---: |
| 1 | `Java Spring Boot` | `Skill` | `Thành thạo Spring Boot, Spring Data JPA, Microservices từ 3 năm trở lên` | `35` | Có (Check) |
| 2 | `Database & SQL Tuning` | `Skill` | `Thành thạo thiết kế CSDL quan hệ và tối ưu hóa truy vấn SQL` | `20` | Không |
| 3 | `Số năm kinh nghiệm` | `Experience` | `Từ 4 năm kinh nghiệm làm Java Backend Engineer` | `25` | Có (Check) |
| 4 | `Bằng cấp chuyên ngành` | `Education` | `Tốt nghiệp Đại học chuyên ngành CNTT, Phần mềm hoặc tương đương` | `10` | Không |
| 5 | `Tiếng Anh kỹ thuật` | `Skill` | `Đọc hiểu tốt tài liệu kỹ thuật tiếng Anh (tương đương TOEIC 600+)` | `10` | Không |
* **Thao tác:** Bấm **"Gửi Giám đốc duyệt"** (`action = submit`).

---

### BỘ DỮ LIỆU REQUISITION 2: Business Analyst (Lưu bản nháp - Draft)

> Mục đích: Kiểm thử tính năng Lưu nháp (chưa cần đủ tiêu chí AI, có thể bỏ qua một số mục không bắt buộc).

* **Tên vị trí (`title`):** `Senior Business Analyst (Fintech & Payment)`
* **Số lượng cần tuyển (`numberOfPositions`):** `1`
* **Loại tuyển dụng (`hiringType`):** `New`
* **Hình thức làm việc (`employmentType`):** `Full-time`
* **Mô hình làm việc (`workModel`):** `On-site`
* **Địa điểm làm việc (`workLocation`):** `Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội`
* **Mức lương tối thiểu (`minSalary`):** `25000000`
* **Mức lương tối đa (`maxSalary`):** `35000000`
* **Lý do tuyển dụng (`reasonForHiring`):** `Mở rộng bộ phận phân tích nghiệp vụ cho mảng cổng thanh toán.`
* **Mô tả công việc (`jobDescription`):** `Lấy yêu cầu từ khách hàng và viết tài liệu SRS, User Story.`
* **Yêu cầu ứng viên (`requirementDetails`):** `Có từ 3 năm kinh nghiệm BA mảng tài chính/ngân hàng.`
* **Tiêu chí AI:** Có thể nhập 1 dòng hoặc để trống hoàn toàn:
  - Tên: `Kinh nghiệm BA Fintech` | Loại: `Experience` | Giá trị: `Tối thiểu 3 năm` | Trọng số: `50`
* **Thao tác:** Bấm **"Lưu bản nháp"** (`action = draft`).

---

### BỘ DỮ LIỆU REQUISITION 3: Frontend Developer (Vue.js / React)

* **Tên vị trí (`title`):** `Frontend Developer (ReactJS / TypeScript)`
* **Số lượng cần tuyển (`numberOfPositions`):** `3`
* **Loại tuyển dụng (`hiringType`):** `New`
* **Hình thức làm việc (`employmentType`):** `Full-time`
* **Mô hình làm việc (`workModel`):** `Hybrid`
* **Địa điểm làm việc (`workLocation`):** `Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội`
* **Yêu cầu giới tính (`gender`):** `Any`
* **Thời gian thử việc (`probationDays`):** `60`
* **Ngày bắt đầu dự kiến (`expectedStartDate`):** `2026-12-01`
* **Mức lương tối thiểu (`minSalary`):** `18000000`
* **Mức lương tối đa (`maxSalary`):** `28000000`
* **Lý do tuyển dụng (`reasonForHiring`):**
```text
Cần tuyển bổ sung nhân sự Frontend nhằm nâng cấp toàn diện giao diện người dùng cho phiên bản Web App 2.0 theo chuẩn thiết kế Design System mới.
```
* **Mô tả công việc (`jobDescription`):**
```text
- Phát triển giao diện người dùng Responsive, tối ưu hóa trải nghiệm khách hàng (UI/UX) với React 18 và TypeScript.
- Chuyển đổi bản vẽ Figma thành các React Component tái sử dụng cao, tuân thủ chặt chẽ Design Tokens.
- Tích hợp RESTful APIs và WebSockets từ backend, quản lý State tập trung hiệu quả (Redux Toolkit/Zustand).
- Tối ưu hóa hiệu năng render trang web, giải quyết vấn đề Core Web Vitals và SEO on-page.
```
* **Yêu cầu ứng viên (`requirementDetails`):**
```text
- Từ 2 năm kinh nghiệm thực chiến với ReactJS, HTML5, CSS3/SCSS và Modern JavaScript/TypeScript.
- Hiểu biết vững chắc về State Management, React Hooks, Component Lifecycle.
- Có tư duy thẩm mỹ giao diện tốt, cẩn thận tỉ mỉ trong từng chi tiết pixel-perfect từ Figma.
- Ưu tiên ứng viên có kinh nghiệm với Next.js, TailwindCSS hoặc Ant Design.
```
* **Bảng tiêu chí sàng lọc AI (Tổng trọng số: 100%):**
  1. `ReactJS & TypeScript` | `Skill` | `Thành thạo ReactJS và TypeScript từ 2 năm trở lên` | `40%` | Bắt buộc
  2. `Figma to Code UI/UX` | `Skill` | `Khả năng cắt layout HTML/CSS chuẩn pixel-perfect từ Figma` | `20%` | Không
  3. `Số năm kinh nghiệm` | `Experience` | `Tối thiểu 2 năm kinh nghiệm Frontend Web` | `25%` | Bắt buộc
  4. `Tư duy logic & Clean code` | `Skill` | `Biết viết Clean code, nắm vững cấu trúc dữ liệu và giải thuật` | `15%` | Không

---

## 2. FORM PHÊ DUYỆT YÊU CẦU TUYỂN DỤNG (DIRECTOR APPROVAL)

* **URL:** `GET /requisitions/{id}` (Trang chi tiết)
* **Quyền:** `Director` (`director` / `12345678`)
* **Trạng thái phiếu để duyệt:** `Pending_Director`

### 2.1 Mẫu nhận xét khi PHÊ DUYỆT (Approve):
* **Hành động:** Bấm nút **"Phê duyệt"** (Approve)
* **Ghi chú phê duyệt (`feedback`):**
```text
Đồng ý phê duyệt yêu cầu tuyển dụng. Kế hoạch headcount và mức ngân sách đề xuất hoàn toàn phù hợp với định mức quý 4 của khối Kỹ thuật. Đề nghị bộ phận HR ưu tiên đăng tin và mở đợt ứng tuyển sớm để kịp tiến độ dự án.
```

### 2.2 Mẫu nhận xét khi TỪ CHỐI (Reject):
* **Hành động:** Bấm nút **"Từ chối"** (Reject)
* **Lý do từ chối (`feedback` — Bắt buộc):**
```text
Yêu cầu tuyển dụng tạm thời chưa được phê duyệt do mức ngân sách đề xuất (45 triệu) vượt quá trần phân bổ cho vị trí này trong quý 4. Đề nghị Hiring Manager rà soát lại định biên, cân nhắc điều chỉnh khoảng lương về mức 30 - 38 triệu hoặc giảm bớt số lượng tuyển để trình lại.
```

---

## 3. FORM TẠO & CẬP NHẬT TIN TUYỂN DỤNG (JOB POSTING)

* **URL:** `GET /internal/job-postings/create` (Tạo mới) hoặc `GET /internal/job-postings/{id}/edit` (Sửa tin)
* **Quyền:** `HR Specialist` (`hr_lan`)
* **Điều kiện tiên quyết:** Phải có ít nhất 1 Job Requisition ở trạng thái **`Approved`**.
* **Quy tắc cốt lõi:**
  - **Tiêu đề tin (`postingTitle`):** Bắt buộc, tối đa 200 ký tự.
  - **Mô tả công việc (`jobDescription`):** Bắt buộc, tối đa 5000 ký tự.
  - **Yêu cầu ứng viên (`jobRequirements`):** Bắt buộc, tối đa 5000 ký tự.
  - **Quyền lợi (`benefits`):** Không bắt buộc, tối đa 5000 ký tự.
  - **Mức lương hiển thị (`salaryMode`):** Chọn 1 trong 3:
    - `range`: Theo yêu cầu (VD: `30 – 45 triệu VND/tháng`)
    - `nego`: Thỏa thuận (Không hiện số)
    - `custom`: Tự nhập (`salaryCustom`: tối đa 100 ký tự)
  - **Số lượng tuyển (`showPositions`):** Bật checkbox thì ô `numberOfPositionsDisplay` bắt buộc nhập (tối đa 50 ký tự).
  - **Hạn nộp hồ sơ (`applicationDeadline`):** Bắt buộc khi Đăng tin; Phải từ hôm nay trở đi và sau Ngày đăng.
  - **Ngày đăng (`publishMode`):** `now` (Đăng ngay) hoặc `later` (Hẹn ngày đăng qua `scheduledAt`, trong vòng 90 ngày tới).

---

### BỘ DỮ LIỆU JOB POSTING 1: Đăng tin ngay (Publish Now)

#### Thông tin hiển thị:
* **Chọn Yêu cầu nguồn (`requisitionId`):** *(Chọn Requisition đã Approved trong dropdown)*
* **Tiêu đề tin tuyển dụng (`postingTitle`):**
```text
Senior Java Backend Engineer (Spring Boot / Microservices) — Thu Nhập Lên Đến 45 Triệu
```
* **Địa điểm làm việc (`workLocation`):**
```text
Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội
```
* **Mức lương hiển thị (`salaryMode`):** Chọn `range` (Theo yêu cầu: 30 – 45 triệu VND/tháng)
* **Hiển thị số lượng tuyển (`showPositions`):** Bật (Checked)
  - **Số lượng hiển thị (`numberOfPositionsDisplay`):** `02 Kỹ sư`

#### Nội dung tin:
* **Mô tả công việc (`jobDescription`):**
```text
- Trực tiếp tham gia thiết kế kiến trúc và phát triển hệ thống lõi Enterprise cho các khách hàng đối tác chiến lược.
- Xây dựng các RESTful microservices chuẩn mực với Java 21 và Spring Boot 3; tích hợp hệ thống Message Broker (RabbitMQ).
- Thiết kế mô hình dữ liệu, tối ưu hóa hiệu năng câu truy vấn trên hệ quản trị SQL Server quy mô lớn.
- Chủ động nghiên cứu, đề xuất áp dụng các công nghệ mới nhằm tối ưu hóa năng suất và tính ổn định của hệ thống.
- Cố vấn kỹ thuật, định hướng chuyên môn và thực hiện code review định kỳ cho các thành viên trong nhóm.
```

* **Yêu cầu ứng viên (`jobRequirements`):**
```text
- Có từ 4 năm kinh nghiệm làm việc thực tế với Java backend và Spring Framework (Spring Boot, JPA/Hibernate, Spring Security).
- Nắm chắc kiến trúc Microservices, Event-driven architecture, Docker và nắm vững nguyên lý thiết kế SOLID.
- Khả năng tư duy logic thuật toán tốt, có kinh nghiệm xử lý dữ liệu lớn (Big Data/High Concurrency).
- Tinh thần làm việc chủ động, trách nhiệm cao, có kỹ năng giải quyết vấn đề độc lập và làm việc nhóm linh hoạt.
- Tốt nghiệp Đại học chuyên ngành CNTT hoặc có chứng chỉ chuyên môn quốc tế tương đương.
```

* **Quyền lợi & Đãi ngộ (`benefits`):**
```text
- Thu nhập cạnh tranh từ 30.000.000 - 45.000.000 VNĐ/tháng (xét tăng lương định kỳ 2 lần/năm).
- Thưởng tháng lương thứ 13, thưởng hiệu quả dự án và thưởng các dịp lễ tết hấp dẫn.
- Chế độ bảo hiểm sức khỏe toàn diện cao cấp (Bảo Việt/PVI Care) dành cho nhân viên và người thân.
- Trang bị đầy đủ máy tính xách tay cấu hình cao (MacBook Pro / Dell XPS) và màn hình phụ 27 inch 4K.
- Hỗ trợ chi phí học tập và thi các chứng chỉ công nghệ quốc tế (AWS, OCP, CKAD) lên tới 100%.
- Tham gia du lịch nghỉ dưỡng hàng năm (Company Trip 5 sao), hoạt động Team Building và teabreak hoa quả hàng tuần.
- Mô hình làm việc Hybrid linh hoạt (hỗ trợ 2 ngày làm việc từ xa mỗi tuần).
```

#### Thời gian nhận hồ sơ:
* **Ngày đăng (`publishMode`):** Chọn `now` (Đăng ngay khi bấm "Đăng tin")
* **Hạn nộp hồ sơ (`applicationDeadline`):** *(Chọn ngày cách hôm nay 30 - 45 ngày, ví dụ: `2026-11-30`)*
* **Thao tác:** Bấm **"Đăng tin"** (`action = publish`).

---

### BỘ DỮ LIỆU JOB POSTING 2: Hẹn ngày đăng (Publish Later / Scheduled)

#### Thông tin hiển thị:
* **Tiêu đề tin tuyển dụng (`postingTitle`):**
```text
Chuyên Viên Phân Tích Nghiệp Vụ (Business Analyst) — Mảng Fintech & Ngân Hàng
```
* **Địa điểm làm việc (`workLocation`):**
```text
Tầng 8, Tòa nhà RMS Tower, Phố Duy Tân, Cầu Giấy, Hà Nội
```
* **Mức lương hiển thị (`salaryMode`):** Chọn `custom` (Tự nhập)
  - **Mức lương tự nhập (`salaryCustom`):** `25.000.000 – 35.000.000 VNĐ + Thưởng KPI dự án`
* **Hiển thị số lượng tuyển (`showPositions`):** Tắt (Unchecked — không công khai số lượng)

#### Nội dung tin:
* **Mô tả công việc (`jobDescription`):**
```text
- Tiếp nhận, khảo sát và phân tích yêu cầu nghiệp vụ từ khách hàng và các bên liên quan trong lĩnh vực tài chính, thanh toán điện tử.
- Viết tài liệu đặc tả yêu cầu phần mềm (SRS), tài liệu phân tích nghiệp vụ (BRD), User Stories kèm tiêu chí nghiệm thu (Acceptance Criteria).
- Xây dựng bản vẽ mô hình quy trình (BPMN), sơ đồ luồng dữ liệu (Data Flow) và Wireframe phác thảo giao diện.
- Làm cầu nối truyền tải thông tin giữa khách hàng, đội ngũ phát triển (Dev) và kiểm thử (QA/QC).
```
* **Yêu cầu ứng viên (`jobRequirements`):**
```text
- Từ 3 năm kinh nghiệm ở vị trí IT Business Analyst, ưu tiên ứng viên có kinh nghiệm trong dự án Fintech, Core Banking hoặc Ví điện tử.
- Kỹ năng phân tích, mô hình hóa quy trình và tư duy logic sắc bén.
- Sử dụng thành thạo các công cụ: Jira, Confluence, Draw.io/Visio, Figma (cơ bản).
- Khả năng giao tiếp, thuyết trình và đàm phán xuất sắc; tiếng Anh giao tiếp và viết tài liệu tốt.
```
* **Quyền lợi (`benefits`):**
```text
- Mức lương cạnh tranh theo năng lực, thưởng hiệu quả kinh doanh cuối năm từ 2 - 4 tháng lương.
- Gói bảo hiểm sức khỏe VIP, khám sức khỏe tổng quát định kỳ hàng năm tại bệnh viện quốc tế.
- Lộ trình thăng tiến rõ ràng lên vị trí Lead BA hoặc Product Owner.
- Môi trường làm việc năng động, tôn trọng ý kiến cá nhân và khuyến khích sáng tạo đổi mới.
```

#### Thời gian nhận hồ sơ:
* **Ngày đăng (`publishMode`):** Chọn `later` (Hẹn ngày đăng)
  - **Thời điểm đăng (`scheduledAt`):** *(Chọn một thời điểm cách hiện tại 3 ngày vào lúc 08:30 sáng, ví dụ: `2026-10-21T08:30`)*
* **Hạn nộp hồ sơ (`applicationDeadline`):** *(Chọn sau ngày đăng ít nhất 15 ngày, ví dụ: `2026-11-25`)*
* **Thao tác:** Bấm **"Lên lịch đăng"** (`action = publish`).

---

### BỘ DỮ LIỆU JOB POSTING 3: Lưu nháp tin tuyển dụng (Draft)

* **Tiêu đề tin tuyển dụng (`postingTitle`):**
```text
[Bản nháp] Thực Tập Sinh Lập Trình Web (Java / ReactJS Internship)
```
* **Địa điểm làm việc (`workLocation`):** `Hà Nội`
* **Mức lương hiển thị (`salaryMode`):** Chọn `nego` (Thỏa thuận)
* **Mô tả công việc (`jobDescription`):**
```text
Đang soạn thảo mô tả công việc chi tiết cho kỳ thực tập mùa đông. Tham gia hỗ trợ các dự án phần mềm thực tế của công ty.
```
* **Yêu cầu ứng viên (`jobRequirements`):**
```text
Sinh viên năm cuối hoặc mới tốt nghiệp chuyên ngành CNTT, có kiến thức cơ bản về Java hoặc JavaScript.
```
* **Quyền lợi (`benefits`):**
```text
Hỗ trợ phụ cấp thực tập 5.000.000 VNĐ/tháng, cơ hội trở thành nhân viên chính thức sau 3 tháng.
```
* **Hạn nộp hồ sơ:** *(Có thể để trống khi lưu nháp)*
* **Thao tác:** Bấm **"Lưu nháp"** (`action = draft`).

---

## 4. DỮ LIỆU CÁC THAO TÁC QUẢN LÝ TIN (ACTION DIALOGS)

### 4.1 Đóng tin tuyển dụng (`POST /internal/job-postings/{id}/close`):
* **Điều kiện:** Tin đang ở trạng thái `Published` hoặc `Paused`.
* **Quyền:** Chỉ `HR` (`hr_lan`).
* **Lý do đóng tin (Gợi ý điền vào hộp thoại xác nhận):**
  - Mẫu 1: `Đã tuyển dụng đủ số lượng nhân sự chất lượng cao theo đúng yêu cầu đề ra.`
  - Mẫu 2: `Tạm dừng đợt tuyển dụng do khách hàng điều chỉnh lại kế hoạch triển khai dự án sang quý 1 năm sau.`

### 4.2 Tạm dừng tin tuyển dụng (`POST /internal/job-postings/{id}/pause`):
* **Điều kiện:** Tin đang ở trạng thái `Published`.
* **Mục đích:** Tạm dừng hiển thị trên trang việc làm công khai để xử lý lượng hồ sơ đã nhận quá tải.

### 4.3 Mở lại tin tuyển dụng (`POST /internal/job-postings/{id}/publish`):
* **Điều kiện:** Tin đang ở trạng thái `Paused`.
* **Mục đích:** Cho phép ứng viên tiếp tục nộp CV nếu hạn nộp hồ sơ vẫn còn hiệu lực.

---

## 5. DỮ LIỆU ĐẦU VÀO CHO BỘ LỌC & TÌM KIẾM (SEARCH & FILTER)

### 5.1 Tìm kiếm danh sách Yêu cầu tuyển dụng (`/requisitions`):
| Tiêu chí lọc | Giá trị hợp lệ mẫu | Ghi chú |
| :--- | :--- | :--- |
| **Từ khóa (`keyword`)** | `Java`, `Engineer`, `Backend`, `BA` | Tìm trong tiêu đề vị trí và mã yêu cầu |
| **Phòng ban (`departmentId`)** | `1` (Engineering), `2` (Sales & Marketing), `3` (HR) | Danh mục phòng ban đang Active |
| **Hình thức (`employmentType`)**| `Full-time`, `Part-time`, `Internship`, `Contract` | Theo danh mục chuẩn |
| **Trạng thái (`status`)** | `Draft`, `Pending_Director`, `Approved`, `Rejected` | HR chỉ thấy `Approved` |
| **Phân trang (`page` & `size`)**| `page=0&size=10` | Mặc định 10 dòng/trang |

### 5.2 Tìm kiếm danh sách Tin tuyển dụng (`/internal/job-postings`):
| Tiêu chí lọc | Giá trị hợp lệ mẫu | Ghi chú |
| :--- | :--- | :--- |
| **Từ khóa (`keyword`)** | `Senior Java`, `Fintech`, `Engineer` | Tìm trong tiêu đề tin và mã yêu cầu |
| **Trạng thái (`status`)** | `Draft`, `Scheduled`, `Published`, `Paused`, `Closed` | Dropdown trạng thái tin |
| **Phòng ban (`departmentId`)** | `1`, `2`, `3` | Lọc theo phòng ban nguồn |
| **Hình thức (`employmentType`)**| `Full-time`, `Part-time`, `Internship` | |
| **Sắp xếp (`sort`)** | `newest`, `oldest`, `deadline_asc`, `deadline_desc` | Whitelist chuẩn trong code |

---

## 6. DANH SÁCH GIỚI HẠN DỮ LIỆU CẦN LƯU Ý (CHEAT SHEET VALIDATION)

```text
┌───────────────────────────────┬────────────────────────────┬─────────────────────────────┐
│ TRƯỜNG DỮ LIỆU                │ GIỚI HẠN TỐI ĐA (MAX)      │ QUY TẮC ĐẶC BIỆT            │
├───────────────────────────────┼────────────────────────────┼─────────────────────────────┤
│ Tiêu đề (Requisition / Posting)│ 200 ký tự                  │ Không được để trống         │
│ Số lượng tuyển (Headcount)    │ 1 đến 30 người             │ Số nguyên dương             │
│ Ngày bắt đầu dự kiến          │ Trong vòng 1 năm tới       │ Phải từ hôm nay trở đi      │
│ Mức lương (VND/tháng)         │ Tối đa 1.000.000.000       │ Min <= Max, tối đa 2 số thập│
│ Thời gian thử việc            │ 1 đến 180 ngày             │ Theo BLLĐ 2019              │
│ Mô tả / Yêu cầu / Lý do       │ 2.000 ký tự (Requisition)  │ Đổi \r\n thành \n           │
│ Mô tả / Yêu cầu / Quyền lợi   │ 5.000 ký tự (Job Posting)  │ Phù hợp hiển thị bài viết   │
│ Tiêu chí AI (ScreeningCriteria)│ Tối đa 50 tiêu chí         │ Tổng trọng số PHẢI = 100%   │
│ Hẹn ngày đăng (ScheduledAt)   │ Trong vòng 90 ngày tới     │ Phải sau thời điểm hiện tại │
│ Hạn nộp hồ sơ                 │ Cuối ngày đã chọn 23:59:59 │ Bắt buộc sau Ngày đăng      │
└───────────────────────────────┴────────────────────────────┴─────────────────────────────┘
```
