---
name: Withdraw Feature Review (2026-04-07)
description: Production readiness review of DELETE /api/v1/users/me withdraw flow — controller, service, exception handling
type: project
---

Key findings from the April 7 2026 focused review of the withdraw flow.

**Why:** Recorded so future reviews don't re-report known issues and can track which prior findings have been resolved.

**How to apply:** When reviewing future changes to UserController, UserService.withdraw, or cookie/logout patterns, check these first.

## Issues confirmed resolved (from 2026-04-03 review)

- Nickname overflow risk (issue #5) is still present — `User.withdraw()` line 113 does `"탈퇴" + (this.id % 99_999_999L)`. Modulo caps the number at 99,999,997 (8 digits), making max value "탈퇴99999997" = 11 chars, which still exceeds the VARCHAR(10) column limit. Not fixed.

## New issues found in this review

1. **`withdraw()` cookie clear is best-effort only** — Cookie is cleared after `userService.withdraw()` commits. If the response is lost (connection drop, proxy timeout), the DB is withdrawn but the cookie persists in the client. Not blocking, but worth noting.

2. **`userId` can be null if `@AuthenticationPrincipal` resolution fails** — No null guard in the controller. If the security filter passes but the principal resolver returns null, `userService.withdraw(null)` is called, which hits a DB query with a null key.

3. **`WITHDRAWN` status reuses `NotFoundException` error code** — Semantically misleading; a `CONFLICT` or `GONE` is more accurate but current GlobalExceptionHandler maps NotFoundException → 404.

4. **No password confirmation step for withdrawal** — High-value destructive action with no re-authentication. Industry standard is to require current password or step-up auth before account deletion.

5. **Cookie path /api/v1/auth does not cover the withdraw endpoint** — Cookie is issued at path `/api/v1/auth` but the DELETE is at `/api/v1/users/me`. The browser will not include the cookie on requests to `/api/v1/users/**`, which is correct behavior, but it also means the same client-set cookie for logout and withdraw has inconsistent scoping. Not a bug, but a design inconsistency.
