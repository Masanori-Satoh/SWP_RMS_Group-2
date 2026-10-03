# Auth + Admin visual sync — test từng bước, 01/10/2026

## Phạm vi và nguồn dữ liệu

Login, Register, Forgot/Reset, Dashboard, Account List, Create/Edit và API Monitoring.
Giao diện English; dynamic DB text nguyên văn. Không đổi DB/schema/seed/Java production/
SecurityConfig trong đợt này. Candidate vẫn có trong Accounts; Delete vẫn là deactivation.

- MockMvc: controller thật, Thymeleaf thật, Spring Security/CSRF thật; business services
  và UserRepository **mock** trong SecurityFlowTests. Không đăng ký/reset/deactivate dữ liệu thật.
- Read-only context/query tests: SQL Server hiện có, JPA validate + SELECT; không chạy
  DB scripts, seeder hoặc RequisitionReviewProbeTests.
- Browser: HTML MockMvc xuất opt-in; tên/email/roles/metrics là **fixture test hư cấu**,
  assets chính là CSS/JS/font production. Python server không xử lý POST, login hoặc SMTP.
- Screenshot/test helper không phải datasource của website runtime.
- Chỉ submit các ca thay đổi dữ liệu bên dưới trên database test được cho phép; đừng
  dùng tài khoản/dữ liệu thật để test Delete/Register/Reset.

## 1. Commands tái hiện kiểm tra tự động

Từ thư mục có `pom.xml`, dùng explicit test list; **không wildcard**:

```powershell
mvn.cmd -q '-Dtest=SecurityFlowTests,CareerFlowTests,CareerReadOnlyTests,MailEnvironmentBindingTests,RmsApplicationTests,AccountListQueryTests,AccountListServiceTests,AccountManagementServiceTests,ApiMonitoringServiceTests' '-Dui.preview=true' '-Dspring.jpa.show-sql=false' test
py scripts/prepare-auth-admin-preview.py
py -m http.server 8768 --bind 127.0.0.1 --directory target/auth-admin-preview
```

Browser tab/terminal khác:

```powershell
node scripts/check-auth-admin-ui.mjs http://127.0.0.1:8768 round1
# Sau một batch sửa các vấn đề được xác nhận:
node scripts/check-auth-admin-ui.mjs http://127.0.0.1:8768 round2
```

Driver dùng Chrome có sẵn + helper CDP repo, private profile, không npm dependencies.
Port9250 dành root driver; đổi port/helper nếu port đang dùng, không kill process khác.
Server dừng Ctrl+C sau test. Preview routes `/preview/login/`, `register/`, `forgot/`,
`reset/`, `dashboard/`, `accounts/`, `create/`, `edit/`, `monitoring/`.
Các link/action vẫn là route Spring thật, nên static preview **không** chứng minh
navigation/server submit thành công. Không submit form vào Python rồi ghi PASS.

## 2. Manual cases — thực hiện trên Spring app khi HTTP hoạt động

