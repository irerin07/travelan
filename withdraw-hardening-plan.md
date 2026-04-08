# Withdraw Flow Hardening Plan

## Context

`UserController.withdraw()` / `UserService.withdraw()` 리뷰 결과 다음 이슈를 반영한다:

1. 서비스 레이어의 `userId == null` 방어 코드가 `auth-principal-utility-review.md`의 "현행 유지(필터 체인이 null을 보장)" 결론과 일관되지 않음 → 제거.
2. 탈퇴한 사용자의 **access token**이 만료까지 유효함 → `JwtAuthenticationFilter`에서 사용자 상태를 확인해 ACTIVE가 아니면 인증을 설정하지 않음. (이 변경은 동시 탈퇴 요청에서 두 번째 요청이 인증 단계에서 차단되는 부수 효과도 있어, 별도 낙관적 락 없이도 중복 WITHDRAWAL 이력 위험을 크게 줄임.)
3. 미인증 401 통합 테스트(`withdraw_미인증_401_반환`)는 이미 존재 — 새로 추가할 필요 없음. 대신 `@Validated` 불필요 어노테이션을 제거한다.

낙관적 락(`@Version`) 추가는 사용자 지시에 따라 본 플랜에서 제외.

## 변경 대상 파일

### 1. 서비스 null 체크 제거
- `src/main/java/com/irerin/travelan/user/service/UserService.java:74-77`
  - `if (userId == null) throw new AuthException(...)` 블록 삭제
  - `import com.irerin.travelan.common.exception.AuthException;` — 서비스 내 다른 사용처가 없다면 함께 제거
- `src/test/java/com/irerin/travelan/user/service/UserServiceTest.java:227-233`
  - `withdraw_userId가_null이면_AuthException()` 테스트 삭제

### 2. 탈퇴 사용자 access token 무효화 — JwtAuthenticationFilter 상태 검증

- `src/main/java/com/irerin/travelan/auth/jwt/JwtAuthenticationFilter.java`
  - `UserRepository` 의존성 주입
  - 토큰 유효성 통과 후 `userRepository.findById(userId)` 로 사용자 로드
  - 사용자가 없거나 `user.getStatus() != UserStatus.ACTIVE` 이면 `SecurityContextHolder`에 인증을 세팅하지 **않음** → `.anyRequest().authenticated()` 필터가 401로 거부
  - 매 요청당 추가 쿼리 1회 비용은 수용 (캐시는 후속 과제)
- `src/main/java/com/irerin/travelan/common/config/SecurityConfig.java`
  - `JwtAuthenticationFilter` 빈 생성 부분에 `UserRepository` 인자 추가
- 테스트 (TDD)
  - `src/test/java/com/irerin/travelan/auth/jwt/JwtAuthenticationFilterTest.java` 보강
    - 유효 토큰 + ACTIVE 사용자 → SecurityContext에 인증 설정됨 (기존 테스트 업데이트)
    - 유효 토큰 + WITHDRAWN 사용자 → 인증 설정 안 됨 (신규)
    - 유효 토큰 + DB에 사용자 없음 → 인증 설정 안 됨 (신규)
    - 무효 토큰 → 기존 동작 유지
  - 선택: `UserControllerTest`에 "탈퇴된 사용자 JWT로 `DELETE /api/v1/users/me` → 401" 케이스 추가

### 3. `@Validated` 제거
- `src/main/java/com/irerin/travelan/user/controller/UserController.java:20`
  - 클래스 레벨 `@Validated` 어노테이션 및 import 제거 (현재 endpoint가 `withdraw()` 하나뿐이며 검증 대상 파라미터 없음)

## 재사용 가능한 기존 컴포넌트
- `AuthCookieFactory.expiredRefreshTokenCookie()` — 변경 없음
- `UserHistory.ofEvent(user, UserAction.WITHDRAWAL)` — 변경 없음
- `UserStatus.ACTIVE` — `AuthService.login()`이 이미 같은 패턴 사용 중
- `GlobalExceptionHandler` — `AuthException` → 401 매핑 그대로 사용

## 검증 방법

1. **단위 테스트**
   - `./gradlew test --tests JwtAuthenticationFilterTest` — 신규/수정 테스트 green
   - `./gradlew test --tests UserServiceTest` — null 체크 테스트 삭제 후 전부 green
   - `./gradlew test --tests UserControllerTest` — 기존 `withdraw_미인증_401_반환` 포함 green
2. **전체 회귀**
   - `./gradlew test`
3. **수동 시나리오**
   - 로그인 → access token 획득 → `DELETE /api/v1/users/me` → 204
   - 같은 access token으로 보호된 API 재호출 → 401 (access token 무효화 검증)
   - 미인증 `DELETE /api/v1/users/me` → 401

## 순서 (TDD)
1. `JwtAuthenticationFilterTest` 상태 검증 테스트 작성 (RED) → 필터 + SecurityConfig 수정 (GREEN) → 정리 (REFACTOR)
2. `UserService.withdraw()` null 체크 제거 + 해당 테스트 삭제
3. `UserController` `@Validated` 제거
4. `./gradlew test` 전체 회귀 확인
