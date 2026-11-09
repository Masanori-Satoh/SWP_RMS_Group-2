# 2026-11-07 — Lịch phỏng vấn: gửi email ngầm, tự hoàn thành khi hết giờ, thay thế lịch cũ

## 1. Người thực hiện

Đức (ducnm)

## 2. Yêu cầu / Quyết định

| Chủ đề | Quyết định |
|---|---|
| Email tạo / đổi / hủy lịch | Gửi ngầm sau khi đã lưu DB, HR không phải chờ SMTP. Nút "Gửi lại Email" vẫn gửi trực tiếp để HR thấy kết quả ngay. |
| Gửi email thất bại | Không hiện trên màn hình HR nữa. Hệ thống ghi `MailFailed:*` vào AuditLog và ghi log; HR dùng "Gửi lại Email" ở trang kết quả phỏng vấn. |
| Hết giờ phỏng vấn | Lịch `Scheduled` / `Rescheduled` đã qua `EndTime` chuyển sang `Completed` ở đầu các luồng xem và ghi của module `interview`. Không dùng job chạy nền. |
| Tạo lịch khi hồ sơ đã có lịch | Lịch chưa bắt đầu chuyển `Cancelled`, lịch mới là `Rescheduled`. Mỗi hồ sơ chỉ có một lịch đang hoạt động. |
| Lịch cũ đang diễn ra | Không cho tạo lịch mới, báo lỗi "Hồ sơ này đang có buổi phỏng vấn diễn ra, chưa thể tạo lịch mới." |
| Thông báo khi thay lịch | Ứng viên và hội đồng mới nhận "Lịch phỏng vấn đã thay đổi"; ứng viên nhận thêm thư mời mới. Hội đồng của lịch cũ không được báo riêng, HR tự trao đổi. |

## 3. Database

Không đổi schema và seed. Khi chạy bản này, các lịch cũ đã qua giờ kết thúc sẽ chuyển sang `Completed` ngay lần đầu có người mở màn hình lịch phỏng vấn.

## 4. Mã nguồn / Tài liệu

**Mới**
- `core/config/AsyncConfig.java`: bật `@Async`, thread pool gửi email (2–4 luồng, hàng đợi 100; đầy thì luồng gọi tự gửi), chuyển tài khoản đăng nhập sang luồng nền.
- `core/config/AsyncConfigTests.java`.

**Sửa**
- `RecruitmentEmailService`: `@Async` cho `sendInterview`, `sendInterviewRescheduled`, `sendInterviewCancelled`.
- `InterviewSchedulingController`: bỏ bắt lỗi email ở 3 luồng trên.
- `InterviewScheduleRepository`: thêm `findActiveByApplicationId`, `completeEndedSchedules`; bỏ `existsActiveScheduleForApplication`.
- `InterviewSchedulingServiceImpl`: tự hoàn thành lịch hết giờ; hủy lịch cũ trong cùng transaction khi tạo lịch mới, lỗi ở bước sau sẽ rollback.
- `InterviewResultServiceImpl.getResult`: tự hoàn thành lịch hết giờ.
- `InterviewSchedulingServiceTests`: thêm test thay lịch cũ, chặn thay lịch đang diễn ra, tự hoàn thành trước khi đọc.

## 5. Kiểm tra (Testing)

- `mvn clean test` trên bản này: 2237 test, 19 failure, 42 error.
- Cùng lệnh trên `main` gốc (`0013bcdd`): 2234 test, 19 failure, 42 error. So danh sách theo tên test: 61 test fail giống hệt, không có test mới hỏng. 3 test tăng thêm là 3 test mới và đều đạt.
- Chưa kiểm tra trên trình duyệt và chưa gửi thử email qua SMTP thật.

## 5b. Sửa lỗi bổ sung: banner "không có quyền" hiện cả với HR / Director

- **Hiện tượng:** Director (và HR) mở `/interviews/new` thấy form nhưng vẫn có banner đỏ "Bạn không có quyền truy cập vào trang này." Trang `/interviews/{id}/result` cũng bị như vậy.
- **Nguyên nhân:** `th:if` và `th:replace` nằm trên cùng một thẻ. Thymeleaf chạy `th:replace` trước `th:if`, nên banner luôn được chèn và điều kiện `${accessDenied}` không có tác dụng (lỗi từ PR #158).
- **Sửa:** `interview/form.html` và `interview/result.html` chuyển điều kiện sang `<th:block th:if="${accessDenied}">` bọc ngoài `th:replace`.
- **Test:** thêm `InterviewScheduleFormWebTests.directorSeesFormWithoutAccessDeniedBanner`; HR không thấy banner ở `formWithoutApplicationIdHasNothingPreselected` và `InterviewResultWebTests.hrSeesAllEvaluationsWithoutEvaluateButton`. `mvn -Dtest='InterviewScheduleFormWebTests,InterviewResultWebTests' test`: đạt.

## 6. Vấn đề tồn đọng

- Timeline trên trang đơn ứng tuyển của ứng viên có thể vẫn ghi "Hẹn phỏng vấn" với lịch đã qua giờ cho tới khi lịch được chuyển trạng thái, vì trang này đọc DB trực tiếp và không kích hoạt việc tự hoàn thành.
- 61 test fail có sẵn trên `main`, phần lớn do DB local chưa khớp schema (thiếu giá trị mặc định `RequiredGender`, CHECK `ApplicationStatus` thiếu `Withdrawn` / `Cancelled`).
- Cập nhật SRS, SDS, Sequence Diagram và State Machine theo rule mới.
