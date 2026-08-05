[# Task 4: Spring Boot security
**Date:** 2026-08-04
**Status:** In progress

## Description
Implemented the initial Spring Security feature from `.claude/specs/2026-08-04-add-spring-boot-security.md` on `feature/add-spring-security`.

## Solution
- Added Spring Security, OAuth2 JOSE, OAuth2 resource-server, and Spring Security test dependencies.
- Added SQLite/Liquibase security migration `TABLE.SECURITY.sql` with `users`, `roles`, and `user_roles` tables, indexes, foreign keys, rollback statements, and seeded `USER`, `MODERATOR`, and `ADMIN` roles.
- Added `RegisterRequest`, `LoginRequest`, `AuthResponse`, `UserAccount`, `UserRepository`, `CustomUserDetailsService`, `AuthService`, `AuthServiceImpl`, and `JwtTokenService`.
- Added `AuthController` endpoints: `POST /api/v1/auth/register` and `POST /api/v1/auth/login`.
- Added `SecurityProperties` and `SecurityConfig` with BCrypt, stateless JWT, JWT issuer/audience validation, method security, public auth/health/static routes, protected API/web routes, and security headers.
- Added `ApiExceptionHandler` for generic API errors and validation failures.
- Added method-level role restrictions to `MessageController`: sending requires `USER`, `MODERATOR`, or `ADMIN`; consumer management requires `MODERATOR` or `ADMIN`.
- Updated `.claude/CLAUDE.md` with security architecture, role mapping, public/protected routes, configuration, and secret-handling rules.

## Decisions Made
- Use short-lived stateless bearer JWTs and no refresh-token subsystem in this iteration.
- Public registration assigns only `USER`.
- JWT keys are generated at startup when external key material is absent; this is local-development-only because restart invalidates tokens. Production must provide stable external signing keys and HTTPS.
- CSRF is disabled for the stateless bearer-token model.
- Consumer ownership migration and browser login/token integration remain follow-up work; current role restrictions are in place, but full per-user ownership isolation is not yet implemented.
- Dynamic Kafka broker allowlisting remains specified but not yet implemented in `MessageController`/service.

## Verification
- `mvn -DskipTests compile` passed.
- `mvn test` passed: 42 tests, 0 failures, 0 errors.

## Next Steps
- Add dedicated security tests for registration, login, JWT claims, anonymous `401`, role-based `403`, invalid/expired tokens, and security headers.
- Implement stable external RSA key loading and fail-fast production validation.
- Implement authenticated browser login and bearer-token injection for the Thymeleaf UI.
- Add `owner_id` migration and owner-scoped consumer repository/service operations.
- Implement broker allowlist/SSRF protection and rate limiting.
- Update README and operational configuration documentation.
