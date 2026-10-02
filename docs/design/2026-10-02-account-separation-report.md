# System Admin — tách Internal/Candidate account lifecycles

Ngày02/10/2026. Repo: `D:\kì 5\SWP\hireflow-api\RetireManagement\SWP_RMS_Group-2`. Implementation backend/UI hoàn tất theo rule user xác nhận; live HTTP E2E chưa xác nhận được. Không database/schema/seed/fileDB nào bị sửa trong đợt này.

## 1. Tiêu chí Internal / Candidate

Currentrole xác định lifecycle: Candidate là external; năm role nội bộ được RoleAuthorities/seed hỗ trợ là internal. Không dùng Candidate profile lịch sử để phân nhóm. Không chuyển account giữa hai loại; khi hired tạo User nội bộ mới và Department, giữ/deactivate Candidate cũ bằng hành động riêng. Không automaticdeactivate khi hired. MissingCandidateprofile là cảnh báo, không tựcreate/ghépemail/di chuyển dữ liệu.

## 2. Files changed — code

Paths dưới đây tính từ repo root.

| File | Sửa gì và vì sao |
|---|---|
| src/main/java/com/group2/rms/security/RoleAuthorities.java | Shared INTERNAL_ROLE_NAMES cho list/guard/metrics; authoritymappinglogin không đổi. |
| src/main/java/com/group2/rms/repository/CandidateRepository.java | BatchfindAllByAccountUserIdIn để linkage thật cho page, tránh mỗirowquery. |
| src/main/java/com/group2/rms/service/AccountListService.java | Internalpredicate/roles, Candidatepredicate/PageCandidateRow/date/profileID, scopeddeactivation; không classifytheoprofile. |
| src/main/java/com/group2/rms/service/AccountManagementService.java | createInternal/updateInternal/findForEdit guards; updatecross-lifecycle reject trước fieldassign, bỏ oldauto-profilecreation; CandidateDepartmentreject. |
| src/main/java/com/group2/rms/controller/AccountController.java | Admincreate/update dùngguardedmethods; CandidateUserId invalideditPOSTgate trướcerrors. |
| src/main/java/com/group2/rms/config/SecurityConfig.java | Chỉ matcherCandidateAdminrouteSystemAdmin, không authentication/CSRF/session redesign; GitindexUU giữ nguyên. |
| src/main/java/com/group2/rms/repository/DashboardMetricsRepository.java | GROUPBYstatus theo internalsharedroles/Candidaterole riêng, realdata. |
| src/main/java/com/group2/rms/service/DashboardService.java | Admin2groups, total=sumstatuses; health riêng; roleothermetrics giữ. |
| src/main/java/com/group2/rms/service/DashboardView.java | AccountSummaryfield/record; compatibleoldconstructor cho otherroles. |
| src/main/resources/templates/admin/accounts/list.html | Internalcontext/filter/EnglishCreateInternal; Deactivate/Inactivehidden; sameGETpagination/CSRF/dialog. |
| src/main/resources/templates/admin/accounts/form.html | InternalCreate/Edit, nativeDeptrequired, khôngCandidate-specialcase/JS. |
| src/main/resources/templates/dashboard/index.html | Accounts2summaryrows/realroute links, SystemHealthsection riêng. |
| src/main/resources/templates/fragments/sidebar.html | Workspace Accounts/Internal/Candidate và System/Monitoring/Requisitions; legacyfragmentuntouched. |
| src/main/resources/static/css/account-list.css | Candidatefiltergrid/note, finaldesktop5filterbaseline fix; nativeMobiledisclosure giữ. |
| src/main/resources/static/css/dashboard.css | Quietsummaryrows4counts; tabletstack/mobile2cols; existingrolemetrics giữ. |
| src/main/resources/static/css/workspace.css | Groupheading restrained, gridspanwholemobile nav; Menu behavior khôngđổi. |
| src/main/resources/static/js/account-list.js | Nullguard optionaladvancedfilters để reuseCandidatepage; dialog callbacks giữ. |
| src/test/java/com/group2/rms/SecurityFlowTests.java | Candidateadmin access/CSRF/data/UI, inactiveactions, forgedrole/CandidateeditURL, guardedcalls/fixtureexport và selectedinternalvalues. |
| src/test/java/com/group2/rms/service/AccountManagementServiceTests.java | Replaceoldconversion-success test bằngreject + bothdirections + CandidateDepartmentconstraint; registration/hash/email regression giữ. |
| src/test/java/com/group2/rms/service/AccountListServiceTests.java | Scopeddeactivationwronggroup/idempotency/hashpreservation. |
| src/test/java/com/group2/rms/service/AccountListQueryTests.java | ActualJPAreadonlycounts/statuses/filter/search/pagination, Dashboard/list consistency/profileID/date/legacyaudit. |

