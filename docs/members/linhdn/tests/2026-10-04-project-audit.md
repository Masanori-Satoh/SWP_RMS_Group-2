# Test project trước Candidate UI — 04/10/2026

## Phạm vi và điều kiện

- Ghi lại audit hiện tại và các bước retest sau khi refactor; chưa sửa production code.
- Source thay đổi trong lượt audit. Kết quả đầu lượt và trạng thái cuối phải phân biệt.
- Không chạy schema drop/create, seed hoặc endpoint xóa trên DB thật.
- Không ghi password/token/secret vào tài liệu.

## Kết quả đã thực hiện

| ID | Kiểm tra | Kết quả thực tế |
|---|---|---|
| T01 | `mvn.cmd -q -DskipTests compile` | PASS trên snapshot đầu lượt |
| T02 | RmsApplicationTests + bốn bộ unit, ép ddl-auto=none và SQL init never | 15 test: 14 PASS, 1 ERROR ở Context; trùng bean dashboardController |
| T03 | SecurityFlowTests,CareerFlowTests | Không chạy được test; testCompile FAIL vì import Dashboard package đã bị xóa |
| T04 | `mvn.cmd -q -DskipTests clean compile` | PASS sau khi đổi package |
| T05 | Startup trực tiếp / Hibernate validate | Hibernate validate PASS; startup FAIL vì VerifyError ở AccountFieldException bytecode, source có kế thừa đúng |
| T06 | Browser tại 1440/1024/768/375 | NOT TESTED |
| T07 | Build/test sạch cuối sau cập nhật source | 45 test: 26 PASS, 1 FAILURE, 18 ERROR; compile và testCompile đã đi qua |
| T08 | Startup trên bản sao classes riêng | FAIL ở Tomcat/HTTP: Unable to establish loopback connection / Invalid argument: connect; không còn VerifyError |

Log T02/T03: `assets/2026-10-04-project-audit/`.
14 unit test PASS không chứng minh whole application startup hoặc browser flow PASS.

T07: 18 ERROR trong SecurityFlowTests là cùng lỗi thiếu bean HealthService trong WebMvcTest slice. 1 FAILURE trong ApiMonitoringServiceTests là assertion còn expect IllegalStateException thay vì MonitoringSessionRequiredException. Log cuối ở target/project-audit-final-tests.log; các import Dashboard đã cập nhật, lỗi import T03 không còn là lỗi cuối.

T08 dùng bản sao classes trong target/audit-runtime, runtime classpath từ Maven, server.port=0, ddl-auto=validate và SQL init never. Process đã thoát với exit code 1; không để lại server audit chạy nền. Browser không được kiểm chứng vì HTTP server chưa khởi động thành công.

## Retest tự động sau khi cập nhật imports

1. Search lại `dashboard.controller`, `dashboard.service`, `dashboard.repository`, `dashboard.dto`, `DashboardView` trong src.
2. Đảm bảo mỗi route/controller/service/repository chỉ có một bean đang hoạt động; không dùng bean overriding để che lỗi.
3. Chạy `mvn.cmd -q -DskipTests clean compile`.
4. Chạy `mvn.cmd -q test-compile`.
5. Chạy các unit và MockMvc:

```powershell
mvn.cmd -q '-Dtest=AccountListServiceTests,AccountManagementServiceTests,ApiMonitoringServiceTests,MailEnvironmentBindingTests,SecurityFlowTests,CareerFlowTests' '-Dspring.jpa.hibernate.ddl-auto=none' '-Dspring.sql.init.mode=never' '-Dspring.jpa.show-sql=false' test
```

6. Chạy Context sau khi package được thống nhất:

```powershell
mvn.cmd -q '-Dtest=RmsApplicationTests' '-Dspring.jpa.hibernate.ddl-auto=validate' '-Dspring.sql.init.mode=never' '-Dspring.jpa.show-sql=false' test
```

7. Nếu validate FAIL, ghi nguyên lỗi bảng/cột/type thực tế; không sửa schema để test pass.
8. Repository/database tests hiện có tạo bản ghi rồi rollback: chỉ chạy trên DB test riêng sau khi được phép, không coi rollback là đảm bảo tuyệt đối không ghi DB.

## Manual flow sau khi startup thành công

