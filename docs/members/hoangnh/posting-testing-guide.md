# Hướng dẫn test module Tin tuyển dụng (Job Posting)

Cập nhật 2026-10-18, sau đợt làm sạch module theo `CLAUDE.md` (mục 27). Quy ước viết test: `CLAUDE.md` Phần E; cách viết từng tầng xem thêm `requisition-testing-guide.md` (cùng mẫu).

## 1. Lệnh chạy

```bash
# Toàn bộ test job posting (integration cần SQL Server đang chạy)
./mvnw -o clean test -Dtest='JobPosting*'

# Máy thiếu RAM ("The forked VM terminated" / "insufficient memory"): chạy từng nhóm, heap thấp, tắt jacoco
./mvnw -o clean test -Dtest='JobPosting*Tests' -Dsurefire.failIfNoSpecifiedTests=false \
  '-DargLine=-Xmx384m -XX:+UseSerialGC' -Djacoco.skip=true

# Một class / một hàm (class có @Nested thì thêm *)
./mvnw -o test -Dtest='JobPostingServiceTests*#deleteJobPosting_shouldAuditBeforeDelete'
```

Gặp "TestEngine ... failed to discover tests" hoặc "Unresolved compilation problems" → chạy lại với `clean` (VS Code cũng biên dịch vào `target/`).

## 2. Class test và tầng

| Class test | Tầng | Class được test | Spring / DB |
|---|---|---|---|
| `JobPostingAccessTests` | Unit — Access | `JobPostingAccess` (bảng role × trạng thái, HM phòng mình, D4 / D5 / D7 / D10) | Không / Không |
| `JobPostingValidatorTests` | Unit — Validator | `JobPostingValidator` (ô chữ, lương, hạn nộp, hẹn đăng, biên ngày theo `Clock.fixed`) | Không / Không |
| `JobPostingLabelsTests` | Unit — Labels | `JobPostingLabels` (nhãn, tông badge, nhóm hạn nộp, tên hoạt động lịch sử) | Không / Không |
| `JobPostingMapperTests` | Unit — Mapper | `JobPostingMapper` (cờ thao tác, dải thông báo, form tạo / sửa) | Không / Không |
| `JobPostingAuditLoggerTests` | Unit | `JobPostingAuditLogger` (snapshot, mô tả "cũ → mới", thời điểm theo Clock) | Không / Không |
| `JobPostingListServiceTests` | Unit — Service (Mockito) | `JobPostingListService` (ô lọc theo vai trò, làm sạch URL, chip, phân trang, chống N+1) | Không / Không |
| `JobPostingServiceTests` | Unit — Service (Mockito) | `JobPostingServiceImpl` (tạo, sửa, chuyển trạng thái, xóa, chi tiết, dashboard) | Không / Không |
| `JobPostingControllerTests` | Controller (`@WebMvcTest`) | `JobPostingController` (view / redirect / flash, `@Valid`, 403 / 404, CSRF, whitelist) | Web / Không |
| `JobPostingIntegrationTests` | Integration (`@SpringBootTest`) | Specification + JPQL mới trên SQL Server, render trang qua bảo mật thật | Đủ / **Có** |

Service test dựng service bằng tay: repository / `JobPostingAccess` / notification / talent pool là `@Mock`; `JobPostingValidator`, `JobPostingMapper`, `JobPostingAuditLogger` dùng bản thật với `Clock.fixed(2026-10-18 10:00 VN)`.

## 3. Hàm nào đã có test

