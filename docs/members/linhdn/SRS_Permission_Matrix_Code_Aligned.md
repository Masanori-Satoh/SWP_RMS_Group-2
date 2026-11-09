# SRS I.4.4 Permission Matrix — đối chiếu code (2026-11-07)

Nguồn code: `SecurityConfig`, `ApplicationAccess`, `RequisitionAccess`, `JobPostingAccess`, `InterviewFinalResultServiceImpl`, `MyApplicationService`, `TalentPoolService`.
SRS gốc (docx) không nằm trong repo; file này liệt kê dòng cần sửa trong I.4.4 để khớp code. Xem thêm SRS-05/SRS-10 trong `2026-11-06-srs-internal-consistency-review.md`.

## A. Lệch giữa matrix và code (cần sửa matrix hoặc code)

| # | Dòng matrix | Matrix ghi | Code đang làm | Đề xuất |
|---|---|---|---|---|
| 1 | Job posting — View internal details | Director = `No` | `GET /internal/job-postings`, `/*` cho HR, HM, **Director**; Director không thấy tin Draft (`JobPostingAccess.canView`) | Sửa Director thành `Restricted` (chỉ xem, ẩn Draft). Thêm footnote. |
| 2 | Interview — Record final result | Chỉ HM (`Restricted10`) | HM phụ trách phòng ban, **hoặc Director** khi vị trí tuyển là Hiring Manager (`requiresDirectorDecision`, SecurityConfig cho phép ROLE_DIRECTOR) | Thêm Director `Restricted10`; sửa footnote 10. |
| 3 | Application — Withdraw own (footnote 7) | Applied **hoặc AI_Screened** | `WITHDRAWABLE_STATUSES = {"Applied"}`; không còn trạng thái `AI_Screened` trong code (AI screening đã gỡ) | Sửa footnote 7: chỉ `Applied`. |
| 4 | Application — Hiring Manager review | Director `No`; footnote 6 "Director cannot perform ... Hiring Manager review" | `ApplicationAccess.canReview`: Director duyệt thay HM ở `HR_Passed`, mọi phòng ban | Chọn một: sửa matrix (Director `Restricted6`, duyệt thay HM) hoặc bỏ nhánh Director trong code. |
| 5 | Application — View applications and CVs (footnote 1) | HM chỉ thấy đơn `HR_Passed` trở đi hoặc từ Talent Pool | `canView`: HM thấy **mọi** đơn phòng mình, kể cả chưa qua HR (comment Q1.4) | Sửa footnote 1 (bỏ điều kiện HR_Passed cho phần Application) hoặc siết code. |
| 6 | Application — View applications and CVs, Interviewer `Restricted8` ("xem CV ứng viên") | Interviewer xem được CV | `/applications/**` chỉ HR/HM/Director; không thấy endpoint CV nào cho Interviewer | Cần kiểm tra thêm; nếu đúng thì sửa matrix (Interviewer `No`) hoặc thêm endpoint. |
| 7 | Application — HR screening | HR `Restricted6` | Đúng (HR chỉ duyệt `Applied`) | Không đổi. |

## B. Thiếu trong matrix (code có, matrix chưa có)

| Chức năng | Code | Đề xuất dòng mới |
|---|---|---|
| Quản lý tài liệu chính sách (Terms/Privacy) | `/admin/policy-documents` — System Admin | `Policy document / Create-edit` : Admin `Full`, còn lại `No` |
| Đọc Terms / Privacy Policy | `GET /terms`, `/privacy-policy` — permitAll | `Policy document / View`: tất cả `Full` kể cả Guest |
| Đồng ý chính sách | `POST /policy-consents` — chỉ Candidate | `Policy consent / Accept`: Candidate `Full`, còn lại `No` |
| Upload avatar | `POST /profile/avatar` | Gộp vào dòng "View / edit own profile" |
| Đóng tin từ Talent Pool | `/talent-pool/jobs/*/close` — HR | Đã có "Close (stop recruiting)" HR; không cần thêm |
| Candidate xem lịch phỏng vấn | `GET /portal/interviews` — Candidate; mọi method khác `denyAll` | Đã có (Restricted2); ghi rõ read-only |

## C. Đã khớp (không cần sửa)

- Admin: account, candidate account, department, API monitoring, audit log (`/admin/**` → System Admin); Admin không vào requisition/application/offer/interview.
- Requisition: tạo/copy HM + Director; sửa/xóa chỉ người tạo khi Draft/Rejected; rút chỉ người tạo khi Pending_Director; duyệt chỉ Director không phải người tạo; HR chỉ xem Approved; Director không xem Draft của người khác.
- Job posting: tạo/sửa/đăng/tạm dừng/đóng chỉ HR; HM chỉ thấy tin phòng mình, không thấy Draft.
- Talent pool: HR recommend/confirm/decline/resend; HM suggest + quyết định recommendation; sửa/xóa note chỉ tác giả (`TalentPoolService` so `author` với viewer).
- Interview: tạo/sửa HR + Director; xem lịch 4 vai trò nội bộ; nộp đánh giá theo panel.
- Offer: `/offers/**` HR + Director; approve/reject chỉ Director; export Excel HR + Director.
- Candidate: apply, `/portal/applications`, `/portal/profile`, `/portal/offers` chỉ ROLE_CANDIDATE.

## D. Cần kiểm tra ngoài matrix (có thể là lỗi code)

- (Đã kiểm tra: `/dashboard/admin` chỉ authenticated ở SecurityConfig nhưng `AdminDashboardService` gọi `DashboardAccess.requireAdmin`, nên không phải lỗi.)
- `JobPostingAccess.requireManage` ghi lỗi "HR hoặc Quản trị viên hệ thống" trong khi code chỉ cho HR — sửa message.
