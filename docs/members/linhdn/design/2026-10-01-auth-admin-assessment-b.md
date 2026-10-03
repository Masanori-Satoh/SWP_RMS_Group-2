# Auth / Admin — Independent Assessment B

Method: isolated detector/browser assessment by `/root/auth_admin_detector_review`. Assessment A, Gemini output, prior critique scores, and prior critique snapshots were not read. Findings were withheld from the parent until its release gate.

## Scope and evidence boundary

Source target: `src/main/resources/templates` in `D:\kì 5\SWP\hireflow-api\RetireManagement\SWP_RMS_Group-2`.

Review scope: Login, Register, Admin Dashboard, Account List, Create Account, Edit Account; related Forgot Password, Reset Password, API Monitoring, and the new shared interface fragments/CSS. The intended visual system is Mộc RMS for one company: warm offwhite, forest green, Lora and Source Sans 3, English interface labels, and database names displayed verbatim. Candidate accounts and deactivation remain part of account management.

Browser target: frozen MockMvc visual fixtures at `http://127.0.0.1:8768/preview/{login,register,forgot,reset,dashboard,accounts,create,edit,monitoring}/`. These contain fictional data. Browser evidence verifies rendered HTML, CSS, scripts, and the specific local interactions recorded below. No forms were submitted; it does not verify real authentication, persistence, SQL Server, mail delivery, or production E2E behavior.

The parent removed leftover duplicate header/logout markup in three templates between the static scan and the frozen browser preview. The original CLI JSON is retained unchanged. Current runtime measurements show one Log Out button on each operational page. The heading findings below concern the shared CSS hierarchy and are unaffected by that markup cleanup.

## One CLI scan

Command, run exactly once:

```text
.agents/skills/impeccable/scripts/impeccable.cmd detect --json src/main/resources/templates
```

Raw JSON: `docs/tests/assets/2026-10-01-auth-admin/assessment-b-cli.json`.

The launcher returned exit code **0**, although the JSON contains **33 warnings**. The JSON is the finding evidence; this run must not be described as a clean scan solely from its exit code. No second CLI pass was run.

| Rule | Raw count, entire templates tree |
|---|---:|
| low-contrast | 21 |
| flat-type-hierarchy | 5 |
| tiny-text | 3 |
| overused-font | 3 |
| cramped-padding | 1 |
| **Total** | **33** |

### In-scope disposition

There are **3 raw in-scope findings**, all `flat-type-hierarchy`, reported at line 0:

| Template | Static detector claim | Current browser evidence | Disposition |
|---|---|---|---|
| `admin/accounts/list.html` | Body, h1, h2 all 16px | Source Sans 3 body 16px; Lora h1 36px; Account List h2 20px | False positive from unresolved shared CSS |
| `admin/api-monitoring/index.html` | Body, h1, h2 all 16px | Body 16px; Lora h1 36px; Endpoint Status h2 20px | False positive from unresolved shared CSS |
| `dashboard/index.html` | Body, h1, h2, h3 all 16px | Body 16px; Lora h1 36px; section h2 20px; h3 18px; compact metric labels deliberately 16px beside 32px values | False positive from unresolved shared CSS |

These templates obtain `design-tokens.css`, `interface.css`, and `workspace.css` through Thymeleaf `interfaceHead`; the raw template scan does not see the fully resolved page. `interface.css` defines the 36px desktop h1, 20px h2, 18px h3, and 16px body hierarchy. All nine browser views confirmed both local font families loaded.

No raw findings were emitted for Login, Register, the shared Create/Edit template, Forgot/Reset Password, or the new individual shared fragments. This is a narrow detector result, not a full accessibility or design certification.

### Outside the Auth/Admin scope

**30 raw findings** belong to other surfaces and are retained for provenance. They were not fixed, dismissed by an ignore rule, or promoted into this Auth/Admin critique.

| Template | Count |
|---|---:|
| `careers/apply.html` | 1 |
| `careers/index.html` | 1 |
| `requisitions/detail.html` | 7 |
| `requisitions/form.html` | 8 |
| `requisitions/list.html` | 13 |
| **Total outside scope** | **30** |

The detailed rule, snippet, absolute file, and detector location are preserved in the raw JSON and scope counters. The legacy `head(title)` fragment remains a separate teammate surface; no legacy head finding was emitted by this scan. `.impeccable/critique/ignore.md` was absent, and no detector hook or ignore configuration was changed.

## Browser detector and computed verification

The native browser attempt was real: `cua.createBrowserTab("iab", "about:blank", { visible: false })` failed before creating a usable tab. The exact output was `node_repl kernel exited unexpectedly`; diagnostics contained `windows sandbox failed: helper_unknown_error: apply deny-read ACLs`. Read-only shell probes also encountered the same ACL helper error; explicit PowerShell execution outside that sandbox completed the evidence work.

Fallback: the repository's existing `scripts/career-browser.mjs`, with a new private headless Chrome profile, own CDP port **9252**, and fresh tabs. No screenshots were taken by B.

