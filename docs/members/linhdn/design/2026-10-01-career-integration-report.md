# Báo cáo tích hợp career homepage và English UI — 01/10/2026

## 1. Files changed

Paths dưới đây tính từ root repo. Chỉ liệt kê thay đổi của đợt career/English này; không nhận là tác giả những thay đổi requisition/merge khác đang có trong checkout.

### Integration

| Path | Sửa gì / tại sao |
|---|---|
| `src/main/java/com/group2/rms/repository/JobPostingRepository.java` | Thêm query open list/detail Published + deadline và EntityGraph; tái dùng entity/quan hệ có sẵn, tránh đọc lazy ngoài transaction. |
| `src/main/java/com/group2/rms/controller/DashboardAccessController.java` | Root homepage do CareerController sở hữu; Dashboard vẫn ở `/dashboard`. |
| `src/main/java/com/group2/rms/controller/AuthController.java` | Bỏ mapping `/` redirect requisitions xuất hiện trong checkout; tránh trùng endpoint và để root là career homepage. Giữ GET login. |
| `src/main/java/com/group2/rms/config/SecurityConfig.java` | GET root/fonts public, Candidate apply matcher trước public jobs, request cache/saved URL và fallback Dashboard; giữ session, BCrypt, CSRF, guard/admin rules. Index đang UU, không stage/resolve merge hộ. |
| `prototype/index.html` | Entry HTML English dẫn đến homepage Spring; bỏ bộ jobs/cards/hash detail tĩnh trùng dữ liệu. Không còn homepage thứ hai. |
| `scripts/check-career-homepage.mjs` | Compatibility entrypoint sang harness integration; dependency search không thấy code import helper cũ. |
| `PRODUCT.md` | Cập nhật capability thật, English UI/DB-language exception, backend gaps và provenance. |
| `docs/recruitment-homepage-design-system.md` | Thêm state hiện tại ở đầu; giữ phần standalone cũ như lịch sử. |
| `docs/flows/authentication-login-flow.md` | Saved URL ưu tiên, Dashboard là fallback; sửa trace và expected test cũ. |
| `docs/flows/README.md` | Thêm career flow, điều chỉnh tuyên bố cũ “job chưa có controller”. |
| `docs/tests/README.md` | Thêm đợt test có steps và evidence. |
| `docs/WORK_LOG.md` | Ghi toàn bộ quá trình/kiểm chứng/điểm tiếp tục. |

### English literals — giữ điều kiện/rule/dynamic data

