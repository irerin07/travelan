# UserController.withdraw() 코드 리뷰

## Summary
`DELETE /api/v1/users/me` 흐름 리뷰. `UserController`는 refresh token 쿠키를 제거하고 `UserService.withdraw()`에 위임. 서비스는 `@Transactional` 안에서 PII 익명화, refresh token 폐기, 탈퇴 이력 기록을 원자적으로 처리. 전반적으로 컨벤션 준수와 테스트 커버리지는 양호하나, 운영에서 실제 터질 수 있는 버그 1건과 보안/의미론 이슈 다수 존재.

---

## ✅ Best Case
- 해피패스에서 `user.withdraw(clock)`이 PII 익명화를 도메인에 캡슐화하고, refresh token 폐기와 `WITHDRAWAL` 이력 기록까지 단일 트랜잭션으로 묶음.
- 컨트롤러는 `HttpOnly`, `Secure`, `SameSite=Strict`, `Max-Age=0`으로 쿠키 제거 — `AuthController.logout`과 동일 패턴.
- 컨벤션 준수
  - `Map<String, T>` 응답 없음 (`ResponseEntity<Void>`).
  - 웹 DTO를 서비스에 전달하지 않음 (스칼라 `Long userId` 전달, 적절).
  - `User` 엔티티: `@NoArgsConstructor(PROTECTED)` + `@Builder` + `of()`.
- 테스트: 컨트롤러 4시나리오(204/쿠키/401/404) + 서비스 5시나리오(상태/PII/토큰/이력/중복탈퇴) 모두 커버.

---

## ⚠️ Worst Case
- 서비스 성공 후 응답이 유실되면 DB는 영구 탈퇴 상태인데 브라우저 쿠키와 access token은 잔존 → 사용자 측 상태 불일치.
- `userService.withdraw(null)` 호출 시 `findById(null)` → `IllegalArgumentException` → 500 응답.

---

## 🔍 Production Risks

### 🔴 Critical
없음 (트랜잭션 경계 정확).

### 🟡 Warning

**1. 닉네임 컬럼 오버플로우 — `User.java:113`**
```java
this.nickname = "탈퇴" + (this.id % 99_999_999L);
```
- `nickname` 컬럼 `length = 10` (`User.java:52`).
- `id`가 8자리 나머지를 만들면 "탈퇴99999998" = 11자 → DB 제약 위반.
- **Fix**: `% 9_999_999L` (최대 "탈퇴9999999" = 9자) 또는 컬럼 길이 확장.

**2. `userId` null 미검증 — `UserService.withdraw()` 진입부**
- 시큐리티 필터 오설정/정책 변경으로 principal이 null이면 `findById(null)` → 500.
- **Fix**: 서비스 레이어 진입부에 `Objects.requireNonNull(userId, ...)` 또는 `AuthException` throw로 방어 위치를 한 곳에 통일 (컨트롤러마다 분산 방어 X).

**3. 탈퇴 시 재인증 없음**
- 유효 access token만으로 영구 삭제 가능. XSS/탈취 시 복구 불가.
- **Fix**: `WithdrawRequest { @NotBlank currentPassword }` + `passwordEncoder.matches()`.

**4. access token 무효화 부재 — `UserController.java:30-38`**
- refresh만 DB에서 폐기. access token은 TTL까지 유효 → 탈퇴 후에도 API 호출 가능.
- **Fix**: 정책적 결정 사항으로 spec에 명시하거나 denylist 도입.

### 🔵 Improvement

**5. `@Validated` 누락 — `UserController.java:16`**
- `AuthController`는 클래스 레벨 `@Validated` 적용. `UserController`는 미적용 → 향후 제약 어노테이션이 무효화됨.

**6. Cookie 빌드 로직 중복 — 4곳 (`AuthController` login/refresh/logout + `UserController` withdraw)**
- `httpOnly/secure/sameSite/path` 옵션이 4곳에 복붙. 한 줄(예: `secure(true)`) 누락 시 보안 약화.
- **Fix**: `auth.support.AuthCookieFactory` 추출 — `refreshTokenCookie(value, ttl)` / `expiredRefreshTokenCookie()` 두 메서드로 통합.

**7. Cookie path 하드코딩 — `UserController.java:35`, `AuthController.java:69, 95, 117`**
- `"/api/v1/auth"` 문자열이 4곳에 중복. path 변경 시 한 곳 누락하면 만료 Cookie가 다른 path로 세팅되어 기존 쿠키 덮어쓰기 실패 → 클라이언트에 stale token 잔존.
- **Fix**: `JwtProperties` 또는 `AuthCookies.REFRESH_TOKEN_PATH` 상수로 추출 (W6와 함께 해결).

**8. `HttpServletResponse` 직접 조작 — `UserController.java:38`**
- Spring 권장 방식은 `ResponseEntity.noContent().header(SET_COOKIE, cookie.toString()).build()`. `HttpServletResponse` 파라미터 자체를 제거 가능.
- **Fix**: `AuthController` login/refresh/logout과 일관성을 위해 동시에 전환할 것 (UserController만 바꾸면 컨벤션 갈라짐).

**9. Phase 3 통합 시나리오 미자동화 — `withdraw-tasks.md:54-57`**
- 토큰 재사용 후 401, 재가입 시 201, 이력 기록 검증이 수동 체크박스로 남아 있음 → `@SpringBootTest`로 승격 권장.

---

## 💡 Recommendations (우선순위)

1. **[Critical 수정]** `User.java:113`의 `% 99_999_999L` → `% 9_999_999L`. 운영에서 실제 터질 버그.
2. **[null 가드]** `UserService.withdraw()` 진입부에 `userId` null 방어 (서비스 레이어에 단일 지점으로).
3. **[보안 강화]** 탈퇴 요청에 비밀번호 재확인 추가.
4. **[리팩터링]** `AuthCookieFactory` 추출 — Cookie 빌드 로직 + path 상수를 한 곳에서 관리. `AuthController` login/refresh/logout + `UserController` withdraw 4곳 동시 적용.
5. **[리팩터링]** `HttpServletResponse` 직접 조작 제거 → `ResponseEntity.header(SET_COOKIE, ...)` 빌더 사용. 4개 엔드포인트 일괄 전환.
6. **[테스트 자동화]** `withdraw-tasks.md` Phase 3 수동 시나리오를 통합 테스트로 승격.

---

## 📌 의도된 설계 (수정 대상 아님)

- **이미 탈퇴한 사용자에 404 반환** (`UserService.java:77`): 409/410을 사용하면 해당 이메일/전화번호로 가입했던 이력이 존재한다는 정보가 노출되어 계정 열거(account enumeration) 공격의 단서가 됨. "존재하지 않음"과 "탈퇴됨"을 외부 응답에서 구분하지 않는 것이 의도된 보안 정책.
