# LinhDN Dashboard data catalogue (milestone 4)

All numbers below are queried from the current SQL Server database on each dashboard request. A displayed `0` means the query returned zero. Status names follow the existing entity values; no metric is seeded or hardcoded in the view.

| Role | Widget | Source and rule | Data scope |
| --- | --- | --- | --- |
| System Admin | Total accounts; Active, Inactive, Blocked accounts | `[User]`, `AccountStatus` | All accounts, including Candidate accounts |
| HR | Active job postings | `JobPosting`: `Published` and `ApplicationDeadline` absent or not past | All postings; current model has no HR assignment field |
| HR | Total applications; candidates by recruitment stage | `Application`; distinct Candidate count grouped by `ApplicationStatus` | All applications; the same Candidate may appear in multiple stages when applying for different jobs |
| HR | Upcoming interviews | `InterviewSchedule`: future `StartTime`, status `Scheduled` or `Rescheduled` | All interviews; no HR assignment field beyond `CreatedBy` |
| HR | Pending HR actions | `Application`: `Applied` with no `ApplicationReview` row whose `ReviewerRole` is `HR` | New applications awaiting HR review; other HR tasks are not modelled as assigned actions |
| Hiring Manager | Own / department requisitions | `JobRequisition.HiringManagerId` / `DepartmentId` | Logged-in manager ID / manager's department ID |
| Hiring Manager | Pending requisitions | `JobRequisition.ApprovalStatus = Pending_Director` | Logged-in manager's requisitions |
| Hiring Manager | Assigned candidates | Distinct `Application.CandidateId` through postings on manager's requisitions | Logged-in manager's requisitions; this is candidates in own requisitions, not a separate assignment table |
| Hiring Manager | Upcoming interviews | Future `InterviewSchedule` for applications on manager's requisitions | Logged-in manager's requisitions |
| Hiring Manager | Pending offer actions | `OfferProposal` with `ProposedBy = current user` and status `Draft` or `Approved` | Own offers; draft or approved offers are the statuses available for further manager action |
| Director | Requisitions / offers awaiting approval | `JobRequisition.Pending_Director`; `OfferProposal.Pending_Director` | Global approval queues, since no pre-approval Director assignment exists |
| Director | Recent approval / rejection activity | Latest 5 `RequisitionApproval` and `OfferApproval` rows, ordered by decision time | Only rows with logged-in `DirectorId` |
| Interviewer | Upcoming assigned interviews | `InterviewPanel` joined to future `InterviewSchedule` with status `Scheduled` or `Rescheduled` | Only panel rows for logged-in `InterviewerId` |
| Interviewer | Assigned candidates | Distinct Candidate through `InterviewPanel` and `InterviewSchedule.Application` | Only logged-in interviewer's panel rows |
| Interviewer | Pending evaluations | Completed panel interviews with no matching `InterviewEvaluation` for that interviewer | Only logged-in interviewer's panel rows |
| Candidate | Own applications; application status summary | `Application` joined through `Candidate.UserId` | Only linked profile of logged-in account |
| Candidate | Upcoming interviews | Future `InterviewSchedule` joined through Application and `Candidate.UserId` | Only linked profile of logged-in account |
| Candidate | Offer status | `OfferProposal` grouped by status through Application and `Candidate.UserId` | Only linked profile of logged-in account; only `Sent_Candidate`, `Accepted`, `Rejected` are exposed, never internal draft/approval states |

## Missing sources and explicit limits

- System Admin API/integration health now summarizes the three target states from `ApiMonitoringService`: the fixed internal probe uses real HTTP results, while AI and email are explicitly unconfigured. A target is never shown as operational before a successful probe.
- The AI Configuration shortcut is unavailable because the configuration keys and form are not approved/implemented. The Dashboard has no dead link to a 404 route.
- Candidate notifications are unavailable: the project has no Notification entity, repository, or service.
- The SQL seed now creates Candidate profiles with required `Candidate.UserId` and inserts rows into all 20 tables. Personal widgets still filter by logged-in account or assignment; an empty result means no matching rows in that scope.
- The seeder creates sample records, but the Dashboard never inserts or fabricates records. A posting with `Published` status whose deadline has passed is excluded from the active count.
- The Director queues are global because approval assignment exists only on completed approval records. If Director queues must be partitioned by department, the assignment rule needs a separate requirement.

Guest access remains denied by `SecurityConfig`; all six authenticated roles can open `/dashboard`. Account status and role changes invalidate existing sessions through `AccountSessionGuardFilter`.

