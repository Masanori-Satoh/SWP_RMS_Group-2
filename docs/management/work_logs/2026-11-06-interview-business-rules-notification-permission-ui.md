# Báo cáo Thay đổi: Business rule, Notification, Validate & Phân quyền UI của Lịch phỏng vấn

- **Ngày thực hiện:** 2026-11-06
- **Người thực hiện:** ducnm
- **Nhánh (Branch):** `feat/ducnm/interview-br-notifi-validate-config`
- **Module phụ trách:** `interview` (và các điểm nối `notification`, `candidate.talentpool.service.RecruitmentEmailService`, `core.config.SecurityConfig`, `core.exception.GlobalExceptionHandler`)

---

## 1. Yêu cầu / Quyết định (Requirements & Decisions)

Xuất phát từ buổi rà soát luồng phỏng vấn (notification, business rule, error message, phân quyền) theo yêu cầu người dùng; các quyết định dưới đây đều đã được người dùng xác nhận trước khi code.

### 1.1. Sửa lỗi bắt buộc
1. **Director chốt được kết quả phỏng vấn cho vị trí Hiring Manager:** `SecurityConfig` trước đó chặn `POST /interviews/*/final-result` chỉ cho `ROLE_HIRING_MANAGER`, khiến Director (người đúng quyền chốt theo `InterviewFinalResultService.requiresDirectorDecision`) bị 403 ngay ở tầng Security. Nay mở thêm `ROLE_DIRECTOR`; ai đúng người chốt cho từng lịch cụ thể do Service tự kiểm tra (đã có test ở `InterviewFinalResultServiceTests`).
2. **Director gửi được email mời/đổi/hủy phỏng vấn:** `RecruitmentEmailService.actor()` trước đó chỉ cho `ROLE_HR`. Thêm overload `actor(String... allowedAuthorities)`; `sendInterview`/`resendInterview`/`sendInterviewRescheduled`/`sendInterviewCancelled` dùng `actor("ROLE_HR", "ROLE_DIRECTOR")`, còn `sendJobClosure`/`resendClosure` giữ nguyên chỉ HR.
3. **Không sửa được lịch đã Hoàn thành / Đã hủy:** thêm chặn đầu `updateSchedule` (`InterviewSchedulingServiceImpl`), ném `InterviewStatusException` nếu `interviewStatus` hiện tại là `Completed`/`Cancelled`.
4. **Sửa lịch đưa giờ bắt đầu về quá khứ:** `InterviewTimeValidationService.validateForUpdate` trước đó chỉ kiểm tra tương lai khi `targetStatus == Rescheduled` (client tự chọn trạng thái). Nay rule áp dụng vô điều kiện vì mọi lần sửa lịch đang hoạt động đều tự chuyển "Đã đổi lịch" (xem 1.2).
5. **`EntityNotFoundException` (jakarta.persistence) rơi vào lỗi 500 thay vì 404:** thêm `@ExceptionHandler` riêng trong `GlobalExceptionHandler` trả `ErrorPages.view(HttpStatus.NOT_FOUND, ...)`.

### 1.2. Bỏ ô chọn trạng thái trên form, tự suy trạng thái
- Form tạo/sửa (`interview/form.html`) không còn dropdown "Trạng thái buổi phỏng vấn"; `InterviewScheduleRequest.interviewStatus` không còn được Service đọc từ client.
- **Tạo lịch:** hồ sơ chưa có lịch hoạt động nào → `Scheduled`; hồ sơ đã có lịch `Scheduled`/`Rescheduled` → lịch mới tự `Rescheduled` (thêm repository `existsActiveScheduleForApplication`). Notification/email dùng đúng theo trạng thái thực tế (ngữ "Lịch phỏng vấn mới" hay "đã thay đổi").
- **Sửa lịch:** mọi lần lưu thành công một lịch đang `Scheduled` đều tự chuyển `Rescheduled` (không cần client chọn).
- **Chuyển "Đã hoàn thành":** bỏ rule cũ "phiếu đánh giá đầu tiên là chuyển Completed ngay". Rule mới (`InterviewEvaluationServiceImpl.submit`): chuyển `Completed` khi **đủ phiếu của cả hội đồng HOẶC đã qua giờ kết thúc buổi phỏng vấn** — theo quyết định của người dùng.
- Giữ nguyên rule: sau 24h kể từ giờ kết thúc, người chốt vẫn chốt được dù 0 phiếu (không thêm ràng buộc tối thiểu 1 phiếu).

