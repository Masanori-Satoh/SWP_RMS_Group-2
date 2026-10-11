# Worklog Iter 2 + Iter 3 — DucNM (P4, module `interview`)

- **Nhánh:** `feature/ducnm/iter2-interview_evaluation` (tách từ `main` @ `332b5b5`). **Chưa commit.**
- **Kế hoạch:** [`iter2-3-plan.md`](iter2-3-plan.md) · **Rủi ro conflict:** [`iter2-3-conflict-risk.md`](iter2-3-conflict-risk.md)

## 2026-10-11 — Interview Result Details (5.1.28) + Submit Evaluation Form (5.1.29)

### Phạm vi đã làm
| Màn hình | Route | Trạng thái |
|---|---|---|
| Interview Result Details | `GET /interviews/{id}/result` → `interview/result` | Xong |
| Submit Evaluation Form (pop-up) | `POST /interviews/{id}/evaluations` (JSON, AJAX) | Xong |
| Notifications View (5.1.39) | — | **Không làm**: `NotificationService` chưa có API chung (L9) |

### Quyết định áp dụng → nơi thực thi
| Mã | Nội dung | Code |
|---|---|---|
| D1' | Chỉ thành viên panel được nộp, mỗi người 1 phiếu | `InterviewEvaluationServiceImpl.java:47`, `:55`; `SecurityConfig.java:115` (Interviewer/HM/Director) |
| D2' | Nộp khi đã tới giờ bắt đầu, lịch chưa hủy | `InterviewEvaluationService.java:32-34` (`blockedReason`), dùng ở `InterviewEvaluationServiceImpl.java:50`, `InterviewResultServiceImpl.java:100` |
| D3' | Phiếu đầu tiên chuyển Scheduled/Rescheduled → Completed (kiểm `canTransitionTo` trước) | `InterviewEvaluationServiceImpl.java:73-81` |
| D4 | 3 điểm 1–5 bắt buộc, đề xuất `Hire/No_Hire/Consider`, text ≤ 2000 | `SubmitEvaluationRequest.java` |
| D5 | HR/HM/Director/SA xem tất cả; Interviewer chỉ lịch được gán, chỉ phiếu của mình tới khi có Final Result | `InterviewResultServiceImpl.java:43`, `:57-60`, `:67` |
| D6 | Không ghi `InterviewFinalResult`, chỉ hiển thị nếu có | `InterviewFinalResultRepository.findByScheduleId` (method mới) |
| D7 | Không đụng tab chết `/interviews/evaluation` | `interview/list.html` dòng 49 giữ nguyên |
| D8 | Không thêm UNIQUE vào DB, chặn trùng ở Service | `InterviewEvaluationServiceImpl.java:55` |
| D9 | Không cập nhật `Application.applicationStatus` | — |

### File
- **Mới:** `InterviewEvaluationRepository`, `EvaluationRecommendation` (enum, tên = giá trị DB), 3 DTO, `InterviewEvaluationException`, 2 service (interface + Impl), 2 controller, `interview/result.html`, `interview/evaluation-dialog.html`, `css/pages/interview-result.css`, `js/pages/interview-evaluation.js`, 4 file test.
- **Sửa (chỉ thêm):** `InterviewFinalResultRepository` (+`findByScheduleId(Long)`), `interview/list.html` (+link "Kết quả"), `SecurityConfig` (+2 dòng, đã duyệt).
- **Không đụng:** `GlobalExceptionHandler`, `db.sql`, migration, `notification/**`, `offer/**`, `candidate/**`, `dashboard/**`, layout, fragment dùng chung, sidebar.

