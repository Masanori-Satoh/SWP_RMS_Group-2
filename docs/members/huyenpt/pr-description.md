# PR Description: Loại bỏ Luồng Đàm Phán Offer & Chuẩn Hóa Dữ Liệu Nghiệp Vụ (Remove Negotiation Flow)

- **Nhánh thực hiện (Branch):** `fix/huyenpt/inte1/remove-negotiation-flow`
- **Thành viên phụ trách (Assignee):** Phạm Thị Huyền (`HuyenPT`)
- **Phân hệ (Module):** Quản lý Đề xuất Tuyển dụng (`Offer Proposal`) & Tích hợp Hệ thống
- **Mục tiêu PR:** Loại bỏ hoàn toàn luồng đàm phán lương khỏi hệ thống theo yêu cầu nghiệp vụ mới; chuẩn hóa quy tắc tạo mới Offer `GBR-07`; sửa lỗi enum `RoleInPanel` gây lỗi 500; và bổ sung 10 ứng viên đỗ phỏng vấn chưa có Offer vào seed data để phục vụ kiểm thử tạo mới Offer.

---

## 📌 Tóm tắt các Thay đổi Cốt lõi (Summary of Key Changes)

### 1. Loại bỏ Hoàn toàn Luồng & Trạng thái Đàm phán (`Negotiating` / `OfferNegotiation`)
- **Cơ sở dữ liệu (Database Schema & Seeds):**
  - Xóa bỏ định nghĩa bảng `OfferNegotiation` trong schema (`database/schema/db.sql` và `db (1) (1).sql`).
  - Loại bỏ giá trị `Negotiating` khỏi ràng buộc `CHECK (OfferStatus IN (...))` của bảng `OfferProposal`.
  - Cập nhật `database/seeds/build_seed.py` và `database/seeds/seed_data.sql`: Xóa bỏ toàn bộ các câu lệnh `INSERT INTO OfferNegotiation`, chuyển đổi các bản ghi Offer mẫu từ trạng thái `Negotiating` sang các trạng thái hợp lệ (`Sent_Candidate`, `Accepted`, `Declined`).
- **Tầng Backend (Entity, Repository, DTO, Service):**
  - **Xóa vĩnh viễn** thực thể [OfferNegotiation.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/entity/OfferNegotiation.java) và repository [OfferNegotiationRepository.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/repository/OfferNegotiationRepository.java).
  - Cập nhật [OfferProposal.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/entity/OfferProposal.java): Loại bỏ trạng thái `Negotiating` khỏi Javadoc và danh sách giá trị trạng thái hợp lệ.
  - Cập nhật [OfferDetailResponse.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/dto/OfferDetailResponse.java): Loại bỏ trường `negotiationHistory` và inner class `NegotiationRound`.
  - Cập nhật [OfferServiceImpl.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java): Loại bỏ dependency `OfferNegotiationRepository`, xóa logic nạp lịch sử đàm phán trong `getOfferDetailById`, loại bỏ `Negotiating` khỏi tập `LOCKED_STATUSES`.
  - Cập nhật [DashboardMetricsRepository.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/dashboard/DashboardMetricsRepository.java) và [DashboardService.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/dashboard/DashboardService.java): Loại bỏ trạng thái `Negotiating` khỏi danh sách trạng thái ứng viên có thể nhìn thấy và switch-case ánh xạ nhãn hiển thị.
- **Tầng Giao diện Người dùng (Frontend):**
  - [list.html](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/offers/list.html): Xóa bỏ option "Đang đàm phán" trong dropdown lọc trạng thái và badge hiển thị trạng thái `Negotiating`.
  - [offers.js](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/js/offers.js): Xóa bỏ khối hiển thị "Lịch Sử Đàm Phán Lương" trong modal chi tiết Offer (Screen 32) và xử lý liên quan trong modal chỉnh sửa.
  - [offers.css](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/static/css/offers.css): Loại bỏ class `.status-negotiating`.

---

### 2. Chuẩn hóa Quy tắc Nghiệp vụ `GBR-07` & Lọc Danh sách Offer
- **Quy tắc Single Active Offer nghiêm ngặt:**
  - Mục tạo đề xuất Offer chỉ chấp nhận tạo mới cho ứng viên **đã đỗ phỏng vấn (`FinalDecision = 'Passed'`) và chưa từng có bất kỳ đề xuất Offer nào trước đó (kể cả bản `Draft`)**.
  - Tất cả các ứng viên đã có Offer trong hệ thống đều bị loại khỏi danh sách gợi ý trong API `/api/v1/hr/offers/passed-candidates`.