| Path | Sửa gì / tại sao |
|---|---|
| `src/main/resources/templates/hello.html` | Title/demo labels English. |
| `src/main/resources/templates/auth/login.html` | Sign In, trạng thái login/logout/registered/reset, labels và Back to Careers. |
| `src/main/resources/templates/auth/register.html` | Labels/validation UI English, link homepage; fields/business rules giữ nguyên. |
| `src/main/resources/templates/auth/forgot-password.html` | Labels/help/status English. |
| `src/main/resources/templates/auth/reset-password.html` | Title/form/errors English. |
| `src/main/resources/templates/dashboard/index.html` | Labels/widget headings/fallback English; `th:text` dynamic nguyên văn. |
| `src/main/resources/templates/fragments/head.html` | Static shared title metadata English. |
| `src/main/resources/templates/admin/accounts/form.html` | Create/Edit labels/buttons/help English; không thêm fields. |
| `src/main/resources/templates/admin/accounts/list.html` | Filters/table/delete confirmation/buttons English. |
| `src/main/resources/templates/admin/api-monitoring/index.html` | Labels/source help/status English. |
| `src/main/resources/templates/requisitions/form.html` | Chỉ literal/comment tiếng Việt; không refactor hoặc đổi nghiệp vụ teammate. |
| `src/main/resources/templates/requisitions/list.html` | Chỉ literal/comment tiếng Việt. |
| `src/main/resources/templates/requisitions/detail.html` | Fallback tên mẫu tiếng Việt → Hiring Manager; tên từ DB vẫn nguyên văn. |
| `src/main/java/com/group2/rms/controller/AccountController.java` | Success/error/page labels English. |
| `src/main/java/com/group2/rms/controller/PasswordRecoveryController.java` | Recovery UI status/title English. |
| `src/main/java/com/group2/rms/controller/RegistrationController.java` | Registration feedback English. |
| `src/main/java/com/group2/rms/controller/demo/TestDbController.java` | Demo response labels English; giữ query/DTO. |
| `src/main/java/com/group2/rms/controller/demo/TestWebController.java` | Demo greeting English. |
| `src/main/java/com/group2/rms/controller/form/CreateAccountForm.java` | Bean validation messages English, constraints giữ nguyên. |
| `src/main/java/com/group2/rms/controller/form/UpdateAccountForm.java` | Bean validation messages English, không đưa password vào edit. |
| `src/main/java/com/group2/rms/controller/form/RegisterAccountForm.java` | Bean validation messages English, username bắt buộc giữ nguyên. |
| `src/main/java/com/group2/rms/controller/form/ForgotPasswordForm.java` | Email validation messages English. |
| `src/main/java/com/group2/rms/controller/form/ResetPasswordForm.java` | Password validation messages English, min/max giữ nguyên. |
| `src/main/java/com/group2/rms/service/AccountManagementService.java` | Field/service errors English; role/status/unique/deactivate/password logic giữ nguyên. |
| `src/main/java/com/group2/rms/service/ApiMonitoringService.java` | Probe descriptions/errors English; không fake health. |
| `src/main/java/com/group2/rms/service/DashboardService.java` | Widget/scope/help/error labels English; queries/metrics/role scope giữ nguyên. |
| `src/main/java/com/group2/rms/service/PasswordResetEmailSender.java` | Email subject/body và service error English; SMTP/secret flow giữ nguyên. |
| `src/main/java/com/group2/rms/service/PasswordResetService.java` | Validation error English; không đổi token/security policy. |
| `src/main/java/com/group2/rms/repository/DashboardMetricsRepository.java` | Chỉ literal activity-type label Job Requisition; không đổi JPQL hoặc nội dung query trả từ DB. |
| `src/test/java/com/group2/rms/SecurityFlowTests.java` | Expected feedback English và redirect saved target theo foundation mới. |
| `src/test/java/com/group2/rms/service/AccountManagementServiceTests.java` | Expected message English. |
| `src/test/java/com/group2/rms/service/ApiMonitoringServiceTests.java` | Expected description English. |

### Files created

| Path | Mục đích |
|---|---|
| `src/main/java/com/group2/rms/service/CareerService.java` | Read-only service/public records, actual own Candidate lookup; không expose entity/password. |
| `src/main/java/com/group2/rms/controller/CareerController.java` | Homepage/detail/apply GET + viewer/404/409. |
| `src/main/resources/templates/careers/index.html` | Homepage DB `th:each`, filters/culture/benefits/process/account CTA English. |
| `src/main/resources/templates/careers/fragments.html` | Shared head/header/auth/footer/facts, real logout CSRF POST. |
| `src/main/resources/templates/careers/detail.html` | Stable role URL và structured public DB content. |
| `src/main/resources/templates/careers/apply.html` | Read-only own prefill, unavailable upload/submit skeleton. |
| `src/main/resources/templates/careers/unavailable.html` | Honest 404/409 recovery, evaluated title. |
| `src/main/resources/static/css/careers.css` | Reuse approved editorial foundation; self-host font declarations. |
| `src/main/resources/static/css/career-pages.css` | Sticky/auth/workspace/detail/prefill/mobile refinements. |
| `src/main/resources/static/js/careers.js` | DOM-only filtering, menu/focus/copy behavior; no fake jobs or submit. |
| `src/main/resources/static/fonts/career-0.woff2`…`career-5.woff2`, `LICENSE.txt` | Six prior approved Lora/Source Sans3 subsets + OFL; không tải font mới. |
| `src/test/java/com/group2/rms/CareerFlowTests.java` | Five meaningful MVC/security/Thymeleaf flow tests, opt-in fictional visual fixtures. |
| `src/test/java/com/group2/rms/service/CareerReadOnlyTests.java` | Three SELECT-only actual DB/query/FK/render tests, opt-in public visual export. |
| `scripts/career-browser.mjs` | Shared private Chrome CDP helper, main-ready wait and guarded profile cleanup. |
| `scripts/check-career-integration.mjs` | Bounded browser checks, four viewports, source versus fixture provenance, failure exit/evidence. |
| `scripts/prepare-career-preview.py` | Copies tested HTML/assets to target for static preview; no backend/auth emulation. |
| `prototype/README.md` | Current Spring source and runtime/gap guidance. |
| `docs/design/2026-10-01-career-integration-impact.md` | Impact analysis before implementation and unavailable policies. |
| `docs/design/2026-10-01-career-assessment-a.md` | Independent visual/UX baseline. |
| `docs/design/2026-10-01-career-assessment-b.md` | Independent deterministic/runtime findings and limits. |
| `docs/design/2026-10-01-career-impeccable-critique.md` | Synthesized priorities/dispositions, baseline27/40. |
| `docs/design/2026-10-01-career-integration-report.md` | Báo cáo này. |
| `docs/flows/career-homepage-apply-flow.md` | Từng step, package/class/method/repository/security/UI. |
| `docs/tests/2026-10-01-career-integration.md` | C01–C18 steps/expected/actual/limits/rerun instructions. |
| `docs/tests/assets/2026-10-01-career-integration/` | Durable round1/round2 PNG/JSON, driver failures, isolated detector evidence. |
| `.impeccable/critique/2026-10-01T13-34-51Z__src-main-resources-templates-careers-index-html.md` | Fingerprinted baseline, exact snapshot closed after final batch. |

