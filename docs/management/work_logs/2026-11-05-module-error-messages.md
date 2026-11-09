# 05/11/2026 — Thông báo lỗi thao tác trong các module LinhDN

## Yêu cầu và phạm vi

- Người thực hiện: LinhDN.
- Xử lý danh sách lỗi người dùng cung cấp: tài khoản sai nhóm/không còn tồn tại, trạng thái tài khoản không hợp lệ, phòng ban không tồn tại, Talent Pool mất quyền trên bản ghi/dữ liệu đã bị xóa/gợi ý đã rút, lỗi AJAX và lỗi định dạng request.
- Phạm vi: Auth, Account nội bộ/ứng viên, Department, Potential Candidate/Talent Pool; Audit Log được người dùng bổ sung rõ trong cuộc trao đổi.
- Giữ nguyên quyền truy cập, CSRF và quy tắc Service. Không sửa nghiệp vụ Application Pipeline, Job Posting, Requisition, Interview hoặc Offer.
- Tuân thủ `docs/architecture/ARCHITECTURE_GUIDE.md` và `UI/UI_RULES.md`: nghiệp vụ ở module, Controller xử lý lỗi module; core mới chỉ chứa hỗ trợ kỹ thuật, không import module nghiệp vụ. Dùng alert/flash toast hiện có, không sửa layout, topbar, sidebar, CSS/JS dùng chung.

## Thay đổi

| Phần | Nội dung |
| --- | --- |
| Account | `AccountListService`/`AccountManagementService` thay `ResponseStatusException` cho tài khoản không khả dụng bằng `AccountOperationException extends BaseBusinessException`; giữ nguyên điều kiện chặn và thông báo tiếng Việt. Controller nội bộ/ứng viên bắt đúng exception module, quay lại danh sách kèm `errorMessage`; JSON giữ 404/409. |
| Department | Handler cục bộ trong `DepartmentController`: dữ liệu phòng ban không còn tồn tại khi mở/sửa/đổi trạng thái quay về danh sách kèm message. Lỗi field vẫn giữ cách xử lý form hiện có. |
| Talent Pool | Handler cục bộ trong `TalentPoolController`: giữ lý do cụ thể của `AccessDeniedException`, `ResourceNotFoundException` và lỗi module chưa được xử lý. Form quay về `/talent-pool` an toàn, tránh quay lại chính detail đã mất quyền/đã xóa; AJAX trả `ApiResponse(false, message)` với HTTP 400/403/404. Các kiểm tra quyền và trạng thái vẫn thực thi. |
| Request không hợp lệ | `ModuleRequestExceptionResolver` chỉ nhận lỗi binding, tham số thiếu/sai kiểu, JSON sai, file quá lớn, sai method/media type trên URL giới hạn. Không bắt lỗi DB, lỗi lập trình hoặc Exception chung. |
| Trình bày lỗi | `ModuleErrorResponse` dùng `ResponseEntity<ApiResponse>` cho AJAX, redirect 303 + FlashMap cho form; không dùng Referer hoặc return URL do người dùng truyền. |
| Template | Danh sách Account/Department truyền cả `errorMessage` vào flash toast sẵn có. Auth/Profile/Audit Log bổ sung alert dùng `th:text` để hiển thị message an toàn. |
| Tài liệu kiến trúc | Ghi phạm vi xử lý cụ thể ở mục 2 để phân biệt với quy tắc chung của module khác. |

### Danh sách URL giới hạn

`/admin/accounts`, `/admin/candidate-accounts`, `/admin/departments`, `/admin/audit-logs`, `/talent-pool`, `/profile`, `/register`, `/forgot-password`, `/reset-password`, `/login` và các đường dẫn con tương ứng. Prefix phải khớp ranh giới `/`, không áp dụng cho URL chỉ tình cờ bắt đầu bằng cùng chuỗi.

### Lỗi hệ thống và bảo mật

- Không sửa `GlobalExceptionHandler`, `ErrorPages`, `RmsErrorViewResolver` hoặc `SecurityConfig`.
- `GlobalExceptionHandler` tiếp tục bắt lỗi Controller/Service chưa xử lý, bao gồm lỗi hệ thống thật.
- `ErrorPages` vẫn là nguồn dựng trang lỗi; `RmsErrorViewResolver` tiếp tục phục vụ lỗi qua `/error` như Security filter/CSRF hoặc đường dẫn không có Controller.
- Không coi 403/404 là thành công: handler chỉ đổi cách hiển thị lỗi trong phạm vi đã chốt; thao tác bị từ chối không chạy tiếp, AJAX vẫn có mã lỗi.
- Lỗi auth theo bước OTP và lỗi field Department vẫn dùng handler chuyên biệt sẵn có, không bị resolver request bắt thay.

## Database

Không đổi schema, migration, seed hoặc dữ liệu DB.

## Kiểm chứng

- Thêm `ModuleErrorMessageWebTests`: chạy MVC, Security và Thymeleaf thật với service/repository mock. Kiểm tra cả đường nghiệp vụ, JSON, request lỗi, flash hiện trên danh sách, phạm vi module khác, lỗi hệ thống qua Global, sai quyền URL và thiếu CSRF.
- Cập nhật test Service Account sang exception nghiệp vụ, tiếp tục kiểm tra không lưu khi thao tác bị từ chối.
- Maven offline, Mockito 5.17.0 nạp bằng `-javaagent` từ dòng lệnh; không sửa POM: **BUILD SUCCESS, 135/135 test qua, 0 failure, 0 error, 0 skipped**.

| Lớp kiểm tra | Số ca qua |
| --- | --- |
| `ModuleErrorMessageWebTests` | 32 |
| `ErrorPagesTests` | 5 |
| `AccountManagementCoverageTests` | 37 |
| `AccountListCoverageTests` | 10 |
| `AccountActivationDepartmentTests` | 6 |
| `AccountListServiceTests` | 2 |
| `AccountManagementServiceTests` | 7 |
| `AccountEditWebTests` | 7 |
| `DepartmentEditWebTests` | 3 |
| `AdminCreateFormsWebTests` | 7 |
| `InterviewPotentialWebTests` | 13 |
| `AuthBotanicalWebTests` | 6 |

- Test xác nhận lỗi hệ thống thật tiếp tục qua Global → `error/500`; module khác vẫn qua Global → `error/404` hoặc `error/400`; sai quyền URL và thiếu CSRF vẫn 403, không gọi service thao tác.
- Test xác nhận sai method không chạy thao tác ghi, request AJAX trả JSON đúng status, context path được giữ, Referer bên ngoài không đổi được đích redirect.
- Lượt đầu có 4 assertion không qua trong test mới: fixture dùng HR gọi route chỉ HM được phép và stub Controller module khác chưa được import. Sửa fixture/đăng ký stub, giữ nguyên rule Security; lượt cuối 135/135 qua.
- Log kiểm chứng cục bộ: `target/module-error-message-tests.log`; không đưa vào Git.
- `git diff --check`: qua. Rà import của hai class core mới: không import module nghiệp vụ.
- Chưa chạy ứng dụng/DB thật hoặc tái hiện nhiều tab đồng thời.

## Ghi chú workspace

- Giữ nguyên thay đổi validation họ tên/SĐT từ lượt trước.
- Workspace xuất hiện các thay đổi/xóa tài liệu sơ đồ từ công việc khác trong lúc thực hiện; không khôi phục hoặc chỉnh các file đó.
