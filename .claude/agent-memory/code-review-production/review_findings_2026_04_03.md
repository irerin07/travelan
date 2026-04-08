---
name: Production Review Findings (2026-04-03)
description: Key issues discovered during the comprehensive production readiness review of the full codebase
type: project
---

Key production risks found during the April 3 2026 full-codebase review:

**Why:** Recorded to inform future review sessions so the same issues are not re-reported as new.

**How to apply:** When reviewing new PRs in these areas, verify whether the issues below have been resolved before reporting them.

## Critical / Warning issues identified

1. **Hardcoded credentials in application.yaml** — DB username/password are `travelan/travelan` in plaintext. JWT secret has a fallback default value in the same file. Neither uses environment variable substitution for DB creds.

2. **Missing gap in Flyway migrations (V4 and V7 absent)** — The migration sequence jumps V1→V2→V3→V5→V6→V8→V9. V4 and V7 are missing. This is not fatal if they were intentionally dropped, but signals risky history rewriting.

3. **AdminUserController has no input validation on pagination params** — `page` and `size` are unconstrained query params. `page=0` causes a JPA off-by-one (-1 index), `size=100000` causes unbounded memory query.

4. **UserService.signup() has a TOCTOU race condition** — Three separate `existsBy*` checks are followed by a `save()`. Concurrent signups with the same email/phone/nickname can both pass the checks and hit a DB constraint. The `DataIntegrityViolationException` handler masks this, but the behavior is non-atomic.

5. **`withdraw()` truncates nickname to fixed prefix "탈퇴" + id — no length check** — The `nickname` column is VARCHAR(10). For id >= 10000000 the anonymized value "탈퇴10000000" exceeds 10 chars and will cause a DB truncation error.

6. **`withdraw()` truncates phone to "del_" + id — unique constraint risk** — `del_1`, `del_2` etc. are stored in the `phone` column which has a UNIQUE KEY. If a user withdraws, another account later takes the same id slot (unlikely with IDENTITY but worth noting), re-withdrawal fails.

7. **`UserHistoryRepository.findByUserOrderByCreatedAtDesc` is unbounded** — Returns `List<UserHistory>` with no pagination; for an active user with many events this loads everything into memory.

8. **`logout` endpoint is not protected by Spring Security** — It's under `/api/v1/auth/**` which is fully `permitAll()`. The logout logic re-validates the token manually, but a request with no Authorization header throws `AuthException` which is mapped to 401. This is functionally correct but the manual re-validation duplicates the filter logic and is fragile.

9. **JWT secret key entropy risk** — The key is derived from a UTF-8 string via `Keys.hmacShaKeyFor`. The default fallback `"travelan-dev-secret-key-must-be-changed-in-production!!"` is 51 chars (408 bits), which is fine. But there is no runtime validation that the production override is long enough.

10. **No `@Transactional` on `@Modifying` `revokeAllByUser`** — The custom JPQL `@Modifying` query in `RefreshTokenRepository` does not declare `@Transactional` on itself; it relies entirely on the caller. This is correct for current call sites but is fragile — a future direct call without a transaction context will fail silently or throw.

11. **`spring.profiles.active: dev` is committed to application.yaml** — The dev profile is the default even in production unless the environment overrides it. If `show-sql: true` is enabled in dev, SQL is logged in production if the profile is not switched.

12. **`UserInterestRegionRepository` is unused** — Declared but never injected anywhere.

13. **No `@NotNull`/`@Min` on `SignupRequest.interestRegions` list items** — The list size is capped at 5 but individual region strings have no length or blank validation. Empty strings ("") can be persisted as regions.

14. **`AdminUserController` does not validate `size` upper bound** — `size=Integer.MAX_VALUE` is accepted. Combined with a large user table this OOMs the JVM.
