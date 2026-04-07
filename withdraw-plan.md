# 회원 탈퇴 - 개발 계획

## 기술 스택
기존 프로젝트와 동일 (Java 21 / Spring Boot 4.0.5 / Spring Security / JPA / JWT / MySQL).
신규 의존성 없음.

## 기존 자산 (재사용)
- `User.withdraw(Clock)` — 익명화 로직 (User.java:109)
- `UserStatus.WITHDRAWN` — 상태 enum
- `UserService.withdraw(User)` — 탈퇴 + UserHistory 기록 (UserService.java:67)
- `UserHistory.ofEvent(user, UserAction.WITHDRAWAL)` — 이벤트 이력
- `RefreshTokenRepository.revokeAllByUserId(Long)` — 세션 무효화
- `V8__add_withdrawal_columns_to_users.sql` — 컬럼 이미 존재
- `JwtAuthenticationFilter` — Access Token에서 `userId` 주입 (SecurityContextHolder principal)
- `AuthController.logout()` — Cookie 만료 처리 패턴 참고

## 미구현 (이번 작업 범위)
- `DELETE /api/v1/users/me` 컨트롤러 엔드포인트
- 탈퇴 시 RefreshToken revoke 호출 (현재 `UserService.withdraw()`는 토큰 revoke 안 함)
- 이미 WITHDRAWN인 회원에 대한 404 처리
- Refresh Token Cookie 만료 응답
- SecurityConfig에 `/api/v1/users/me` 인증 요구 등록
- 컨트롤러/서비스 단위 테스트

---

## Phase 1 — 서비스 계층 보강

### 1-1. UserService.withdraw() 확장
- 이미 `WITHDRAWN` 상태면 `NotFoundException`(신규 또는 기존) → 404
- 내부에서 `refreshTokenRepository.revokeAllByUserId(user.getId())` 호출
- 또는 `AuthService.logout(userId)` 재사용 검토

### 1-2. 단위 테스트 (TDD RED→GREEN)
- 정상 탈퇴 시 status=WITHDRAWN, 익명화 확인
- 정상 탈퇴 시 RefreshToken revoke 호출 verify
- 정상 탈퇴 시 UserHistory `WITHDRAWAL` 1건 저장 verify
- 이미 탈퇴된 회원 → 예외 발생

### 완료 기준
- `UserServiceTest`의 신규 케이스 GREEN

---

## Phase 2 — 컨트롤러 & 보안

### 2-1. UserController 신규 작성
- `user.controller.UserController`
- `DELETE /api/v1/users/me` 엔드포인트
- `SecurityContextHolder`에서 `userId` 추출 (AuthController.logout 패턴 동일)
- `userRepository.findById(userId)` → 없으면 401 (이론상 토큰 유효 시 발생 안 함)
- `userService.withdraw(user)` 호출
- Refresh Token Cookie `Max-Age=0` 만료 헤더 추가
- `204 No Content` 반환

### 2-2. SecurityConfig
- `/api/v1/users/me`는 인증 필요 (DELETE) → `requestMatchers(HttpMethod.DELETE, "/api/v1/users/me").authenticated()`
- 기존 `anyRequest().authenticated()` 정책으로 자동 적용된다면 별도 등록 불필요 — 확인 필요

### 2-3. 컨트롤러 테스트 (`@WebMvcTest`)
- 정상 탈퇴 → 204 + Set-Cookie 만료 검증
- 미인증 → 401
- 이미 탈퇴된 회원 → 404

### 완료 기준
- `UserControllerTest` 전체 GREEN
- `./gradlew test` 전체 GREEN

---

## Phase 3 — 통합 검증

### 3-1. 수동 검증 (옵션)
- 로그인 → 토큰 발급 → DELETE /api/v1/users/me → 204
- 동일 토큰으로 재요청 → 401 (status != ACTIVE)
- 동일 이메일로 재가입 → 201 (익명화 덕분에 unique 제약 통과)

### 완료 기준
- 회귀 테스트(`./gradlew test`) 전체 GREEN
- spec 4.2 "탈퇴 후 동작" 시나리오 확인

---

## 개발 순서 요약
```
Phase 1  서비스 보강     withdraw 확장 + RefreshToken revoke + 중복 탈퇴 방지
   ↓
Phase 2  컨트롤러         DELETE /api/v1/users/me + Cookie 만료 + Security
   ↓
Phase 3  통합 검증       전체 회귀 + 시나리오 확인
```