- **Cơ chế Lọc Toàn cục & Loại bỏ Nút "Xóa bộ lọc":**
  - Cả 3 bộ lọc (Từ khóa ứng viên, Trạng thái Offer, Sắp xếp) đều lọc chính xác trên toàn bộ tổng số bản ghi đề xuất hiện có (server-side query).
  - Loại bỏ hoàn toàn nút/tính năng "Xóa bộ lọc" theo yêu cầu trải nghiệm người dùng tinh gọn.

---

### 3. Sửa Lỗi Enum `RoleInPanel` (Khắc phục Triệt để Lỗi 500)
- **Vấn đề phát hiện:** Khi truy cập `/interviews` hoặc các trang có nạp hội đồng phỏng vấn, Hibernate ném lỗi `IllegalArgumentException: No enum constant ... RoleInPanel.Director` do enum Java trước đó chỉ có `HR` và `HM`, trong khi DB và seed data chứa `HR`, `HM`, `Interviewer`, `Director`.
- **Giải pháp:** Cập nhật [RoleInPanel.java](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/java/com/group2/rms/interview/entity/RoleInPanel.java) bổ sung đầy đủ 2 giá trị `Interviewer` và `Director`, đảm bảo khớp 100% với ràng buộc DB và dữ liệu mẫu.

---

### 4. Bổ sung 10 Ứng viên Đỗ Phỏng vấn (`Passed`) Chờ Tạo Mới Offer
- Cập nhật 10 cuộc phỏng vấn trong `seed_data.sql` (các Interview ID `1`, `2`, `5`, `7`, `9`, `10`, `11`, `13`, `14`, `16`) sang trạng thái `Passed` kèm mức lương đề xuất (`RecommendedSalary`) chuẩn định biên:
  1. **Nguyễn Duy Phong** (`ApplicationId: 1`) — *Senior Java Backend Engineer* — 36,000,000 VND
  2. **Phan Hải Ngọc** (`ApplicationId: 3`) — *QA Automation Engineer* — 22,000,000 VND
  3. **Đinh Văn Hiếu** (`ApplicationId: 23`) — *Frontend ReactJS Developer* — 26,000,000 VND
  4. **Phan Quốc Trung** (`ApplicationId: 28`) — *QA Automation Engineer* — 22,000,000 VND
  5. **Bùi Tiến Tuấn** (`ApplicationId: 33`) — *Chuyên Viên B2B Software* — 20,000,000 VND
  6. **Đỗ Thu Phương** (`ApplicationId: 36`) — *Chuyên Viên B2B Software* — 20,000,000 VND
  7. **Hoàng Thế Bình** (`ApplicationId: 37`) — *Frontend ReactJS Developer* — 26,000,000 VND
  8. **Dương Hoàng Trung** (`ApplicationId: 50`) — *Senior Java Backend Engineer* — 36,000,000 VND
  9. **Huỳnh Văn Toàn** (`ApplicationId: 51`) — *Chuyên Viên B2B Software* — 20,000,000 VND
  10. **Phạm Diệu Hà** (`ApplicationId: 58`) — *QA Automation Engineer* — 22,000,000 VND
- **Chuẩn hóa Encoding:** Dữ liệu nhận xét được nạp vào MS SQL Server bằng UTF-8 (`-f 65001`), khắc phục triệt để lỗi phông chữ / Mojibake trên giao diện modal tạo Offer.
- Cập nhật [build_seed.py](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/database/seeds/build_seed.py) đồng bộ logic để khi re-generate vẫn giữ nguyên 10 ứng viên này.

---