| ID | Các step | Expected / evidence |
|---|---|---|
| U01 Visual continuity | 1 Mở Careers bằng navbar. 2 Mở Sign In/Register. 3 Login Admin rồi xem Dashboard/List/Create/Edit/API. 4 So sánh wordmark/font/palette/inputs. | Mộc/Lora/Source Sans 3/offwhite/forest nhất quán; không gradient/glass/card shadow; CMS/DB text không tự dịch. |
| U02 Login keyboard | 1 Tab từ đầu trang. 2 Enter Skip to content. 3 Điền identifier/password bằng tài khoản test được phép. 4 Show/Hide bằng Space/Enter. 5 Submit đúng/sai. | Skip focus main; toggle type=button không submit/đổi value/name/autocomplete; POST/login + CSRF; success theo saved URL hoặc Dashboard, failure English. |
| U03 Auth navigation | 1 Click Forgot Password. 2 Back to Sign In. 3 Create a Candidate Account. 4 Back to Careers/wordmark. | Routes `/forgot-password`, `/login`, `/register`, `/` thật; không fake modal hoặc localStorage auth. |
| U04 Register invalid | 1 Để field required trống, Submit. 2 Nhập email sai hoặc password khác confirm, Submit. 3 Click link trong server error summary. 4 Sửa lỗi. | Native required/format trước request; server invalid summary+inline giữ fullname/username/email; password trống sau roundtrip; focus summary rồi field; không tạo User/Candidate khi invalid. |
| U05 Register valid | 1 Trên DB test, điền 5 fields bằng username/email mới. 2 Register. 3 Login account vừa tạo. 4 Admin xem Accounts. | Existing Candidate registration transaction; redirect login registered; Candidate có account + profile, xuất hiện List. Không thêm checkbox/field/business rule khác. |
| U06 Forgot Password | 1 Tới Forgot. 2 Nhập email không hợp lệ để xem lỗi. 3 Nhập email test có mailbox SMTP được phép. 4 Kiểm tra generic confirmation. 5 Làm tương tự email không tồn tại. | Cả hai không tiết lộ tồn tại user; service/email flow thật; link15 phút, không JWT/bảng mới. SMTP chưa cấu hình phải báo lỗi thật, không success giả. Xem setup link dưới. |
| U07 Reset states | 1 Click link thật từ mailbox test. 2 Thử expired/used link. 3 Với valid link nhập mismatch. 4 Nhập đúng hai password. 5 Login mới/old password. | Valid form hoặc invalid recovery link; no-referrer/no-store; mismatch summary+inline; reset redirect login; new works/old fails theo service hiện có. Không echo password/token vào log/screenshot. |
| U08 Dashboard roles | 1 Login từng role test có sẵn (Admin/HR/Manager/Director/Interviewer/Candidate). 2 Xem heading/name/scope/widgets. 3 Guest mở URL Dashboard. | Scope/metrics theo DashboardView thật, không số hardcode UI. Admin nav Account/API; role khác không thấy các link đó; Guest cần login. Không coi dashboard thiếu datasource là widget0 giả. |
| U09 Account filters | 1 Admin mở List. 2 Điền search, role, department, status, sort. 3 Apply Filters. 4 Xem GET query/results/count. 5 Next/Previous. 6 Clear. | Tên params không đổi; pagination giữ filters; count từ Page; reset route thật. Text DB tiếng Việt nguyên văn. |
| U10 Candidate row | 1 Tìm một Candidate có sẵn. 2 Xem role, department có thể rỗng. 3 Mở Edit. | Candidate vẫn thuộc Account Management; profile nghiệp vụ riêng; không chỉ5internalroles. |
| U11 Table mobile | 1 375px mở List/API. 2 Tab tới named table region. 3 Arrow Right/Left hoặc touch scroll. 4 Xem Actions/cột cuối. | Bảng scroll cục bộ; toàn page không overflow. Caption/th scopes/named keyboard-focusable region; scroll cue chỉ khi cần. |
| U12 Create fields | 1 Create Account. 2 Xem Full Name/Username/Email/Phone/Role/Department/Password/Confirm. 3 Chọn internal role rồi Candidate. 4 Test invalid password/email bằng DB test. | Department required internal, optional Candidate; phoneoptional; password8–32/match; create không chọn Status; Active default. Server errors liên kết đúng field. |
| U13 Create valid | 1 Tạo bằng username/email chưa dùng trên DB test. 2 Kiểm tra success message/list. 3 Thử tạo lại username/email đó. | Encoder/hash và unique service rule giữ nguyên; duplicate lỗi, không fake success. Không in/hash/password trong bảng. |
| U14 Edit | 1 Edit account test. 2 Username readonly. 3 Xem không có Password/Confirm. 4 Đổi fullname/email/phone/role/department/status rồi Save. 5 Reload. | Action `/admin/accounts/{id}`, UpdateAccountForm fields đúng; hash/username giữ nguyên; unchanged email không tự fail unique. Candidate email synchronization giữ nguyên backend. |
| U15 Delete cancel | 1 Click Delete trên row. 2 Đọc name/inactive/history message. 3 Cancel. 4 Mở lại rồi Escape. | Native modal; focus Cancel an toàn; Cancel/Escape return Delete trigger; chưa POST/không mất record. |
| U16 Delete confirm | 1 Chỉ với account test được phép, Delete→Deactivate. 2 Xem feedback/liststatus. 3 Thử login bằng account đó. | POST đúng id + CSRF; Inactive, giữ record/business history; không deleteById; login denied. |
| U17 API Monitoring | 1 Admin mở trang. 2 Xem status/lastHTTP/time/checkcount. 3 Send GET nội bộ khi được phép. 4 Xem feedback/reload. | Probe thật qua service hiện có. AI/email không cấu hình hiển thị unavailable, không fake200 hoặc tự gửi tới endpoint tùy ý. |
| U18 RBAC/direct route | 1 Guest mở `/admin/accounts`. 2 HR/Candidate mở trực tiếp list/new/edit/API. 3 Admin mở cùng URLs. | Guest→login; nonAdmin403; Admin được phép. Hidden nav không thay server authorization. |
| U19 Logout/CSRF | 1 Log Out bằng nút thật. 2 Back/open protected URL. 3 Dùng HTTP test môi trường được phép POST thiếu CSRF tới login/register/create/edit/delete/probe/logout. | Logout POST invalidates session; protected cần login; POST thiếu CSRF403. Toggle/Menu/Cancel không có POST. |
| U20 Mobile menu | 1 768/375 mở Admin. 2 Menu click/Enter. 3 Tab qua links. 4 Escape. 5 Resize desktop. | aria-controls/expanded khớp, focus order tự nhiên; Escape→Menu; desktop sidebar hiện; active state semantic. |
| U21 Responsive | 1 Đo1440/1024/768/375. 2 Scroll form/table dài. 3 Kiểm tra header/CTA/error. | Form2/2/2/1 cols; fields/buttons≥44pxheight; sidebar desktop/mobile behavior; Careers/RMS identity còn; không page overflow. |
| U22 A11y beyond automation | 1 Keyboard-only/NVDA hoặc VoiceOver. 2 Browserzoom200%. 3 OS reducedmotion. 4 Test Safari/Firefox/device thật. | Labels/state/error announcements/focus visible; sticky header không che field; không motion bắt buộc. Ghi actual riêng, không suy từ Chrome screenshots. |
| U23 No-JS resilience | 1 Disable JS. 2 Reload Auth/Admin. 3 Xem nav và native forms. 4 Enable JS lại để Delete. | Forms/logout/links còn native, password vẫn masked; mobile sidebar visible. Show/Hide/Menu enhancement và Delete dialog cần JS như implementation hiện có. |

