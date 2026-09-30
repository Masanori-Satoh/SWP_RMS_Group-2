# LinhDN specification decisions and open data gaps

## Candidate accounts

The original SRS describes `User` as an internal account in some sections while its Candidate registration/login screens require Candidate authentication. The old database had Candidate profiles without a required account link; that historical inconsistency is resolved differently by the new schema.

`database/schema/db.sql` now requires a unique, non-null `Candidate.UserId`; name, email and phone exist only on `User`. Admin Create Account creates both records in one transaction for the Candidate role. Phone is optional. Changing a User to Candidate creates a profile if absent; changing away preserves the linked profile and history, as previously agreed. The separate Candidate self-registration flow remains future work. `001_candidate_account_link.sql` is a historical patch for the old snake_case database and must not be applied to the fresh PascalCase schema.

## Account behavior confirmed for later milestones

- Account Management includes the five internal roles and Candidate accounts.
- Create requires full name, unique username, unique valid email, role, password, and matching confirmation. Use BCrypt and the documented 8–32 character rule. New status is Active. Department is required for supported internal employee roles and may be empty for Candidate. Phone is optional for all accounts; `Candidate` has no phone column.
- Update may change full name, unique valid email, phone, role, department, and status. Username stays read-only; ordinary edits preserve the existing password hash. Password change/reset is a separate action.
- Delete remains visible with confirmation. It sets status to Inactive and preserves the account and business history. Inactive and Blocked accounts cannot authenticate or keep access to protected resources.
- Dashboard access and role-specific widgets are recorded in `Doc/RMS_SRS.docx`. Widgets require real data with role-scoped queries; a missing datasource must be reported.

The security route contract reserves `/admin/accounts/**`, `/admin/api-monitoring/**`, and `/admin/ai-configuration/**` for System Admin, plus `/dashboard/**` for all authenticated roles. Controllers added in later milestones must use these paths or update the route rules and tests together.

## AI configuration audit

`SystemConfig` stores a unique key, value, description, and update time. The new SQL seed contains sample `AI_SCREENING_ENDPOINT` and `AI_SCORE_THRESHOLD` rows, but no AI integration client or configuration reader exists in `src/main/java`. The seed values are not an approved production contract. `AIScreeningResult` stores results, and `ScreeningCriteria` stores per-requisition criteria; neither defines global AI configuration.

| Key | Display name | Data type | Required | Default | Valid range or allowed values | Secret | Admin editable | Current source |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `AI_SCREENING_ENDPOINT` | AI Screening endpoint (provisional label) | Unconfirmed; name suggests URL | Unconfirmed | Sample seed value only | Unconfirmed | Unconfirmed | Unconfirmed | `database/seeds/seed_data.sql`; no active code reads it |
| `AI_SCORE_THRESHOLD` | AI score threshold (provisional label) | Unconfirmed; seed uses decimal text | Unconfirmed | Sample seed value only | Unconfirmed | No | Unconfirmed | `database/seeds/seed_data.sql`; no active code reads it |

No implementable AI configuration contract is confirmed yet. The two seed keys above must not be treated as approved form fields without types, ranges, secret policy and integration behavior. Model, temperature, timeout, retry, prompt, token, and other values must not be invented. The AI Configuration form remains unimplemented pending review of this catalogue and the integration contract.

If future settings include secrets, the UI must mask existing values, preserve a secret when an update input is blank, overwrite it only after explicit new input, and never render or log its plaintext. Secrets must not be committed in public config files. Non-secret settings may use `SystemConfig` after their keys and validation are agreed.

## API monitoring scope for milestone 5

Monitor both internal APIs and the two external integration categories. The requested probe should send a request and report whether a response is returned. The concrete probe method and target must be checked against each integration contract before implementation so the health check does not accidentally submit a CV or send email.

The current internal probe, missing external contracts, statistics window, and local schema validation incident are recorded in `linhdn-api-monitoring.md`.