**Created:** `src/main/java/com/group2/rms/controller/CandidateAccountController.java`, `src/main/resources/templates/admin/accounts/candidates.html`, `scripts/check-account-separation-ui.mjs`. Newcontroller chỉ list/deactivate supportedaccountaccess; no profileediting.

**Deleted:** `src/main/resources/static/js/account-form.js`: Candidate-in-internal-form logic đã sai; dependencysearch xác nhận chỉ form dùng runtime, nativeDepartmentrequired thay thế; oldflowrefs explicitlyhistorical. Không xóa entity/repository/profile/history.

**Documentation modified:** PRODUCT.md, docs/WORK_LOG.md, docs/tests/README.md, docs/flows/README.md, docs/flows/{create-account-flow,update-account-flow,deactivate-account-flow,role-dashboard-flow}.md (currentnote, lịch sử retained). Newdocs: impact/report/critique/A/Breports underdocs/design; teststeps `docs/tests/2026-10-02-account-separation.md`; currentflow `docs/flows/account-lifecycle-flow.md`; preflight historical doc. QAartifacts/rawJSON/screenshots underdocs/tests/assets/2026-10-02-accounts và docs/design; Impeccable archiveexacttarget retained. No unrelatedRequisition/Career/Authredesign.

## 3. DashboardService / repository

Admin bỏ broadUsermetrics trongrenderpath, gọi internalAccountStatuses/candidateAccountStatuses với same sharedrolepredicate list. AccountSummary total là tổng statusrows, các statusthiếu=0. ApiMonitoringService.rows health giữdatasource cũ, khôngfakeprobe/metric. Entity schema giữnguyên; no applicationcounts invent.

## 4. Routes

Reused `/admin/accounts`, `/admin/accounts/new`, `/admin/accounts/{id}/edit`, POST `/admin/accounts`, `/{id}`, `/{id}/deactivate` chointernal. Added GET `/admin/candidate-accounts` và POST `/admin/candidate-accounts/{id}/deactivate`. Tất cả chỉSystemAdmin; routewronggroup404, Candidate/nonAdmin403, Guestredirectlogin. Publicregister/logout/login không đổi.

## 5. Sidebar

Dashboard → Accounts: Internal/Candidate → System: API Monitoring/JobRequisitions → BacktoCareers. Actualroutes, aria-current; newgroupheading span2columns mobile để category khônglẫnlinks. Khôngicons/newfeatures hoặcmobileMenu redesign. Legacyteammatesidebar giữ.

## 6. Internal Accounts

List onlysupportedinternalroles ởbackend. Search name/username/email, role/dept/status/sort/page. Dept meaningful. Title/subtitle/CreateInternal rõ. Deactivate English, namednativeconfirmation/Cancel/ESC/focusreturn; Inactivehideaction, stillEdit to manuallystatuschange supported.

## 7. Candidate Accounts

OnlyroleCandidatequery. Useridentity/status/date, linkedprofileID theoFK, missingprofilehonest. Search/status/sort/pagination. Không Department/internalroles/CreateCandidate/Editfake/Applicationcountguessed. Existingsoftdeactivation scopedCandidate, profile/historypreserved. Không Activate/Block/Edit mới trongscope.