SMTP/full external test: [password-reset-test-setup.md](../testing/password-reset-test-setup.md).
Trace từng package/class/method: [auth-admin-ui-flow.md](../flows/auth-admin-ui-flow.md).

## 3. Actual results

Các FAIL gốc giữ tại target/ và assets/; nguyên nhân/khôi phục được ghi rõ ở WORK_LOG
và critique report. Không gộp stale reports hoặc static preview vào live E2E.

| Gate cuối | Kết quả | Bằng chứng / giới hạn |
|---|---|---|
| Compile + selected Maven tests | **PASS38**,0fail/error/skip,9suite | `final-regression.json`, `target/auth-admin-final-regression.log`; explicit list mục1. |
| Spring Context/JPA/SELECT queries | **PASS** | RmsApplicationTests/CareerReadOnlyTests; không HTTP listener hoặc SMTP. |
| Browser four widths / client controls | **PASS sau sửa driver assertion** | Raw round2 **203/204PASS**; check duy nhất fail đếm cả track0px auto-fit. `round2-grid-confirmation.json` xác minh4track visible/4metrics. Giữ rawFAIL, không ghi lại JSON thành204green. |
| Labels/ARIA/error references/focus/contrast/runtime | **PASS trong Chrome fixtures** | Bốn widths, all9base views +7errorstates +activefilters, local fonts/scripts, no pageoverflow/runtimeerrors. Không WCAG/screenreader certification. |
| Production Java/DB/config file integrity | **PASS87/87 SHA256 bằng baseline** | Sửa đúng backtick đã được duyệt đưa config về baseline;0protectedfile netchange, không đổi data/schema/security. |
| Real HTTP startup | **FAIL** | Tomcat NIO/PipeImpl loopback socket, dưới đây. |
| Real auth/persistence/SMTP E2E | **NOT TESTED/BLOCKED** | MockMvc và static browser không chứng minh các bước này. |

