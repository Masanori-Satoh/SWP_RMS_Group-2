# Mộc Career Homepage

The homepage is now rendered by the existing Spring Boot application at `/`.
`index.html` opens that application; it does not maintain a second set of jobs.

Source:

- `src/main/resources/templates/careers/`
- `src/main/resources/static/css/careers.css` + `career-pages.css`
- `src/main/resources/static/js/careers.js`
- `CareerController` → `CareerService` → existing repositories/entities.

Run the existing project with your normal external DB configuration (`mvn.cmd spring-boot:run`),
then use `http://localhost:8082/` or your configured port. No schema/seed changes needed.
Do not use a static HTML server to test real sign-in or DB data.

Current boundary: DB jobs/detail and session login-return/candidate prefill.
CV upload, profile editing and application submission remain unavailable.
See `docs/tests/2026-10-01-career-integration.md` for step-by-step verification.

## Requisition detail preview

`indexv1.html` previews the requisition detail layout with clearly marked sample data.
It shares the CSS in `src/main/resources/static/css/` with the live RMS page at
`/requisitions/{id}`. Open the HTML file directly for visual review; run Spring Boot
for authenticated actions and real data. The desktop layout places requisition
content on the left and only Activity Log on the right. On narrow screens, the
activity card follows the main content. Approval Timeline is omitted; authorized
decision, edit, copy, withdrawal and deletion actions remain in the live template.