## 8. Create/Update

Admininternalform excludesCandidate, backendrejectcraftedrole. AllcurrentinternalrolesrequireDepartment theoexistingbusinesslogic. CandidateUserId rejectedinternaledit eveninvalidPOST. Bothdirections cross-lifecycle rejectedsharedservice; internalrolechangeswithinapprovedgroup cònđược. Username/hashunchangedonedit; uniqueemail excludingcurrent; CreateActive/hash BCrypt/confirm8–32. Candidatepublicregistration usesexistingcreate()transactionUser+Profile. ReviewoldsetRole/autoprofile mismatch donebeforemodification; no recordremediation silently.

## 9. Dashboard

Accounts2quietgroups4values each +workingViewlinks; no8cardwall. SystemHealth separate. TypographyLora/SourceSans3, warm/forest, native/hairline giữ; noAuthredesign. Admincounts actualqueries, otherrole-scopedwidgets unchanged.

## 10. Business rules preserved

SchemaSOURCEOFTRUTH, noDB/seed/datarun. GlobalUsername/Emailuniqueness, readonlyusername, noordinarypasswordreplacement, Active/Inactive/Blocked, authInactive/Blockeddenied, session/formlogin/CSRF/noJWT. Candidateprofile separation and FK remain, registrations independent, historicalrelationspreserved. JobRequisitionteammateauthorization notredesigned; SecurityConfigunmergedindex notresolved/staged.

## 11. Backend / verification limitations

- Useremployeenewidentity mustunique toexistingCandidate evenifdeactivated; don't reuseemail or dropDBunique.
- CandidateUserwithoutProfile schemaallows: reportstate, noautorepair. Currentreadonlyaudit0historicalinternalprofiles/0CandidateDepartments; do notclassifyfuturehistoricalprofile bylinkage.
- New/unapprovedroles notsilentlyclassifiedinternal; extendconfirmedRoleAuthoritiesbusinessrule whenapproved.
- CandidateActivate/Block/Edit/profilecorrection/applicationcounts notimplemented inthisoversightpass. Needexplicitfuture requirement, notdeadbuttons.
- ActualHTTPstartup8771 FAIL TomcatPipeImpl loopback / SocketExceptionInvalidargumentconnect. ApplicationContext/JPA testsPASS butbrowserlogin/accountwrites/SMTP/liveE2E NOTTESTED. No productionreadinessclaim; environmentfix outsidethisscope.

## 12. Tests and actual results

| Check | Result / evidence |
|---|---|
| Earlycompile + finalcompile | PASS |
| Explicit9suite Maven final | **42PASS**,0failure/error/skip; target/account-separation-final-tests.log/SurefireXML |
| MVCContext / RmsApplicationContext / Hibernate-JPA queries | PASS |
| RealSQLServer readonlycomparison | PASS: internal10/Candidate265, category/status/list/Dashboard consistent; linkage/date actual; legacy0/0 |
| Browser initial / finalconfirmation | **112/112 →119/119PASS**, fivepages4width +modalCancel/ESC/focus/navgroups/filterbaseline/selectedformvalues; noPOST |
| Independentreview | A28/40baseline2P2; B2CLItypefalsepositives,3livepages,126textsamples min5.94:1/0fail; twoP2 +notewidthfixedonebatch |
| JSsyntax / gitdiffcheck | PASS |
| RealHTTPstartup | **FAIL**, loopbacksocketenvironmenterror; target/account-separation-startup.log |
| RealbrowserAuth/DBmutation/SMTP E2E | **NOT TESTED/BLOCKED**; manualA03/A04/A08/A13/A14 requiredafterHTTPenvironmentfix |

Runsteps: [testdoc](../tests/2026-10-02-account-separation.md). Actualpackages/class/methods: [flow](../flows/account-lifecycle-flow.md). Process/handoff: [WORK_LOG](../WORK_LOG.md). Source/runtime split implemented; not a visual-only separation.
