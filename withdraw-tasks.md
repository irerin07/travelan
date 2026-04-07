# 회원 탈퇴 - 구현 태스크

---

## Phase 1 — 서비스 계층 보강

### 1-1. 중복 탈퇴 방지
- [ ] `UserService.withdraw(User)`에서 `user.getStatus() == WITHDRAWN`이면 예외 발생
- [ ] `NotFoundException` 사용 (신규 작성 또는 기존 재사용) → 404 매핑

### 1-2. 세션 무효화
- [ ] `UserService.withdraw()` 내부에서 `RefreshTokenRepository.revokeAllByUserId(user.getId())` 호출
- [ ] 또는 `AuthService.logout(userId)` 의존성 주입 후 위임 (택1)

### 1-3. 서비스 단위 테스트 (TDD)
- [ ] `withdraw_정상_탈퇴_status_WITHDRAWN_변경`
- [ ] `withdraw_정상_탈퇴_email_phone_nickname_익명화`
- [ ] `withdraw_정상_탈퇴_RefreshToken_revoke_호출`
- [ ] `withdraw_정상_탈퇴_UserHistory_WITHDRAWAL_저장`
- [ ] `withdraw_이미_탈퇴된_회원_예외_발생`

---

## Phase 2 — 컨트롤러 & 보안

### 2-1. UserController 작성
- [ ] `user.controller.UserController` 클래스 생성
- [ ] `DELETE /api/v1/users/me` 엔드포인트 추가
- [ ] `SecurityContextHolder`에서 `userId` 추출
- [ ] `userRepository.findById(userId)` 조회
- [ ] `userService.withdraw(user)` 호출
- [ ] Refresh Token Cookie `Max-Age=0` 만료 헤더 추가 (`refreshToken=; Path=/api/v1/auth; HttpOnly; Secure; SameSite=Strict`)
- [ ] `ResponseEntity.noContent().build()` 반환

### 2-2. SecurityConfig
- [ ] `/api/v1/users/me` DELETE에 `authenticated()` 적용 (기존 anyRequest 정책 확인 후 필요 시 명시)

### 2-3. 예외 매핑
- [ ] `GlobalExceptionHandler`에 `NotFoundException` → 404 매핑 (이미 존재하면 생략)

### 2-4. 컨트롤러 테스트 (`@WebMvcTest`)
- [ ] `withdraw_인증된_사용자_204_반환`
- [ ] `withdraw_응답에_RefreshToken_Cookie_만료_헤더_포함`
- [ ] `withdraw_미인증_401_반환`
- [ ] `withdraw_이미_탈퇴된_회원_404_반환`

---

## Phase 3 — 통합 검증

### 3-1. 회귀
- [ ] `./gradlew test` 전체 GREEN

### 3-2. 시나리오 검증 (선택)
- [ ] 로그인 → 탈퇴 → 동일 토큰 재사용 시 401 확인
- [ ] 탈퇴 → 동일 이메일 재가입 201 확인
- [ ] UserHistory 테이블에 `WITHDRAWAL` 1건 기록 확인

---

## 완료 기준
- 모든 체크박스 `[x]`
- spec.md의 처리 흐름 1~8 단계 모두 동작
- 전체 테스트 GREEN
