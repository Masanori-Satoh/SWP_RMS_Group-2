# Authentication diagram edge evidence

Java paths below are relative to `src/main/java/com/group2/rms/` unless stated otherwise.

| ID | Directed relation / meaning | Source evidence |
|---|---|---|
| E01 | auth.controller → auth.service (injects service); auth.controller → auth.service (injects) | `auth/controller/RegistrationController.java:3,20-23; auth/controller/PasswordRecoveryController.java:4,18-21` |
| E02 | auth.controller → templates/auth/ (view names) | `auth/controller/AuthController.java:9-11; auth/controller/RegistrationController.java:26-72; auth/controller/PasswordRecoveryController.java:25-125` |
| E03 | auth.service → user.service (candidate registration); auth.service → user.service (checks / creates); auth.service → user.service (registration methods) | `auth/service/RegistrationVerificationService.java:7,30,36-40,50,104-108` |
| E04 | auth.service → user.entity (uses User type); auth.service → user.repository (password reset persistence); auth.service → user.repository (queries / updates) | `auth/service/PasswordResetService.java:6-7,31-44,58,85,107,115-116` |
| E05 | user.service → user.repository (User / Role access); user.service → user.repository (account / role access) | `user/service/AccountManagementService.java:27-28,94-130` |
| E06 | user.service → candidate.repository (creates candidate profile); user.service → candidate.repository (persists profile) | `user/service/AccountManagementService.java:30,128-130,224-225` |
| E07 | user.repository → user.entity (entity type) | `user/repository/UserRepository.java:3,24; user/repository/RoleRepository.java:3,14` |
| E08 | candidate.repository → candidate.entity (entity type) | `candidate/repository/CandidateRepository.java:3,16` |
| E09 | candidate.entity → user.entity (User account type); candidate.entity → user.entity (account type) | `candidate/entity/Candidate.java:4,29-31` |
| E10 | core.config → core.security (injects / constructs); core.config → core.security (wires) | `core/config/SecurityConfig.java:4-6,50-56,174-175` |
| E11 | core.security → user.entity (uses domain type); core.security → user.repository (injects / queries); core.security → user.repository (reloads account) | `core/security/DatabaseUserDetailsService.java:3-4,17-20,32-33; core/security/AccountSessionGuardFilter.java:3-4,19-23,34` |
| E12 | core.config → user.repository (injects repository) | `core/config/SecurityConfig.java:3,50-56,174` |
| E13 | core.config → Spring Security (configures filter chain) | `core/config/SecurityConfig.java:11-25,45-46,162-175` |
| E14 | core.security → Spring Security (implements / extends) | `core/security/DatabaseUserDetailsService.java:5-7,15,51-55; core/security/AccountSessionGuardFilter.java:9-13,17,30-52` |
| E15 | auth.service → Spring Security (PasswordEncoder) | `auth/service/RegistrationVerificationService.java:36-40; auth/service/PasswordRecoveryService.java:39-49; auth/service/PasswordResetService.java:11,31-44` |
| E16 | auth.service → Jakarta Servlet (session state API) | `auth/service/RegistrationVerificationService.java:155-173; auth/service/PasswordRecoveryService.java:91,136,161,187-210` |
| E17 | auth.service → Spring Mail (mail API); auth.service → Spring Mail (uses JavaMailSender) | `auth/service/RegistrationEmailSender.java:12-13,22-25,33-45; auth/service/PasswordResetEmailSender.java:7-8,28-33,47-62` |
| E18 | Spring Mail → SMTP Server («configured SMTP integration») | `Spring Mail adapter + src/main/resources/application.properties:50-55; configuration evidence, not a live SMTP test` |
| E19 | candidate.repository → Spring Data JPA / JDBC (JpaRepository); user.repository → Spring Data JPA / JDBC (JpaRepository) | `user/repository/UserRepository.java:24; user/repository/RoleRepository.java:14; candidate/repository/CandidateRepository.java:16` |
| E20 | Spring Data JPA / JDBC → SQL Server Database («configured integration») | `pom.xml:49,62; src/main/resources/application.properties:15; configured JPA/JDBC integration, not direct repository SQL calls` |
| E21 | auth.controller → auth.dto (form contracts) | `auth/controller/RegistrationController.java:4-5; auth/controller/PasswordRecoveryController.java:3,5-6` |
| E22 | auth.service → auth.dto (uses records); auth.service → auth.exception (throws) | `auth/service/RegistrationVerificationService.java:3-10; auth/service/PasswordRecoveryService.java:14-17; auth/service/PasswordResetService.java:2-5` |
| E23 | auth.dto → core.validation (@PasswordMatches) | `auth/dto/RegisterAccountRequest.java:8,12; auth/dto/ResetPasswordRequest.java:3,11` |
| E24 | auth.exception → core.exception (base type) | `auth/exception/RegistrationFlowException.java:3,5; auth/exception/PasswordRecoveryFlowException.java:3,5; auth/exception/PasswordRecoveryUnavailableException.java:3,5; auth/exception/PasswordResetDeliveryException.java:3,5` |
| E25 | core.exception → auth.exception (handler imports) | `core/exception/GlobalExceptionHandler.java:9,11-13,46,144-148,243` |
| E26 | core.exception → auth.dto (rebuilds form DTOs) | `core/exception/GlobalExceptionHandler.java:5-10,51-60,151,254-263` |
| E27 | user.service → user.exception (identifier conflict) | `user/service/AccountManagementService.java:200-213` |
| E28 | user.exception → core.exception (base type) | `user/exception/AccountFieldException.java:3,6` |
| E29 | candidate.entity → core.base (base type); user.entity → core.base (base type) | `user/entity/User.java:3,20; candidate/entity/Candidate.java:3,22` |
| E30 | auth → Spring Security (Authentication API) | `auth/NavigationAdvice.java:3,7-9` |
| E31 | user.service → candidate.entity (constructs); user.service → user.entity (constructs) | `user/service/AccountManagementService.java:107,117-126,224-225` |
| E32 | auth.service → templates/auth/ (loads email resources) | `auth/service/RegistrationEmailSender.java:51-54; auth/service/PasswordResetEmailSender.java:68-71` |
| E33 | auth.service → user.exception (catches / translates) | `auth/service/RegistrationVerificationService.java:8,51-53,109-112` |
