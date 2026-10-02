# 2026-10-01 — Đại phẫu Kiến trúc: Package-by-Feature & Clean Docs

**Người thực hiện:** Nhóm 2 (Cập nhật bởi Dũng)

**Yêu cầu và quyết định:** 
- Chuyển đổi toàn bộ cấu trúc dự án từ Layered Architecture sang **Package-by-Feature** để dễ dàng scale và maintain (phẳng mặc định, phân lớp khi phình to).
- Tổ chức lại thư mục docs/ để chấm dứt tình trạng rác tài liệu và chống Git Conflict (Zero-Conflict) khi nhiều người cùng viết doc.
- Sửa lỗi mật khẩu khởi tạo trong script hạt giống.
- Cập nhật thêm cột cho bảng OfferProposal, InterviewFinalResult và JobRequisition.

**File code/tài liệu thay đổi:**
- Đã di chuyển toàn bộ Controller, Service, Repository, Entity, DTO vào các thư mục feature tương ứng (admin, auth, candidate, core, dashboard, interview, offer, requisition, user).
- Chuẩn hóa lại Naming Convention cho Controller và DTO. Đổi tên feature `application` thành `candidate` để tránh trùng lặp khái niệm với Spring Boot Application.
- Đã đưa toàn bộ DTOs vào chung thư mục `dto/` của từng feature, xóa các thư mục `request/`, `response/` dư thừa. Xóa toàn bộ file `.gitkeep` rác.
- Cập nhật Database Entity: 
  - Thêm thuộc tính `RecommendedSalary` cho `InterviewFinalResult`.
  - Fix `OfferStatus` của `OfferProposal` để phù hợp với swimlane mới (HR oriented).
  - Thêm `RequiredGender`, `ProbationDuration`, `WorkModel`, `Work location`, `ExpectedStartDate` cho `JobRequisition`.
- Cơ cấu lại `docs/`: Tạo các thư mục `management`, `architecture`, `database`, `tests`, và đặc biệt là `members/` chứa thư mục riêng của từng thành viên (hoangnh, linhdn, ducnm, huyenpt, dunglt).
- Gọt dũa lại `ARCHITECTURE_GUIDE.md` thành bản quy chuẩn tĩnh thuần túy.
- Biến root `README.md` thành một Navigation Hub dẫn link tới tất cả các tài liệu chuẩn.

**Kiểm tra:**
- Chạy `tree /f` để xác minh cây thư mục đã hoàn toàn chính xác theo mô hình mới.
- Đã chạy script quét tất cả file `.md` trong dự án để cập nhật lại mọi đường dẫn tham chiếu thư mục cũ sang cấu trúc mới -> PASS.