### 1.3. Business rule bổ sung
- Buổi phỏng vấn phải dài **15 phút – 4 giờ** (`InterviewTimeValidationService.validateDuration`, áp dụng cả tạo và sửa).
- Trùng lịch kiểm tra theo **ứng viên** (mọi hồ sơ của cùng người), không chỉ theo từng `ApplicationId` riêng lẻ — thêm `InterviewScheduleRepository.countConflictingSchedulesForCandidate`.
- Địa điểm/liên kết phòng họp **bắt buộc điền** (`@NotBlank`); hình thức Online phải là liên kết `http://`/`https://` (thêm `@AssertTrue isLocationValidForFormat` trong `InterviewScheduleRequest`).
- Sửa luôn lỗi nhận diện link cũ (`#strings.startsWith(x, 'http')` nhận nhầm chuỗi như `httpabc`) tại `interview/list.html`, `interview/candidate-list.html`, `interview/result.html`.
- Quyết định **không làm** (đã hỏi và người dùng từ chối để tránh phức tạp hoá nghiệp vụ / ảnh hưởng demo): nhắc lịch tự động qua email/cron, giờ làm việc & thời gian báo trước tối thiểu, trạng thái "Không tham dự" riêng, file `.ics`.

### 1.4. Notification & Email
- **Hủy lịch:** chỉ báo **ứng viên** (in-app + email, có kèm lý do); không còn báo hội đồng nội bộ (lý do: nội bộ công ty tự báo nhau được). Thêm `NotificationMessages.interviewCancelledCandidateContent`, sửa `NotificationServiceImpl.notifyInterviewCancelled`.
- **Người bị gỡ khỏi hội đồng khi sửa lịch:** không cần thông báo riêng (HR tự báo trực tiếp).
- **Ứng viên không đạt kết quả phỏng vấn:** giữ nguyên hiện trạng — chỉ báo HR, không báo ứng viên qua email (ứng viên tự theo dõi trên hệ thống hoặc hỏi HR).
- Thêm email báo ứng viên khi **đổi lịch** (`RecruitmentEmailService.sendInterviewRescheduled`) và **hủy lịch** (`sendInterviewCancelled`), gửi sau khi transaction nghiệp vụ đã commit (gọi từ Controller, cùng khuôn với `sendInterview` cũ) — lỗi gửi mail không chặn thao tác của HR, chỉ hiện `errorMessage` phụ.

### 1.5. Phân quyền UI (không còn trang lỗi 403 toàn trang cho một số trường hợp)
- `GET /interviews/new`, `GET /interviews/{id}/edit`, `GET /interviews/{id}/result`: vai trò xem được danh sách lịch nhưng không đủ quyền thao tác (Hiring Manager, Interviewer, hoặc Interviewer ngoài hội đồng của buổi cụ thể) vẫn vào được trang — còn sidebar/topbar/tiêu đề — chỉ ẩn phần nội dung/thao tác riêng, thay bằng banner đỏ dùng chung "Bạn không có quyền truy cập vào trang này." (`fragments/ui/access-denied-banner.html`, tái dùng class `.adw-validation-summary` có sẵn).
- `SecurityConfig`: tách `POST /interviews/new`, `POST /interviews/*/edit` (vẫn chặn cứng HR/Director) khỏi `GET` cùng path (mở cho cả 4 vai trò nội bộ, để Controller tự quyết định hiện form hay banner).
- Các action ghi (POST) vẫn trả 403/redirect như cũ — banner chỉ áp dụng cho trang xem (GET).

---

## 2. Thay đổi Cơ sở dữ liệu (Database)

- **Cấu trúc bảng (Schema):** Không thay đổi.
- **Dữ liệu mẫu (Seeds):** Không thay đổi.
- Chỉ thêm 2 JPQL query mới trong `InterviewScheduleRepository` (`existsActiveScheduleForApplication`, `countConflictingSchedulesForCandidate`), không đổi cấu trúc cột.

---

## 3. Mã nguồn & Tài liệu (Source Code & Docs)

### 3.1. File tạo mới
- `src/main/resources/templates/fragments/ui/access-denied-banner.html`
- `docs/management/work_logs/2026-11-06-interview-business-rules-notification-permission-ui.md` (báo cáo này)