### Kiểm tra
- Test mới: 30/30 pass (`InterviewEvaluationServiceTests` 7, `InterviewResultServiceTests` 7, `SubmitEvaluationRequestTests` 4, `InterviewResultWebTests` 12, gồm render Thymeleaf thật của `result.html` + dialog).
- `./mvnw test` toàn bộ: 574 test, 13 failure + 9 error. **Trùng khớp với `main` @ `332b5b5`** (đã chạy 9 class hỏng trên worktree sạch của `origin/main`: cũng 13 + 9), nên không có test nào hỏng do thay đổi này. Nguyên nhân có sẵn: test dùng DB thật với `ddl-auto=validate` báo `InterviewEvaluation.InterviewId` là `int` nhưng entity `InterviewSchedule.interviewId` là `Long`; vài test requisition lệch chuỗi tiếng Việt.
- **Chưa kiểm tra trên trình duyệt** (chưa chạy `spring-boot:run` với từng role).

### Điểm lệch / việc còn lại
1. `interview/list.html` (iter1): các nút cũ dùng `button button-secondary button-sm`, nhưng trang cũ chỉ nạp `interface.css` (có `.btn .btn-primary/.btn-secondary`), nên nút "Sửa lịch"/"Hủy lịch" đang **không có style**. Link mới dùng `btn btn-secondary`/`btn btn-primary` để hiển thị đúng. Sửa triệt để khi migrate trang sang `layout/internal`.
2. `InterviewSchedule.interviewId` kiểu `Long` trong khi DB là `INT`, làm hỏng các test `ddl-auto=validate` của nhóm. Sửa thì phải đổi entity mà `offer` và `dashboard` đang dùng, nên cần bàn với nhóm.
3. `InterviewFinalResultRepository.findByInterviewSchedule_InterviewId(Integer)` sai kiểu (Long). Hiện không ai gọi; giữ nguyên.
4. Controller iter1 còn `resolveEffectiveUser` (fallback lấy user HR khi chưa đăng nhập) và `IllegalArgumentException`/`EntityNotFoundException`, đúng như ARCHITECTURE_GUIDE mục 8 #2. Ngoài phạm vi iter này.
5. Notifications 5.1.39: cần HoangNH/Lead thêm một method chung trong `NotificationService` trước.
6. Báo LinhDN: D3' làm chỉ số "chờ đánh giá" trên dashboard bắt đầu có số liệu.

## 2026-10-11 (chiều) — Sửa 3 lỗi phát hiện khi chạy thử

| # | Phiên | Lỗi | Nguyên nhân | Cách sửa |
|---|---|---|---|---|
| B1 | HR | `/interviews/new?applicationId=<id>` (nút "Lên lịch phỏng vấn" ở `/applications/{id}`) không chọn sẵn hồ sơ | `showCreateForm` bỏ qua tham số `applicationId` | Nhận `@RequestParam(required = false) Integer applicationId` và gán vào request; `th:field` tự chọn option (`InterviewSchedulingController`) |
| B2 | HM | Không có chức năng chốt kết quả (InterviewFinalResult) | Chưa ai ghi bảng này | Nút "Chốt kết quả" + pop-up trên trang kết quả, `POST /interviews/{id}/final-result` (JSON) |
| B3 | Candidate | Menu "Lịch phỏng vấn của tôi" lỗi | **Không phải phân quyền**: `SecurityConfig` đã cho Candidate `GET /portal/interviews`, nhưng không có controller nào xử lý route này (404) | `CandidateInterviewController` + trang mới `interview/candidate-list.html` (layout/internal, chỉ dùng catalog). Không sửa `SecurityConfig` |

**Quyết định cho B2 (đã chốt với DucNM):**
- Người chốt: role `Hiring Manager` **và** quản lý phòng ban của yêu cầu tuyển dụng (dùng lại `RequisitionAccess.inManagedDepartment`, giống bước HM duyệt hồ sơ). Director/Admin không chốt.
- Chốt được kể cả khi hội đồng chưa nộp đủ phiếu (dialog hiện cảnh báo x/y phiếu). Lịch đã hủy hoặc chưa tới giờ thì không chốt được.
- Mỗi lịch chốt một lần, **không sửa**; phiếu đánh giá nộp muộn **vẫn nhận**.
- Lịch `Scheduled`/`Rescheduled` chuyển sang `Completed` khi chốt (GBR-01).
- **Không** cập nhật `Application.applicationStatus`. Kết quả `Passed` tự vào danh sách "ứng viên đạt" của HR (`OfferServiceImpl.getPassedCandidatesForOffer`).

