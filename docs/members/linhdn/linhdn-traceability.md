# LinhDN Iteration 1 traceability

This matrix tracks only the features owned by LinhDN. The account-list entries reflect the decisions confirmed after the original SRS was written.

| Requirement | SRS | Mockup | Entity/DB | Backend | Frontend | Status | Question |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Dashboard access for six authenticated roles | Permission Matrix, screen inventory, 5.1.5; role correction recorded in SRS | Role-specific dashboard panels | `User`, `Role` and recruitment entities | `/dashboard` plus role-scoped read-only aggregates | Role-specific real-data panels and explicit unavailable states | Milestone 4 implemented; see `linhdn-dashboard-data-catalogue.md` | API health, AI Configuration and Candidate notifications lack a usable source |
| Account List including Candidate accounts | View Account UC, Permission Matrix, screen inventory, 5.1.6 | Search; role, department, status filters; columns name, email, username, role, department, status | `User` authentication accounts; Candidate profiles have required unique `UserId` in the new schema | `/admin/accounts`, paged/filterable `User` query | Thymeleaf list with filters, sorting and pagination | Milestone 2 implemented | None |
| Delete account as deactivation | Delete Account UC, 5.1.6; confirmed decision clarifies SRS delete wording | Delete action with confirmation | `User.accountStatus = Inactive`; record/history retained | CSRF-protected POST `/admin/accounts/{id}/deactivate` | Confirmation modal and success alert | Milestone 2 implemented | None |
| Create Account | Create Account UC, 5.1.7 | Account form | `User`, `Role`, `Department`; Candidate profile is linked separately | Validated create service, BCrypt, uniqueness checks, Active default | Admin create form | Milestone 3 implemented | None |
| Update Account | Update Account UC, 5.1.7 | Account form | Name, email and phone live in `User`; Candidate keeps only profile fields | Validated update service; username and hash unchanged | Admin edit form with status | Milestone 3 implemented | None |
| API Monitoring Dashboard | Monitoring API System UC, 5.1.12 | Endpoint status table | No existing monitoring log or production AI/email client; see `linhdn-api-monitoring.md` | Admin-only fixed internal HTTP probe and real in-memory probe statistics | Endpoint table, status, latency, error rate, recent errors and manual probe | Internal part implemented; AI/email remain unconfigured | Provide safe AI/email target and method, or approve unconfigured status for this milestone |
| AI Configuration | 5.1.13 | Configuration form | `SystemConfig`; no confirmed active AI keys | No form until catalogue approved | No form until catalogue approved | Specification gap | Exact keys, types, ranges, secret policy and integration contract |

Account forms use the new `User` column lengths: username 50, email 150, full name 100, and phone 20. Candidate phone is optional because contact data lives in `User`. Creating a Candidate account or changing an account to Candidate creates a linked profile if absent. Switching a linked Candidate account to an internal role preserves its profile and history.

The SRS Candidate registration mockup says email may be 255 characters, while the new `User.Email` column is 150 characters. The account form now validates at 150. Supporting 255-character registration emails requires an explicit schema decision.

