Method: independent Assessment A by `/root/auth_admin_design_review`; no Assessment B, detector output, or prior critique read.

# Auth/Admin design assessment A — 01/10/2026

## Scope and evidence

Primary journey: Login, Register, Dashboard, Account List, Create Account, Edit Account. Related views: Forgot Password, Reset Password, API Monitoring; login error/logout, registration and account validation errors, recovery request received, expired reset link, and Delete confirmation.

Read current `PRODUCT.md`, the current design-system direction, relevant templates/fragments/styles/scripts, and the Impeccable critique checklist. Reviewed the frozen templates after the shared-header correction. All six primary views were visually inspected at 1440/1024/768/375 px using root's existing round1 captures; related base views were inspected at the same four widths. Error/recovery/confirmation captures were inspected where available. Initial unstyled Login captures were rejected as unreliable: independent fresh-tab layout and the valid `round1-recovery-login-*` captures agree with the source. They are not a product design finding.

Independent browser evidence: `docs/tests/assets/2026-10-01-auth-admin/assessment-a-browser-check.json`, produced by a new Chrome profile and fresh tabs on port 9251. It contains 64 route/width observations, 19 reversible control checks, zero JavaScript runtime exceptions and no horizontal page overflow. Show/Hide, mobile Menu/Escape, error-summary links, and Delete-open/Cancel/Escape worked. Delete focused Cancel and restored focus to its trigger. Chrome and the validated temporary profile were cleaned up. No new screenshots and no POST were made by Assessment A.

The server uses fictional MockMvc model data. These observations establish preview rendering and client interaction, not real login/session expiry, account persistence, SMTP delivery, real metrics, or authorization E2E. Source observation of conditional role navigation is distinguished below from visual evidence; the supplied Dashboard fixture is System Admin.

## Design specificity

**Verdict: coherent and recognizably authored for the Mộc recruitment journey, with moderate specificity in the operational screens.** The leaf wordmark, forest/offwhite palette, Lora headings and Source Sans 3 controls connect the public career experience to authentication and administration. Authentication has a calm two-part composition; the workspace uses hairlines, tables and grouped forms. The role/status vocabulary and account scope belong to this recruitment product.

Native operational patterns are appropriate here. They do not need more decoration or marketing narrative. The strongest opportunity is to carry the same clarity into mobile account work and role navigation. The repeated brand statement is generic, and its desktop line wrapping is less deliberate than the otherwise restrained type system.

## Design health

All ten heuristics apply to this complete Auth/Admin journey. Scores are 0–4; no heuristic is marked n/a.

| # | Heuristic | Score | Evidence and remaining issue |
|---|---|---:|---|
| 1 | Visibility of System Status | 3 | Active navigation, role/scope, matching count, status badges, success/error messages and focused validation summary are visible. Real server transitions were not exercised. |
| 2 | Match System / Real World | 3 | Account roles, Active/Inactive/Blocked and the deactivation explanation are understandable. `No Datasource` and Candidate's unconditional Job Requisitions link weaken audience clarity. |
| 3 | User Control and Freedom | 3 | Back links, Cancel, Clear Filters, Menu Escape and dialog Cancel/Escape provide clear exits. No immediate undo is shown for deactivation; status editing supplies a later route. |
| 4 | Consistency and Standards | 3 | Shared branding, controls, typography, alerts and shell remain consistent across all six views. Some responsive composition and account identity details diverge. |
| 5 | Error Prevention | 3 | Required/select controls, password length constraints, Show/Hide, conditional department requirement, fixed username on Edit and safe confirmation help. Register hides password rules; immutable username and status consequences are not explained at the decision point. |
| 6 | Recognition Rather Than Recall | 3 | Text navigation, labels, source-preserved values and linked errors are clear. Narrow tables separate account identity from row actions. |
| 7 | Flexibility and Efficiency | 2 | Search, filter, sort, pagination and native controls offer useful paths. On mobile, filters occupy most of the first viewport and actions require additional horizontal movement. No need for new bulk actions was established. |
| 8 | Aesthetic and Minimalist Design | 3 | Flat surfaces, restrained color and clear forms avoid visual clutter. Dashboard at 768 creates a 3+1 metric arrangement; filter density and some wrapping weaken the hierarchy. |
| 9 | Error Recovery | 3 | Errors sit beside fields and in a summary; checked links focus the invalid field. Expired reset provides a direct new-link action. Login failure is visible but offers only `Please try again` as immediate guidance. |
| 10 | Help and Documentation | 2 | Department, password and readonly username hints provide basic contextual help. Register/Create need earlier guidance for fixed username and Register password length; Edit needs access consequences beside Status. |
| **Total** | | **28/40** | **Good — targeted usability improvements remain.** |

### Screen notes

