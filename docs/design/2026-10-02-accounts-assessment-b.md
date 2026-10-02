# Account Management Assessment B — 2026-10-02

Independent detector and browser evidence for `src/main/resources/templates/admin/accounts/list.html`, with the candidate list, account form, and System Admin dashboard as neighboring surfaces. Assessment B findings were withheld from the parent until the parent explicitly reported Assessment A complete and released B. No application template, CSS, JavaScript, business rule, or ignore file was edited by this assessment.

## Deterministic scan

Exactly one invocation: `.agents/skills/impeccable/scripts/impeccable.cmd detect --json src/main/resources/templates/admin/accounts`. This narrow directory contains `list.html`, `candidates.html`, and `form.html`. Raw output: `docs/design/2026-10-02-accounts-detector.json`.

The launcher returned exit **0**, while JSON contained **2 warning findings**. The actual returned status is recorded; a clean scan must not be inferred from that exit value.

| Rule | File | Detector location | Disposition |
|---|---|---|---|
| `flat-type-hierarchy` | `templates/admin/accounts/list.html` | line 0; assumed body/h1/h2 all 16px | False positive: browser computes h1 36px, section h2 20px, regular body 16px. External shared styling and Thymeleaf head replacement were not resolved by the source scan. |
| `flat-type-hierarchy` | `templates/admin/accounts/candidates.html` | line 0; assumed body/h1/h2 all 16px | Same verified false positive. |

No deterministic finding was returned for the form. The dashboard was browser inspected rather than included in the CLI directory.

## Browser procedure and overlays

Native CUA was unavailable: the parent had already attempted it and observed a kernel failure. B used the existing `scripts/career-browser.mjs` Chrome CDP helper, a private temporary headless browser profile, and its own port **9262**. This was a documented fallback rather than a native browser inspection. It cannot establish a persistent user-visible browser tab.

Three fresh tabs opened the source-derived static fixtures served by the parent at port 8770:

- `/preview/accounts/`
- `/preview/candidate-accounts/`
- `/preview/dashboard/`

Each tab first mutated `document.title` to include `[Human] Assessment B`, appended an inline script, and verified that the script executed. All three mutable preflights passed before the server launch.

Exactly one `live-server --background` invocation returned connection JSON successfully, port **8400**, process **5244**, exit **0**. Same-attempt state recovery was unnecessary; no restart occurred. Health responded. Ephemeral helper token was omitted from the retained evidence.

`http://localhost:8400/detect.js` loaded successfully on all three pages; B waited 2.8 seconds on each page and recorded the detector console summary and overlay DOM. Browser findings: **Internal Accounts 1**, **Candidate Accounts 2**, **Dashboard 0**. These are runtime DOM findings, separate from the two source CLI warnings.

Screenshots verify the precise runtime labels:

| Page | Runtime overlay | Disposition |
|---|---|---|
| Internal Accounts | table-wrapper: children flush against border-top/bottom+outline on all sides (no inset) | Intentional edge-aligned data table inside its scroll region. Adding wrapper inset solely to satisfy a generic surface detector would not improve table scanning. Treat as a false positive for this product surface. |
| Candidate Accounts | same table-wrapper label | Same false positive. |
| Candidate Accounts | Line length too long, on the profile/history explanatory note | Minor reading-width observation: the note spans the table region. Restricting explanatory copy to a comfortable measure would improve reading; severity P3. |
| Dashboard | console “No anti-patterns found” | Clean runtime detector result for this fixture, not proof of all dashboard states. |

Artifacts: `2026-10-02-b-accounts-overlay.png`, `2026-10-02-b-candidate-accounts-overlay.png`, `2026-10-02-b-dashboard-overlay.png`, and `2026-10-02-accounts-browser-b.json` in `docs/design/`.

These screenshots are retained evidence of the injected overlay. The private headless tabs were closed. Do not claim the user currently has visible overlays in a live `[Human]` browser tab.

## Nonmutating control and measurable evidence

Internal and candidate account Deactivate triggers opened their real fixture dialog without submitting. Cancel was focused on opening; Escape closed the dialog and restored focus to the corresponding trigger on both surfaces. The captured dialog copy names the account and explains Inactive status and preservation of related data. No POST or account change occurred.

Keyboard Tab from Search focused Role for internal accounts and Status for candidate accounts. Both computed `:focus-visible=true`, a **3px solid** outline, and **4px** outline offset. The table scroll region was keyboard focusable and responded to ArrowRight. All sampled main account buttons, links and inputs measured at least **44px** high.

At 375px viewport width the account table scroll regions were **343px** wide with **920px** scrollable content. Page document width remained **375px**, so the overflow was contained in the intended table region. More Filters was collapsed on narrow internal accounts and open on desktop. This proves the tested fixture behavior, not all data lengths or zoom configurations.

A second bounded browser pass resolved the missing quantitative contrast measurement, with no detector rerun and no helper restart. It sampled visible main text nodes and input placeholders, compositing ancestor backgrounds and applying size/weight-dependent WCAG AA text thresholds. **51 internal**, **43 candidate**, and **32 dashboard** measurements returned **0 sampled failures**; the lowest ratio on each page was **5.94:1**. This is measured text contrast evidence, not a full accessibility certification or contrast proof for unrendered states.

The desktop Internal Accounts screenshot also shows Department stacked above Sort By while Search/Role/Status align with the lower row, leaving extra vertical space above the primary filters. This is visible layout evidence the parent may evaluate alongside Assessment A; it is not a deterministic finding.

## Scope and cleanup

- `PRODUCT.md` and Impeccable critique/audit procedures were read. Parent confirmed context launcher had already run once this session, so B did not rerun it.
- No applicable `AGENTS.md` or `.impeccable/critique/ignore.md` was present in the checked project and parent paths. No ignore edit occurred.
- Source inspection included the account form and candidate list; overlays ran only on the three representative pages above.
- Fixtures have rendered Thymeleaf structures and current local assets but do not establish authenticated Spring, SQL Server, CSRF, persistence, email, or production E2E behavior.
- Sandbox execution repeatedly failed before process creation with `helper_unknown_error: apply deny-read ACLs`. Necessary commands were rerun with approved escalation; this is an execution fallback, not an application defect.
- `live-server stop --keep-inject` returned exit **0** and “Stopped live server on port 8400.” A subsequent health request failed, confirming the port closed. This flag prevented source injection removal from modifying app files. Injection existed only in temporary browser DOM.
- Both private browser passes closed Chrome and removed their validated temporary profiles through the existing helper. B left the parent-owned fixture server running for the parent’s subsequent work.
- Temporary B execution scripts were removed after saving the retained evidence. The source scan JSON, browser measurements, overlay screenshots, and this assessment remain as review artifacts.

Questions skipped: Assessment B evidence is handed to the parent for synthesis and the user-facing close.
