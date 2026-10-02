Method: dual-agent critique — isolated Assessment A (`/root/career_design_review`). Assessment B and detector findings were not inspected.

# Assessment A — Mộc Careers, 2026-10-01

## Target and evidence

Primary target: `src/main/resources/templates/careers/index.html`. Reviewed the shared fragments, detail, apply and unavailable templates, `careers.css`, `career-pages.css`, and `careers.js`.

Visual evidence:

- Existing first-round home captures at 1440, 1024, 768 and 375 px, plus `round1-inspection-positions-375.png`, in `docs/tests/assets/2026-10-01-career-integration/`.
- Fresh private Chrome tab on port 9241, opened at `http://127.0.0.1:8766/`. Actual activation of the first `.job-link` opened `/jobs/4/`, showing the B2B role from the database.
- Added and inspected only the missing detail/apply captures: `round1-assessment-a-detail-1440.png`, `round1-assessment-a-detail-375.png`, `round1-assessment-a-apply-1440.png`, and `round1-assessment-a-apply-375.png`.
- Detail and apply measured `scrollWidth === clientWidth` at 375 px. This is evidence for those captured pages, not a general accessibility or device certification.

The public home/detail HTML was produced by Spring MockMvc with database content and served by the static preview. `/preview-apply.html` is a fictional authenticated visual fixture. It demonstrates the template layout and read-only/disabled controls; it does not prove real login, candidate lookup, upload, submission, or a continuous backend HTTP journey. The supplied native-browser attempt failed with the sandbox deny-read ACL error; the approved existing Chrome CDP helper provided the fallback. Its private process/profile were closed and removed after inspection.

Assessment A did not change product files, SQL or application code. Findings and scores describe the inspected baseline before the parent's final repair batch. The critique used the Impeccable scoring guide, where **0 = bad and 4 = excellent**. No matching critique ignore file was present in the source read.

## Design specificity verdict

**Moderately specific, with a coherent authored direction.** Mộc feels like one thoughtful software employer. The forest green, warm off-white, Lora headings, Source Sans 3 body copy, leaf mark, and thin rules fit the calm promise of clear handovers and visible decisions. The project-notes illustration gives the hero an intelligible connection to workflow software. The policies use concrete hours, leave and learning-budget details, which help a candidate assess the employer.

The composition is still recognizable as a standard careers site: aspiration, role grid, employer description, policies, values, hiring steps, account CTA. The rounded vacancy containers are the most category-interchangeable part. The hero notes and working principles establish a point of view, but they provide little evidence of a particular product or team. An approved concrete example of a decision, handover or product problem would strengthen specificity. The existing illustration is correctly described as illustrative; it should retain that honesty.

The visual direction deserves preservation. The next useful work is expectation-setting and small interaction refinements within that direction.

## Nielsen quality scores

This is a careers browsing surface with an application-review boundary, so all ten heuristics have an applicable state to review. No `n/a` scores were used. The score covers the current preview's clarity and behavior; it does not certify production readiness.

| # | Heuristic | Score / 4 | Evidence / principal limitation |
|---|---|---:|---|
| 1 | Visibility of system status | 3 | Result counts, selected departments, clear-filter visibility, sharing status and the submission notice provide feedback. Submission availability appears after the role's affirmative CTA. |
| 2 | Match between system and real world | 3 | Salary, location, employment type and deadline support an ordinary candidate decision. “Apply Now” implies an action that this page cannot complete. English UI with unchanged Vietnamese job content is an explicit project constraint. |
| 3 | User control and freedom | 3 | Back links, Return to Role, clear filters and menu Escape handling provide clear exits. There is no productive continuation after the unavailable submission state. |
| 4 | Consistency and standards | 3 | Shared typography, controls, spacing and headers hold together across pages. Read-only profile fields retain the appearance of editable controls, and the detail tab title exposes a template expression. |
| 5 | Error prevention | 3 | CV and submission are explicitly disabled, fields are read-only, and the form submission handler prevents accidental submission. Profile information that needs correction has no contextual route for correction. |
| 6 | Recognition rather than recall | 3 | Role title is repeated on the application page; facts, input labels and section names are explicit. Mobile department choices extend beyond the visible strip without an explicit scrolling cue. |
| 7 | Flexibility and efficiency of use | 2 | Keyword/location/department filters and Copy Role Link offer useful shortcuts. Filter state is held in page memory with no restoration in the inspected script, so a full return to the listing resets the comparison context. |
| 8 | Aesthetic and minimalist design | 3 | Restrained palette, strong typography, readable lines and clear section pacing. Repeated generic company statements and duplicate bullet markers add small amounts of noise. |
| 9 | Error recovery | 2 | Empty results explain what to change, and copy failure gives a browser-address fallback. The submission notice names the restriction clearly but gives no useful way to advance an application or correct profile data. |
| 10 | Help and documentation | 2 | The four hiring steps and local submission notice provide basic task guidance. They do not clarify the currently available application path before a candidate commits to it. |
| **Total** | | **27 / 40** | **Acceptable — 67.5%. Visual foundation is strong; expectation-setting weakens the end of the candidate journey.** |

