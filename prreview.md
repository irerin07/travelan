# Code Review — `52bee74` (Refactoring 14단계 + Phase 4)

## Overview

31개 파일, +1835/-191. 두 가지 큰 작업이 하나의 커밋에 포함:
1. **refactortasks.md 14단계 리팩토링** — 탈퇴 익명화, UserHistory, 입력검증, Clock PSA, SecretKey 캐싱 등
2. **Phase 4** — Refresh Token Rotation (`POST /refresh`) + Logout (`POST /logout`)

---

## Positive

- **Clock PSA** 일관 적용 — `Clock.fixed()`로 시간 의존 코드 전부 테스트 가능
- **Replay attack detection** — revoked 토큰 재사용 시 해당 유저 전체 세션 무효화, jwt.md 설계 충실 반영
- **SecretKey 캐싱** — 매 호출마다 `Keys.hmacShaKeyFor()` 재생성 제거
- **SecurityConfig** — 하드코딩 JSON 상수 제거, `ObjectMapper` 활용
- **프로필 분리** — `show-sql`/`format_sql`을 `application-dev.yaml`로 이동
- **테스트 커버리지** — 새 기능마다 service + controller 양쪽 테스트 작성

---

## Issues & Suggestions

### 1. [Medium] Cookie 생성 로직 중복

`AuthController`에서 `login()`, `refresh()`, `logout()` 3곳에 거의 동일한 `ResponseCookie` 빌드 코드가 반복됨.

```java
// login(), refresh() 둘 다 동일:
ResponseCookie.from("refreshToken", tokens.getRefreshToken())
    .httpOnly(true).secure(true).sameSite("Strict")
    .maxAge(Duration.ofSeconds(jwtProperties.getRefreshTokenExpiry()))
    .path("/api/v1/auth").build();
```

**제안**: `private ResponseCookie buildRefreshCookie(String value, long maxAge)` 헬퍼 메서드로 추출.

### 2. [Medium] `refresh()` 순서 — revoked 체크 후 expired 체크

```java
// AuthService.refresh()
if (stored.isRevoked()) { ... }   // 먼저
if (stored.isExpired(clock)) { ... } // 나중
```

현재 `isUsable(Clock)` 메서드가 있지만 사용하지 않음. 의도적 설계(revoked와 expired를 구분해 다른 메시지 반환)이므로 문제는 아니지만, `isUsable()`이 dead code처럼 보일 수 있음. 주석이나 `isUsable()` 활용 여부를 명확히 하면 좋겠음.

### 3. [Low] `withdraw()` 이력 저장 — N+1 INSERT

```java
// UserService.withdraw() — 4번의 개별 save
userHistoryRepository.save(UserHistory.of(user, UserAction.WITHDRAWAL, "email", ...));
userHistoryRepository.save(UserHistory.of(user, UserAction.WITHDRAWAL, "phone", ...));
userHistoryRepository.save(UserHistory.of(user, UserAction.WITHDRAWAL, "nickname", ...));
userHistoryRepository.save(UserHistory.of(user, UserAction.WITHDRAWAL, "status", ...));
```

**제안**: `saveAll(List.of(...))` 한 번 호출로 배치 처리. 현재 트래픽에서는 성능 이슈 없지만, 컨벤션상 더 깔끔함.

### 4. [Low] `logout()` 엔드포인트 — `permitAll()` 경로에서 수동 토큰 검증

`/api/v1/auth/**`가 `permitAll()`이므로 `JwtAuthenticationFilter`가 SecurityContext를 설정하더라도 인증이 필수가 아님. 현재 컨트롤러에서 수동으로 `Authorization` 헤더를 파싱하고 있음.

```java
String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
if (bearer == null || !bearer.startsWith("Bearer ")) { throw ... }
String token = bearer.substring(7);
if (!jwtProvider.isValid(token)) { throw ... }
```

이 방식 자체는 동작하지만, 필터가 이미 SecurityContext를 설정했다면 `SecurityContextHolder.getContext().getAuthentication().getPrincipal()`에서 userId를 가져올 수도 있음. 다만 `permitAll()`이므로 인증 없이도 요청이 통과되어 수동 검증이 필요한 현재 방식이 맞음. 향후 `/logout`을 `authenticated()` 경로로 옮기면 코드를 단순화할 수 있음.

### 5. [Low] 커밋 크기

14단계 리팩토링 + Phase 4가 하나의 커밋에 들어있어 cherry-pick이나 revert가 어려움. 향후에는 논리 단위별로 분리 커밋을 권장.

### 6. [Info] `UserAction` enum — 미사용 값

`PROFILE_UPDATE`, `STATUS_CHANGE`가 선언되어 있지만 아직 사용처가 없음. 향후 프로필 수정 기능에서 사용될 것으로 보임.

---

## Security

- Refresh Token: HttpOnly + Secure + SameSite=Strict + Path 제한 — 적절
- Rotation + replay detection — 토큰 탈취 시나리오 대비 완료
- Access Token 만료 시간 15분(900초) — 적절한 blast radius

---

## Test Coverage

| 영역 | 테스트 수 | 평가 |
|------|----------|------|
| AuthService (login/refresh/logout) | 14개 | 충분 |
| AuthController (signup/login/check/refresh/logout) | 31개 | 충분 |
| GlobalExceptionHandler | 4개 | 충분 |
| RefreshToken entity | 별도 테스트 있음 | 충분 |
| UserService/UserTest | 기존 + 신규 | 충분 |

---

## 결론

전체적으로 설계 문서(jwt.md)에 충실한 구현이며, 보안 모범 사례를 잘 따르고 있음. Cookie 빌드 중복 추출과 `withdraw()` 이력 배치 저장이 개선 포인트이나, 기능적으로 문제 없음.