Không xóa Entity/Repository/DB/business feature. Chỉ dọn hai script one-off của đợt này (`tmp-career-strings.py`, `tmp-career-english.py`) và body/scratch test tạm; harness standalone được thay bằng compatibility entrypoint sau dependency search.

## 2. Reused entities và routes

JobPosting → JobRequisition → Department cho public job fields/department/type. User → Role cho authentication/header; Candidate.account/UserId cho own profile. Reuse existing register/login/logout/Dashboard/Account Management/reset. Không đưa internal requisition notes/approval reasons vào public detail.

Routes: GET `/`, `/jobs`, `/jobs/{id}` public; `/jobs/{id}/apply` Candidate only. GET login + framework POST login + real POST logout/CSRF như trước. No POST apply implementation.

## 3. Frontend behavior

Không job array/duplicate cards. NFD+đ/Đ search và department/location AND gần results, live count/clear/empty/pressed states. Real link card/detail, role URL copy khi Clipboard thật hỗ trợ. Detail giữ salary/date/deadline và DB wording. Missing optional data có English fallback. Head title được evaluate. Header/footer/bottom CTA theo authenticated viewer; mobile giữ CAREERS.

Static auth/admin/dashboard/requisition copy và service/validation/email feedback English; dynamic DB tiếng Việt giữ nguyên. Giữ “Mộc” như tên thương hiệu riêng, không coi là đoạn copy cần dịch.

## 4. Security và apply

Saved request được lưu trong session, `defaultSuccessUrl("/dashboard", false)` giữ đúng URL. Candidate lookup bằng authentication username→UserId, không nhận CandidateId từ client. Internal403, missing profile409, closed job404, inactive session revoked. CSRF vẫn bật. Form chỉ đọc, CV/submit disabled; POST thiếu token403, valid Candidate+token405. Không gọi ApplicationRepository.save, không tạo tài khoản hoặc Candidate khi GET.

## 5. DB assumptions tránh được

Không sửa file nào trong database/schema, SQL, seed, Entity hoặc application properties. Không chạy drop/create/migration/seeder. Published/deadline dùng đúng định nghĩa Dashboard hiện có; không thêm điều kiện approval/future-date tự đoán. Application cần AppliedCvUrl thật, không dùng fake URL. Không bịa PreferredQualifications, cover letter/note, Company/TalentPool/Resume table, profile route hoặc contact domain. DB text không được dịch.

