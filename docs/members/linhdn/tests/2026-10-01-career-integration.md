# Test từng bước — Career homepage tích hợp Thymeleaf

Ngày: 01/10/2026. Scope: homepage một công ty, dữ liệu JobPosting thật, job detail,
login-return, prefill Candidate, auth header, UI English (DB giữ nguyên), responsive/a11y.

## 1. Điều kiện và giới hạn

- Java21/Maven/SQL Server cấu hình sẵn ngoài Git; `ddl-auto=validate` giữ nguyên.
- **Không chạy db.sql/seed_data.sql, không INSERT/UPDATE/DELETE để chuẩn bị test.**
- Dùng tin Published chưa quá ApplicationDeadline đang có sẵn. Nếu không có, case
  cần tin được ghi NOT AVAILABLE; không thêm tin giả vào DB để làm xanh.
- Real application dùng cổng cấu hình hiện tại8082. Xác nhận log `Tomcat started`
  và GET `/` thành công; Maven exit0 không đủ.
- CV/storage/profile edit/submit chưa có implementation: trạng thái đúng là disabled,
  chưa tạo đơn. Không coi submission thành công là expected result.
- Candidate/User test thật chỉ dùng tài khoản được phép. Không ghi credentials,
  password hash, session hoặc CSRF token vào report/ảnh.
- Trong môi trường hiện tại Tomcat HTTP startup FAIL loopback. Các kết quả browser
  được chạy trên **HTML MockMvc render từ DB**, còn authenticated/apply HTML dùng
  **fictional test fixture**. Chúng không xác minh browser login/session E2E.

## 2. Lệnh chạy

### Backend: danh sách cụ thể, không test ghi DB

PowerShell ở root repo:

```powershell
mvn.cmd -q '-Dtest=CareerFlowTests,CareerReadOnlyTests,SecurityFlowTests,MailEnvironmentBindingTests,RmsApplicationTests,AccountListQueryTests,AccountListServiceTests,AccountManagementServiceTests,ApiMonitoringServiceTests' '-Dspring.jpa.show-sql=false' test
```

Không dùng `*Tests` khi checkout có test probe/Requisition hoặc DatabaseTests ghi dữ
liệu, kể cả rollback. Unit test repository mock `.save()` không thực hiện ghi DB.
`CareerReadOnlyTests` chỉ SELECT và render GET; không fixtures SQL.

### Browser trên ứng dụng thật khi HTTP server chạy được

```powershell
mvn.cmd spring-boot:run
node scripts/check-career-integration.mjs http://localhost:8082 --round=live
```

Script đọc DOM/data thật, click card và guest Apply→Login; không tự đăng nhập hoặc
tự POST/seed DB. Authenticated/login-return phải làm case thủ công phía dưới hoặc MockMvc.

### Visual fallback đã dùng lần này

```powershell
mvn.cmd -q '-Dtest=CareerFlowTests,CareerReadOnlyTests' '-Dcareer.preview=true' '-Dspring.jpa.show-sql=false' test
py -X utf8 scripts/prepare-career-preview.py
py -m http.server 8766 --bind 127.0.0.1 --directory target/career-db-preview
# Terminal khác:
node scripts/check-career-integration.mjs http://127.0.0.1:8766 --fixture --round=manual
```

Public homepage/detail được controller/service/repository/Thymeleaf render từ DB;
`preview-authenticated.html`/`preview-apply.html` là fixture MockMvc hư cấu.
Server Python không login, logout, upload hoặc submit. Dừng server bằng Ctrl+C.
Screenshot/JSON lưu theo round dưới `docs/tests/assets/2026-10-01-career-integration/`.
Chrome profile riêng được cleanup sau khi kiểm tra resolved path trong TEMP.

## 3. Manual cases