| Hàm | Test (tên rút gọn) |
|---|---|
| `JobPostingAccess.viewStatus / isEditableStatus` | `viewStatus_shouldBeScheduled_whenPublishedInFuture`, `..._whenPostingDateIsNow` (biên), `isEditableStatus_shouldRejectOnlyClosed` |
| `JobPostingAccess.canManage / canRead / canCloseAny / canSeeTalentPool` | `canX_shouldFollowRoleTable` (5 vai trò) |
| `JobPostingAccess.canView / requireView` | `canView_shouldFollowRoleStatusAndDepartmentTable` (8 dòng), `requireView_shouldThrow_whenDirectorOpensDraft` |
| `JobPostingAccess.canPublish / canChangeSchedule / canReopen / canEdit / canPause / canClose / canDelete / canRepost` | `canX_shouldFollowStatusTable_whenViewerIsHr`, `canX_shouldBeFalse_whenViewerIsDirector`, `canDelete_...`, `canRepost_...` |
| `JobPostingValidator.normalizeAndValidate` | null / rỗng / khoảng trắng, `TITLE_LIMIT` và `+ 1`, `\r\n`, action lạ, lương ngoài danh mục (kể cả null, sai hoa thường), hạn nộp hôm qua / hôm nay, hẹn đăng đã qua / đúng 90 ngày / 91 ngày / sau hạn nộp, tin đã đăng không hẹn lại |
| `JobPostingValidator.requireApproved / requireEditable / requirePublishableContent / deadlineOf / defaultDeadline` | mỗi hàm ≥ 1 test |
| `JobPostingMapper.toListRow` | đang đăng (HR), hẹn đăng, nháp (Admin), tạm dừng (Admin không đóng), đã đóng, Director |
| `JobPostingMapper.toDetail / toActivityHistory` | dải sắp hết hạn / quá hạn / đã đóng / hẹn đăng, nhãn talent pool HR / HM, lương trống, thứ tự lịch sử |
| `JobPostingMapper.toCreateForm / toEditForm` | điền sẵn từ yêu cầu, không lương → thỏa thuận, sửa tin hẹn đăng (lương tự nhập, ẩn số lượng), cách viết cũ "Thoả thuận" |
| `JobPostingListService.getListPage` | 403, ô lọc HM / Director / Admin / HR, URL bị sửa, size 0, trang vượt tổng, chip + sort theo hồ sơ, đếm 1 câu cho cả trang, trang rỗng không đếm |
| `JobPostingServiceImpl.create*` | nháp, đăng ngay, hẹn đăng, không quyền, yêu cầu bị trả lại, đã có tin mở, hạn nộp quá khứ, jobPostingId giả |
| `JobPostingServiceImpl.update*` | sửa tin đang đăng (audit "cũ → mới"), **không đổi gì → không lưu / không audit**, hẹn đăng → đăng ngay, tạm dừng giữ / mở lại, tin đã đóng, requisitionId giả, 404 |
| `publishJobPosting / publishNow / unschedule / pauseJobPosting / closeJobPosting / deleteJobPosting` | thành công + trạng thái không hợp lệ (trạng thái giữ nguyên), quá hạn, chỉ HR đóng, audit trước khi xóa, có hồ sơ không xóa |
| `getInternalJobPostingDetail` | số liệu theo trạng thái + ngưỡng AI "70.00", hẹn đăng không đếm, 403, các lần đăng đếm gộp 2 câu |
| `prepareCreateForm / prepareRepostForm / prepareEditForm` | lần đăng kế tiếp, thiếu id / id không tồn tại, đăng lại chỉ từ tin đã đóng, sửa tin đã đóng |
| `requisitionChoices / getDashboard` | số lần đăng tra gộp (không N+1), đếm bằng SQL (không `findAll()`) |
| `JobPostingController` | danh sách HR / Director / chưa đăng nhập / ứng viên, HM POST bị 403, form tạo / sửa / đăng lại lỗi, lỗi theo ô, `@Size` chặn trước service, flash lưu nháp / hẹn đăng, sửa publish, thiếu CSRF, chi tiết (talent pool có / không), 404, 403, xóa, đăng lỗi, đóng tin + email |
| `JobPostingSpecifications` + JPQL mới | `JobPostingIntegrationTests`: Hẹn đăng / Đang đăng, nhóm hạn nộp, phạm vi HM, tìm phòng ban tiếng Việt + sort số hồ sơ, `findMaxPostingNoByRequisitionIds`, đếm dashboard, render list / detail / edit, HM mở tin nháp → 403 |

## 4. Kết quả lần chạy gần nhất (2026-10-18)

- Unit: 141 PASS. Controller: 23 PASS. Integration: 8 PASS.
- Test cũ đã thay thế: `JobPostingControllerTest`, `JobPostingServiceTest`, `JobPostingFormTests`, `JobPostingListTests`, `JobPostingDetailTests` (tên lẫn `*Test`, dùng `LocalDate.now()`, lỗi sẵn `create_salaryFromRequisition_andHiddenPositions` do lệch ngày).
- Lỗi thật test mới bắt được: lựa chọn lương `null` làm `List.of(...).contains(null)` ném `NullPointerException` (đã sửa trong validator).
