# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Recruitment Management System (RMS): Spring Boot 3.5.16, Java 21, Spring Security, Spring Data JPA on Microsoft SQL Server, Thymeleaf + Layout Dialect, Lombok, Spring Mail, Apache POI. Team docs and UI copy are in Vietnamese.

The authoritative rules live in `../../architecture/ARCHITECTURE_GUIDE.md` (backend) and `docs/architecture/UI/README.md` (frontend; it points to `UI_CATALOG.md`, `UI_RULES.md`, `UI_NEW_PAGE.md`, `UI_MIGRATION.md`, `UI_CSS_GUIDE.md`). Read the relevant one before non-trivial changes. Its section 8 lists legacy code that deviates from the rules; do not copy those patterns.

## Commands

```bash
mvn clean compile spring-boot:run          # or .\mvnw.cmd spring-boot:run
mvn test                                    # all tests (JaCoCo report generated in test phase)
mvn test -Dtest=CareerFlowTests             # single test class
mvn test -Dtest=CareerFlowTests#methodName  # single test method
```

- Config: `application.properties` activates profile `local`; personal credentials (DB password, port, SMTP, `app.password.reset.secret`) go in the gitignored `../../../src/main/resources/application-local.properties`, copied from `application-local.properties.example` (never rename the example).
- There is no embedded/H2 test DB. `@SpringBootTest` tests (e.g. `*QueryTests`, `service/*DatabaseTests`, `RequisitionIntegrationTest`) connect to the real local SQL Server database `RitirementManagement2`; `@WebMvcTest` tests do not need a DB.
- Some test files are entirely commented out (e.g. `SecurityFlowTests`, `AdminDepartmentWebTests`) — they are not compiled or run.

## Database

- Schema source of truth: `../../../database/schema/db.sql` (drops and recreates the DB). Incremental scripts in `database/migrations/`, seeds in `database/seeds/` (`seed_data.sql`, then `seed_extra_jobs_and_pipeline.sql`, which is re-runnable).
- Do NOT regenerate `seed_data.sql` with `build_seed.py` — it has hand-added data not in the script.
- Hibernate does not manage schema (`ddl-auto=none`). Any table/column change must be written into `db.sql` (+ a migration, and `../../database/model.md` if the design changes).
- `spring.jpa.open-in-view=false` and `use_nationalized_character_data=true`: read services need `@Transactional(readOnly = true)` and must map entities to DTOs inside the service; never return lazy entities to controllers/views. Vietnamese text columns must be `NVARCHAR`.

## Architecture

Package-by-feature under `com.group2.rms`. Small modules (`dashboard`, `notification`) are flat; large modules (`auth`, `user`, `requisition`, `jobposting`, `candidate`, `interview`, `offer`, `admin`, `career`) split into `controller/ service/ repository/ entity/ dto/` (+ `exception/`, `validator/`). `core/` is shared infrastructure only (security, exception handling, `ApiResponse<T>`, `@ControllerAdvice` model attributes). `user` and `candidate` are foundational modules many others depend on — changing their entities requires checking dependents. No circular module dependencies; prefer calling another module's Service over its Repository.

**Request handling conventions (for new code):**
- Default is traditional form POST: `@Valid @ModelAttribute("form") XxxRequest form, BindingResult errors` → `redirect:` on success, re-render the same form template on error. AJAX (`fetch` + JSON, returning `core.dto.ApiResponse<T>`) only for in-page actions (modals, list buttons). Pick one style per action, never both.
- Errors: input errors → Bean Validation on DTO; business violations → module exception `{Context}Exception extends BaseBusinessException` (add a `field` to target a form field, see `AccountFieldException`), caught by the controller by exact type; not-found → `ResourceNotFoundException`; no-permission → Spring's `AccessDeniedException`. Do not use `IllegalArgumentException`/`IllegalStateException`/`EntityNotFoundException`/`ResponseStatusException` for business errors, and do not add module-specific handlers to `core/exception/GlobalExceptionHandler`.
- Business validators that need DB access are `@Component`s in `{module}/validator/`, called from services. Vietnamese name regex: `^[\p{L}][\p{L}\s.'-]*$`.
- DTO naming: `*Request` / `*Response` directly in `dto/` (no `Form`/`Dto` suffix, no `request/`/`response/` subfolders); responses are `record`s. Large-module services are interface + `*ServiceImpl`. Use constructor injection (`@RequiredArgsConstructor` + `private final`); name tests `*Tests`.

**Security:** six roles (`Candidate`, `Interviewer`, `HR`, `Hiring Manager`, `Director`, `System Admin` → `ROLE_CANDIDATE` … `ROLE_SYSTEM_ADMIN`, see `core/security/RoleAuthorities`). All URL authorization is centralized in `core/config/SecurityConfig` (no `@PreAuthorize`). Every new route needs a rule there — otherwise it falls through to "any authenticated user", including candidates. Get the current user via `core/security/CurrentUserService`. Candidate-facing routes live under `/portal/**`; internal ones under `/requisitions`, `/internal/job-postings`, `/interviews`, `/offers`, `/admin/**`.

**Views (Thymeleaf):** three-level inheritance — `layout/base.html` (shared head, CSRF meta) → `layout/internal.html` (role-based sidebar + topbar) or `layout/public.html` → page templates that use `layout:decorate="~{layout/internal}"` and fill `layout:fragment="content"`. Controllers only set `model.addAttribute("activeMenu", ...)`; `topbarUser` and `pageLinks` come from `core/web` advices. Sidebar menus are per-role files in `templates/fragments/layout/sidebars/`. Reusable content fragments are in `templates/fragments/ui/`.
- No inline `style`, `<style>`, `<script>`, or hex colors — use `var(--...)` tokens from `static/css/tokens.css`; page-specific CSS goes in `static/css/pages/<page>.css`. Check `UI_CATALOG.md` before writing new CSS/markup.
- Shared files (`components.css`, `fragments/ui/`, layouts) are "append-only / do not touch" per `UI_RULES.md` section 2 — ask the user before modifying them.
- Do not rename/move templates or change backend-bound names (`${...}`, `th:field`, `name`, `id`).
- AJAX must send CSRF: read `meta[name="_csrf"]` and set header `X-CSRF-TOKEN`.

## Docs workflow

- Personal notes go in `docs/members/{name}/`; everything else in `../..` must reflect the current code. Don't create new top-level folders under `docs/`.
- Work logs: write a detail file in `docs/management/work_logs/YYYY-MM-DD-task.md` (or `docs/members/{name}/`), then add one line to `../../management/WORK_LOG.md`; on conflicts keep both sides' lines.

## Git Commit Guidelines
- NEVER add "Co-authored-by: Claude" or any AI attribution in commit messages.
- Always commit strictly using standard commit messages without AI co-author tags.