| ID | Các step thực hiện | Expected result / bằng chứng cần ghi |
|---|---|---|
| C01 Guest homepage | 1 Mở `/` bằng tab chưa đăng nhập. 2 Nhấn wordmark/header links. 3 Xem labels/card. | Không login bắt buộc ở trang công khai; English labels. Card title/content đúng DB, không dịch nội dung DB. Sign In/Register có link thật. |
| C02 Nguồn job | 1 Đối chiếu SELECT Published và deadline với cards. 2 Đọc title/salary/location/date của một ID. 3 Xem DevTools Sources. | Số/cards/ID đúng DB; không `const jobs` hoặc sáu fixture cards cũ. `th:each` sinh HTML ở server. Không thấy hash/password/internal requisition reason. |
| C03 Detail | 1 Click phần thân card. 2 Ghi URL. 3 Mở URL trong tab mới. 4 Kiểm tra salary/date/dept/type/deadline/JD/requirements/benefits. 5 Copy Role Link nếu browser hỗ trợ. | Stable `/jobs/{id}`, title tab đúng postingTitle, metadata đầy đủ theo DB; common benefits là link, role benefits theo JobPosting. Không fake preferred qualifications. |
| C04 Unavailable job | 1 Mở ID không tồn tại. 2 Nếu DB có Draft/Paused/Closed/expired ID, mở ID đó. 3 Nhấn View Open Positions. |404 recovery page; không tiết lộ draft/closed nội dung. Thiếu record loại này →NOT AVAILABLE cho nhánh đó. |
| C05 Keyword | 1 Tìm title đang có bằng bản không dấu. 2 Tìm một từ trong public JD/requirements. 3 Nhập từ không tồn tại. | Match không dấu và đ/Đ; live count đúng, empty state rõ, không gọi backend giả hoặc tạo dữ liệu mới. |
| C06 Combined filters | 1 Chọn location. 2 Chọn department. 3 Thêm keyword. 4 Quan sát count từng pill/active. | Kết quả AND theo cả3; `aria-pressed` đúng; department0 muted/dashed, vẫn có tên/count đủ contrast. |
| C07 Clear | 1 Tạo kết quả0. 2 Tab tới Clear Filters/Enter hoặc click. 3 Quan sát focus/scroll. | Reset tại section positions; focus vào All Departments, không mất focus do nút hidden hoặc về Hero. Clear hidden khi không filter. |
| C08 Guest Apply | 1 Tab mới chưa login mở role. 2 Đọc availability. 3 Click Apply Now. 4 Login Candidate đúng. | Guest→login→**cùng `/jobs/{id}/apply`**; không homepage/Dashboard fallback. Notice cho biết submission chưa mở trước login. |
| C09 Retry/direct login | 1 Từ Apply nhập sai password. 2 Thử lại đúng. 3 Ở session/tab mới login trực tiếp không saved URL. | Sai có lỗi English, retry giữ target. Login trực tiếp→Dashboard. Không lộ lý do account tồn tại hay password hash. |
| C10 Candidate prefill | 1 Login Candidate A. 2 Mở Apply. 3 Đối chiếu name/email/phone User và LinkedIn/portfolio/address Candidate A. 4 Thử query `candidateId` khác. | Chỉ identity session được lookup qua UserId; không dùng ID client; fields readonly. Không update profile khi GET. |
| C11 Missing profile | 1 Chỉ khi có account Candidate sẵn thiếu profile, mở Apply. Không tự tạo record test. |409 rõ cách nhờ Admin kiểm tra, không auto-create/ghép email. Nhánh đã xác minh bằng MockMvc fixture; DB không có case →NOT AVAILABLE live. |
| C12 Internal role | 1 Login HR/Admin/Director bằng tài khoản được phép. 2 Mở Apply. |403; không tạo Candidate. Dashboard và admin routes tiếp tục quyền hiện có. |
| C13 No fake submission | 1 Vào Apply. 2 Kiểm tra notice. 3 Thử file picker/Submit/Enter. 4 Nếu test HTTP: POST có/không CSRF trong môi trường được phép. | CV/Submit disabled. Không success, không Application.save. POST thiếu CSRF403; Candidate+valid CSRF405 vì chưa có handler. Không nhập note unsupported. |
| C14 Auth header/logout | 1 Guest mở menu mobile và desktop. 2 Login Candidate. 3 Kiểm tra display name/Dashboard/Log Out. 4 Submit logout thật. 5 Mở lại Apply. | State từ Spring session/DB, không localStorage. Mobile cùng controls. Logout POST+CSRF, session mất hiệu lực; Apply yêu cầu login lại. |
| C15 Registration/Talent Pool | 1 Guest click Create a Candidate Account ở cuối trang. 2 Kiểm tra username/name/email/password/confirm labels/errors English. Không submit để giữ DB. 3 Logged-in xem bottom CTA/footer. | `/register` thật; label phản ánh đăng ký tài khoản, không talent subscription giả. Logged-in CTA/footer tới Dashboard thật. Existing registration business rule giữ nguyên. |
| C16 Responsive | 1 Inspect1440/1024/768/375. 2 Scroll trang/menu/anchor. 3 Đọc dài address/title DB. 4 Xem detail/apply/footer. | Không page overflow; job3/2/2/1, process4/2/2/1; header sticky; CAREERS visible; pills mobile một hàng scroll; apply2/2/2/1. Text wrap không che controls. |
| C17 Keyboard/a11y | 1 Tab từ skip link qua nav/filter/card. 2 Enter/card, Escape mobile menu. 3 Kiểm tra pressed/live count. 4 Reduce motion. 5 Zoom200%, NVDA/VoiceOver/device thật. | Focus visible/order hợp lý, không trap/teleport, names native. Text≥4.5/control border≥3. Browser AX tree không thay cho screen reader thật. |
| C18 English regression | 1 Xem auth, Account List/Create/Edit, Dashboard/API Monitoring. 2 Trigger validation an toàn (không save). 3 Kiểm tra dynamic name/department/job tiếng Việt có trong DB. | Literal/error/static labels English, dynamicDB nguyên văn. Không đổi role/status/business rule hoặc dữ liệu. |

