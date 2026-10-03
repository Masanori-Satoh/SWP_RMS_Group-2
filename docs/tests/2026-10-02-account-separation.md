# Test System Admin — Internal/Candidate separation

## Phiên bản hiện tại sau business confirmation

Người dùng chốt hai lifecycle riêng; **current role** phân nhóm, Candidate không được đổi thành internal hoặc ngược lại. Tạo User nội bộ mới khi hired; không tự deactivate account Candidate vì hiring. Query/server guards/UI đã triển khai. Các section preflight/pending bên dưới được giữ làm lịch sử trước quyết định; dùng bảng hiện tại này khi test.

### Gate cuối

Command: `mvn.cmd -q "-Dtest=SecurityFlowTests,AccountManagementServiceTests,AccountListServiceTests,AccountListQueryTests,CareerFlowTests,CareerReadOnlyTests,MailEnvironmentBindingTests,RmsApplicationTests,ApiMonitoringServiceTests" "-Dui.preview=true" "-Dcareer.preview=true" "-Dspring.jpa.show-sql=false" test`

Initial41PASS; **final42PASS, 0failure/error/skip, Mavenexit0** sau bổ sung forged-form guards/linkage audit và fixture correction. Suites counts: Security18, AccountManagement7, AccountListService2, AccountListQuery1, CareerFlow5, CareerReadOnly3, MailBinding2, RmsApplication1, ApiMonitoring3. Test real DB chỉ SELECT trong transaction readOnly, không seed/INSERT/UPDATE/migration. Actual internal10/Candidate265; allstatus counts so với unfiltered list/repository/Dashboard PASS. Legacy audit: internal-linked-profiles0, candidate-with-department0. Không hardcode counts trong assertions. Log `target/account-separation-final-tests.log`, Surefire XML; compile/MVC/JPAContextPASS.

Browser commands: `py scripts/prepare-auth-admin-preview.py`; `py -m http.server 8770 --bind 127.0.0.1 --directory target/auth-admin-preview`; `node scripts/check-account-separation-ui.mjs http://127.0.0.1:8770 round2`. Preview render từ MockMvc, .test sample identities/metrics chỉ trong tests, không backend POST. Initial112/112; **one final confirmation119/119PASS**. Năm surfaces×4width + selectedfields/filterbaseline/navgroups/dialogbehavior, screenshots/JSON `docs/tests/assets/2026-10-02-accounts/round2-*`. Root đã xem lại Internal1440/menu375, không thêm round3. Root/A/B profilescleaned; preview8770stoppedCtrlC, helpersstopped. BrowsernativeCUA kernelerror, headlessCDPfallback; no verified liveHuman tab. JSsyntax/gitdiffcheckPASS.

**HTTP startup:** fresh `mvn.cmd -q spring-boot:run "-Dspring-boot.run.arguments=--server.port=8771 --spring.jpa.show-sql=false"` FAIL Tomcat/PipeImpl loopback `SocketException: Invalid argument: connect`. Log `target/account-separation-startup.log`; Mavenrunnerexit0 không phảiHTTPPASS. SpringContext/JPA trong tests tách riêng vớiHTTP/liveE2E.

### Test từng bước hiện tại

Chuẩn bị AdminActive, một HR/Department thật, Candidate test có FK profile, một User test mỗi status; chỉ thao tác ghi ở DBtest được phép. Không chọn Admin chính, không dùng credentials production. UI English, dữ liệu DB giữ tiếng Việt khi có.

