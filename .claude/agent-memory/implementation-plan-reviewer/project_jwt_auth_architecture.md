---
name: Project JWT Auth Architecture
description: How authentication principal is wired — SecurityConfig catchall + JwtAuthenticationFilter sets Long userId as principal; controllers use @AuthenticationPrincipal Long userId
type: project
---

SecurityConfig has `.anyRequest().authenticated()` as the catch-all rule, with a custom `authenticationEntryPoint` that returns 401 JSON for unauthenticated requests. `JwtAuthenticationFilter` sets `userId` (Long) as the principal in `UsernamePasswordAuthenticationToken`. Controllers receive it via `@AuthenticationPrincipal Long userId`. No null check is required in controllers for protected endpoints because Spring Security rejects unauthenticated requests before the controller is ever reached.

**Why:** Understanding this eliminates a class of phantom null-check concerns. Any future plan proposing null checks for `@AuthenticationPrincipal` in protected controller methods should be flagged as unnecessary — the filter chain already handles it.

**How to apply:** When reviewing plans involving principal extraction or auth utilities, verify whether the endpoint is already protected via SecurityFilterChain before treating null-safety as a real requirement.