### 5. Việt hóa & Chuẩn hóa Sidebar (`sidebar.html`)
- Việt hóa toàn bộ nhãn điều hướng trên [sidebar.html](file:///c:/Users/Admin/Documents/Ky_5/SWP391/SWP_RMS_Group-2/src/main/resources/templates/fragments/sidebar.html) sang tiếng Việt thống nhất (Bảng điều khiển, Yêu cầu tuyển dụng, Ứng viên, Lịch phỏng vấn, Quản lý Đề xuất, Thông báo, Hồ sơ cá nhân, Quản lý tài khoản, Quản lý phòng ban, Giám sát API).
- Bổ sung menu Quản lý phòng ban (`/admin/departments`) cho System Admin.

---

## 📂 Danh sách Tệp tin Thay đổi (Files Changed)

| STT | Tệp tin | Thao tác | Mô tả thay đổi |
| :---: | :--- | :---: | :--- |
| 1 | `src/main/java/com/group2/rms/offer/entity/OfferNegotiation.java` | **Deleted** | Xóa bỏ entity đàm phán |
| 2 | `src/main/java/com/group2/rms/offer/repository/OfferNegotiationRepository.java` | **Deleted** | Xóa bỏ repository đàm phán |
| 3 | `src/main/java/com/group2/rms/offer/entity/OfferProposal.java` | Modified | Bỏ trạng thái `Negotiating` khỏi entity |
| 4 | `src/main/java/com/group2/rms/offer/dto/OfferDetailResponse.java` | Modified | Bỏ `negotiationHistory` và DTO liên quan |
| 5 | `src/main/java/com/group2/rms/offer/service/OfferServiceImpl.java` | Modified | Bỏ logic nạp đàm phán, cập nhật `LOCKED_STATUSES` |
| 6 | `src/main/java/com/group2/rms/dashboard/DashboardMetricsRepository.java` | Modified | Bỏ `Negotiating` khỏi query hiển thị ứng viên |
| 7 | `src/main/java/com/group2/rms/dashboard/DashboardService.java` | Modified | Bỏ nhãn `Negotiating` |
| 8 | `src/main/java/com/group2/rms/interview/entity/RoleInPanel.java` | Modified | Bổ sung enum `Interviewer`, `Director` khớp database |
| 9 | `src/main/resources/templates/offers/list.html` | Modified | Bỏ lọc trạng thái và badge `Negotiating` |
| 10 | `src/main/resources/static/js/offers.js` | Modified | Bỏ timeline đàm phán trong modal chi tiết Screen 32 |
| 11 | `src/main/resources/static/css/offers.css` | Modified | Xóa CSS class `.status-negotiating` |
| 12 | `src/main/resources/templates/fragments/sidebar.html` | Modified | Việt hóa thanh điều hướng và bổ sung link phòng ban |
| 13 | `database/schema/db.sql` | Modified | Xóa bảng `OfferNegotiation`, cập nhật check constraint |
| 14 | `database/schema/db (1) (1).sql` | Modified | Xóa bảng `OfferNegotiation`, cập nhật check constraint |
| 15 | `database/seeds/build_seed.py` | Modified | Loại bỏ đàm phán và giữ 10 ứng viên Passed chưa có Offer |
| 16 | `database/seeds/seed_data.sql` | Modified | Cập nhật 10 ứng viên Passed và loại bỏ đàm phán |
| 17 | `docs/database/model.md` | Modified | Cập nhật tài liệu mô hình dữ liệu (19 bảng) |
| 18 | `docs/members/huyenpt/offer-inter1-flow-testing.md` | Modified | Cập nhật kịch bản kiểm thử không còn đàm phán |
| 19 | `docs/members/huyenpt/pr-description.md` | Modified | Cập nhật tài liệu mô tả Pull Request |
| 20 | `src/test/java/com/group2/rms/service/OfferServiceTests.java` | Modified | Cập nhật các test case phù hợp với luồng mới |
| 21 | `src/test/java/com/group2/rms/requisition/validator/RequisitionValidatorTests.java` | Modified | Đồng bộ hằng số validator |

---

## 🧪 Kết quả Kiểm thử & Nghiệm thu (Test & Verification Results)

1. **Kiểm thử Tự động (Automated Test Suite):**
   ```powershell
   .\mvnw.cmd test "-Dtest=OfferServiceTests,InterviewSchedulingServiceTests"
   ```
   - **Kết quả:** `BUILD SUCCESS`, **67/67 tests PASS (100%)**, không có lỗi hay thất bại nào.

2. **Kiểm thử HTTP Thực tế trên Live Server (Tất cả phản hồi HTTP 200 OK):**
   - `GET /offers`: **200 OK** (Trang danh sách Offer hoạt động trơn tru, không còn lỗi 500).
   - `GET /api/v1/hr/offers/passed-candidates`: **200 OK** (Trả về chính xác danh sách **10 ứng viên** đỗ phỏng vấn chưa có Offer).
   - `GET /interviews`: **200 OK** (Khắc phục triệt để ngoại lệ Enum `RoleInPanel`).
   - `GET /dashboard`, `GET /requisitions`, `GET /`, `GET /admin/accounts`, `GET /profile`: **200 OK**.
   - Kiểm tra giao diện modal *"Tạo Mới Offer Proposal"*: Tên ứng viên, vị trí, phòng ban, lương gợi ý và nhận xét của Hội đồng hiển thị chuẩn Unicode tiếng Việt sắc nét.
