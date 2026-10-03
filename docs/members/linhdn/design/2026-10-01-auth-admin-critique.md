# Auth + Admin — combined critique and technical audit

Assessment baseline: 01/10/2026, immediately before the final polish batch.
Target file for archive identity: `src/main/resources/templates/admin/accounts/list.html`.
Scope: Login/Register/Dashboard/Account List/Create/Edit plus Forgot/Reset/API and shared UI.
User requested the implementation and review; this report records the review input,
not a production-readiness certification. Final verification is in the test document.

## Method and specificity

**PASS: coherent one-company recruitment product.** Warm forest/offwhite, local Lora/
Source Sans 3, shared leaf wordmark, hairlines and restrained operational forms/tables.
No marketplace/company directory/employer pricing or invented KPI was introduced.
Candidate accounts and Delete→Inactive remain part of account management.

Assessment A completed and was read before parent received Assessment B findings.
A: independent design/UX plus own64 route-width views and19 reversible controls.
B: isolated detector/browser verification, one CLI scan and five injected pages.
Both use fictional MockMvc Auth/Admin fixtures, not live account mutations/login.

## Design health — baseline

| # | Nielsen heuristic | Score | Main evidence / remaining issue |
|---|---|---:|---|
| 1 | Visibility of System Status | 3 | Active nav, scope/count, statuses, real-bound feedback; live server transitions not exercised. |
| 2 | Match System / Real World | 3 | Recruitment account vocabulary; No Datasource/role navigation need clearer orientation. |
| 3 | User Control and Freedom | 3 | Back/Cancel/Clear/Escape; deactivation explicit and reversible through Status, no invented undo. |
| 4 | Consistency and Standards | 3 | Shared identity/type/controls; some tablet grouping and header identity differences. |
| 5 | Error Prevention | 3 | Required/length/confirmation/readonly; missing pre-commit hints. |
| 6 | Recognition Rather Than Recall | 3 | Native labels/linked errors; narrow table separates identity/actions. |
| 7 | Flexibility and Efficiency | 2 | GET filters/sort/pagination; phone filter panel delays first record. |
| 8 | Aesthetic and Minimalist Design | 3 | Restrained surfaces; four metrics become3+1 at768. |
| 9 | Error Recovery | 3 | Summary + inline errors; expired reset/new-link recovery; login recovery copy can be clearer. |
| 10 | Help and Documentation | 2 | Existing hints; fixed username/password length/status consequences need earlier guidance. |
| **Total** | | **28/40 — Good** | All ten apply; no n/a. Score is the baseline, not a post-polish score claim. |

### Cognitive load

Eight checks: Single focus PASS; Chunking FAIL; Grouping PASS; Visual hierarchy PASS;
One thing at a time PASS; Minimal choices FAIL; Working memory FAIL; Progressive
disclosure PASS. **3/8 failures, moderate.** Main decision points: five Register
fields, five account search/filter/sort controls. Native role select contains six
allowed options, not six simultaneously competing CTAs. Preserve legitimate data.

### Emotional journey and persona red flags

Auth entry is calm; uncertainty peaks while choosing a fixed username/password.
List mobile adds vertical then horizontal movement. Safe confirmation and linked
errors restore control. Final recovery/logout states remain honest.

- Alex/Admin: first result and actions should stay close; do not invent bulk actions.
- Jordan/Candidate: explain identity choices upfront; keep navigation focused on confirmed candidate destinations.
- Sam/keyboard: preserve focus/native semantics and account context; screen-reader proof still absent.

### Three strengths

1. A shared company identity with a quiet, practical operational reading rhythm.
2. Working password visibility/error links/Menu Escape/Cancel and safe dialog focus.
3. Honest role/account lifecycle and real-bound metrics/unavailable states.

## Five priority groups — P2; no P0/P1 established

| # | Confirmed observation | Final batch action / scope limit |
|---|---|---|
| 1 | Table identity leaves view while reaching Actions; confirmation uses name only; API status words break | Sticky account identity column; include row username in dialog/accessibility names; nowrap status; contextual scroll cue. Keep all existing columns and routes. |
| 2 | Register lacks visible8–32 hint; fixed username explained after creation; Edit Status consequences unclear | Add approved-rule hints with aria-describedby, retain inline error association/DTO policy; generic Login recovery link. |
| 3 | Mobile filters fill first screen | Native More Filters disclosure for Department/Sort on narrow screens; keep Search/Role/Status, GET names/values; auto-open when advanced filters active; no-JS visible fields. |
| 4 | Four metrics become3+1 at768 | Two-column intermediate overview, same model loops/data. |
| 5 | Candidate sees internal requisition nav; No Datasource is technical wording | New workspace nav hides staff link for Candidate, keeps Dashboard/Careers. Rename literal Unavailable Features. **No change to teammate routes/backend/security**; direct-route requisition authorization remains its existing rule. |

Minor P3: Auth statement measure creates isolated word; adjust measure/remove hard
break. Admin user name is absent from model outside Dashboard: document it, don't
invent header identity. Duplicate Careers exit is useful on mobile and stays;
no new missing-page links, remember-me, profile or AI Config forms.

## Detector synthesis — findings are verified, not blindly applied

CLI run once against whole templates tree: **33 warnings**, exit0. Raw21 low-contrast,
5 flat-type-hierarchy,3 tiny-text,3 overused-font,1 cramped-padding. In scope3 flat-type
claims are **false positives**: shared Thymeleaf CSS resolves body16/h136/h220/h318,
metric labels16 beside values32. Outside scope30: Careers2/Requisitions28; preserved,
not silently fixed or added to ignore rules.

Browser injection five views: **5 raw warnings** (3 unique observations). Four
gray-on-color caption occurrences are intentional forest treatment, measured5.99:1;
one wrapper cramped-padding is false positive because actual table cells have12px
inset. No browser warning promoted to priority. A found genuine workflow/density
issues missed by the narrow static/runtime aesthetic detectors. No ignore/hook edits.

Native CUA failed ACL/kernel; headless injection executed but **no user-visible overlay
was verified**. One background helper startup used same-attempt server.json when
stdout stayed open; first runner timeout was kept, recovery used that helper.
B's helper/profile/token-bearing temp files were cleaned; parent preview stayed for QA.

## Technical audit — baseline, conservative score

| Dimension | Score /4 | Evidence / limit |
|---|---:|---|
| Accessibility | 3 | Labels/reference IDs/native controls/linked errors/focus and measured contrast; no screen-reader/zoom certification. |
| Performance | 3 | Local fonts, small vanilla JS, no image/animation/framework payload; no production perf benchmark. |
| Responsive | 3 | Four widths and contained table overflow; the tablet/mobile composition issues above remain before polish. |
| Theming | 3 | Shared light-theme tokens; semantic error/on-brand tokens; no requested dark mode, no claim of tested dark theme. |
| Implementation integrity | 4 | Real template binding/actions/CSRF, no fake backend/KPI; context verified detector exceptions. |
| **Total** | **16/20 — Good** | Audit describes tested scope, not whole legacy project. |

## Verification boundaries and close

First root shots had one failed asset load and are not visual proof. Valid recovery
shots agree with A's fresh pages. A table ArrowRight failure was a background-tab
driver error: foreground target + codeArrowRight moved0→40; fix driver, not UI.
Keep original FAIL artifacts. HTTP startup failed Tomcat/PipeImpl loopback socket;
MockMvc/controller and static browser checks do not replace live E2E/SMTP.

The authorized polish handles these five groups without backend/schema changes.
Remaining model-name/real HTTP/teammate authorization questions are documented as
limits. No aesthetic redesign or unsupported business decision is needed.
