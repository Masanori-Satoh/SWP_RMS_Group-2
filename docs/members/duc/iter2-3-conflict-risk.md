# Rủi ro conflict — Iter 2 + Iter 3 (DucNM)

> Lập ngày 2026-10-11, so với `origin/main` @ `332b5b5` và các nhánh remote chưa merge (sau `git fetch --prune`).
> Kế hoạch đi kèm: [`iter2-3-plan.md`](iter2-3-plan.md). **Trước khi mở PR, fetch lại và cập nhật bảng này.**

Nhánh remote còn commit chưa vào `main` và có chạm vùng liên quan:

| Nhánh | Người | Commit mới nhất | Chạm vùng liên quan |
|---|---|---|---|
| `feat/hoangnh-requisition-enhancements` | HoangNH | 2026-10-10 | `notification/NotificationService(+Impl)` (thêm `notifyJobPostingPublished`), `fragments/sidebar.html` (+3 dòng) |
| `feature/hoangnh/jobposting` | HoangNH | 2026-10-08 (22 commit, nền 10-07) | `SecurityConfig`, `db.sql`, migration `006`, `007`, cả module `notification`, `dashboard`, **`interview/list.html`, `interview/form.html`** (Việt hóa chữ) |
| `feat/huyenpt/inte2/director-offer-approval` | HuyenPT | 2026-10-09 | Không chạm vùng của mình |

Các nhánh của mình (`fix/ducnm/crud-interviews`, `feature/iter1-interview-schedule_duc`) không còn commit nào chưa vào `main`.

---

## 🔴 Tỉ lệ cao và conflict nhiều

Chỉ xảy ra nếu **làm ngoài kế hoạch**. Kế hoạch hiện tại đã né hết.

| Vùng | Với ai | Khi nào sẽ dính | Cách né (đã đưa vào plan) |
|---|---|---|---|
| `interview/list.html`, `interview/form.html` | **HoangNH** (`feature/hoangnh/jobposting` Việt hóa breadcrumb, nhãn trạng thái, form) | Nếu migrate `list.html` sang `layout:decorate` hoặc dọn inline style trong iter này: sửa gần như toàn file, đè lên các dòng HoangNH đã đổi | Chỉ **thêm** 1–2 link trên thẻ lịch; không động vào breadcrumb, nhãn trạng thái, `form.html`. Migrate trang để PR riêng, sau khi nhánh của HoangNH merge hoặc đóng |
| `notification/NotificationService` + `NotificationServiceImpl` | **HoangNH** (cả 2 nhánh đều sửa interface và impl) | Nếu làm phần tùy chọn Notifications 5.1.39 bằng cách thêm method mới: cả hai bên cùng append vào cuối interface/impl và test | **Bỏ** khỏi iter này (L9). Nếu bắt buộc phải làm thì nhờ HoangNH thêm một method chung trong PR của họ, mình chỉ gọi |
| `GlobalExceptionHandler` | **DungLT** (Lead) + mọi người | Nếu sửa handler `MethodArgumentNotValidException` để `/interviews/**` trả JSON | Không sửa; bắt `BindingResult` trong controller (L3) |

## 🟠 Tỉ lệ cao (nhưng ít dòng, dễ gỡ)

| Vùng | Với ai | Lý do | Cách xử lý |
|---|---|---|---|
| `core/config/SecurityConfig.java` | **DungLT** (chủ `core`), **HoangNH** (`jobposting` sửa khối `/dashboard`, `/notifications`, `/internal/job-postings`), và bất kỳ ai thêm route | File ai cũng chạm. Hunk của mình (+2 dòng trước rule ghi `/interviews/**`) không sát hunk của HoangNH, nên Git có thể tự merge được, nhưng vẫn có khả năng phải sửa tay | Diff tối thiểu, không format lại file. Báo DungLT trước khi mở PR. Khi conflict: giữ cả hai, kiểm tra **thứ tự** rule `POST /interviews/*/evaluations` vẫn nằm trước rule ghi chung |
| `database/migrations/` + `db.sql` + `docs/database/model.md` (chỉ khi duyệt D8) | **HoangNH** (`jobposting` đã dùng `006_requisition_code_and_round.sql`, `007_add_missing_requisition_and_offer_columns.sql`, và sửa `db.sql` khối `JobRequisition`) | Nội dung không đè nhau (khác bảng), nhưng **trùng số migration** rất dễ xảy ra. `main` hiện đã có hai file cùng số `005` | Nếu làm D8: đặt tên `008_interview_evaluation_unique.sql` hoặc hỏi nhóm số tiếp theo. Chỉ sửa khối `InterviewEvaluation` trong `db.sql` |
| `docs/management/WORK_LOG.md` | Cả nhóm | Bảng mục lục chung, ai cũng append cuối ngày | Theo `docs/README.md`: viết file `work_logs/` trước, pull rồi mới thêm dòng, conflict thì **Accept Both** |