## Cognitive load

**Moderate by the skill checklist: 2 failures out of 8.** The failures are bounded; the interface does not feel generally overloaded.

| Checklist item | Result | Observation |
|---|---|---|
| Single focus | Pass | The hero foregrounds open roles; each later page has one principal reading path. |
| Chunking, groups of at most four | Fail | Benefits contain six equal-weight rows; role facts and profile review each contain six items. Their spacing helps, but a light grouping would reduce the flat sequence. |
| Grouping | Pass | Policies, responsibilities, requirements, facts and form labels are visibly grouped. |
| Visual hierarchy | Pass | Headline, primary CTA, salary and section titles have distinct weights. |
| One thing at a time | Pass | Browse, read a role, and review profile details are separated into stages. |
| Minimal choices, at most four visible options | Fail | Desktop has five section links plus two account actions. The live listing itself has four department choices and four role choices, each within the threshold. |
| Working memory | Pass | Role context is kept in the detail title and repeated in the apply title. |
| Progressive disclosure | Pass | Long role content is available on the detail page; filters remain simple. |

Decision points above four visible options: desktop section navigation has five choices; including account actions makes seven visible destinations in the header. The six policy/fact/profile items are information groups rather than six competing actions. The mobile filter strip reduces visible options by concealing them, which creates discoverability friction rather than reducing the number of actual choices.

## Emotional journey

The arrival is calm and credible. The typography and illustrative project notes make the employer feel attentive to daily work. Seeing real roles, actual salary ranges, locations and deadlines is the strongest practical reassurance. The clear working-policy rows give a candidate a second useful moment of confidence.

The emotional valley occurs when “Apply Now” leads to a page saying online submission is unavailable. That message is clear and appears before the fields, which protects trust once the candidate arrives. The late timing still interrupts momentum. On mobile, six read-only controls and a disabled CV field extend this valley before the Return to Role action.

The homepage ends with a friendly candidate-account invitation. “Join our Talent Pool” can suggest participation in a recruitment list beyond simple registration; the nearby account copy helps but the label deserves alignment with the demonstrated account route. The application page ends with a disabled submit button and a return link, leaving the candidate without a resolved next step.

## Strengths to preserve

1. **Cohesive editorial identity.** Warm off-white, forest green, serif headings and thin rules stay consistent across 1440/1024/768/375 home views and the detail/apply views. The mobile layouts reflow without horizontal page overflow in the measured detail/apply samples.
2. **Decision-relevant role content.** Salary, employment type, location and deadlines are available without decorative statistics or multiple-employer branding. The title is a real link with an expanded hit area, so the role container has a clear destination.
3. **Honest local constraints and useful accessibility foundations.** Submission has a prominent plain-language notice; disabled controls visibly communicate the limitation. The source includes a skip link, visible focus treatment, result announcements, labelled controls, reduced-motion support and menu Escape handling. These are inspected provisions, not a completed screen-reader audit.

## Priority issues

### 1. P1 — Application availability is revealed after an affirmative promise

**Evidence:** The homepage process says “Submit Application”; both detail CTAs say “Apply Now”. Only the apply page explains that uploads and submission are unavailable. The detail capture contains no availability note near either CTA.

**Locations:** `index.html:67-71` (`.process-list`); `detail.html:7` (`.role-page-top .button`) and `detail.html:14` (`.role-page-actions .button`); `apply.html:9` (`#submission-notice`).

**Candidate effect:** A candidate can read a full role and potentially sign in with the expectation of applying, then discover a dead end. This is a major expectation-setting problem in the current UI. Missing upload/submission backend is an agreed scope boundary, and should remain disabled.

**Suggested direction:** Preserve the brief-required “Apply Now” label and state current availability beside the role CTA, before a guest may enter the sign-in journey. Qualify the process copy so it communicates the intended hiring process together with the present availability. Give the existing Return to Role/account routes a clear explanation of what they accomplish today.

### 2. P2 — Mobile department filters hide an active choice

**Evidence:** At 375 px the strip shows All Departments, Engineering and part of the zero-count Human Resources chip. Sales & Marketing, which has one real opening, is beyond the viewport. The strip clips its contents with no textual instruction or deliberate end affordance.

**Locations:** `career-pages.css:91-92` (`.filter-pills`, `.department-filter`); `index.html:32-35` (`.department-filters`). Screenshot: `round1-inspection-positions-375.png`.

