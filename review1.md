# Production Code Review #1

**대상**: 토큰 갱신/로그아웃 (Phase 4) + UserHistory 이벤트 팩토리 리팩토링
**일시**: 2026-04-03

---

## Best Case

- Refresh token rotation이 올바르게 구현됨 — 재사용 감지 시 `revokeAllByUser` 호출하는 방어적 패턴
- `HttpOnly; Secure; SameSite=Strict` 쿠키 설정 적절
- 로그인 시 "이메일 없음/비밀번호 틀림" 동일 에러 메시지 — enumeration 방지
- `Clock` 주입으로 시간 의존 로직 테스트 용이
- CLAUDE.md 컨벤션 대부분 준수

---

## Worst Case

- **JWT 시크릿 미설정 시** 공개된 하드코딩 키로 서명 → 토큰 위조 가능
- **로그인 고동시성 상황에서** `revokeAllByUser` + `save` 사이 크래시 시 유효한 토큰 없이 재로그인 필요
- **`logout` 컨트롤러**에서 비정상 subject로 `NumberFormatException` → 500 반환 (401이어야 함)
- **`UserHistory` 테이블에 FK `ON DELETE` 미설정** — `users` 행 직접 삭제 시 제약 조건 위반

---

## Production Risks

### Critical

#### 1. JWT 시크릿 하드코딩 폴백

- **위치**: `src/main/resources/application.yaml`
- **문제**: `${JWT_SECRET:travelan-dev-secret-key-must-be-changed-in-production!!}` — `JWT_SECRET` 미설정 시 공개된 키로 서명되어 토큰 위조 가능
- **수정**: 기본값 제거 (`secret: ${JWT_SECRET}`) + `JwtProvider`에 `@PostConstruct` 시작 검증 추가

#### 2. `logout` 컨트롤러 `NumberFormatException` → 500

- **위치**: `AuthController.java`
- **문제**: `jwtProvider.getUserId(token)`에서 subject가 비정상이면 `NumberFormatException` 발생, `GlobalExceptionHandler`에 핸들러 없어 500 반환
- **수정**: `JwtProvider.getUserId` 내부에서 `NumberFormatException` 캐치하거나, `GlobalExceptionHandler`에 401 매핑 추가

---

### Warning

#### 3. `Clock.systemDefaultZone()` 사용

- **위치**: `JpaConfig.java`
- **문제**: JVM 타임존에 의존 → DB 타임존과 불일치 시 토큰 만료 비교 오류
- **수정**: `Clock.systemUTC()` 사용 권장

#### 4. `AuthService.logout`에서 불필요한 `findById` DB 조회

- **위치**: `AuthService.java`
- **문제**: JWT에서 userId를 이미 검증했으므로 `findById` SELECT는 순수 오버헤드
- **수정**: `revokeAllByUserId(Long userId)` 메서드 추가, `WHERE r.user.id = :userId` 사용

#### 5. Signup 경쟁 조건 (check-then-act)

- **위치**: `UserService.java`
- **문제**: 3개 `exists` 쿼리 후 INSERT — 동시 요청 시 모두 통과 가능. DB unique 제약으로 잡히지만 에러 메시지가 제네릭함
- **수정**: `DataIntegrityViolationException` 핸들러에서 제약 조건명 파싱하여 구체적 필드 에러 반환

#### 6. `SecurityConfig`에서 `/api/v1/auth/**` 전체 permitAll

- **위치**: `SecurityConfig.java`
- **문제**: `/logout`도 인증 없이 접근 가능, 수동 토큰 검증에 의존
- **수정**: 공개 엔드포인트만 명시적 나열, logout은 `SecurityContextHolder`에서 userId 읽도록 변경

#### 7. `@Modifying`에 `clearAutomatically = true` 누락

- **위치**: `RefreshTokenRepository.java`
- **문제**: 벌크 UPDATE 후 1차 캐시와 DB 정합성 불일치 → 토큰 재사용 감지 실패 가능
- **수정**: `@Modifying(clearAutomatically = true)` 적용

#### 8. `UserHistoryRepository` 무제한 조회

- **위치**: `UserHistoryRepository.java`
- **문제**: `findByUserOrderByCreatedAtDesc` — `LIMIT` 없이 전체 이력 로딩 → 메모리 이슈 가능
- **수정**: `Page<UserHistory>` 반환 + `Pageable` 파라미터 추가

---

### Improvement

#### 9. `User.withdraw()` nickname 길이 초과 위험

- **위치**: `User.java`
- **문제**: `"탈퇴" + this.id` — `VARCHAR(10)` 컬럼에서 9자리 ID 이상 시 `DataTruncation` 예외
- **수정**: `"탈퇴" + (this.id % 99_999_999L)` 또는 랜덤 서픽스 사용

#### 10. `UserService`에서 `User.builder()` 직접 호출

- **위치**: `UserService.java`
- **문제**: CLAUDE.md 규칙상 `User.of(...)` 정적 팩토리 메서드 사용 필요
- **수정**: `User`에 `public static User of(...)` 추가 후 `UserService`에서 호출

#### 11. `HandlerMethodValidationException` 필드명이 항상 "parameter"

- **위치**: `GlobalExceptionHandler.java`
- **문제**: 모든 쿼리 파라미터 검증 에러가 `field: "parameter"`로 반환 — 클라이언트가 어떤 파라미터인지 구분 불가
- **수정**: `ConstraintViolation`에서 실제 파라미터명 추출

---

## 우선순위 권장 조치

| 순위 | 항목 | 심각도 |
|------|------|--------|
| 1 | JWT 시크릿 기본값 제거 + 시작 검증 | Critical |
| 2 | `@Modifying(clearAutomatically = true)` 적용 | Warning |
| 3 | `SecurityConfig` permitAll 범위 축소 + logout 인증 | Warning |
| 4 | `revokeAllByUserId(Long)` 추가 (불필요 SELECT 제거) | Warning |
| 5 | `Clock.systemUTC()` 전환 | Warning |
| 6 | `UserHistoryRepository` 페이징 적용 | Warning |
| 7 | `User.of(...)` 정적 팩토리 메서드 추가 | Improvement |
| 8 | `User.withdraw()` nickname 길이 안전 처리 | Improvement |