### M01 — Login / account scope

1. Mở Dashboard khi chưa login → chuyển tới Login.
2. Login bằng Candidate Active trong môi trường test → Dashboard Candidate.
3. Kiểm tra menu không dẫn tới thao tác quản trị nội bộ.
4. Dùng hai Candidate có dữ liệu khác nhau → mỗi người chỉ thấy application/interview/offer của mình.
5. Dùng account Inactive/Blocked → không đăng nhập; phiên đang tồn tại bị chặn theo guard.
6. Không nhập credential thật vào screenshot hoặc report.

Kết quả hiện tại: NOT TESTED trong browser.

### M02 — Custom Validator (sau khi gắn annotation)

1. Với Create/Register/Reset, để password trống → lỗi @NotBlank.
2. Nhập password/confirm khác nhau → chỉ lỗi confirmPassword đúng field.
3. Nhập password bằng nhau nhưng dưới/ngoài giới hạn 8–32 → lỗi @Size.
4. Nhập hợp lệ → Controller tiếp tục tới service; không xuất password ra HTML/log.
5. Kiểm tra mất validation thủ công không làm mismatch vượt qua service.

Cuối lượt annotation đã gắn đủ ba DTO và kiểm tra thủ công đã bỏ; cần hoàn tất kiểm chứng field errors sau khi sửa dependency WebMvcTest.

### M03 — Health endpoint (sau khi nối HealthService)

1. Dùng MockMvc/mock JDBC cho DB UP → 200, JSON `{"status":"UP"}`, Cache-Control no-store.
2. Cho mock JDBC ném DataAccessException → 503, JSON `{"status":"DOWN"}`, no-store.
3. Không được trả HTML error/500 hoặc message SQL.
4. Account không phải Admin không được gọi endpoint.
5. DB down toàn hệ thống có thể làm AccountSessionGuardFilter lỗi trước Controller; test toàn filter chain riêng, không chỉ test service.

### M04 — Public Jobs / JD

1. GET /jobs → keyword/Department/Type khớp query và giữ filters khi phân trang.
2. Kiểm tra job Published với deadline NULL, tương lai, đã hết hạn trong DB test riêng.
3. View JD → title/salary/benefits/location/deadline khớp DB, nội dung tiếng Việt giữ nguyên.
4. Với chuỗi HTML trong fixture, nội dung phải hiển thị như text hoặc đã sanitize; không chạy script/handler.
5. Không đánh dấu Apply PASS: current handler là ITERATION_2_PENDING, chưa có upload/submit.

### M05 — Candidate UI sau khi nối panel

1. Kiểm tra recent applications, upcoming interviews và offers từ UserId phiên đăng nhập.
2. Draft/Pending_Director/Director_Approved/Director_Rejected không xuất hiện với Candidate.
3. Sent_Candidate/Negotiating/Accepted/Declined xuất hiện theo record thật.
4. Response/HTML không chứa AI/interview score, internal review/comment, internal salary discussion.
5. Không có dữ liệu → thông báo dễ hiểu và link Browse Open Positions thật.
6. Tab/Shift+Tab, Enter, Escape; menu mobile và focus hoạt động.
7. Kiểm tra 1440/1024/768/375, font Lora/Source Sans 3 được load, không overflow toàn trang.
8. Nhãn UI tiếng Anh, không copy header/CSS riêng, không fake Notifications.

### M06 — Requisition security (chỉ trên môi trường test)

1. Dùng fixture/mock service, kiểm tra Candidate không được gọi mutation nội bộ.
2. GET delete phải không thay đổi dữ liệu sau khi sửa; không gọi route đang xóa thật trên DB dùng chung.
3. POST thiếu CSRF bị chặn; POST hợp lệ vẫn cần role/ownership server-side.
4. Audit actor phải là User đang login, không mock user ID = 1.
5. Submit/approve/reject phải được kiểm chứng thật trước khi đánh dấu hoàn thành; current service methods đang rỗng.

## Điểm tiếp tục

Xem `../update architecture/2026-10-04-project-audit-before-candidate-ui.md` để biết file bị ảnh hưởng, lý do và thứ tự sửa. Chỉ cập nhật PASS khi đã có bằng chứng của đúng loại test.