Mutable preflight succeeded: the title changed, an inline script was appended, and that script executed a dataset marker in the document. One `impeccable live-server --background` startup was attempted. Its inherited stdout pipe remained open, so the structured `.impeccable/live/server.json` from that same attempt was parsed. The helper was **not restarted**. Its token was not printed and token-bearing temporary inputs were removed.

The first runner timed out while waiting 60 seconds for the overlay configuration during helper startup/approval. It cleaned its Chrome process and private profile. The complete failure is preserved in `assessment-b-browser-attempt1.json`. A second fresh runner used the already running helper and completed successfully.

`detect.js` loaded successfully on five representative pages. Each page waited **2.6 seconds** after injection before console evidence was read. Nine views were measured at desktop **1440 × 1000** and mobile **390 × 844**. The five injected views reported:

| View | Console finding count | Rule and element |
|---|---:|---|
| Login | 2 | `gray-on-color`: the auth story caption paragraph and `p.auth-brand-foot` |
| Register | 2 | The same two `gray-on-color` caption elements |
| Admin Dashboard | 0 | Console: `[impeccable] No anti-patterns found.` |
| Account List | 1 | `cramped-padding`: `div.table-wrapper` top/bottom border has no wrapper inset |
| Create Account | 0 | Console: `[impeccable] No anti-patterns found.` |
| **Raw total** | **5** | **3 unique DOM observations, repeated across views** |

Browser disposition:

- **Four intentional brief exceptions:** `gray-on-color` is an aesthetic classification of caption color `#d6e4d8` on forest `#2e5a45`. This is the requested forest brand treatment, and the calculated text contrast is **5.99:1**. It is not a low contrast failure. The same two captions are counted once per injected Auth page.
- **One false positive:** `cramped-padding` inspects the table wrapper boundary. Table text is inset by **12px** cell padding in `workspace.css`; aligning a collapsed table's edges with its scroll wrapper is the intended table construction. Adding padding to the wrapper would not repair a text inset defect.
- **No browser detector finding was promoted to a priority issue** after the brief and table construction were checked. The parent should combine this disposition with Assessment A's independent UX judgment.

There is **no verified user-visible overlay**. Injection and detector execution succeeded inside headless Chrome; the native presentation path failed. Console output and computed measurements are the fallback signal. Edit, Forgot, Reset, and API Monitoring received computed verification without further injection, keeping the browser detector sample at five views.

## Concrete runtime evidence

- All nine views used Source Sans 3 body text at 16px, Lora h1 at 36px on desktop, and loaded local Lora/Source Sans 3 fonts. These agree with the requested design system.
- At 390px, all nine documents had `documentWidth = viewportWidth = 390`; no whole-page horizontal overflow was measured. Account and Monitoring tables intentionally overflow inside their labelled scroll regions: Account List **920px content / 358px region**; API Monitoring **950px / 358px**.
- Account List's first Delete control activated the native confirmation dialog. It displayed the fixture account name verbatim, stated that the account becomes Inactive and related data is preserved, and initially focused **Cancel**. Escape closed the dialog and restored focus to the trigger. No deactivation POST was submitted.
- Create Account contained the **Candidate** role option. Choosing Candidate left Department optional; choosing an internal role made it required. This verifies the page's local requirement switch, not backend acceptance.
- Current Dashboard, Account List, Create, Edit, and API Monitoring each had one Log Out control. The operational `main` remained inside `.page-wrapper` after the parent's source cleanup.
- No warning/error-level console messages were recorded during these completed page checks.

## Evidence files and cleanup

All paths below are relative to the repository:

- `docs/tests/assets/2026-10-01-auth-admin/assessment-b-cli.json`: original single scan, unchanged.
- `docs/tests/assets/2026-10-01-auth-admin/assessment-b-cli.stderr.txt`: preserved CLI stderr capture, empty.
- `docs/tests/assets/2026-10-01-auth-admin/assessment-b-counters.json`: raw counts, scope disposition, browser counters, and failure/cleanup provenance.
- `docs/tests/assets/2026-10-01-auth-admin/assessment-b-browser-attempt1.json`: original timeout and successful private profile cleanup.
- `docs/tests/assets/2026-10-01-auth-admin/assessment-b-browser.json`: completed per-view console/computed/interaction evidence.
- `docs/tests/assets/2026-10-01-auth-admin/assessment-b-browser-summary.json`: concise per-view measurements.
- `docs/tests/assets/2026-10-01-auth-admin/assessment-b-live.json`: redacted helper startup metadata.

Cleanup completed: the sole helper stopped with `live-server stop --keep-inject` (exit 0), own Chrome sessions stopped, both private profiles removed by the driver's validated temp-directory cleanup, and B's temporary runner/config/preflight/token-bearing startup files were removed from validated paths under `target`. Parent preview port 8768 was left running for the parent's inspection. B changed no UI, Java, SQL, hook, or ignore source files.