| Screen | What works | Specific remaining concern |
|---|---|---|
| Login | Two fields, a clear Sign In action, password visibility, recovery/account links; compact single-column mobile layout. | Failure message does not point directly to the existing recovery path. Desktop brand phrase leaves `starts` alone on a line. |
| Register | Clear one-company account purpose, consistent labels, Show/Hide, usable invalid-state summary and field errors. | Five fields form one uninterrupted block; password length and fixed username are not visible before input. |
| Dashboard | Role/scope precede metrics; values and details are clearly separated; honest unavailable section; no invented chart. | Four metrics become 3+1 at 768. `No Datasource` describes implementation rather than a user's next step. |
| Account List | Candidate is explicitly included in copy, rows and role choices; useful GET filters, text actions, status words, pagination, safe dialog. | At 375, rows begin below the first viewport; table identity and Actions cannot be seen together. Horizontal scroll is contained, not page overflow. |
| Create | Account Information / Access & Status / Initial Password reduce scanning cost; default Active is stated; Cancel remains visible at the end. | The username decision lacks the warning shown only later on Edit. Mobile completion requires a long but structured scroll. |
| Edit | Username is visibly readonly; existing password is preserved; Candidate/department behavior remains clear; Save and Cancel are distinct. | Status is a consequential access decision with no local explanation. Signed-in person's name appears on Dashboard but disappears from Admin headers. |

## Cognitive load — eight checks

| Check | Result | Reason |
|---|---|---|
| Single focus | Pass | Each auth/form view has one primary completion action; list-level Create and filter actions belong to distinct sections. |
| Chunking | Fail | Register has five consecutive fields; Account List presents search plus four peer filter/sort controls. |
| Grouping | Pass | Create/Edit use semantic fieldsets and consistent proximity; overview and unavailable content are separated. |
| Visual hierarchy | Pass | Serif page title, quieter explanatory text, labelled controls and one forest completion button make the main task clear. |
| One thing at a time | Pass | Inputs and selects follow a stable sequence; deactivation is disclosed only after Delete. |
| Minimal choices | Fail | The account filter decision presents five controls together, followed by Apply/Clear. Six roles are behind one native select, not six competing persistent buttons. |
| Working memory | Fail | At 375–1024, horizontal movement separates row identity from Actions. The confirmation repeats a full name but omits the unique username/email. |
| Progressive disclosure | Pass | Mobile navigation, Delete confirmation, create-only password controls and role-dependent department validation expose complexity where needed. |

**Three failed checks: moderate cognitive load.** Main decision points over four visible peers are the five account filter/sort controls and the five Register fields. The operational seven-column table also asks for extensive comparison; that is legitimate task information, so preserve its semantic table and improve context rather than removing DB fields.

## Emotional journey

The entry is calm and credible: the same company identity survives the move from careers to Sign In, and mobile keeps the form first. Register's valley is uncertainty while choosing a username and password; the UI explains the fixed username only after that decision has already been made. Linked errors improve recovery and make failure feel repairable.

The workspace peak is clarity about account scope and status. The phone list creates the main valley: scroll through filters, then scroll the table to reach actions. Deletion recovers trust through a named account, an explicit Inactive outcome, preserved-data explanation and a safe default focus on Cancel. The final states are restrained and truthful: signed out, recovery request received, and expired reset link. They do not claim mail delivery or a completed account mutation during this preview review.

## Strengths

1. **A consistent company identity with an operational reading rhythm.** The forest Auth panel, quiet workspace, hairlines and serif/sans pairing connect the full journey without making administration feel promotional.
2. **Good recovery and control foundations.** Labels, visible focus, linked validation summary, Show/Hide, Cancel and Escape worked in the independent browser check. Error colors are backed by text and semantic alerts.
3. **Honest account and data semantics.** Candidate appears alongside internal roles. Delete explains Inactive, sign-in loss and retained related data; Edit preserves username/password. Unavailable features stay visibly unavailable and existing metrics remain bound to the supplied model.

## Priority improvements

No P0 or P1 design blocker was established by this review. The following five groups are P2: users have a workable route, but unnecessary effort or unclear consequences remain.

### [P2] Keep account identity visible when reaching table actions

**Evidence:** `round1-accounts-375/768/1024.png`; `admin/accounts/list.html:84–115`; `account-list.css:7–10`. Actions are to the right of a minimum 920 px table. The dialog identifies only `fullName`; two people can share that name. Related API screenshots at 768/1024 break `Operational` into `Operationa` and `l`.

**Why it matters:** Admins must keep the account in memory while scrolling to Edit/Delete. A unique identity in confirmation would further reduce mistaken-account risk. Broken status words slow endpoint scanning.

**Fix:** Preserve the semantic table and bounded scroll. Keep an account identity column visible while horizontally scrolling, and repeat the existing row username or email in the confirmation beside full name. Give status badges enough width or prevent breaks inside status words. Show the horizontal-scroll cue when the table actually overflows; it is currently also shown at 1440 when all columns fit.

**Suggested command:** `$impeccable adapt`, followed by `$impeccable harden` for the confirmation identity.

### [P2] Explain existing input and access consequences before commitment

**Evidence:** `auth/register.html:18–31`; `admin/accounts/form.html:37–45,75–87`; valid Register/Create/Edit captures. Register has `minlength=8` and `maxlength=32` but no visible length hint. The fixed-username warning exists only on Edit. Status lacks a local consequence hint.