| ID | Steps | Expected | Actual evidence |
|---|---|---|---|
| A01 | Login Admin → Dashboard → View Internal Accounts → tìm Candidate username/email | Candidate không hiện; Role filter chỉ5internal; sidebar đúng Internal active | RealJPA scopedquery + initialbrowser PASS; live login NOTTESTED |
| A02 | Internal→Role/Department/Status→Apply→Next; thử search name/email/username và Sort | Giao của criteria đúng internal, params giữ qua pagination | JPAfilter/status/search/pagePASS; browser visualcontrolsPASS; live GETnavigation manual |
| A03 | Create Internal→chọnHR+Department→uniqueusername/email/password8–32/confirm→Submit | NewUserActive/BCrypt, khôngCandidateinsert; successredirect | Service/MockMvcPASS; livewriteNOTTESTED |
| A04 | Edit internal→đổi name/email/role internal/Department/status→Save | Username/hash giữ; sameemail không tựconflict; Candidate role khôngavailable | Service/MockMvcPASS, fixtureselection browserPASS; livewriteNOTTESTED |
| A05 | DevTools/HTTPtest POST Create với roleIdCandidate và validCSRF; GET/POST internal edit bằngCandidateUserId | Fieldrole error/404; khôngUserwrite, khôngroleconversion | Service+MockMvcforgedformPASS; liveNOTTESTED |
| A06 | CandidateAccounts→search/status/sort/Next→đối chiếu UserId vớiCandidate.UserId bằngSELECT | ChỉCandidate; noDepartment/role selector/Create/Edit; FKID/date thực; missingprofilehonest | JPA linkage/createdAt + MockMvc + browserPASS |
| A07 | List từnggroup→Active/Blocked Deactivate→Cancel/ESC; Inactive xemActions | CorrectgroupURL/name; focusCancel+restore; Inactive khôngDeactivate | MockMvc/browserPASS; noPOST |
| A08 | DBtest: Deactivate Candidate→Submit→SELECT User/profile/history→trylogin existingtab | Inactive/historypreserved; khôngharddelete; session/loginblocked | Service+MockMvcstatus/CSRF/authPASS; livebrowserwriteNOTTESTED |
| A09 | HTTPtest AdminPOST internalDeactivate bằngCandidateId và ngược lại, cóCSRF | Scope404, accountkhôngđổi | UnitguardPASS; liveNOTTESTED |
| A10 | Guest/nonAdmin GET/POST candidate-admin route; AdminPOST thiếuCSRF | Guest→login; HR/Candidate403; missingCSRF403 | MockMvcPASS |
| A11 | Dashboard hai nhóm total/3statuses→mởunfilteredlists→SELECT COUNT theoRole/status | Total=sum3statuses, totalskhớplist; healthriêng | RealJPA/ServiceDashboardPASS; browsergroupingPASS |
| A12 | ExistinginternalUser cóhistoricalCandidateprofile nếuDBtest có→list/dashboard/edit | Xếpinternaltheorole; khôngxóaprofile/reversehistory | Sourcepredicate/SELECTaudit; không tựtạo fixtureDB |
| A13 | Simulate sameCandidate→internal update inservice/test; hiredcandidate→Create Internal mới vớiidentityunique | Conversionreject trướcfieldwrite; originalCandidate/Profilegiữ; newUserdistinct | UnitconversionbothdirectionsPASS; newinternalnormalcreateflow; livehiringNOTTESTED |
| A14 | Careers→Register→Login→Logout, savedApplyURL và statusInactive/Blocked | Luồngregistration/session/CSRFcũ khôngđổi | Security/CareerregressionPASS; liveSMTP/authNOTTESTED |
| A15 | 1440/1024/768/375: 5pages; mobileMenu; Tab vào nativeSelect/table; modalESC | Nooverflow, fonts/nativecontrols/focus; fivefiltersdesktopalign; navgroupsfullrowmobile | Initial112PASS; final119PASS; A/Bsampledkeyboard/contrastPASS |

Known limits: Candidate activate/block/edit/profile correction không có route được triển khai trong đợt này; không thêm action giả. Username/emailuniqueglobal kể cảInactive: nhânviênmới cầnđịnhdanhrieng. Khôngscreenreader/deviceSafariFirefox testing; usekeyboardonly/browserfixtureskhôngclaimWCAGcertification.

## Trạng thái hiện tại

Audit hoàn tất; tiêu chí phân nhóm **WAITING FOR BUSINESS CONFIRMATION**. Chỉ đổi thuật ngữ Deactivate và ẩn action Inactive. Danh sách/Dashboard hiện vẫn trộn hai nhóm; không claim đã tách. Không DB/schema/seed/auth modification.

## Tự động

`mvn.cmd -q "-Dtest=SecurityFlowTests,AccountListServiceTests" test`

