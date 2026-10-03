# Auth + Admin UI synchronized with Mộc careers

01/10/2026. Scope is the existing single-company Spring Boot/Thymeleaf UI. See
[impact analysis](2026-10-01-auth-admin-impact.md) before edits,
[combined critique/audit](2026-10-01-auth-admin-critique.md),
[test steps/results](../tests/2026-10-01-auth-admin-ui.md) and
[package/class/method flow](../flows/auth-admin-ui-flow.md).

## 1. Templates changed

Paths below are relative to `src/main/resources/templates/`:

| File | Change / reason |
|---|---|
| `auth/login.html` | Shared head/brand, focused form, English recovery link/feedback, native actions. |
| `auth/register.html` | Shared Auth layout, fixed-username/password hints, summary/inline field error association; same five fields. |
| `auth/forgot-password.html` | Same Auth family, generic confirmation/error accessibility; preserve route/form/email flow. |
| `auth/reset-password.html` | Same Auth family, correct valid/expired/mismatch states, no password echo; token route unchanged. |
| `dashboard/index.html` | Shared shell, real-bound flat metric rows/sections, clear Unavailable Features label, role scope retained. |
| `admin/accounts/list.html` | Dense backend-bound table, contextual mobile More Filters, same GET names/options/pagination; stable identity/unique Delete context. |
| `admin/accounts/form.html` | Shared shell, Account Information / Access & Status / Initial Password fieldsets; approved-rule hints, linked errors, same binding. |
| `admin/api-monitoring/index.html` | Shared shell, readable native endpoint table/feedback; retain actual probe action. |
| `careers/fragments.html` | Reuse same wordmark fragment; homepage direction/content/layout retained. |

Removed duplicate old header/logout leftovers during migration; one POST logout
remains per operational screen. No template file was deleted.

## 2. Shared fragments created/reused

| File under `templates/fragments/` | Status | Purpose |
|---|---|---|
| `brand.html` | Created | `wordmark(subtitle)` reused by careers, Auth and workspace. |
| `auth-layout.html` | Created | `brandPanel` flat forest/editorial desktop composition; compact mobile brand header. |
| `workspace-header.html` | Created | `header(role, fullName)` native logout, optional real name, Menu and skip link. |
| `messages.html` | Created | `validationSummary` backed by actual BindingResult. |
| `head.html` | Modified | Add `interfaceHead(title,pageStyles)` with local assets; preserve legacy `head(title)`. |
| `sidebar.html` | Modified | Add `workspace(activeMenu,role)` for confirmed routes/role context; preserve legacy teammate fragment. |

No security dialect/framework was added. Dashboard supplies a real name; other
Admin models supply no name, so only the known route role is shown there.

## 3. CSS / design tokens changed

All below under `static/css/`:

- **Created `design-tokens.css`:** exact existing font faces/palette/spacing extracted
  from careers.css. Source Sans 3 (400–600), Lora real600, Vietnamese subsets, local WOFF2.
- **Created `interface.css`:** native buttons/inputs/labels/focus/alerts/error summary,
  password toggle, semantic error/on-brand colors and restrained radius4/12.
- **Created `workspace.css`:**208/184px desktop sidebar,72px header, responsive nav/main,
  hairline tables, status text, breadcrumbs, contained scrolling.
- **Modified `careers.css`:** imports shared tokens; remaining homepage art direction unchanged.
- **Modified `auth.css`:** viewport two-zone brand/form,440px focused form, mobile compact header.
- **Modified `dashboard.css`:** real metric rows, tabular numbers,2-column tablet grouping.
- **Modified `account-form.css`:** semantic fieldset rhythm,2→1 columns, common buttons.
- **Modified `account-list.css`:** practical filters, sticky identity column, native disclosure,
  readable pagination/deactivation dialog; no card decoration.
- **Modified `api-monitoring.css`:** compact table/metadata, status words not broken.

Tokens4/8/12/16/24/32/48/64/80; offwhite `#faf9f6`, ink `#252923`, forest `#2e5a45`,
control border `#7b8b7d`, focus `#a06325`. Hairlines are decorative; controls have
separate contrast border. No gradient, blur, glass, new photograph or generated character.

## 4. Auth UI synchronized

Login/Register/Forgot/Reset share branding, width, labels, focus, buttons and recovery
feedback. Added safe Show/Hide (type=button, no storage/log/submit), linked server
error summary and hint associations. Native form names/actions/autocomplete/limits
and no-store/no-referrer reset behavior retained. Username immutability/password8–32
are previously approved rules, not new policy.

Background is a flat brand surface with type; no unapproved stock image or fake
workspace photography was needed. Mobile hides decorative story, preserves company
identity and practical form. No remember-me, Change Password or custom auth JS.

## 5. Admin UI synchronized