**Why it matters:** People must discover password rules through rejection and learn that their username is fixed after creation. Admins can remove sign-in access through Status without the explanation supplied by Delete.

**Fix:** Add the already-confirmed `8–32 characters` hint to Register; put `Username cannot be changed after creation` beside Username in Register and Create; put a concise English explanation of Inactive/Blocked sign-in access beside Status on Edit. Associate these hints through `aria-describedby` while preserving error references and all DTO constraints. Do not add new password policy or status rules.

**Suggested command:** `$impeccable clarify`.

### [P2] Reduce how much the account filter panel delays the first row on a phone

**Evidence:** `round1-accounts-375.png`; `admin/accounts/list.html:36–81`; `account-list.css:4–6,19`. Search, Role, Department, Status, Sort and Apply/Clear precede the table; the first row is below a 900 px first viewport.

**Why it matters:** The frequent task is finding and acting on a person. Filters currently consume the first screen before any account is visible, then the table introduces a second scrolling direction.

**Fix:** Keep Search and frequent access filters exposed; group Department and Sort under a small native disclosure at narrow widths, or reduce the panel's vertical rhythm. Keep all existing GET names and selected values. Expand the disclosure when a contained non-default filter is active and provide a clear active-filter indication. No backend or filtering behavior change is needed.

**Suggested command:** `$impeccable distill`, then `$impeccable adapt`.

### [P2] Preserve the overview grouping at tablet width

**Evidence:** `round1-dashboard-768.png`; `dashboard.css:5,26`. Four account metrics lay out as three columns followed by one orphan metric; 1440/1024 show four and 375 shows two-by-two.

**Why it matters:** Blocked Accounts becomes a separate visual group even though all four values describe the same account overview. It adds height before shortcuts and makes comparison less immediate.

**Fix:** Use two columns in the intermediate width range where four metrics do not comfortably fit. Keep the existing values, titles, details and model binding.

**Suggested command:** `$impeccable layout`.

### [P2] Make workspace orientation match the viewer's role

**Evidence:** source `fragments/sidebar.html:38–45` renders Job Requisitions unconditionally while Accounts/API are limited to System Admin. `dashboard/index.html:83` labels the unavailable area `No Datasource`. Candidate-specific rendering is a source observation; the supplied Dashboard visual fixture is System Admin. This is not evidence of an authorization failure.

**Why it matters:** Candidate and staff use the same identity system but need different destinations. An internal requisition link makes a Candidate interpret staff operations as part of their journey. `No Datasource` adds implementation vocabulary to a user-facing status.

**Fix:** Use the existing role parameter to expose only the confirmed destinations appropriate to that viewer, including the public career return for Candidate. Keep the teammate's requisition implementation unchanged. Rename the literal unavailable-section title to `Unavailable Features` or equivalent clear English; retain source-bound reasons and data.

**Suggested command:** `$impeccable clarify`.

## Persona red flags

**Alex — frequent System Admin:** Search/filter/sort and readable desktop rows support fast work. At 375, the first record is below the initial screen and Edit/Delete are off to the right. The 768 overview's 3+1 metrics also interrupt comparison. No requirement for bulk editing or keyboard shortcuts was established, so their absence is not a demand to add features.

**Jordan — Candidate registering for the first time:** The company and next action are clear. Username looks like an ordinary editable preference, and Password gives no visible length guidance. Jordan finds both rules too late. Source-level Job Requisitions in Candidate navigation would introduce an internal recruiting task unrelated to their expected public-career path.

**Sam — keyboard/low-vision user:** Native inputs, text labels, focus styling and alert text are strong. Summary error links and Escape return were actually checked. Narrow tables still impose context movement and the API status word breaks at tablet width. Full keyboard completion, screen-reader announcements and a WCAG audit were not exercised; semantic source alone does not prove them.

No project-specific persona was invented: no `AGENTS.md` Design Context was found in the checked project/parent locations.

## Minor observations

- **[P3] Auth statement wrapping:** At 1440 `Good work / starts / with clarity.` isolates one word, while 1024 produces a cleaner two-line phrase. Size the story width to the heading, or remove the hard break and control the measure; the body-text `38ch` measure currently constrains the larger heading.
- **[P3] Repeated Back to Careers:** Login/Register show it in both the brand header and form links. It is understandable, but the phone Login footer becomes an extra row for the same destination.
- **[P3] Signed-in identity continuity:** Dashboard names the person, while Account List/Create/Edit headers show only System Admin. Keeping available identity context consistent would help on shared or multiple-session machines; do not invent model data to fill the gap.
- Login error could point to the existing Forgot Password link in its recovery wording. Preserve the generic authentication outcome rather than revealing whether an account exists.
- `Register` and `Create your account` refer to the same action. A uniform action phrase would be slightly easier to scan; this is cosmetic, not a broken flow.

## Questions for synthesis

1. Can a phone Admin keep the account's identity in view from search result through confirmation?
2. Which existing rules should be visible before a user creates an identity or removes access?
3. What should a Candidate's workspace invite them to do next using only the routes already confirmed?

Questions skipped: this is the independent Assessment A handoff; the parent will present the combined critique and final follow-up.