Chạy explicit suites, không wildcard/probe ghi DB. SecurityFlowTests dùng MockMvc/mock service; AccountListServiceTests kiểm tra soft deactivation bằng mocked repository. **PASS 18 tests:** SecurityFlowTests 17, AccountListServiceTests 1; 0 failure/error/skip, Maven exit 0. Compile và MVC test context PASS. Không đại diện full application startup/JPA/SQL/browser/live mutation. Log: `target/account-separation-preflight.log`; Surefire XML đúng package `com.group2.rms.SecurityFlowTests` và `com.group2.rms.service.AccountListServiceTests`.

`git diff --check` trên các file tracked chỉnh trong lượt này PASS. Không chạy lại full suites vì query/auth chưa thay đổi. Audit read-only/source, chưa truy vấn live DB trong lượt này.

## Manual steps — phần đã sửa

| ID | Điều kiện | Steps | Mong đợi | Thực tế |
|---|---|---|---|---|
| D01 | Admin Active, User test Active | Login → Accounts → tìm User test → xem Actions → bấm Deactivate | English label/accessible name; modal Deactivate Account, tên+username và cảnh báo Inactive/history; chưa ghi DB | NOT TESTED browser |
| D02 | Modal D01 đang mở | Tab đến Cancel → Enter; mở lại → Escape | Đóng, focus về nút; status không đổi | NOT TESTED browser |
| D03 | User test Inactive | Tìm row → xem Actions | Không có Deactivate; Edit còn | MockMvc render PASS; browser NOT TESTED |
| D04 | User test Blocked | Tìm row → xem Actions | Có Deactivate; mở modal đúng User | MockMvc action/label PASS; modal browser NOT TESTED |
| D05 | Môi trường test được phép ghi, target không phải Admin chính | Deactivate → xác nhận → SELECT UserId,AccountStatus và related history → thử login target | Inactive, User/profile/history còn; login bị từ chối; CSRF vẫn bắt buộc | NOT TESTED live |

## Manual steps — sau xác nhận/implementation

| ID | Steps | Mong đợi | Thực tế |
|---|---|---|---|
| S01 | Admin → Internal Accounts → search Candidate username/email | Không hiện Candidate, không có Candidate role filter | PENDING implementation |
| S02 | Chọn lần lượt internal role, Department, Status và kết hợp search; paginate | Kết quả giao của filters trong nhóm nội bộ, query params giữ qua trang | PENDING implementation |
| S03 | Create/Edit internal → kiểm tra role options; gửi roleId Candidate hoặc Candidate userId bằng HTTP test | Không có Candidate option; backend chặn cross-group, không thay dữ liệu | PENDING implementation |
| S04 | Admin → Candidate Accounts → tìm Candidate | User identity + đúng FK profile; không Department/internal role selector/Create Candidate | PENDING implementation |
| S05 | Candidate page → tìm username nội bộ → lọc status → paginate | Internal không hiện; status/pagination đúng nhóm | PENDING implementation |
| S06 | Đối chiếu hai totals/statuses Dashboard với unfiltered lists và SELECT chỉ đọc cùng rule | Mỗi total bằng sum Active/Inactive/Blocked; counts/list total cùng scope, không double-count | PENDING business rule |
| S07 | Dataset test role Candidate thiếu profile và internal role có historical profile | Hiển thị theo rule đã duyệt; không sửa dữ liệu hoặc âm thầm tạo profile | PENDING business rule |
| S08 | Guest/HR/Candidate GET và POST candidate-admin route; Admin POST thiếu CSRF | Guest login redirect; non-Admin 403; thiếu CSRF 403 | PENDING route |
| S09 | Careers → Register → Login → Logout với test account | Public registration riêng; User+Candidate cùng transaction; saved login URL/session/logout không đổi | NOT TESTED live |
| S10 | Hai lists + Dashboard/forms tại 1440/1024/768/375; Tab, table horizontal scroll, modal Cancel/Escape | Không viewport overflow; readable rows/counts, real links, keyboard/focus đúng; mobile nav hiện có giữ nguyên | PENDING implementation |

Không ghi credential/hash/token thật. Với thao tác ghi chỉ dùng môi trường/account test được phép; kiểm tra read-only không tự tạo/chuyển dữ liệu. Build/MockMvc/SQL SELECT/browser/live auth báo riêng.