**File:** mới `SubmitFinalResultRequest`, `InterviewFinalResultException`, `InterviewFinalResultService(+Impl)`, `InterviewFinalResultController`, `CandidateInterviewController`, `interview/final-result-dialog.html`, `interview/candidate-list.html`, test `InterviewFinalResultServiceTests`, `InterviewScheduleFormWebTests`. Sửa: `InterviewResultService.getResult(Long, User)` (trước nhận id + tên role), `InterviewResultDetailResponse` (+`canFinalize`, `finalizeBlockedReason`), `result.html`, `evaluation-dialog.html`, `SecurityConfig` (+2 dòng, tổng +4), `InterviewSchedulingController` (+B1). JS đổi thành `js/pages/interview-result.js` dùng chung cho cả hai pop-up (bỏ `interview-evaluation.js`).

**Phụ thuộc mới:** `interview → requisition` (chỉ gọi `RequisitionAccess`, không tạo vòng).

**Kiểm tra:** test module interview 60/60 pass. Toàn bộ suite 594 test, hỏng 13 + 9, đúng 9 class như `main`. Chưa thử trên trình duyệt.

## 2026-10-11 (tối) — Sửa B4: HR được tham gia hội đồng phỏng vấn

- **Hiểu lầm cũ:** yêu cầu "HR không được đặt lịch cho chính mình" bị hiểu thành "HR không được vào hội đồng", nên service chặn mọi tài khoản HR và form ẩn toàn bộ HR.
- **Luật đúng:** HR được tham gia hội đồng; HR đặt/sửa lịch **không tự thêm chính mình**, nhưng xếp được HR khác (HR Lan xếp HR Quang và ngược lại). Director vẫn được tự gán.
- **Sửa:**
  - `InterviewSchedulingServiceImpl`: bỏ 2 chỗ chặn `memberRole == HR` (tạo, sửa). Giữ chặn HR tự gán; khi **sửa** lịch, HR đã có sẵn trong hội đồng (do HR khác xếp) thì được giữ chỗ, chỉ chặn việc tự **thêm** mới.
  - `InterviewSchedulingController.prepareFormModel`: danh sách chọn gồm mọi tài khoản nội bộ Active (kể cả HR); chỉ ẩn chính HR đang thao tác, trừ khi đang sửa lịch mà mình đã có trong hội đồng (tránh bị gỡ ngầm khi lưu).
  - `form.html`: chỉ sửa dòng chú thích quy tắc (không thêm nút lọc "HR" vì phải chép inline style của trang cũ; tìm "hr" trong ô tìm kiếm vẫn lọc được).
  - `SecurityConfig`: thêm `ROLE_HR` vào rule `POST /interviews/*/evaluations` (HR trong hội đồng nộp được phiếu; Service vẫn kiểm tra tư cách thành viên).
- **Lỗi kèm theo đã sửa:** `InterviewScheduleRequest` gán cứng `RoleInPanel.HM` cho mọi người được chọn qua `interviewerIds`, nên đoạn suy vai trò trong Service không bao giờ chạy (Interviewer, Director cũng bị lưu là HM). Nay để trống vai trò, Service suy từ role tài khoản qua `defaultRoleInPanel`: HR → `HR`, Director → `Director`, Interviewer → `Interviewer`, còn lại → `HM`. Dữ liệu cũ đã lưu là `HM` **không** tự đổi.
- **Kiểm tra:** test module interview 63/63 pass (thêm: HR xếp HR khác, HR giữ chỗ khi sửa, HR không tự thêm khi sửa, form ẩn chính mình). Toàn bộ 597 test, hỏng 13 + 9 như `main`.