## 4. Actual results — ghi theo loại bằng chứng

- Backend selected suite: **36 tests PASS,0 failures/errors/skips** ở 9 lớp explicit
  mục2. Sau batch title/filter/read-only, chạy lại đúng CareerFlowTests5,
  CareerReadOnlyTests3, SecurityFlowTests14: **22 PASS**. Không gộp XML/txt cũ ngoài
  danh sách. Compile: PASS. MockMvc: guest/saved target/CSRF/internal denial/missing profile/
  inactive revocation/escaped data/context-path/empty/405. SELECT-only: query và FK +
  actual DB→controller→Thymeleaf.
- Browser first inspection:38 checks PASS sau sửa driver (không chụp lại vòng đó).
  `round1-results.json`: driver DOM-ready FAIL trước inspection.
  `round1-inspection-results.json`:21 PASS rồi driver contrast regex FAIL.
  `round1-driver-recovery-results.json`:38 PASS,0 FAIL, min text5.36:1; không có runtime errors.
  Giữ nguyên files FAIL; không sửa chúng thành xanh.
- Confirmation `round2-results.json`: **46 PASS,0 FAIL**, đầy đủ bốn viewport,
  job3/2/2/1, process4/2/2/1, apply2/2/2/1; không overflow/runtime errors. Minimum
  visible homepage text5.36:1; control-border checks PASS. Kiểm tra thêm title được
  evaluate, notice trước login, openings trước department0, overflow cue thực tế và
  optional blank Not provided. Root xem ảnh desktop/mobile/positions/apply cuối;
  không mở vòng polish thứ ba. Review độc lập A/B nằm trong `docs/design/`.
- Spring Context/JPA: PASS trong read-only integration tests. HTTP startup: FAIL
  `Unable to establish loopback connection` / `java.net.SocketException: Invalid argument: connect`.
- Real browser auth/login/logout/upload/submit: **NOT TESTED/BLOCKED**, không gộp
  với static preview. CV/submit là intentionally unavailable, không PASS E2E.
- Zoom200%, NVDA/VoiceOver, Safari/Firefox, device thật: NOT TESTED; C17 ghi steps để nối tiếp.

### Evidence files

- `target/career-tests-final.log`: lượt 36 test; `target/career-tests-confirmation.log`:
  lượt22. Report cụ thể trong target/surefire-reports; target là output build không
  phải tài liệu durable. Khi rerun kiểm tra đúng timestamp/class, không tổng hợp stale reports.
- `assets/2026-10-01-career-integration/round2-results.json` và
  `round2-{home,detail,apply}-{1440,1024,768,375}.png`, mobile positions/auth captures:
  durable browser evidence (public DB-rendered; auth/apply fictional).
- `assessment-b-*.json`: detector raw/counters/failure/recovery. CLI2 warnings,
  accepted0/false-positive2; runtime injected3pages,0 logged issues. Headless nên không
  xác nhận overlay thấy được bởi người dùng. Local helper đã stop/profile dọn.
- DB diff: không sửa schema/seed/SQL; không chạy script DB. Sự cố wildcard bên dưới
  là transient test writes, cần giữ riêng với tuyên bố file không đổi.

### Sự cố test ngoài phạm vi

Lượt wildcard `*Tests,!*DatabaseTests` bắt thêm `RequisitionReviewProbeTests` mới có
ghi thử. Source/compiled annotation xác nhận `@Transactional` + `@Rollback`; dữ liệu
test rollback nhưng SQL identity counters có thể tăng. Không tuyên bố DB hoàn toàn
không có transient writes trong toàn bộ lượt này. Từ đó dùng explicit list ở mục2,
không chạy lại probe và không sửa/drop/reseed DB. Không che log constraint errors của probe.
