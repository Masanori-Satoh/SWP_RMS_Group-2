# Accounts separation — Assessment A

Independent design review, 02 October 2026. Operate mode. Assessment A was completed without seeing Assessment B or detector output.

## Scope and evidence

Reviewed `admin/accounts/list.html`, `admin/accounts/candidates.html`, `admin/accounts/form.html`, `dashboard/index.html`, workspace sidebar/header, design tokens, shared interface styles/scripts, and account styles/scripts. Read PRODUCT.md and Impeccable critique guidance. DESIGN.md does not exist. The session context launcher had already run in the parent session. No application source was edited.

The current confirmed policy takes precedence over older PRODUCT.md account descriptions: current Candidate role defines the external account lifecycle; Candidate profile linkage does not classify an account; conversion to or from internal roles is forbidden.

Browser inspection used five fresh tabs in an isolated headless Chrome session, CDP port 9261, against the Thymeleaf fixture server at `http://127.0.0.1:8770/preview/`. Each page was inspected once at 1440 and 375 px. Native/Node kernel access failed; the repository's `scripts/career-browser.mjs` shell fallback succeeded. Screenshots and structured evidence are in `docs/tests/assets/2026-10-02-accounts/a-*`. The Chrome process and its validated temporary profile were closed/removed by the helper's `finally` cleanup. The parent owns the preview server.

This is browser evidence for rendered fixtures and client interactions. It does not prove authentication, persistence, DB classification/counts, or POST operations. No forms were submitted.

## Design specificity

The composition is grounded in Mộc's established visual language: Lora headings/wordmark, Source Sans 3 operational text, warm page background, forest actions, hairline divisions, and native controls. It supports account administration through compact rows and explicit account categories. The dashboard repeats the same Internal Accounts / Candidate Accounts vocabulary and provides visible entry links. This is a coherent extension of the incumbent workspace.

## Priority issues

1. **P2 — Desktop internal filters lose their shared baseline.** In `a-accounts-1440.png`, Department sits above Sort By in one column while Search, Role, and Status align below it; the fifth grid column remains unused. This adds an unnecessary vertical step and makes Department look separate from the filter set. Relevant rules: `account-list.css:4` and `account-list.css:13`; the native details content does not produce the intended five peer grid items. Give the desktop secondary filters an explicit grid area/container, or place all five fields in a stable layout while retaining mobile disclosure. Candidate filters already show a clean aligned row.

2. **P2 — Mobile navigation group labels become peer grid cells.** In `a-menu-375.png`, Accounts appears beside Dashboard rather than above its account links, and System shares a row with API Monitoring. At 375 px, the two-column `.workspace-nav` rule (`workspace.css:47`) treats group spans (`sidebar.html:42,45`) like links. Make each group label span both columns or group its child links structurally so the category is clear. Keep the existing labels, routes, and compact menu behavior.

Only these two issues have enough source and browser evidence to warrant action. No blocking issue was observed in this fixture review.

## Nielsen heuristic scores

All ten apply to this operational surface; no n/a scores. Total **28/40 — Good**.

| Heuristic | Score | Evidence / practical limit |
|---|---:|---|
| Visibility of system status | 3 | Matching counts, text status badges, current nav state, active filter copy, and success/validation regions exist. Save feedback was not exercised through a POST. |
| Match between system and real world | 3 | Account categories, role/status terms, and access consequences are understandable. Profile missing is honest but offers no correction path in current scope. |
| User control and freedom | 3 | Clear Filters, breadcrumbs, form Cancel, modal Cancel/Escape, and focus restoration. No status operation was committed. |
| Consistency and standards | 3 | Shared shell, native inputs/selects/dialog, consistent actions. The two evidenced layout issues weaken group relationships. |
| Error prevention | 3 | Candidate absent from internal role options, required departments, fixed username, password bounds, and explicit deactivate confirmation. Backend policy enforcement is outside this review's proof. |
| Recognition rather than recall | 3 | Visible category names, labels, hints, and sticky identity column retain context during mobile table scrolling. |
| Flexibility and efficiency | 2 | Search/filter/sort/pagination and keyboard-friendly controls support routine use. No batch path or other expert accelerator is present; do not add one without a business requirement. |
| Aesthetic and minimalist design | 3 | Calm, coherent hierarchy with one primary creation action and restrained status color. Desktop internal filters add avoidable empty space. |
| Recognize, diagnose, and recover from errors | 3 | Source has per-field errors, aria references, and linked validation summary preserving form context. Actual validation response was not exercised. |
| Help and documentation | 2 | Task-specific inline hints explain password length, immutable username, account access, and preserved candidate history. No searchable help is exposed on these screens. |

