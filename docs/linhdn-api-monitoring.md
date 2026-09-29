# LinhDN API Monitoring: source and probe contract

## Current sources

The SRS API Monitoring table calls for Endpoint, Status, average response time, error rate, and recent errors. The source tree has no production AI HTTP client, email service, health collector, or API request log. The new SQL seed contains sample AI endpoint and SMTP host keys, but there is no verified health-check contract or production integration client for either target. `.env.example` mentions mail credentials. The existing `/test-db` route is a demo page that renders database details, so the monitor does not use it.

## Implemented target

| Target | Request | Authentication | Success rule | Result shown |
| --- | --- | --- | --- | --- |
| Internal application and SQL Server | Fixed loopback `GET /admin/api-monitoring/internal/health` | Current System Admin session; no credential leaves loopback | HTTP 2xx after a read-only `SELECT 1` | HTTP status, elapsed time, latest status, average elapsed time, error rate, recent error count |
| AI CV Screening | None: integration endpoint and health contract absent | N/A | N/A | Unconfigured; no fabricated status or metric |
| Email integration | None: provider endpoint or SMTP host/handshake contract absent | N/A | N/A | Unconfigured; no test email is sent |

Only System Admin can view the monitoring page or health endpoint and submit a probe. The probe form uses CSRF. The server builds the fixed loopback URL from its local port and servlet context path; it never accepts an Admin-entered URL. Redirects, 4xx/5xx, and connection failures count as failures. The response body and exception details are not rendered or logged.

Statistics are computed from the last 100 actual manual probes in this application process. Before the first probe, average time, error rate, and error count show `—`, and status is `Chưa kiểm tra`. History is intentionally in memory and resets when the process restarts; it is not an audit log or historical uptime service. No monitoring table, scheduled job, or library was added.

## Open integration contract

To probe AI or email safely, the team must provide the actual target and method. For AI, identify a side-effect-free health/readiness request and its authentication mechanism without putting credentials in source control. For email, identify the provider health endpoint or confirm a connection-only SMTP handshake with host/port and TLS mode. A probe must never submit a CV or send a test email merely to report health.

## Local schema validation incident, 2026-09-28

During a local `spring-boot:run` attempt, the ignored local `application.properties` still had `spring.jpa.hibernate.ddl-auto=update`. Passing `-Dspring.jpa.hibernate.ddl-auto=validate` to Maven did not pass that value into the forked application. Hibernate issued `ALTER TABLE` statements on 10 existing tables at 07:01:51. A read-only metadata check found 19 `nvarchar(max)` columns in those tables and confirmed table modification time. The observed record counts afterward were 17 User, 51 Candidate, and 100 Application. There is no pre-run schema snapshot, so the previous column definitions cannot be proved or safely restored from this check.

The ignored local `application.properties` was changed to `ddl-auto=validate` immediately afterward. A subsequent startup attempt using validate did not advance the tables' schema modification timestamp. That attempt could not serve HTTP in this environment because the JDK/Tomcat failed to establish a loopback selector connection; MVC, service, and database tests remain the available verification. Keep `ddl-auto=validate` in local runtime configuration before starting the app on another machine.