> **Cập nhật 2026-10-11 (sau khi sửa B1–B3):** `SecurityConfig` nay +4 dòng (POST `/interviews/*/evaluations` và `/interviews/*/final-result`), vẫn cùng một hunk trước rule ghi `/interviews/**`. Nhánh `feature/hoangnh/jobposting` đã cũ, bỏ qua.

## 🟢 Tỉ lệ thấp

| Vùng | Với ai | Vì sao thấp | Lưu ý |
|---|---|---|---|
| `interview/entity/InterviewFinalResult`, `InterviewFinalResultRepository` | **HuyenPT** (`OfferServiceImpl`, `OfferServiceTests` dùng builder và 2 query) | Mình chỉ **thêm** 1 method repository, không sửa entity | Không đổi tên field, builder, method cũ (kể cả method `findByInterviewSchedule_InterviewId(Integer)` đang sai kiểu) |
| `interview/entity/InterviewEvaluation` | **LinhDN** (`DashboardMetricsRepository.pendingInterviewEvaluations` dùng `e.interviewSchedule`, `e.interviewer`, `e.evaluationId`) | Không sửa entity | Không phải conflict Git mà là **lệch logic**: phải theo D3' để chỉ số "chờ đánh giá" có nghĩa. Báo LinhDN biết khi chốt D3' |
| `requisition/service/RequisitionAccess` | **HoangNH** | Mình chỉ **gọi** `inManagedDepartment` (chốt kết quả), không sửa. Phụ thuộc mới `interview → requisition`, không tạo vòng | Nếu HoangNH đổi tên/chữ ký `inManagedDepartment` thì `InterviewFinalResultServiceImpl` sẽ lỗi biên dịch: báo HoangNH |
| `candidate/**` | **DungLT** (`ApplicationRepository.findInterviewEvents`, `ApplicationAccess.canScheduleInterview`, trạng thái `Interviewing`) | Mình chỉ đọc, không cập nhật trạng thái hồ sơ (D9) | Nếu sau này cần chuyển `Interviewing → ...` thì phải gọi service của DungLT |
| `fragments/layout/sidebars/*.html`, `fragments/sidebar.html` | **DungLT** (khung), **HoangNH** (`sidebar.html` +3 dòng) | Plan không thêm menu (trang vào từ danh sách lịch) | Nếu sau này thêm menu: chỉ append vào `sidebars/interviewer.html` |
| `templates/interview/result.html`, `evaluation-dialog.html`, `static/css/pages/interview-result.css`, `static/js/pages/interview-evaluation.js`, các class mới trong `interview/**` | — | File mới, chỉ mình sở hữu | — |
| `layout/*`, `components.css`, `tokens.css`, `UI_CATALOG.md`, `core/web/*` | DungLT | Không sửa | Nếu dialog cần style mới thì đặt ở `pages/interview-result.css`, không thêm vào `components.css` |

---

## Việc nên làm để giảm rủi ro

1. **Hỏi HoangNH** nhánh `feature/hoangnh/jobposting` còn định merge không. Nó có nền cũ (10-07) và chạm cả `interview/list.html` lẫn `SecurityConfig`. Nếu còn, nên chờ nó merge trước rồi mới thêm link vào `list.html`.
2. **Báo DungLT** trước PR: +2 dòng `SecurityConfig`, và (nếu có) migration D8.
3. **Báo LinhDN** về D3': chỉ số "chờ đánh giá" trên dashboard sẽ bắt đầu có số liệu.
4. Rebase hoặc merge `main` ngay trước khi mở PR, rồi chạy lại `./mvnw test`.