Dashboard/List/Create/Edit/API use the same shell. Dashboard keeps actual DashboardView
loops/scope/data; no invented chart/metric. List includes both internal/Candidate
accounts, seven existing columns and real filters. Mobile Department/Sort disclosure
keeps names/values; advanced-active state opens automatically. First identity column
remains visible during horizontal scrolling. Edit/Delete accessible names and modal
include stable username; Delete still confirms deactivation and safe Cancel focus.

Create keeps Active default/no status input; Edit username readonly/no password
replacement; status hint explains existing access outcome. API table retains real
backend endpoints/checks and unavailable external probes. Candidate workspace nav
has Dashboard/Careers; staff requisition link shown only for internal roles in the
**new** shell. This does not alter direct-route authorization.

## 6. Backend functionality preserved

- No production Java/entity/repository/service/DTO/controller/security/schema/seed edits
  were required for UI; native POST/GET/CSRF/session/RBAC retained.
- One **explicitly approved config correction**: `src/main/resources/application.properties`,
  remove trailing backtick from `spring.thymeleaf.encoding=UTF-8`. The typo appeared
  after the initial protected-file baseline and caused real `IllegalCharsetNameException`.
  Only that character was corrected, no secret/config rule printed or modified.
- `src/test/java/com/group2/rms/SecurityFlowTests.java`: two meaningful regression cases
  covering shared templates/routes/one logout/CSRF/readonly/invalid associations/no
  service writes plus optional fictional fixture export; role nav check and active filters.
- New QA helpers `scripts/prepare-auth-admin-preview.py` and
  `scripts/check-auth-admin-ui.mjs`; no production dependency/backend.
- UI fixtures are exported only with `-Dui.preview=true` under ignored target;
  reset/CSRF tokens in screenshots are fictional/replaced, not live credentials.

Final test/verification results are recorded in the test document; do not infer
real login/persistence/SMTP from a static fixture screenshot.

Final explicit9suite run: **38 tests PASS**,0failure/error/skip; Context/JPA/SELECT
PASS. Protected baseline87/87 hashes match, including config after the approved
one-character correction. Browser raw203/204PASS; only failed assertion counted a
collapsed0px Grid track. Corrected targeted checkPASS4visible columns, no UI edit or
additional screenshot round; raw failure is retained. UI contrast minimum5.935:1
for measured visible text. HTTP startupFAIL remains separate from these results.

## 7. Existing inconsistencies discovered

1. Auth/main/Admin previously used divergent styles/external font shell; resolved for scope.
2. Legacy sidebar lists unimplemented profile/interview/notification/offer routes;
   new shell includes implemented destinations only. Legacy teammate navigation remains.
3. Admin name is absent from Account/API model; retained honest role-only header.
4. Requisition route authorization is not a confirmed per-role rule in this milestone;
   hiding staff navigation for Candidate is not a security fix. Teammate backend unchanged.
5. API external probes, AI Config and several Dashboard datasources remain unimplemented;
   real-bound reasons retained. UI cannot implement them by inventing settings/metrics.
6. Git index has pre-existing `UU SecurityConfig.java`; working source has no markers.
   No stage/merge resolution/overwrite was performed.
7. HTTP Tomcat startup encounters NIO/PipeImpl loopback socket failure; config typo
   caused an additional later Context failure. Preserve both actual error logs.

## 8. Pages not changed and why

Hello/demo and teammate Requisition list/form/detail use their existing legacy shell;
outside requested Auth/Admin visual scope. No new AI Configuration, Account Detail,
Role Management, Candidate Profile, Applications, CV upload, Notifications or
Change Password page was fabricated. Public homepage received shared wordmark/token
reuse only; no redesign of the existing editorial direction or DB job content.

## 9. Remaining prototype / mock elements

Mộc name/leaf/editorial story remain previously approved fictional company branding,
pending official company assets. QA HTML/photos/metrics/accounts are **test fixtures**,
not records seeded into the product; there is no mocked runtime business datasource.
Actual labels are English and database titles/names/departments stay verbatim.
Signed-in header-name continuity needs a future confirmed model change. CV/application
submission, AI/external monitoring and missing datasource limits remain existing backend
gaps. Real SMTP/browser auth/device/screen-reader proof still needs the external tests.

## 10. UI UX Pro Max + Impeccable results

UI UX Pro Max recommendations applied to native error summary/labels/local table
overflow/44px controls, font/palette/type/spacing and mobile density; no new SaaS system.
Impeccable A independent design baseline **28/40 Good**, cognitive3/8fail; B one CLI
scan33raw warnings, in-scope3shared-CSS false positives,30outside scope. Five browser
warnings:4intentional contrast-safe forest captions +1table-wrapper false positive.
No ignore/hook changes; detector exit0 was not described as a clean scan.

Combined technical audit **16/20 baseline**, conservative; no WCAG certification.
Five P2 groups were handled in one final polish batch: identity/context, hints,
mobile filters, tablet metric grouping, role-oriented nav/clear unavailable label.
No follow-up score was invented. Exact snapshot/trend and confirmation results are
recorded in WORK_LOG/test documentation after verification and cleanup.
