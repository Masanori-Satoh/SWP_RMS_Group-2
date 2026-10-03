# Career integration — Assessment B

Method: isolated detector/browser agent `/root/career_detector_review`. Findings were withheld from the parent until Assessment A completed and the parent released B. No Assessment A or Gemini report was read. Product source and SQL were read only.

Target: `src/main/resources/templates/careers/index.html`. Markup scope: `src/main/resources/templates/careers`, including fragments, detail, application, and unavailable templates. No critique ignore file was present.

## Deterministic scan

The requested CLI command ran once:

```text
.agents/skills/impeccable/scripts/impeccable.cmd detect --json src/main/resources/templates/careers
```

It produced **2 warnings**, both `flat-type-hierarchy`, at `index.html:0` and `apply.html:0`. The observed launcher exit was **0 despite findings**; the finding count is taken from JSON, not the exit status. Line 0 is a whole-document rule without a precise source line.

Both are false positives. The scan reports every role as 16px, while the rendered page resolves shared styles through the Thymeleaf head fragment. This suggests the static scan did not resolve that stylesheet context. Browser computed sizes establish the real hierarchy:

| View | Body | Main heading | Section heading | Role heading |
|---|---:|---:|---:|---:|
| Homepage 1440px | 16px | 46px | 34px | 20px |
| Homepage 375px | 16px | 32px | 28px | 20px |
| Apply fixture 1440px | 16px | 40px | 20px notice | — |

The largest adjacent steps are 1.70× for desktop homepage, 1.40× for mobile homepage, and 2.00× for apply. These exceed the detector's 1.25× threshold. Accepted CLI issues: **0**; false positives: **2**. A clean disposition does not establish that the entire UX is free of problems.

## Browser evidence

The parent had already attempted native CUA/node_repl and encountered `apply deny-read ACLs`. B used the provided CDP helper with its own new headless Chrome profile and port 9242. B's default shell attempt encountered the same ACL failure; escalated execution succeeded.

The mutable preflight changed the title and appended an inline script whose counter executed. One wrapper attempt failed to quote the launcher path before the live-server command could start, and cleaned its own browser/profile. The corrected invocation started **one** live-server in background on port 8400. Its inherited stdout pipe did not close, so B parsed the server-created connection JSON from `.impeccable/live/server.json` and continued that same attempt without a restart. The localhost token was not included in evidence or reporting.

`detect.js` loaded successfully on three representative pages. After a 2600ms wait, each logged **`[impeccable] No anti-patterns found.`** Runtime issue count: **0 on each page; 0 total**. The runtime functions were present and the script tag was recorded. No runtime exceptions were recorded.

| Page | Evidence provenance | Runtime issues |
|---|---|---:|
| Homepage | Actual database → Spring MockMvc rendered public HTML, served by local preview | 0 |
| `/jobs/4/` detail | Actual database → Spring MockMvc rendered public HTML, served by local preview | 0 |
| `/preview-apply.html` | Fictional MockMvc visual fixture; no real authentication/submission backend | 0 |

The same homepage computed inspection at 375px recorded document scroll width 375px. This is a bounded computed check, not a new screenshot round or proof of every interaction.

Chrome was headless. Injection and execution succeeded, but **no user-visible `[Human]` overlay is claimed**. The public preview is not evidence that Tomcat actual HTTP startup, production authentication, or application submission succeeded.

An additional observed issue outside the detector rules: the actual role detail document title is literal `job.postingTitle | Mộc Careers`; the apply fixture title is `Apply for job.postingTitle | Mộc Careers`. The page headings themselves contain actual titles. Correct the fragment arguments to evaluate the job property so browser tabs and assistive technology receive a useful page title.

## Cleanup and evidence

`live-server stop --keep-inject` reported `Stopped live server on port 8400.` and its health endpoint was unavailable afterward. This stop mode avoided editing source injection tags. The original starter then ended, closed its own Chrome process, and removed its validated temporary profile. It recorded a fetch failure because recovery had deliberately stopped the server; that original-runner failure is retained rather than overwritten as success. The completed evidence is `assessment-b-browser-recovery.json`.

Only B's three temporary target runner/runtime files were removed. The parent preview server was retained. No page source, SQL, or authentication data was changed.

Raw CLI, original preflight failure, original starter completion, completed browser recovery, and computed dispositions are saved under `docs/tests/assets/2026-10-01-career-integration/assessment-b-*.json`.

Questions skipped: Assessment B is an evidence subtask; the parent performs synthesis and the critique close.