Vòng cuối đã xem ảnh Login1440/List375/Dashboard768/Delete375/Create-invalid375;
không mở thêm vòng polish. `round2-results.json` chứa203checks thực tế PASS, một
assertion sai trong testdriver. Fresh targeted computed check cho đúng gate đó PASS:
tracks274/274/274/274/**0px**;4visible columns. Driver sửa cách đếm track collapse,
**không sửa UI**, không thêm screenshots/fullround3. Source runtime đã đủ evidence
cho gate này nhưng raw round2 vẫn giữfailed=1 để truy lại.

- First Maven gate38 tests:37 pass,1 **test parser error** (XPath XML không nhận
  HTML `defer`). Giữ `target/auth-admin-regression.log`; không sửa HTML hợp lệ để né parser.
- Recovery explicit suite38: PASS; Spring Context/JPA SQL Server/SELECT-only PASS.
- Lượt confirmation21 tests ban đầu: **21Context errors** vì property encoding mới
  có trailing backtick (`UTF-8` + backtick). User duyệt sửa đúng một ký tự; config
  hash sau sửa khớp baseline. Giữ `target/auth-admin-confirmation-tests.log`.
  Lệnh finalMaven đầu bị từ chối quyền chạy; chỉ chạy lại sau user chấp thuận. Final38PASS
  dùng config thực, không dùng `-Dspring.thymeleaf.encoding` để override lỗi.
- HTTP8769: **FAIL** `Unable to establish loopback connection` →
  `SocketException: Invalid argument: connect`, Tomcat NIO/PipeImpl. Source/backend
  không đổi để workaround môi trường. Port8082 curl cũng timeout.
- Native browser tool: ACL/kernel failure; headless Chrome/CDP fallback. Root first
  browser pass144 checks pass rồi resource/driver completion fail; chưa đủ proof.
- Recovery first inspection183/184 pass; table ArrowRight fail ở tab nền. Fresh
  foreground tab + code ArrowRight xác nhận scrollLeft0→40; fix driver, không sửa UI.
- Agent A fresh-tab own64 route-width checks +19 safe interactions; source UI styles
  hoạt động. Agent B runtime injection/computed findings có report riêng.
- Real browser login/logout/create/edit/deactivate/SMTP: **NOT TESTED/BLOCKED**.
  MockMvc POST contracts được test; không dùng Python server để tuyên bố E2E PASS.
- NVDA/VoiceOver/zoom/device/Safari/Firefox: NOT TESTED; U22 cho bước tiếp tục.

Evidence folder: `assets/2026-10-01-auth-admin/`; raw `round1-results.json`,
`round1-recovery-results.json`, snapshots cùng prefix, `assessment-a-*`, `assessment-b-*`.
Ảnh chứa fixture hư cấu, không credential/SMTP/reset token thật. Reset route fixture
`visual-token` và CSRF `visual-csrf-fixture` không dùng được trên server thật.

### Cleanup / tiếp tục

Root/A/B Chrome profiles và detector helper đã dọn; preview8768 được dừng khi kết thúc.
Snapshot baseline28/40 đã đóng đúng identity sau xử lý5P2. Chưa có scorepostpolish mới.
Flow report/data source/constraints giữ nguyên; bước tiếp tục là xử lý môi trường HTTP
và chạy U02–U19 trên app thật/DBtest được cho phép, U06–U07 theo SMTP external test guide.