**Sự cố kiểm thử:** wildcard `*Tests,!*DatabaseTests` đã bắt thêm RequisitionReviewProbeTests mới của checkout, có INSERT/UPDATE thử trong transaction rollback. Source/compiled annotations xác nhận Transactional/Rollback; identity counters có thể tăng. Không tuyên bố toàn bộ phiên không có transient DB writes, không sửa/reset/reseed để che sự cố. Từ đó chỉ chạy explicit safe classes. Chi tiết test doc/WORK_LOG.

## 6. UI/UX đã cải thiện và giữ lại

Giữ warm editorial palette, fonts Việt-safe có weight thật, restrained hairline/radius/shadow, compact type, salary transparency, native links/details/labels, skip link/focus/reduced motion/aria-live. Hero illustrative project document thay empty shapes; working principles thay fake photo/character art. Section rhythm có split/rows/timeline; common benefits một lần. Sticky/anchor offsets, job3/2/1, process4/2/1, application2/1. Mobile chips overflow cue và actual active departments trước0. Read-only fields có legend/style/Not provided. Không redesign app shell hoặc nghiệp vụ requisition.

Independent baseline27/40; 1P1 +3P2 +1P3 groups đã được xử lý trong một batch, không tự chấm điểm tăng. CLI2 hierarchy warnings là computed false positives; browser runtime detection3pages0 issues. Đây không chứng nhận accessibility toàn hệ thống.

## 7. Phần vẫn mang tính prototype

Mộc branding, company-wide hours/hybrid/leave/learning/equipment/review copy và project notes là fiction đã được duyệt trước, chưa là policy doanh nghiệp thật. Public role data là DB thật. Authenticated/apply screenshot fixture là Taylor Nguyen hư cấu, không dùng credential/profile người thật. Prototype file chỉ dẫn đến app; không chạy login/upload trên Python preview.

## 8. Backend còn thiếu

- CV upload/storage/validation/download/retention và transaction tạo Application cần triển khai riêng.
- Duplicate application, resubmission và profile correction policy chưa được xác nhận.
- Talent Pool enrollment/notification subscription, My Profile/My Applications routes chưa có backend; không tạo dead-end CTA.
- Official contact/legal/company media chưa có nguồn; không invent.

## 9. Verification, conflicts và điểm tiếp tục

| Check | Kết quả thực tế |
|---|---|
| Compile | PASS qua Maven test compile |
| Selected tests | PASS36 tests/9 explicit classes; focused confirmation22 PASS sau batch |
| Spring Context | PASS trong actual SQL Server read-only integration tests |
| JPA/schema/queries | PASS validate, actual FK/query/render SELECT checks |
| Browser layout/interaction | PASS46 checks ở confirmation1440/1024/768/375; no overflow/runtime errors, text min5.36:1 |
| JS syntax / diff whitespace | PASS |
| Real HTTP startup | **FAIL**: `Unable to establish loopback connection` / `java.net.SocketException: Invalid argument: connect`, Tomcat/PipeImpl |
| Real browser login/logout/apply E2E | NOT TESTED/BLOCKED bởi HTTP startup; MockMvc security flow không thay browser E2E |
| Real CV/upload/submission | NOT AVAILABLE, controls disabled |
| Zoom200 / NVDA / VoiceOver / Firefox / Safari / real device | NOT TESTED; steps C17 có sẵn |
| DB/schema/seed file edits | NO |
| Git merge conflict | SecurityConfig index vẫn UU, working file không conflict markers và compile/tests PASS; không tự stage/finish merge |

Giữ tất cả raw driver failures: Maven PowerShell property quoting, first DOM-ready/contrast-regex runner failures, B wrapper/background-pipe cleanup/recovery. Không sửa artifact FAIL thành PASS. Lượt cuối46checks dùng HTML public DB render và fictional authenticated/apply fixture; không tuyên bố production readiness.

Nối tiếp bằng WORK_LOG, flow doc và cases C01–C18. Khôi phục HTTP startup môi trường để test login journey thật trước khi mở upload/submit hoặc deploy. Không chạy wildcard probes/DB reset; không tự xử lý Git merge của teammate.

Questions skipped: 0 quyết định mới cần hỏi để hoàn tất phạm vi đã duyệt; những business/backend gaps trên chưa được tự triển khai.
