---
name: Travelan Project Architecture
description: Spring Boot travel app — auth/JWT, user management, Flyway migrations, layered DTO separation
type: project
---

Spring Boot travel application with layered architecture:

- Auth: JWT access token (15min) + refresh token (30d) stored in DB (`refresh_token` table), refresh token sent via HttpOnly cookie
- User: signup, withdrawal (email anonymized), interest regions, UserHistory audit log
- DB: MySQL, Flyway migrations (V1..V9), JPA + Spring Data
- Security: stateless, `JwtAuthenticationFilter` (OncePerRequestFilter), BCrypt(10)
- DTO pattern: web-layer DTOs in `auth.dto`, service-layer Commands in `user.dto` / `auth.dto`, conversion via static `from()` factory
- Entities: `@NoArgsConstructor(PROTECTED)` + `@Builder` + static `of()` factory
- Response envelope: `ApiResponse<T>` with builder for paginated + meta responses

**Why:** Confirms project follows CLAUDE.md conventions carefully.
**How to apply:** Check layering, DTO factory methods, no Map<String,T> in responses, no first-class collections on JPA entities.
