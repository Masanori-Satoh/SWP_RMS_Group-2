# Talent Pool screenshot execution — 2026-11-02

## Continuation update

The user authorized local test-data writes and test email, then clarified that screenshots must show the actual RMS interface, not the JSON evidence viewer. TC27's JSON image was replaced with a real RMS screenshot showing the validation message. Real UI screenshots were added at TC30 (HM suggestions list), TC31 (HM suggested Phan Hải Ngọc), TC40 (HM accepted HR's recommendation for Nguyễn Duy Phong), TC42 (redirect to interview scheduling after consent), and TC43 (scheduled interview; second image shows its actual details). Google Docs reported Saved to Drive.

The following test records were created via RMS UI and have not yet been removed: Application 215 (Candidate 1, job 2, HR recommendation accepted by HM); Application 216 (Candidate 3, job 2, HM suggestion confirmed by HR, now Interviewing); InterviewSchedule 48 (Application 216, Scheduled, 20/11/2026 10:00–11:00, test link https://meet.google.com/abc-defg-hij). AuditLog 71 records MailSent:InterviewInvitation for interview 48; SMTP acceptance is evidenced, not delivery. The seed candidate recipient is ngoc.hai.phan@gmail.com. User authorized test email and fixture cleanup; restore only these exact test records and related dependencies after evidence is complete, without disturbing existing data.

The user still wants remaining JSON screenshots replaced: TC17,18,19,20,33,35,36,38, and all remaining blank cases filled. Do not claim GET screenshots prove POST-only security cases; explicitly identify any UI limitations. A script that would add notes and unmark Candidate 1 was rejected by automatic approval review after the user requested UI screenshots; it did not execute. Separate cases 22,28,34 ran with Candidate 4 temporarily unmarked and restored; their API-viewer images were not added because the user rejected JSON screenshots. Candidate flags 1 and 4 were verified restored to true.

User scope: execute test cases and paste screenshots into the existing Google Doc, starting at TC-PC-017. Do not redo TC1–16.

Document: https://docs.google.com/document/d/1D7iztbH4InajwvhhUXDndScztea8GU5fXydPNTVqZH0/edit?tab=t.0

RMS: http://localhost:8081, local SQL Server database RitirementManagement2. Product source unchanged. Seed accounts were read from database/seeds/seed_data.sql.

## Images pasted

- TC-PC-017: Candidate authenticated POST mark → 403, tracked DB state unchanged.
- TC-PC-018: Interviewer authenticated POST mark → 403, tracked DB state unchanged.
- TC-PC-019: HR POST nonexistent application → 404, tracked DB state unchanged.
- TC-PC-020: HR marks already potential Candidate 1 twice → two redirects, no duplicate mark audit, tracked DB state unchanged.
- TC-PC-024: real RMS browser form rejects empty note with native required-field validation.
- TC-PC-025: three-space note POST → redirect and “Vui lòng nhập ghi chú.”, tracked DB state unchanged.
- TC-PC-027: 1001-character note POST → redirect and “Ghi chú tối đa 1.000 ký tự.”, tracked DB state unchanged.
- TC-PC-033: existing candidate/job suggestion → 400 “Ứng viên đã có đơn hoặc đề xuất cho vị trí này.”, tracked DB state unchanged. Used source Application 199 (same Candidate 1, different job) to reach duplicate validation instead of the same-source-job validation.
- TC-PC-035: outside-department suggestion → 403 generic forbidden HTML. The specific service message is not exposed by the rendered response. Tracked DB state unchanged.
- TC-PC-036: Paused posting → 400 “Tin tuyển dụng không còn nhận hồ sơ.”, tracked DB state unchanged.
- TC-PC-038: Closed posting, hm_khanh → 400 with same message, tracked DB state unchanged.

For POST-only integration cases, screenshots show the local evidence viewer containing actual RMS request/response plus actual SQL before/after comparison. These are explicitly API evidence, not RMS UI screens. JSON files retain the full evidence. TC024 is an actual RMS UI screenshot. No pass status was written into the source test-case sheet.

TC17–20 and TC24 were pasted before paste-progress.log was added. TC19/20 placement was visually verified after browser recovery. TC33/35/36/38 placement was also visually verified. Google Docs reported Saved to Drive after these changes.

## Pending authorization

Automatic approval review rejected the original broad command with mutations and fixture SQL updates. No command from that rejected batch ran. The user was asked via request_user_input_async whether to allow local test data changes and test email sending; no response has arrived yet.

Separate narrower commands for known non-mutating rejection/idempotence cases were approved and executed. No fixture UPDATE, notes creation, suggestion creation, interview creation, or email sending has been performed by this task. Authentication may create normal login audit events.

Remaining cases: TC21,22,23,26,28,29,30,31,32,34,37,39,40,41,42,43,44,45. Do not run dependent mutations until the user responds to the pending question.

Important current baseline: Candidates 1,3,4,7 are all already potential. Candidate 4/7 differ from the pasted test baseline. Candidates 1 and 3 each have one Completed interview, and no Scheduled fixture. There are no Draft postings. No Candidate 1/3 application to Job 2 existed at the initial snapshot. Relevant notes are Note 1 (Candidate 1, HR) and Note 2 (Candidate 3, HM).

Numbering discrepancy to handle when resuming: the pasted HR-recommendation case has no ID; suggestion cases 31–38 use TC-INT IDs, while the Doc labels all as TC-PC. The pasted “HM views suggested candidates” case is labelled TC-PC-030 after case 38, while the Doc also has TC-PC-039. Suggested positional mapping is Doc TC30 = HR recommendation; Doc TC39 = HM suggested list. Add clear captions if using that mapping.

## Tool state for continuation

- Chrome browser id 3.
- Google Docs tab 1250515090, marked deliverable.
- Evidence viewer tab 1250515107 (current case 27).
- Earlier RMS/evidence tabs 1250515101 and 1250515104 may return Debugger unattached after a browser runtime reset. Prefer a fresh task tab in browser 3 if needed.
- Local static evidence server: python -m http.server 8766 --bind 127.0.0.1 --directory .qa/talent-pool-2026-11-02; exec session 12996.
- Read/execute helper: api_evidence.py. Its mutation branches remain unexecuted and unapproved. Never rerun a broad batch without the user's authorization.

Browser clipboard image insertion was successful. Find the exact TC label with Ctrl+F, fill the explicitly observed “Find in document” textbox, observe; Escape, observe; Right, End, Return; write the JPEG clipboard entry and Ctrl+V; observe. `tab.typeText(null, ...)` can focus document content even when the search dialog is open, so use the locator for the Find textbox.

Screenshot files are in screenshots/. Google Docs screenshot backend returns 5120×1981 captures; cropping the saved image to x=800..1700 gives the document column for visual review. Preserve original screenshots.