### 3.2. File chỉnh sửa (chính)
- `src/main/java/com/group2/rms/core/config/SecurityConfig.java`
- `src/main/java/com/group2/rms/core/exception/GlobalExceptionHandler.java`
- `src/main/java/com/group2/rms/interview/repository/InterviewScheduleRepository.java`
- `src/main/java/com/group2/rms/interview/service/InterviewTimeValidationService.java`
- `src/main/java/com/group2/rms/interview/service/InterviewSchedulingServiceImpl.java`
- `src/main/java/com/group2/rms/interview/service/InterviewEvaluationServiceImpl.java`
- `src/main/java/com/group2/rms/interview/dto/InterviewScheduleRequest.java`
- `src/main/java/com/group2/rms/interview/controller/InterviewSchedulingController.java`
- `src/main/java/com/group2/rms/interview/controller/InterviewResultController.java`
- `src/main/java/com/group2/rms/notification/service/NotificationMessages.java`
- `src/main/java/com/group2/rms/notification/service/NotificationServiceImpl.java`
- `src/main/java/com/group2/rms/candidate/talentpool/service/RecruitmentEmailService.java`
- `src/main/resources/templates/interview/form.html`, `interview/result.html`, `interview/list.html`, `interview/candidate-list.html`

### 3.3. Test chỉnh sửa / bổ sung
- `InterviewSchedulingServiceTests`: bỏ test rule cũ về trạng thái client chọn; thêm test tự suy `Rescheduled`, thời lượng 15p–4h, chặn sửa lịch Completed/Cancelled, chặn lùi quá khứ, auto-transition khi sửa.
- `InterviewEvaluationServiceTests`: thêm test "chưa đủ phiếu & chưa hết giờ → giữ Scheduled" và "đủ phiếu hội đồng → Completed dù chưa hết giờ".
- `InterviewResultWebTests`: `onlyHiringManagerMayFinalize` → `interviewerAndHrMayNotFinalize` (Director không còn bị chặn ở tầng Security); `interviewerOutsidePanelGets403` → `interviewerOutsidePanelSeesAccessDeniedBanner` (200 + banner thay vì 403).
- `InterviewScheduleFormWebTests`: tách `hiringManagerAndInterviewerStillCannotManageSchedules` thành `hiringManagerSeesAccessDeniedBannerInsteadOfForm` (GET /new nay 200 + banner) và `interviewerStillCannotManageSchedules` (POST vẫn 403).
- `NotificationServiceImplTests`: `notifyInterviewCancelled_shouldShowReasonOnlyToPanel` → `notifyInterviewCancelled_shouldNotifyCandidateOnlyWithReason` (chỉ ứng viên nhận, không còn báo hội đồng).

---

## 4. Kiểm thử (Testing)

1. **Test trực tiếp liên quan:** `mvn -Dtest='com.group2.rms.interview.**,com.group2.rms.notification.service.**,com.group2.rms.core.exception.**' test` → **BUILD SUCCESS**, toàn bộ pass.
2. **Full suite toàn repo:** `mvn test` → còn 3 cụm fail (`auth.service.PasswordRecoveryServiceTests` 7 test, `candidate.application.pipeline.controller.ApplicationPipelineWebTests` 7 test, `candidate.application.pipeline.repository.ApplicationPipelineQueryTests` 1 test) + `notification.integration.NotificationIntegrationTests` (2 test, lỗi CHECK/NOT NULL constraint DB).
3. **Xác minh các fail trên không do thay đổi lần này gây ra:** `git stash -u` toàn bộ thay đổi, chạy lại đúng 3 class fail trên baseline sạch → **fail với số lượng và tên test giống hệt** (same stack traces: cooldown theo thời gian thực của Auth, CHECK constraint `CK_Application_Status` của Application pipeline). Đã `git stash pop` khôi phục đầy đủ, `git status` xác nhận không mất file.
4. Chưa chạy thử bằng tay trên UI thật (browser) — chỉ kiểm chứng qua test tự động.

---

## 5. Vấn đề tồn đọng & Hướng phát triển (Pending Issues)

- 3 cụm test fail nêu ở mục 4 là lỗi môi trường/pre-existing (không thuộc phạm vi lần này), cần người phụ trách Auth/Application pipeline xử lý riêng.
- AJAX 403 (`/interviews/*/evaluations`, `/interviews/*/final-result`) vẫn trả HTML lỗi thay vì JSON khi bị `AccessDeniedException` — biết nhưng chưa sửa vì nằm ngoài phạm vi yêu cầu lần này.
- Banner "không có quyền" hiện chỉ áp dụng cho 3 trang của module `interview`; nếu muốn áp dụng toàn hệ thống cần một đợt rà soát riêng ở quy mô lớn hơn.