## Cognitive load checklist

| Check | Result | Evidence |
|---|---|---|
| Single focus | Pass | Each account list/form has a clear account-access task; candidate page offers no internal create/edit controls. |
| Chunking | Fail on desktop internal filters | Five exposed filter fields exceed the checklist's four-item group; their unintended stacking compounds it. Forms split account information, access/status, and initial password. |
| Grouping | Fail in expanded mobile nav | Accounts/System headings are interleaved with unrelated grid cells. Desktop nav and form fieldsets group correctly. |
| Visual hierarchy | Pass | Page title, section heading, filter labels, and data rows are differentiated without decorative clutter. |
| One thing at a time | Pass | Forms follow a natural top-to-bottom sequence; dialogs isolate the deactivation decision. |
| Minimal choices | Pass with native disclosure caveat | Closed selects show one selection; normal task actions stay below four. Opening Role reveals five internal choices plus a placeholder; keep this native domain selector rather than removing legitimate roles. |
| Working memory | Pass | Name and username are repeated in confirmation; mobile table retains the name column. |
| Progressive disclosure | Pass | Mobile secondary filters close by default; menu and deactivate dialog appear only on demand. |

Two checklist failures: **moderate cognitive load** concentrated in the two layout issues. Explicit decision points above four: desktop internal filters expose five criteria; opened internal Role selectors contain five valid roles. Candidate list filters contain three criteria; each dashboard account summary shows four counts.

## Emotional journey and personas

Entry is reassuring: account categories are named consistently in nav, title, and dashboard summary. Candidate access and recruitment history are visibly distinguished. Deactivation is the highest-stakes moment; the dialog names the person/username, states that sign-in ends, and confirms history preservation. Cancel receives initial focus, Escape closes, and focus returns to the triggering button.

For a first-time System Admin, the mobile menu grouping is the main orientation risk. For a busy admin working at desktop, uneven internal filters interrupt quick scanning. For a mobile user, horizontal tables require a deliberate scroll, but the text instruction and sticky name preserve context, and actions were reachable after scrolling. No decorative motion or forced onboarding impedes these personas.

## Strengths

- Internal and Candidate account tasks are visibly separated across dashboard, sidebar, headings, and role options. The internal create/edit fixture contains the five supported internal roles and no Candidate option.
- Native dialogs and controls give predictable keyboard behavior. Mobile Menu open/Escape/focus return, both deactivate dialogs' Escape/focus return, and password Show/Hide were exercised successfully.
- All five 375 px pages kept document width at 375 px. Table overflow is confined to its focusable region (343 px viewport / 920 px table), with a sticky identity column and a scroll instruction. Dashboard summaries reflow to four counts in two columns and forms to one column.

## Minor observations and limitations

- Edit fixture shows Role/Department placeholders rather than populated selections. The parent confirmed that its sample user retained legacy Candidate role 6 / null Department after being changed to an internal row. This is a fixture data issue; the parent will align it to Interviewer role 5 / Department 1. Source uses `th:field`. This observation is not a production regression and does not trigger another browser round for Assessment A.
- Candidate Profile missing is a truthful exceptional state. A correction flow has not been authorized/implemented; do not invent an action.
- The department hint says “Required for all supported internal roles.” A future copy pass could use more direct task wording, but this is not a release issue.
- Mobile filter DOM rectangles can exist inside closed details even though contents are not painted. The screenshot and `details.open=false` are the evidence for disclosure; the JSON field list is not a visibility audit.
- The candidate-dialog capture contains browser scroll/skip-link artifacts from full-page capture after table interaction; the dialog itself remains legible. Modal status and focus were measured directly.
- No live POST, login/session, screen-reader, empty result, pagination activation, active secondary-filter rendering, or server-validation response was exercised. Parent backend test results must remain separately labeled.

## Questions for synthesis

- Should fixing the desktop filter row and mobile navigation grouping be the final visual scope for this change?
- Is there an approved future workflow for Candidate account recovery, or should the current deactivate-only access screen remain its explicit scope?

Questions are recommendations to the parent synthesis, not permission requirements for this read-only assessment.