**Candidate effect:** A sales candidate has less immediate access to their department than an engineering candidate. The third, empty department consumes the visible edge before a useful department is shown.

**Suggested direction:** With only four options, wrapping the existing chips is the simplest visible layout. If horizontal scrolling is retained, add an unmistakable cue and consider placing departments with openings before zero-count departments. Keep zero-count buttons understandable and selectable if that is the intended policy.

### 3. P2 — Read-only profile review looks like a form waiting for input

**Evidence:** Six account fields have the same outlined input appearance as the editable search control. The explicit read-only explanation is below all six fields. A blank Portfolio URL looks like an unfinished input; it gives no “Not provided” state. On mobile this sequence occupies most of the journey after the unavailable-submission notice.

**Locations:** `apply.html:12-20` (`.application-fields`, `.process-note`); `career-pages.css:64-65` (`.application-fields input[readonly]`). Screenshots: `round1-assessment-a-apply-1440.png`, `round1-assessment-a-apply-375.png`.

**Candidate effect:** A person with missing or incorrect details is invited visually to fill them in, then discovers they cannot. There is no contextual correction instruction.

**Suggested direction:** Put “Account details — read-only” above the group, identify absent optional data explicitly, and distinguish these fields visually from editable search inputs. Preserve the required read-only inputs. Add an existing correction route only if it is actually supported; otherwise explain the present restriction at the point of review.

### 4. P2 — The role's browser title is a literal template expression

**Evidence:** Activating the database role link opened `/jobs/4/` with the tab title **`job.postingTitle | Mộc Careers`**. The body title was correct.

**Location:** `detail.html:3`, the `head(job.postingTitle)` fragment invocation. This is a source and live-render finding, independent of any detector result.

**Candidate effect:** Multiple role tabs, browser history and bookmarks lose the role's identity. It also makes the otherwise polished page feel unfinished.

**Suggested direction:** Pass the evaluated title value to the head fragment. Check the apply fragment's expression under its actual render path as part of the same focused repair; do not infer its title from the fictional fixture.

### 5. P3 — Database list markers are rendered twice

**Evidence:** The B2B detail requirements and benefits show a browser bullet followed by a literal leading hyphen from the stored content. This appears in both captured sizes.

**Locations:** `detail.html:11-12` (`.job-description ul li`), with database content supplied through `job.requirements` and `job.benefits`.

**Candidate effect:** A minor but conspicuous punctuation defect makes detailed job content look pasted rather than composed. It also adds clutter at the start of every narrow mobile line.

**Suggested direction:** Treat the leading marker consistently in the presentation mapping or list rendering. Preserve the Vietnamese wording and stored facts; no database rewrite is needed for this visual correction.

## Persona red flags

- **Jordan, first-time candidate:** “Apply Now” and the hiring steps establish a submission expectation that changes only after reaching the application page. A plain explanation before that click would prevent surprise.
- **Casey, distracted mobile candidate:** The useful sales filter is beyond the clipped strip. The application page then asks the candidate to scan six inactive inputs before reaching a useful exit.
- **Sam, keyboard or assistive-technology user:** Labels, focus, skip navigation and announcements are present in the source. Six focusable read-only fields still create extra stops in a page that permits no edits, and a blank optional value has no descriptive absent-data state. Real screen-reader behavior was not tested in this assessment.

## Minor observations

- The zero-opening Human Resources option is helpfully differentiated with a dashed outline and count. Its presence could support a meaningful future-interest policy, but that policy is not demonstrated by the inspected current UI.
- Long street addresses repeat across engineering roles and dominate more lines on narrow screens. A shorter city-level presentation could improve comparison only if an approved derived display value exists; preserve the database content.
- “Join our Talent Pool” links to account registration. Aligning its label with the actual candidate-account outcome would keep promises precise.
- The footer still offers “Create a Candidate Account” in the authenticated apply fixture. It is a small state-aware copy opportunity, visible in `fragments.html:47`.
- The mobile role facts remain in two columns. They fit, but the long salary/location values form dense adjacent blocks. A single column is a reasonable optional refinement at the narrowest width.
- About copy repeats the hero's workflow-software sentence. A concrete approved example would earn this section more attention.

## Meaningful remaining questions for the combined critique

1. While submission remains unavailable, what useful next outcome should the application-review page emphasize using the routes that already exist: return to comparing roles, or create/use a candidate account?
2. Should departments with zero openings remain visible for future interest, or should current vacancies determine filter ordering and prominence?
3. Is an approved concrete product/working example available to make the project notes and employer story more specific, or should the present illustrative framing remain the intended public representation?

These questions are for the parent synthesis and final user close. Assessment A has completed independently; no detector findings, score anchoring, additional screenshot loops or product edits were used.
